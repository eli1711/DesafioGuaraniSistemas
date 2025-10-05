package br.com.aweb.sistema_vendas.controller;

import br.com.aweb.sistema_vendas.model.Usuario;
import br.com.aweb.sistema_vendas.model.UsuarioRole;
import br.com.aweb.sistema_vendas.service.UsuarioService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    // Listar (ADMIN)
    @GetMapping
    public ModelAndView listar() {
        List<Usuario> usuarios = usuarioService.listarTodos();
        return new ModelAndView("usuario/list", Map.of("usuarios", usuarios));
    }

    // Novo (ADMIN)
    @GetMapping("/novo")
    public ModelAndView novo() {
        return new ModelAndView("usuario/form",
                Map.of("form", new UsuarioForm(), "roles", UsuarioRole.values()));
    }

    // Salvar (ADMIN)
    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("form") UsuarioForm form,
                         BindingResult result,
                         RedirectAttributes attrs) {
        if (result.hasErrors()) return "usuario/form";
        try {
            usuarioService.criar(form.getUsername(), form.getEmail(), form.getSenha(), form.getRole());
            attrs.addFlashAttribute("mensagem", "Usuário criado com sucesso!");
            return "redirect:/usuarios";
        } catch (Exception e) {
            attrs.addFlashAttribute("erro", e.getMessage());
            return "redirect:/usuarios/novo";
        }
    }

    // Editar (ADMIN pode editar qualquer um; usuário comum só pode editar o próprio ID)
    @GetMapping("/editar/{id}")
    public ModelAndView editar(@PathVariable Long id,
                               @AuthenticationPrincipal User principal,
                               RedirectAttributes attrs) {

        Usuario atual = usuarioService.buscarPorUsername(principal.getUsername());
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // se não for admin e tentar editar outro id -> nega e redireciona para o próprio
        if (!isAdmin && !atual.getId().equals(id)) {
            attrs.addFlashAttribute("erro", "Você só pode editar seu próprio usuário.");
            return new ModelAndView("redirect:/usuarios/editar/" + atual.getId());
        }

        Usuario u = usuarioService.buscarPorId(id);
        UsuarioUpdateForm f = new UsuarioUpdateForm();
        f.setId(u.getId());
        f.setEmail(u.getEmail());
        f.setRole(u.getRole().name());
        f.setAtivo(u.isAtivo());

        // você pode passar um flag para a view esconder campos de role/ativo para não-admin
        return new ModelAndView("usuario/edit",
                Map.of("form", f, "usuario", u, "roles", UsuarioRole.values(), "isAdmin", isAdmin));
    }

    // Atualizar (ADMIN pode qualquer; usuário comum só o próprio e NÃO pode mudar role/ativo)
    @PostMapping("/atualizar/{id}")
    public String atualizar(@PathVariable Long id,
                            @AuthenticationPrincipal User principal,
                            @ModelAttribute("form") UsuarioUpdateForm form,
                            BindingResult result,
                            RedirectAttributes attrs) {
        if (result.hasErrors()) return "usuario/edit";

        Usuario atual = usuarioService.buscarPorUsername(principal.getUsername());
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !atual.getId().equals(id)) {
            attrs.addFlashAttribute("erro", "Você só pode atualizar seu próprio usuário.");
            return "redirect:/home";
        }

        try {
            // 🔒 Se NÃO for admin, não permitir alterar role/ativo (usa os valores atuais do banco)
            String roleParaSalvar;
            boolean ativoParaSalvar;

            if (isAdmin) {
                roleParaSalvar = form.getRole();
                ativoParaSalvar = form.isAtivo();
            } else {
                Usuario doBanco = usuarioService.buscarPorId(id);
                roleParaSalvar = doBanco.getRole().name();
                ativoParaSalvar = doBanco.isAtivo();
            }

            usuarioService.atualizar(id, form.getEmail(), form.getNovaSenha(), roleParaSalvar, ativoParaSalvar);
            attrs.addFlashAttribute("mensagem", "Usuário atualizado!");

            return isAdmin ? "redirect:/usuarios" : "redirect:/home";

        } catch (Exception e) {
            attrs.addFlashAttribute("erro", e.getMessage());
            return "redirect:/usuarios/editar/" + id;
        }
    }

    // Excluir (ADMIN)
    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable Long id, RedirectAttributes attrs) {
        try {
            usuarioService.excluir(id);
            attrs.addFlashAttribute("mensagem", "Usuário excluído!");
        } catch (Exception e) {
            attrs.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/usuarios";
    }

    // --- DTOs de formulário ---
    @Data
    public static class UsuarioForm {
        @NotBlank private String username;
        @NotBlank @Email private String email;
        @NotBlank private String senha; // texto puro no form
        @NotBlank private String role;  // ADMIN | CLIENTE | OPERADOR
    }

    @Data
    public static class UsuarioUpdateForm {
        private Long id;
        @NotBlank @Email private String email;
        private String novaSenha; // opcional
        @NotBlank private String role; // será ignorado se não-admin
        private boolean ativo = true;  // será ignorado se não-admin
    }
}
