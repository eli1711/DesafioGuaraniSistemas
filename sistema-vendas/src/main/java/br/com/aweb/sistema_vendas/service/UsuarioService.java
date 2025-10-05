package br.com.aweb.sistema_vendas.service;

import br.com.aweb.sistema_vendas.model.Usuario;
import br.com.aweb.sistema_vendas.model.UsuarioRole;
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

    /** 🔎 Helper para carregar o usuário logado via username */
    public Usuario buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }

    /**
     * Cria usuário e, por padrão, força troca de senha para perfis de CLIENTE.
     * (ADMIN/OPERADOR não são obrigados a trocar a senha no primeiro login.)
     */
    @Transactional
    public Usuario criar(String username, String email, String senhaPura, String role) {
        boolean mustChange = UsuarioRole.CLIENTE.name().equalsIgnoreCase(role);
        return criar(username, email, senhaPura, role, mustChange);
    }

    /** Overload permitindo controlar explicitamente o mustChangePassword. */
    @Transactional
    public Usuario criar(String username, String email, String senhaPura, String role, boolean mustChangePassword) {
        if (usuarioRepository.existsByUsername(username))
            throw new RuntimeException("Username já existe");
        if (usuarioRepository.existsByEmail(email))
            throw new RuntimeException("E-mail já existe");

        Usuario u = Usuario.builder()
                .username(username)
                .email(email)
                .senhaHash(passwordEncoder.encode(senhaPura))
                .role(Enum.valueOf(UsuarioRole.class, role))
                .ativo(true)
                .mustChangePassword(mustChangePassword)
                .build();

        return usuarioRepository.saveAndFlush(u);
    }

    /**
     * Atualização geral (usada por ADMIN e pelo próprio usuário).
     * Se uma nova senha for informada, desliga mustChangePassword.
     */
    @Transactional
    public Usuario atualizar(Long id, String email, String novaSenhaPuraOuVazia, String role, boolean ativo) {
        Usuario u = buscarPorId(id);

        u.setEmail(email);
        u.setRole(Enum.valueOf(UsuarioRole.class, role));
        u.setAtivo(ativo);

        if (novaSenhaPuraOuVazia != null && !novaSenhaPuraOuVazia.isBlank()) {
            u.setSenhaHash(passwordEncoder.encode(novaSenhaPuraOuVazia));
            u.setMustChangePassword(false); // troca cumprida
        }

        return usuarioRepository.saveAndFlush(u);
    }

    /**
     * Troca de senha pelo próprio usuário:
     * - valida a senha atual
     * - define nova senha
     * - desmarca mustChangePassword
     */
    @Transactional
    public Usuario alterarSenha(Long id, String senhaAtualPura, String novaSenhaPura) {
        Usuario u = buscarPorId(id);

        if (senhaAtualPura == null || !passwordEncoder.matches(senhaAtualPura, u.getSenhaHash())) {
            throw new RuntimeException("Senha atual inválida.");
        }
        if (novaSenhaPura == null || novaSenhaPura.isBlank() || novaSenhaPura.length() < 6) {
            throw new RuntimeException("Nova senha deve ter ao menos 6 caracteres.");
        }

        u.setSenhaHash(passwordEncoder.encode(novaSenhaPura));
        u.setMustChangePassword(false);

        return usuarioRepository.saveAndFlush(u);
    }

    /** Liga/desliga o flag de troca obrigatória de senha. */
    @Transactional
    public Usuario definirMustChangePassword(Long id, boolean mustChange) {
        Usuario u = buscarPorId(id);
        u.setMustChangePassword(mustChange);
        return usuarioRepository.saveAndFlush(u);
    }

    @Transactional
    public void excluir(Long id) { usuarioRepository.deleteById(id); }
}
