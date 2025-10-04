package br.com.sistema.loja_api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String showLoginPage() {
        return "login";  // Retorna o nome do arquivo login.html no diretório templates
    }
    // Mapeia o GET /index para mostrar a tela principal (index.html)
    @GetMapping("/index")
    public String showIndexPage() {
        return "index";  // Retorna o template index.html
    }
}