package br.com.aweb.sistema_vendas.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.validator.constraints.br.CPF;

@Entity
@Table(name = "clientes",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_cliente_email", columnNames = "email"),
           @UniqueConstraint(name = "uk_cliente_cpf", columnNames = "cpf")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nome completo
    @NotBlank(message = "O nome é obrigatório")
    @Size(max = 120, message = "O nome não pode ultrapassar 120 caracteres")
    private String nome;

    // E-mail (único)
    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "E-mail inválido")
    @Size(max = 120, message = "O e-mail não pode ultrapassar 120 caracteres")
    private String email;

    // CPF (único)
    @NotBlank(message = "O CPF é obrigatório")
    @CPF(message = "CPF inválido")
    @Column(length = 14, nullable = false)
    private String cpf;

    // Telefone
    @NotBlank(message = "O telefone é obrigatório")
    @Size(max = 20, message = "O telefone não pode ultrapassar 20 caracteres")
    private String telefone;

    // Endereço
    @NotBlank(message = "O logradouro é obrigatório")
    @Size(max = 120)
    private String logradouro;

    @Size(max = 10)
    private String numero; // opcional

    @Size(max = 60)
    private String complemento; // opcional

    @NotBlank(message = "O bairro é obrigatório")
    @Size(max = 80)
    private String bairro;

    @NotBlank(message = "A cidade é obrigatória")
    @Size(max = 80)
    private String cidade;

    @NotBlank(message = "A UF é obrigatória")
    @Size(min = 2, max = 2, message = "A UF deve ter 2 caracteres")
    private String uf;

    @NotBlank(message = "O CEP é obrigatório")
    @Pattern(regexp = "\\d{5}-?\\d{3}", message = "CEP inválido")
    private String cep;
}
