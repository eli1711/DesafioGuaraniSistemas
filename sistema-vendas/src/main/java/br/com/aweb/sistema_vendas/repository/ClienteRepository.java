package br.com.aweb.sistema_vendas.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.aweb.sistema_vendas.model.Cliente;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // Buscar cliente por e-mail (único)
    Optional<Cliente> findByEmail(String email);

    // Buscar cliente por CPF (único)
    Optional<Cliente> findByCpf(String cpf);

    // Buscar clientes cujo nome contenha um trecho (ignora maiúsculas/minúsculas)
    List<Cliente> findByNomeContainingIgnoreCase(String nome);
}
