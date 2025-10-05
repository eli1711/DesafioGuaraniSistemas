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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final UsuarioRepository usuarioRepository;
    private final FirstLoginSuccessHandler firstLoginSuccessHandler;
    private final MustChangePasswordFilter mustChangePasswordFilter;

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> usuarioRepository.findByUsername(username)
            .filter(Usuario::isAtivo)
            .map(u -> User.withUsername(u.getUsername())
                .password(u.getSenhaHash())
                .roles(u.getRole().name())
                .build())
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado ou inativo"));
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
          .csrf(csrf -> csrf.disable())
          .authorizeHttpRequests(auth -> auth
              // Públicos
              .requestMatchers("/", "/home", "/login", "/css/**", "/js/**", "/images/**").permitAll()

              // Exceções: qualquer autenticado pode abrir/submeter a própria edição
              .requestMatchers("/usuarios/editar/*", "/usuarios/atualizar/*").authenticated()

              // Gestão de usuários (exceto as rotas acima): somente ADMIN
              .requestMatchers("/usuarios/**").hasRole("ADMIN")

              // Clientes: ADMIN e OPERADOR podem listar/cadastrar/editar/excluir
              .requestMatchers("/clientes/**").hasAnyRole("ADMIN","OPERADOR")

              // Produtos e Pedidos: ADMIN, OPERADOR e CLIENTE
              .requestMatchers("/produtos/**", "/pedidos/**").hasAnyRole("ADMIN","OPERADOR","CLIENTE")

              .anyRequest().authenticated()
          )
          .formLogin(form -> form
              .loginPage("/login").permitAll()
              .successHandler(firstLoginSuccessHandler) // mantém o redirecionamento condicional
          )
          .logout(logout -> logout
              .logoutUrl("/logout")
              .logoutSuccessUrl("/login?logout").permitAll()
          );

        // Filtro que força troca de senha (bypass para /senha/**, /usuarios/editar|atualizar/**, estáticos, etc.)
        http.addFilterAfter(mustChangePasswordFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
