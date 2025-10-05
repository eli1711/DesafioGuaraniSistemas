package br.com.aweb.sistema_vendas.config;

import br.com.aweb.sistema_vendas.model.Usuario;
import br.com.aweb.sistema_vendas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

              // Exceções Web: edição do próprio usuário quando autenticado
              .requestMatchers("/usuarios/editar/*", "/usuarios/atualizar/*").authenticated()

              // Gestão de usuários (Web): somente ADMIN
              .requestMatchers("/usuarios/**").hasRole("ADMIN")

              // Clientes (Web): ADMIN e OPERADOR
              .requestMatchers("/clientes/**").hasAnyRole("ADMIN","OPERADOR")

              // Produtos (Web): GET lista/busca autenticado; demais ADMIN/OPERADOR
              .requestMatchers(HttpMethod.GET, "/produtos", "/produtos/buscar").authenticated()
              .requestMatchers("/produtos/**").hasAnyRole("ADMIN","OPERADOR")

              // Pedidos (Web): ADMIN, OPERADOR e CLIENTE
              .requestMatchers("/pedidos/**").hasAnyRole("ADMIN","OPERADOR","CLIENTE")

              // ===================== API =====================
              // Produtos API
              .requestMatchers(HttpMethod.GET, "/api/produtos/**").hasAnyRole("ADMIN","OPERADOR","CLIENTE")
              .requestMatchers("/api/produtos/**").hasAnyRole("ADMIN","OPERADOR")
              // Pedidos API
              .requestMatchers(HttpMethod.DELETE, "/api/pedidos/**").hasRole("ADMIN")
              .requestMatchers("/api/pedidos/**").hasAnyRole("ADMIN","OPERADOR","CLIENTE")
              // Swagger/OpenAPI (opcional liberar em dev)
              .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()

              .anyRequest().authenticated()
          )
          .formLogin(form -> form
              .loginPage("/login").permitAll()
              .successHandler(firstLoginSuccessHandler)
          )
          .logout(logout -> logout
              .logoutUrl("/logout")
              .logoutSuccessUrl("/login?logout").permitAll()
          );

        // Filtro de "must change password"
        http.addFilterAfter(mustChangePasswordFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
