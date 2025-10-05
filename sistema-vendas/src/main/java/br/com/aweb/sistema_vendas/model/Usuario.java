package br.com.aweb.sistema_vendas.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "usuarios",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_usuario_username", columnNames = "username"),
           @UniqueConstraint(name = "uk_usuario_email", columnNames = "email")
       })
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Usuario {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Usuário é obrigatório")
    @Size(max = 60)
    @Column(nullable = false, length = 60)
    private String username;

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    @Column(nullable = false)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UsuarioRole role;

    @Builder.Default
    @Column(nullable = false)
    private boolean ativo = true;
}
