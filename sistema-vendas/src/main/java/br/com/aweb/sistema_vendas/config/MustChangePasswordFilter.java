package br.com.aweb.sistema_vendas.config;

import br.com.aweb.sistema_vendas.model.Usuario;
import br.com.aweb.sistema_vendas.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class MustChangePasswordFilter extends OncePerRequestFilter {

    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();
        String ctx = request.getContextPath();

        boolean bypass = uri.startsWith(ctx + "/senha/")
                      || uri.startsWith(ctx + "/usuarios/editar/")     // ✅ liberar tela de edição
                      || uri.startsWith(ctx + "/usuarios/atualizar/")  // ✅ liberar submit da edição
                      || uri.startsWith(ctx + "/logout")
                      || uri.startsWith(ctx + "/css/")
                      || uri.startsWith(ctx + "/js/")
                      || uri.startsWith(ctx + "/images/")
                      || uri.equals(ctx + "/login")
                      || uri.equals(ctx + "/"); // homepage pública

        if (!bypass) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(String.valueOf(auth.getPrincipal()))) {
                String username = auth.getName();
                Usuario u = usuarioRepository.findByUsername(username).orElse(null);
                if (u != null && u.isAtivo() && u.isMustChangePassword()) {
                    response.sendRedirect(ctx + "/usuarios/editar/" + u.getId()); // ✅ leve direto para a tela correta
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
