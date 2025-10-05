package br.com.aweb.sistema_vendas.repository;

import br.com.aweb.sistema_vendas.model.Pedido;
import br.com.aweb.sistema_vendas.model.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // Lista geral (mais recentes primeiro)
    List<Pedido> findAllByOrderByDataHoraDesc();

    // Lista por status (opcional)
    List<Pedido> findByStatusOrderByDataHoraDesc(StatusPedido status);

    // Lista por e-mail do cliente (mais recentes primeiro)
    List<Pedido> findByClienteEmailOrderByDataHoraDesc(String email);
}
