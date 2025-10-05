package br.com.aweb.sistema_vendas.service;

import br.com.aweb.sistema_vendas.model.Usuario;
import br.com.aweb.sistema_vendas.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public List<Usuario> listarTodos() { return usuarioRepository.findAll(); }

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }

    @Transactional
    public Usuario criar(String username, String email, String senhaPura, String role) {
        if (usuarioRepository.existsByUsername(username))
            throw new RuntimeException("Username já existe");
        if (usuarioRepository.existsByEmail(email))
            throw new RuntimeException("E-mail já existe");

        Usuario u = Usuario.builder()
                .username(username)
                .email(email)
                .senhaHash(passwordEncoder.encode(senhaPura))
                .role(Enum.valueOf(br.com.aweb.sistema_vendas.model.UsuarioRole.class, role))
                .ativo(true)
                .build();

        return usuarioRepository.save(u);
    }

    @Transactional
    public Usuario atualizar(Long id, String email, String novaSenhaPuraOuVazia, String role, boolean ativo) {
        Usuario u = buscarPorId(id);
        u.setEmail(email);
        u.setRole(Enum.valueOf(br.com.aweb.sistema_vendas.model.UsuarioRole.class, role));
        u.setAtivo(ativo);
        if (novaSenhaPuraOuVazia != null && !novaSenhaPuraOuVazia.isBlank()) {
            u.setSenhaHash(passwordEncoder.encode(novaSenhaPuraOuVazia));
        }
        return usuarioRepository.save(u);
    }

    @Transactional
    public void excluir(Long id) { usuarioRepository.deleteById(id); }
}
