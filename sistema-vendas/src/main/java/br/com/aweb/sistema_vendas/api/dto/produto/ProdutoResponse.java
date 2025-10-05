package br.com.aweb.sistema_vendas.api.dto.produto;

import java.math.BigDecimal;

public record ProdutoResponse(
    Long id,
    String nome,
    String descricao,
    BigDecimal preco,
    Integer quantidadeEmEstoque
) {}
