package br.com.aweb.sistema_vendas.controller;

import br.com.aweb.sistema_vendas.model.*;
import br.com.aweb.sistema_vendas.service.ClienteService;
import br.com.aweb.sistema_vendas.service.PagamentoService;
import br.com.aweb.sistema_vendas.service.PedidoService;
import br.com.aweb.sistema_vendas.service.ProdutoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
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

    /** ✅ Agora realmente injetado pelo Spring (sem = null, sem @Nullable) */
    private final PagamentoService pagamentoService;

    private static final String REDIRECT_LIST = "redirect:/pedidos";
    private static final String CHECKOUT_VIEW = "pedido/checkout";
    private static final String FORM_VIEW = "pedido/form";
    private static final String LIST_VIEW = "pedido/list";

    // ---------- helpers ----------
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
    private static boolean isAdminOuOperador(User principal) {
        return principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_OPERADOR"));
    }

    // ---------- LISTAGEM ----------
    @GetMapping
    public ModelAndView listar(@AuthenticationPrincipal User principal) {
        if (principal == null) return new ModelAndView("redirect:/login");

        List<Pedido> pedidos = isAdminOuOperador(principal)
                ? pedidoService.listarTodosOrdenado()
                : pedidoService.listarDoClienteOrdenado(principal.getUsername());

        return new ModelAndView(LIST_VIEW, "pedidos", pedidos);
    }

    // ---------- NOVO ----------
    @GetMapping("/novo")
    public ModelAndView novo(@AuthenticationPrincipal User principal, RedirectAttributes attrs) {
        if (principal == null) return new ModelAndView("redirect:/login");

        ModelAndView mv = new ModelAndView(FORM_VIEW);
        mv.addObject("pedido", new Pedido());
        mv.addObject("produtos", produtoService.listarTodos());

        if (isAdminOuOperador(principal)) {
            mv.addObject("clientes", clienteService.listarTodos());
        } else {
            var opt = clienteService.buscarPorEmail(principal.getUsername());
            if (opt.isEmpty()) {
                attrs.addFlashAttribute("erro", "Seu usuário não está vinculado a um cliente.");
                return new ModelAndView(REDIRECT_LIST);
            }
            mv.addObject("clienteAtual", opt.get());
        }
        return mv;
    }

    // ---------- CRIAR ----------
    @PostMapping("/salvar")
    public String salvar(@RequestParam("clienteId") Long clienteIdParam,
                         @RequestParam("produtoId") Long produtoId,
                         @RequestParam("quantidade") Integer quantidade,
                         RedirectAttributes attrs,
                         @AuthenticationPrincipal User principal) {
        try {
            if (principal == null) return "redirect:/login";
            validarQuantidade(quantidade);

            final Long clienteIdUsar = isAdminOuOperador(principal)
                    ? clienteIdParam
                    : clienteService.buscarPorEmail(principal.getUsername())
                        .map(Cliente::getId)
                        .orElseThrow(() -> new IllegalArgumentException("Seu usuário não está vinculado a um cliente."));

            Cliente cliente = clienteService.buscarPorId(clienteIdUsar)
                    .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado: " + clienteIdUsar));

            Pedido pedido = pedidoService.criarPedido(cliente);
            pedidoService.adicionarItem(pedido.getId(), produtoId, quantidade);

            attrs.addFlashAttribute("mensagem", "Pedido #" + pedido.getId() + " criado com sucesso!");
            return "redirect:/pedidos/checkout/" + pedido.getId();

        } catch (Exception e) {
            attrs.addFlashAttribute("erro", e.getMessage());
            return "redirect:/pedidos/novo";
        }
    }

    // ---------- EXCLUIR/CANCELAR ----------
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

    // ---------- CHECKOUT ----------
    @GetMapping("/checkout/{id}")
    public ModelAndView checkout(@PathVariable Long id,
                                 @AuthenticationPrincipal User principal,
                                 RedirectAttributes attrs) {
        if (principal == null) return new ModelAndView("redirect:/login");

        Pedido pedido = pedidoService.buscarPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));

        if (!isAdminOuOperador(principal)) {
            String email = principal.getUsername();
            if (pedido.getCliente() == null || pedido.getCliente().getEmail() == null
                    || !pedido.getCliente().getEmail().equalsIgnoreCase(email)) {
                attrs.addFlashAttribute("erro", "Você não tem acesso a este pedido.");
                return new ModelAndView(REDIRECT_LIST);
            }
        }

        // Atualiza snapshot se existir pagamento pendente
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

    @PostMapping("/checkout/{id}/pagar")
    public String pagar(@PathVariable Long id,
                        @RequestParam FormaPagamento forma,
                        @RequestParam(required = false) String detalhes,
                        RedirectAttributes attrs) {
        try {
            Pagamento pagamento = pagamentoService.iniciarPagamento(id, forma);

            if (forma == FormaPagamento.CARTAO_CREDITO) {
                if (pagamento.getStatus() != StatusPagamento.APROVADO) {
                    pagamento = pagamentoService.confirmarPagamento(
                            id, true, "AUTZ-" + System.currentTimeMillis(), detalhes
                    );
                }
            } else {
                // Mantém pendente, mas persiste detalhes (boleto/transferência)
                pagamentoService.atualizarDetalhes(id, detalhes);
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
