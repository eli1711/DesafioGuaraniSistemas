// br/com/aweb/sistema_vendas/model/Usuario.java
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

    @NotBlank @Size(max = 60)
    @Column(nullable = false, length = 60)
    private String username;

    @NotBlank @Email @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String email;

    @NotBlank @Size(min = 6)
    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UsuarioRole role;

    @Builder.Default
    @Column(nullable = false)
    private boolean ativo = true;

    // 👇 MAPEAMENTO EXPLÍCITO DA COLUNA
    @Builder.Default
    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = false;

    // 👇 CINTA E SUSPENSÓRIO: se for CLIENTE e não setaram, força true no INSERT
    @PrePersist
    public void prePersist() {
        if (this.role == UsuarioRole.CLIENTE && !this.mustChangePassword) {
            this.mustChangePassword = true;
        }
    }
}
