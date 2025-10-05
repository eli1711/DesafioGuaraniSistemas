package br.com.aweb.sistema_vendas.controller;

import br.com.aweb.sistema_vendas.model.Usuario;
import br.com.aweb.sistema_vendas.model.UsuarioRole;
import br.com.aweb.sistema_vendas.service.UsuarioService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
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

    @GetMapping
    public ModelAndView listar() {
        List<Usuario> usuarios = usuarioService.listarTodos();
        return new ModelAndView("usuario/list", Map.of("usuarios", usuarios));
    }

    @GetMapping("/novo")
    public ModelAndView novo() {
        return new ModelAndView("usuario/form",
                Map.of("form", new UsuarioForm(), "roles", UsuarioRole.values()));
    }

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

    @GetMapping("/editar/{id}")
    public ModelAndView editar(@PathVariable Long id) {
        Usuario u = usuarioService.buscarPorId(id);
        UsuarioUpdateForm f = new UsuarioUpdateForm();
        f.setId(u.getId());
        f.setEmail(u.getEmail());
        f.setRole(u.getRole().name());
        f.setAtivo(u.isAtivo());
        return new ModelAndView("usuario/edit",
                Map.of("form", f, "usuario", u, "roles", UsuarioRole.values()));
    }

    @PostMapping("/atualizar/{id}")
    public String atualizar(@PathVariable Long id,
                            @ModelAttribute("form") UsuarioUpdateForm form,
                            BindingResult result,
                            RedirectAttributes attrs) {
        if (result.hasErrors()) return "usuario/edit";
        try {
            usuarioService.atualizar(id, form.getEmail(), form.getNovaSenha(), form.getRole(), form.isAtivo());
            attrs.addFlashAttribute("mensagem", "Usuário atualizado!");
        } catch (Exception e) {
            attrs.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/usuarios";
    }

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
        @NotBlank private String role;
        private boolean ativo = true;
    }
}
