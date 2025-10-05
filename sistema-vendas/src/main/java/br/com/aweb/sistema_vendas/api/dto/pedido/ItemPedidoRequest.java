package br.com.aweb.sistema_vendas.api.dto.pedido;

import jakarta.validation.constraints.*;

public record ItemPedidoRequest(
    @NotNull Long produtoId,
    @NotNull @Min(1) Integer quantidade
) {}
