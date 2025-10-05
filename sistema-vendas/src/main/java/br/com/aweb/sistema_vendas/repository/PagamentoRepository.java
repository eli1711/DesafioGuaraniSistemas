package br.com.aweb.sistema_vendas.repository;

import br.com.aweb.sistema_vendas.model.Pagamento;
import br.com.aweb.sistema_vendas.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
    Optional<Pagamento> findByPedido(Pedido pedido);
}
