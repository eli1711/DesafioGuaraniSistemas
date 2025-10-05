package br.com.aweb.sistema_vendas.api.mapper;

import br.com.aweb.sistema_vendas.api.dto.produto.ProdutoRequest;
import br.com.aweb.sistema_vendas.api.dto.produto.ProdutoResponse;
import br.com.aweb.sistema_vendas.model.Produto;

public final class ProdutoMapper {
    private ProdutoMapper(){}

    public static Produto toEntity(ProdutoRequest r) {
        Produto p = new Produto();
        p.setNome(r.nome());
        p.setDescricao(r.descricao());
        p.setPreco(r.preco());
        p.setQuantidadeEmEstoque(r.quantidadeEmEstoque());
        return p;
    }

    public static void copyToEntity(ProdutoRequest r, Produto p) {
        p.setNome(r.nome());
        p.setDescricao(r.descricao());
        p.setPreco(r.preco());
        p.setQuantidadeEmEstoque(r.quantidadeEmEstoque());
    }

    public static ProdutoResponse toResponse(Produto p) {
        return new ProdutoResponse(
            p.getId(),
            p.getNome(),
            p.getDescricao(),
            p.getPreco(),
            p.getQuantidadeEmEstoque()
        );
    }
}
