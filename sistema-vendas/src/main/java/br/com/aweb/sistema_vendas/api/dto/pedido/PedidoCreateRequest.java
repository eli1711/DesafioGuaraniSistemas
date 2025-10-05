package br.com.aweb.sistema_vendas.api.dto.pedido;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record PedidoCreateRequest(
    @NotNull Long clienteId,                      // se o cliente estiver logado, você pode ignorar e usar o do token
    @NotEmpty List<ItemPedidoRequest> itens,
    @PositiveOrZero BigDecimal frete,
    @PositiveOrZero BigDecimal desconto
) {}
