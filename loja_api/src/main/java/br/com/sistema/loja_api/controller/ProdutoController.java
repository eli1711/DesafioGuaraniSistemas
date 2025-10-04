package br.com.sistema.loja_api.controller;

import br.com.sistema.loja_api.dto.ProdutoDTO;
import br.com.sistema.loja_api.service.ProdutoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class ProdutoController {

    @Autowired
    private ProdutoService produtoService;

    @GetMapping("/produtos")
    public String listarProdutos(Model model) {
        List<ProdutoDTO> produtos = produtoService.listarTodos();
        model.addAttribute("produtos", produtos);
        return "produtos";
    }
}
