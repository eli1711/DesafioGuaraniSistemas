package br.com.aweb.sistema_vendas.api.mapper;

import br.com.aweb.sistema_vendas.api.dto.pedido.PedidoResponse;
import br.com.aweb.sistema_vendas.model.ItemPedido;
import br.com.aweb.sistema_vendas.model.Pedido;

import java.util.stream.Collectors;

public final class PedidoMapper {
    private PedidoMapper(){}

    public static PedidoResponse toResponse(Pedido p) {
        return new PedidoResponse(
            p.getId(),
            p.getStatus() != null ? p.getStatus().name() : null,
            p.getDataHora(),
            p.getCliente()!=null ? p.getCliente().getNome() : null,
            p.getCliente()!=null ? p.getCliente().getEmail() : null,
            p.getTotalProdutos(),
            p.getDesconto(),
            p.getFrete(),
            p.getValorTotal(),
            p.getItens().stream().map(PedidoMapper::toItem).collect(Collectors.toList())
        );
    }

    private static PedidoResponse.PedidoResponseItem toItem(ItemPedido it) {
        return new PedidoResponse.PedidoResponseItem(
            it.getId(),
            it.getProduto()!=null ? it.getProduto().getId() : null,
            it.getProduto()!=null ? it.getProduto().getNome() : null,
            it.getQuantidade(),
            it.getPrecoUnitario(),
            it.getSubtotal()
        );
    }
}
