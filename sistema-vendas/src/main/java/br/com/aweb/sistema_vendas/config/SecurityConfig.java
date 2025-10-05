package br.com.aweb.sistema_vendas.config;

import br.com.aweb.sistema_vendas.model.Usuario;
import br.com.aweb.sistema_vendas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final UsuarioRepository usuarioRepository;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> usuarioRepository.findByUsername(username)
            .filter(Usuario::isAtivo)
            .map(u -> User.withUsername(u.getUsername())
                .password(u.getSenhaHash())
                .roles(u.getRole().name()) // ADMIN | CLIENTE | OPERADOR
                .build())
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado ou inativo"));
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
          .csrf(csrf -> csrf.disable())
          .authorizeHttpRequests(auth -> auth
              // públicos
              .requestMatchers("/", "/home", "/login", "/css/**", "/js/**", "/images/**").permitAll()

              // ADMIN: gestão de usuários e clientes
              .requestMatchers("/usuarios/**", "/clientes/**").hasRole("ADMIN")

              // Produtos e Pedidos: ADMIN, OPERADOR e CLIENTE
              .requestMatchers("/produtos/**", "/pedidos/**").hasAnyRole("ADMIN","OPERADOR","CLIENTE")

              .anyRequest().authenticated()
          )
          .formLogin(form -> form
              .loginPage("/login").permitAll()
              .defaultSuccessUrl("/home", true)
          )
          .logout(logout -> logout
              .logoutUrl("/logout")
              .logoutSuccessUrl("/login?logout").permitAll()
          )
          .httpBasic(); // sem Customizer

        return http.build();
    }
}
