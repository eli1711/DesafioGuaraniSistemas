package br.com.sistema.loja_api.repository;

import br.com.sistema.loja_api.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
}
