package br.com.aweb.sistema_vendas.repository;

import br.com.aweb.sistema_vendas.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByEmail(String email);                 // ✅ necessário
    Optional<Cliente> findByCpf(String cpf);
    List<Cliente> findByNomeContainingIgnoreCase(String nome);
}
