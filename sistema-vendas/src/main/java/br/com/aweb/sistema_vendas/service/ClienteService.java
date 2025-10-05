package br.com.aweb.sistema_vendas.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.aweb.sistema_vendas.model.Cliente;
import br.com.aweb.sistema_vendas.repository.ClienteRepository;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    // Listar todos os clientes
    @Transactional(readOnly = true)
    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }

    // Buscar cliente por ID
    @Transactional(readOnly = true)
    public Optional<Cliente> buscarPorId(Long id) {
        return clienteRepository.findById(id);
    }

    // Salvar ou atualizar cliente
    @Transactional
    public Cliente salvar(Cliente cliente) {
       

        clienteRepository.findByEmail(cliente.getEmail())
            .filter(c -> !c.getId().equals(cliente.getId()))
            .ifPresent(c -> {
                throw new RuntimeException("E-mail já cadastrado!");
            });

        clienteRepository.findByCpf(cliente.getCpf())
            .filter(c -> !c.getId().equals(cliente.getId()))
            .ifPresent(c -> {
                throw new RuntimeException("CPF já cadastrado!");
            });

        return clienteRepository.save(cliente);
    }

    // Excluir cliente
    @Transactional
    public void excluir(Long id) {
        clienteRepository.deleteById(id);
    }

    // Buscar clientes por nome (parte do nome, ignore case)
    @Transactional(readOnly = true)
    public List<Cliente> buscarPorNome(String nome) {
        return clienteRepository.findByNomeContainingIgnoreCase(nome);
    }
}
