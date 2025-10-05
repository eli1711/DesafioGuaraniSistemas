package br.com.aweb.sistema_vendas.api.dto.produto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProdutoRequest(
    @NotBlank String nome,
    @NotBlank String descricao,
    @NotNull @Positive BigDecimal preco,
    @NotNull @PositiveOrZero Integer quantidadeEmEstoque
) {}
