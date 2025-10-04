package br.com.sistema.loja_api.controller;

import br.com.sistema.loja_api.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        // simplificado: produção exige validação de senha!
        String token = jwtUtil.generateToken(username);
        return Map.of("token", token);
    }
}
