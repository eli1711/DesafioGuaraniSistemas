package br.com.aweb.sistema_vendas.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pedido {

    private static final int SCALE = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Concorrência otimista
    @Version
    private Long version;

    // Relacionamento com Cliente
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    // Status do pedido
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusPedido status = StatusPedido.ATIVO;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime dataHora = LocalDateTime.now();

    // Totais financeiros (BigDecimal!)
    @Builder.Default
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal frete = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal desconto = BigDecimal.ZERO;

    // Relacionamento 1:N com ItemPedido
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItemPedido> itens = new ArrayList<>();

    /** Construtor prático exigindo Cliente */
    public Pedido(Cliente cliente) {
        this.cliente = cliente;
        this.status = StatusPedido.ATIVO;
        this.dataHora = LocalDateTime.now();
        this.valorTotal = BigDecimal.ZERO;
        this.frete = BigDecimal.ZERO;
        this.desconto = BigDecimal.ZERO;
        this.itens = new ArrayList<>();
    }

    /** Soma dos subtotais dos itens (sem frete/desconto) */
    @Transient
    public BigDecimal getTotalProdutos() {
        return itens.stream()
                .map(i -> i.getSubtotal() == null ? BigDecimal.ZERO : i.getSubtotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    /** Adiciona item e recalcula totais */
    public void adicionarItem(ItemPedido item) {
        item.setPedido(this);
        this.itens.add(item);
        recalcularTotais();
    }

    /** Recalcula valorTotal = totalProdutos - desconto + frete (mínimo 0) */
    public void recalcularTotais() {
        BigDecimal totalProdutos = getTotalProdutos();
        BigDecimal d = (desconto == null ? BigDecimal.ZERO : desconto).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal f = (frete == null ? BigDecimal.ZERO : frete).setScale(SCALE, RoundingMode.HALF_UP);

        BigDecimal total = totalProdutos.subtract(d).add(f);
        if (total.signum() < 0) total = BigDecimal.ZERO;

        this.valorTotal = total.setScale(SCALE, RoundingMode.HALF_UP);
        this.desconto = d;
        this.frete = f;
    }

    /** Retrocompatibilidade com código antigo */
    @Deprecated
    public void calcularValorTotal() {
        recalcularTotais();
    }

    /** Normaliza defaults antes de persistir */
    @PrePersist
    public void prePersist() {
        if (this.status == null) this.status = StatusPedido.ATIVO;
        if (this.dataHora == null) this.dataHora = LocalDateTime.now();
        if (this.valorTotal == null) this.valorTotal = BigDecimal.ZERO;
        if (this.frete == null) this.frete = BigDecimal.ZERO;
        if (this.desconto == null) this.desconto = BigDecimal.ZERO;
        if (this.itens == null) this.itens = new ArrayList<>();
        // garante scale
        this.valorTotal = this.valorTotal.setScale(SCALE, RoundingMode.HALF_UP);
        this.frete = this.frete.setScale(SCALE, RoundingMode.HALF_UP);
        this.desconto = this.desconto.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
