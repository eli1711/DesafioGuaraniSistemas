
package br.com.aweb.sistema_vendas.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.aweb.sistema_vendas.model.Produto;
import br.com.aweb.sistema_vendas.service.ProdutoService;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/produtos")
public class ProdutoController {
    
    @Autowired
    private ProdutoService produtoService;
    
    // Listar todos os produtos
    @GetMapping
    public ModelAndView listar() {
        List<Produto> produtos = produtoService.listarTodos();
        return new ModelAndView("produto/list", Map.of("produtos", produtos));
    }
    
    // Formulário para novo produto
    @GetMapping("/novo")
    public ModelAndView create() {
        return new ModelAndView("produto/form", Map.of("produto", new Produto()));
    }
    
    // Salvar novo produto
    @PostMapping("/novo")
    public String create(@Valid Produto produto, BindingResult result, RedirectAttributes attributes) {
        if (result.hasErrors()) {
            return "produto/form";
        }
        
        produtoService.salvar(produto);
        attributes.addFlashAttribute("mensagem", "Produto cadastrado com sucesso!");
        return "redirect:/produtos";
    }
    
    // Formulário para editar produto
    @GetMapping("/editar/{id}")
    public ModelAndView editar(@PathVariable Long id) {
        Produto produto = produtoService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));
        return new ModelAndView("produto/form", Map.of("produto", produto));
    }
    
    // Atualizar produto existente
    @PostMapping("/editar/{id}")
    public String atualizar(@PathVariable Long id, @Valid Produto produto, 
                          BindingResult result, RedirectAttributes attributes) {
        if (result.hasErrors()) {
            return "produto/form";
        }
        
        produto.setId(id);
        produtoService.salvar(produto);
        attributes.addFlashAttribute("mensagem", "Produto atualizado com sucesso!");
        return "redirect:/produtos";
    }
    
    // Excluir produto
    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable Long id, RedirectAttributes attributes) {
        produtoService.excluir(id);
        attributes.addFlashAttribute("mensagem", "Produto excluído com sucesso!");
        return "redirect:/produtos";
    }
    
    // Buscar produtos por nome
    @GetMapping("/buscar")
    public ModelAndView buscarPorNome(@RequestParam String nome) {
        List<Produto> produtos = produtoService.buscarPorNome(nome);
        return new ModelAndView("produto/list", Map.of("produtos", produtos));
    }
}