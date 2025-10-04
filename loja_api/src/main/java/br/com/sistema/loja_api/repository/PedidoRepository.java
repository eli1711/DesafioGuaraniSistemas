package br.com.sistema.loja_api.repository;

import br.com.sistema.loja_api.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
}
