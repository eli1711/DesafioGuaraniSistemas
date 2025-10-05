package br.com.aweb.sistema_vendas;

import br.com.aweb.sistema_vendas.model.Usuario;
import br.com.aweb.sistema_vendas.model.UsuarioRole;
import br.com.aweb.sistema_vendas.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class SistemaVendasApplication {

    public static void main(String[] args) {
        SpringApplication.run(SistemaVendasApplication.class, args);
    }

    @Bean
    CommandLineRunner seedAdmin(UsuarioRepository repo, PasswordEncoder encoder) {
        return args -> {
            if (repo.findByUsername("admin").isEmpty()) {
                repo.save(Usuario.builder()
                    .username("admin")
                    .email("admin@sistema.local")
                    .senhaHash(encoder.encode("admin123"))
                    .role(UsuarioRole.ADMIN)
                    .ativo(true)
                    .build());
            }
        };
    }
}
