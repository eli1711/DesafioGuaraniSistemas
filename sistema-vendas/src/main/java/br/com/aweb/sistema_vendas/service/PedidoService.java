package br.com.aweb.sistema_vendas.service;

import br.com.aweb.sistema_vendas.model.*;
import br.com.aweb.sistema_vendas.repository.PedidoRepository;
import br.com.aweb.sistema_vendas.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;

    /** ✅ Injetado corretamente (sem = null) */
    private final PagamentoService pagamentoService;

    // -------- LISTAGENS --------
    @Transactional(readOnly = true)
    public List<Pedido> listarTodosOrdenado() {
        return pedidoRepository.findAllByOrderByDataHoraDesc();
    }

    @Transactional(readOnly = true)
    public List<Pedido> listarDoClienteOrdenado(String emailCliente) {
        return pedidoRepository.findByClienteEmailOrderByDataHoraDesc(emailCliente);
    }

    @Transactional(readOnly = true)
    public List<Pedido> listarPorStatusOrdenado(StatusPedido status) {
        return pedidoRepository.findByStatusOrderByDataHoraDesc(status);
    }

    // -------- CRUD / REGRAS --------
    @Transactional
    public Pedido criarPedido(Cliente cliente) {
        Pedido pedido = new Pedido(cliente);
        pedido.setDataHora(LocalDateTime.now());
        pedido.recalcularTotais();
        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido adicionarItem(Long pedidoId, Long produtoId, Integer quantidade) {
        if (quantidade == null || quantidade < 1) {
            throw new IllegalArgumentException("Quantidade deve ser >= 1");
        }

        Pedido pedido = buscarPorId(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        if (pedido.getStatus() != StatusPedido.ATIVO) {
            throw new RuntimeException("Não é possível adicionar itens em pedido cancelado.");
        }

        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

        if (produto.getQuantidadeEmEstoque() < quantidade) {
            throw new RuntimeException("Estoque insuficiente para o produto: " + produto.getNome());
        }

        ItemPedido item = new ItemPedido(pedido, produto, quantidade);
        pedido.adicionarItem(item);

        produto.setQuantidadeEmEstoque(produto.getQuantidadeEmEstoque() - quantidade);
        produtoRepository.save(produto);

        pedido.recalcularTotais();
        Pedido salvo = pedidoRepository.save(pedido);

        pagamentoService.atualizarSnapshotSePendente(pedidoId);

        return salvo;
    }

    @Transactional
    public Pedido atualizarQuantidadeItem(Long pedidoId, Long itemId, Integer novaQuantidade) {
        if (novaQuantidade == null || novaQuantidade < 1) {
            throw new IllegalArgumentException("Quantidade deve ser >= 1");
        }

        Pedido pedido = buscarPorId(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        if (pedido.getStatus() != StatusPedido.ATIVO) {
            throw new RuntimeException("Não é possível alterar itens de pedido cancelado.");
        }

        ItemPedido item = pedido.getItens().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item não encontrado no pedido."));

        int atual = item.getQuantidade();
        int delta = novaQuantidade - atual;

        Produto produto = item.getProduto();
        if (delta > 0) {
            if (produto.getQuantidadeEmEstoque() < delta) {
                throw new RuntimeException("Estoque insuficiente para o produto: " + produto.getNome());
            }
            produto.setQuantidadeEmEstoque(produto.getQuantidadeEmEstoque() - delta);
        } else if (delta < 0) {
            produto.setQuantidadeEmEstoque(produto.getQuantidadeEmEstoque() + Math.abs(delta));
        }
        produtoRepository.save(produto);

        item.setQuantidade(novaQuantidade);
        pedido.recalcularTotais();
        Pedido salvo = pedidoRepository.save(pedido);

        pagamentoService.atualizarSnapshotSePendente(pedidoId);

        return salvo;
    }

    @Transactional
    public Pedido removerItem(Long pedidoId, Long itemId) {
        Pedido pedido = buscarPorId(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        if (pedido.getStatus() != StatusPedido.ATIVO) {
            throw new RuntimeException("Não é possível remover itens de pedido cancelado.");
        }

        ItemPedido item = pedido.getItens().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item não encontrado no pedido."));

        Produto produto = item.getProduto();
        produto.setQuantidadeEmEstoque(produto.getQuantidadeEmEstoque() + item.getQuantidade());
        produtoRepository.save(produto);

        pedido.getItens().remove(item);
        pedido.recalcularTotais();
        Pedido salvo = pedidoRepository.save(pedido);

        pagamentoService.atualizarSnapshotSePendente(pedidoId);

        return salvo;
    }

    @Transactional
    public Pedido aplicarFreteEDesconto(Long pedidoId, BigDecimal frete, BigDecimal desconto) {
        Pedido pedido = buscarPorId(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        if (pedido.getStatus() != StatusPedido.ATIVO) {
            throw new RuntimeException("Não é possível alterar pedido cancelado.");
        }

        frete = (frete == null) ? BigDecimal.ZERO : frete;
        desconto = (desconto == null) ? BigDecimal.ZERO : desconto;

        if (frete.signum() < 0 || desconto.signum() < 0) {
            throw new IllegalArgumentException("Frete/Desconto não podem ser negativos");
        }

        pedido.setFrete(frete);
        pedido.setDesconto(desconto);
        pedido.recalcularTotais();

        Pedido salvo = pedidoRepository.save(pedido);

        pagamentoService.atualizarSnapshotSePendente(pedidoId);

        return salvo;
    }

    @Transactional
    public Pedido cancelarPedido(Long pedidoId) {
        Pedido pedido = buscarPorId(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        for (ItemPedido item : pedido.getItens()) {
            Produto produto = item.getProduto();
            produto.setQuantidadeEmEstoque(produto.getQuantidadeEmEstoque() + item.getQuantidade());
            produtoRepository.save(produto);
        }

        pedido.setStatus(StatusPedido.CANCELADO);
        pedido.recalcularTotais();
        return pedidoRepository.save(pedido);
    }

    @Transactional(readOnly = true)
    public Optional<Pedido> buscarPorId(Long id) {
        return pedidoRepository.findById(id);
    }
}
