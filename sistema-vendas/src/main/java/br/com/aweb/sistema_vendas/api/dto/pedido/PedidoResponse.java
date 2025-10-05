package br.com.aweb.sistema_vendas.api.dto.pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
    Long id,
    String status,
    LocalDateTime dataHora,
    String clienteNome,
    String clienteEmail,
    BigDecimal totalProdutos,
    BigDecimal desconto,
    BigDecimal frete,
    BigDecimal valorTotal,
    List<PedidoResponseItem> itens
) {
    public record PedidoResponseItem(
        Long itemId,
        Long produtoId,
        String produtoNome,
        Integer quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal
    ) {}
}
