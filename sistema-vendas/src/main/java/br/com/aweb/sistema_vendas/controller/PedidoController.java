package br.com.aweb.sistema_vendas.controller;

import br.com.aweb.sistema_vendas.model.Cliente;
import br.com.aweb.sistema_vendas.model.FormaPagamento;
import br.com.aweb.sistema_vendas.model.Pagamento;
import br.com.aweb.sistema_vendas.model.Pedido;
import br.com.aweb.sistema_vendas.model.StatusPagamento;
import br.com.aweb.sistema_vendas.service.ClienteService;
import br.com.aweb.sistema_vendas.service.PagamentoService;
import br.com.aweb.sistema_vendas.service.PedidoService;
import br.com.aweb.sistema_vendas.service.ProdutoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;
    private final ClienteService clienteService;
    private final ProdutoService produtoService;
    private final PagamentoService pagamentoService;

    // ======================
    // Helpers
    // ======================
    private static final String REDIRECT_LIST = "redirect:/pedidos";
    private static final String CHECKOUT_VIEW = "pedido/checkout";
    private static final String FORM_VIEW = "pedido/form";
    private static final String LIST_VIEW = "pedido/list";

    private static void validarQuantidade(Integer quantidade) {
        if (quantidade == null || quantidade < 1) {
            throw new IllegalArgumentException("Quantidade deve ser ≥ 1");
        }
    }

    private static void validarFreteDesconto(BigDecimal frete, BigDecimal desconto) {
        if (frete == null || frete.signum() < 0 || desconto == null || desconto.signum() < 0) {
            throw new IllegalArgumentException("Frete/Desconto não podem ser negativos");
        }
    }

    // ======================
    // Listagem / Novo
    // ======================

    @GetMapping
    public ModelAndView listar() {
        List<Pedido> pedidos = pedidoService.listarTodos();
        return new ModelAndView(LIST_VIEW, "pedidos", pedidos);
    }

    @GetMapping("/novo")
    public ModelAndView novo() {
        ModelAndView mv = new ModelAndView(FORM_VIEW);
        mv.addObject("pedido", new Pedido());
        mv.addObject("clientes", clienteService.listarTodos());
        mv.addObject("produtos", produtoService.listarTodos());
        return mv;
    }

    // Cria pedido já com 1 item e redireciona para o checkout
    @PostMapping("/salvar")
    public String salvar(@RequestParam("clienteId") Long clienteId,
                         @RequestParam("produtoId") Long produtoId,
                         @RequestParam("quantidade") Integer quantidade,
                         RedirectAttributes attrs) {
        try {
            validarQuantidade(quantidade);

            Cliente cliente = clienteService.buscarPorId(clienteId)
                    .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado: " + clienteId));

            Pedido pedido = pedidoService.criarPedido(cliente);
            pedidoService.adicionarItem(pedido.getId(), produtoId, quantidade);

            attrs.addFlashAttribute("mensagem", "Pedido #" + pedido.getId() + " criado com sucesso!");
            return "redirect:/pedidos/checkout/" + pedido.getId();
        } catch (Exception e) {
            attrs.addFlashAttribute("erro", e.getMessage());
            return "redirect:/pedidos/novo";
        }
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable Long id, RedirectAttributes attrs) {
        try {
            pedidoService.cancelarPedido(id);
            attrs.addFlashAttribute("mensagem", "Pedido cancelado com sucesso!");
        } catch (Exception e) {
            attrs.addFlashAttribute("erro", e.getMessage());
        }
        return REDIRECT_LIST;
    }

    // ======================
    // CHECKOUT
    // ======================

    @GetMapping("/checkout/{id}")
    public ModelAndView checkout(@PathVariable Long id) {
        Pedido pedido = pedidoService.buscarPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));

        // Atualiza snapshot do pagamento pendente (se existir)
        pagamentoService.atualizarSnapshotSePendente(id);

        ModelAndView mv = new ModelAndView(CHECKOUT_VIEW);
        mv.addObject("pedido", pedido);
        mv.addObject("formasPagamento", FormaPagamento.values());
        return mv;
    }

    @PostMapping("/checkout/{pedidoId}/alterar-quantidade")
    public String alterarQuantidade(@PathVariable Long pedidoId,
                                    @RequestParam Long itemId,
                                    @RequestParam Integer quantidade,
                                    RedirectAttributes attrs) {
        try {
            validarQuantidade(quantidade);
            pedidoService.atualizarQuantidadeItem(pedidoId, itemId, quantidade);
            attrs.addFlashAttribute("mensagem", "Quantidade atualizada.");
        } catch (Exception e) {
            attrs.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/pedidos/checkout/" + pedidoId;
    }

    @PostMapping("/checkout/{id}/resumo")
    public String aplicarResumo(@PathVariable Long id,
                                @RequestParam(defaultValue = "0") BigDecimal frete,
                                @RequestParam(defaultValue = "0") BigDecimal desconto,
                                RedirectAttributes attrs) {
        try {
            validarFreteDesconto(frete, desconto);
            pedidoService.aplicarFreteEDesconto(id, frete, desconto);
            attrs.addFlashAttribute("mensagem", "Resumo atualizado.");
        } catch (Exception e) {
            attrs.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/pedidos/checkout/" + id;
    }

    // ======================
    // PAGAMENTO
    // ======================

    @PostMapping("/checkout/{id}/pagar")
    public String pagar(@PathVariable Long id,
                        @RequestParam FormaPagamento forma,
                        @RequestParam(required = false) String detalhes,
                        RedirectAttributes attrs) {
        try {
            Pagamento pagamento = pagamentoService.iniciarPagamento(id, forma);

            if (forma == FormaPagamento.CARTAO_CREDITO) {
                // Só confirma se ainda não estiver APROVADO
                if (pagamento.getStatus() != StatusPagamento.APROVADO) {
                    pagamento = pagamentoService.confirmarPagamento(
                            id, true, "AUTZ-" + System.currentTimeMillis(), detalhes
                    );
                }
            } else {
                // boleto/transferência permanecem pendentes; salvar detalhes úteis (linha digitável, etc.)
                pagamento.setDetalhes(detalhes);
            }

            attrs.addFlashAttribute("mensagem",
                    "Pagamento " + pagamento.getStatus() + " • Total: " + pagamento.getValorFinal());
            return REDIRECT_LIST;

        } catch (Exception e) {
            attrs.addFlashAttribute("erro", e.getMessage());
            return "redirect:/pedidos/checkout/" + id;
        }
    }
}
