package br.com.aweb.sistema_vendas.service;

import br.com.aweb.sistema_vendas.model.Cliente;
import br.com.aweb.sistema_vendas.model.Usuario;
import br.com.aweb.sistema_vendas.model.UsuarioRole;
import br.com.aweb.sistema_vendas.repository.ClienteRepository;
import br.com.aweb.sistema_vendas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Resultado do salvar contendo o Cliente persistido e,
     * se for cadastro novo, a senha provisória criada para o login.
     */
    public record ClienteSalvarResultado(Cliente cliente, String senhaProvisoria) {}

    @Transactional(readOnly = true)
    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Cliente> buscarPorId(Long id) {
        return clienteRepository.findById(id);
    }

    /**
     * Salva/atualiza o cliente. Caso seja um novo cliente (id == null),
     * cria também um Usuario (ROLE CLIENTE) usando o e-mail como username
     * e retorna a senha provisória gerada.
     */
    @Transactional
    public ClienteSalvarResultado salvar(Cliente cliente) {
        boolean novo = (cliente.getId() == null);

        // unicidade no domínio Cliente
        clienteRepository.findByEmail(cliente.getEmail())
                .filter(c -> !c.getId().equals(cliente.getId()))
                .ifPresent(c -> { throw new RuntimeException("E-mail já cadastrado!"); });

        clienteRepository.findByCpf(cliente.getCpf())
                .filter(c -> !c.getId().equals(cliente.getId()))
                .ifPresent(c -> { throw new RuntimeException("CPF já cadastrado!"); });

        Cliente salvo = clienteRepository.save(cliente);

        String senhaProvisoria = null;

        if (novo) {
            String username = salvo.getEmail(); // usar e-mail como login

            // unicidade no domínio Usuario
            if (usuarioRepository.existsByUsername(username)) {
                throw new RuntimeException("Já existe um usuário com este username.");
            }
            if (usuarioRepository.existsByEmail(salvo.getEmail())) {
                throw new RuntimeException("Já existe um usuário com este e-mail.");
            }

            senhaProvisoria = gerarSenhaProvisoria(10);
            Usuario u = Usuario.builder()
                    .username(username)
                    .email(salvo.getEmail())
                    .senhaHash(passwordEncoder.encode(senhaProvisoria))
                    .role(UsuarioRole.CLIENTE)
                    .ativo(true)
                    .mustChangePassword(true) // <<< força troca de senha no primeiro login
                    .build();

            usuarioRepository.save(u);
        }

        return new ClienteSalvarResultado(salvo, senhaProvisoria);
    }
    @Transactional(readOnly = true)
public Optional<Cliente> buscarPorEmail(String email) {
    if (email == null || email.isBlank()) {
        return Optional.empty();
    }
    return clienteRepository.findByEmail(email);
}

    @Transactional
    public void excluir(Long id) {
        clienteRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Cliente> buscarPorNome(String nome) {
        return clienteRepository.findByNomeContainingIgnoreCase(nome);
    }

    // ---------- util ----------

    private static final String ALFABETO =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RAND = new SecureRandom();

    private static String gerarSenhaProvisoria(int tamanho) {
        StringBuilder sb = new StringBuilder(tamanho);
        for (int i = 0; i < tamanho; i++) {
            sb.append(ALFABETO.charAt(RAND.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }
}
