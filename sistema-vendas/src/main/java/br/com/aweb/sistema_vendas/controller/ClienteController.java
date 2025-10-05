package br.com.aweb.sistema_vendas.controller;

import br.com.aweb.sistema_vendas.model.Cliente;
import br.com.aweb.sistema_vendas.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    // Listar clientes
    @GetMapping
    public ModelAndView listar() {
        List<Cliente> clientes = clienteService.listarTodos();
        return new ModelAndView("cliente/listCliente", Map.of("clientes", clientes));
    }

    // Formulário novo cliente
    @GetMapping("/novo")
    public ModelAndView novo() {
        return new ModelAndView("cliente/formCliente", Map.of("cliente", new Cliente()));
    }

    // Editar cliente
    @GetMapping("/editar/{id}")
    public ModelAndView editar(@PathVariable Long id, RedirectAttributes attributes) {
        return clienteService.buscarPorId(id)
                .map(cliente -> new ModelAndView("cliente/formCliente", Map.of("cliente", cliente)))
                .orElseGet(() -> {
                    attributes.addFlashAttribute("erro", "Cliente não encontrado!");
                    return new ModelAndView("redirect:/clientes");
                });
    }

    // Salvar (cadastrar ou atualizar)
    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("cliente") Cliente cliente,
                         BindingResult result,
                         RedirectAttributes attributes) {
        if (result.hasErrors()) {
            return "cliente/formCliente";
        }

        try {
            // Usa o retorno com possível senha provisória quando for cadastro novo
            ClienteService.ClienteSalvarResultado out = clienteService.salvar(cliente);

            String mensagem;
            if (out.senhaProvisoria() != null && !out.senhaProvisoria().isBlank()) {
                // Cadastro novo -> mostra login e senha provisória
                mensagem = "Cliente salvo! Login: " + out.cliente().getEmail()
                        + " | Senha provisória: " + out.senhaProvisoria();
            } else {
                // Atualização (sem senha gerada)
                mensagem = "Cliente atualizado com sucesso!";
            }

            attributes.addFlashAttribute("mensagem", mensagem);

        } catch (Exception e) {
            attributes.addFlashAttribute("erro", "Erro ao salvar cliente: " + e.getMessage());
            return "redirect:/clientes/novo";
        }
        return "redirect:/clientes";
    }

    // Excluir cliente
    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable Long id, RedirectAttributes attributes) {
        try {
            clienteService.excluir(id);
            attributes.addFlashAttribute("mensagem", "Cliente excluído com sucesso!");
        } catch (Exception e) {
            attributes.addFlashAttribute("erro", "Erro ao excluir cliente: " + e.getMessage());
        }
        return "redirect:/clientes";
    }

    // Buscar clientes por nome
    @GetMapping("/buscar")
    public ModelAndView buscar(@RequestParam String nome) {
        List<Cliente> clientes = clienteService.buscarPorNome(nome);
        return new ModelAndView("cliente/listCliente", Map.of("clientes", clientes));
    }
}
