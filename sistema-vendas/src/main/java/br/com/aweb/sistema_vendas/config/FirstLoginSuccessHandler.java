package br.com.aweb.sistema_vendas.config;

import br.com.aweb.sistema_vendas.model.Usuario;
import br.com.aweb.sistema_vendas.repository.UsuarioRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class FirstLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UsuarioRepository usuarioRepository;
    // RequestCache para voltar à URL que o usuário tentou acessar (quando não for 1º login)
    private final RequestCache requestCache = new HttpSessionRequestCache();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (response.isCommitted()) {
            return; // evita qualquer tentativa de redirect depois da resposta ser comprometida
        }

        final String username = authentication.getName();
        final Usuario u = usuarioRepository.findByUsername(username).orElse(null);
        final String ctx = request.getContextPath();

        log.info("Login OK para '{}'. mustChangePassword={}", username, (u != null && u.isMustChangePassword()));

        // Caso de segurança: usuário não encontrado ou inativo → vai para login
        if (u == null || !u.isAtivo()) {
            response.sendRedirect(ctx + "/login?error");
            return;
        }

        // Primeiro login → direciona para editar o próprio usuário (trocar senha/dados)
        if (u.isMustChangePassword()) {
            response.sendRedirect(ctx + "/usuarios/editar/" + u.getId());
            return;
        }

        // Não é primeiro login → se havia uma URL originalmente solicitada, respeite-a
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest != null) {
            String targetUrl = savedRequest.getRedirectUrl();
            // limpa o saved request para não reaproveitar depois
            requestCache.removeRequest(request, response);
            response.sendRedirect(targetUrl);
            return;
        }

        // Sem saved request → fallback padrão
        response.sendRedirect(ctx + "/home");
    }
}
