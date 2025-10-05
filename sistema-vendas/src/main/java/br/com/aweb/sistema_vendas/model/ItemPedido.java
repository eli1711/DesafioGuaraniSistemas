package br.com.aweb.sistema_vendas.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Entity
@Table(name = "itens_pedido")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemPedido {

    private static final int SCALE = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Concorrência (opcional, mas recomendado)
    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    @NotNull
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    @NotNull
    private Produto produto;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer quantidade;

    @NotNull
    @Column(name = "preco_unitario", nullable = false, precision = 15, scale = 2)
    private BigDecimal precoUnitario;

    /** Subtotal = precoUnitario * quantidade */
    @Transient
    public BigDecimal getSubtotal() {
        if (precoUnitario == null || quantidade == null) return BigDecimal.ZERO.setScale(SCALE);
        return precoUnitario
                .multiply(BigDecimal.valueOf(quantidade))
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    /** Construtor prático garantindo preco do produto e quantidade válida */
    public ItemPedido(Pedido pedido, Produto produto, Integer quantidade) {
        this.pedido = Objects.requireNonNull(pedido, "pedido é obrigatório");
        this.produto = Objects.requireNonNull(produto, "produto é obrigatório");
        setQuantidade(quantidade);
        setPrecoUnitario(produto.getPreco()); // copia o preço vigente do produto
    }

    /** Garante quantidade mínima e evita null */
    public void setQuantidade(Integer quantidade) {
        if (quantidade == null || quantidade < 1) {
            throw new IllegalArgumentException("Quantidade deve ser >= 1");
        }
        this.quantidade = quantidade;
    }

    /** Normaliza preço com 2 casas decimais */
    public void setPrecoUnitario(BigDecimal precoUnitario) {
        if (precoUnitario == null) throw new IllegalArgumentException("Preço unitário é obrigatório");
        if (precoUnitario.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Preço unitário não pode ser negativo");
        }
        this.precoUnitario = precoUnitario.setScale(SCALE, RoundingMode.HALF_UP);
    }

    @PrePersist @PreUpdate
    private void validate() {
        if (pedido == null) throw new IllegalStateException("Item sem pedido");
        if (produto == null) throw new IllegalStateException("Item sem produto");
        if (quantidade == null || quantidade < 1) throw new IllegalStateException("Quantidade inválida");
        if (precoUnitario == null || precoUnitario.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Preço unitário inválido");
        }
    }

    // equals/hashCode por id (entidade)
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemPedido that)) return false;
        return id != null && id.equals(that.id);
    }
    @Override public int hashCode() { return 31; }
}
