package br.com.aweb.sistema_vendas.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagamentos",
       uniqueConstraints = @UniqueConstraint(name = "uk_pagamento_pedido", columnNames = "pedido_id"))
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pagamento {

    private static final int SCALE = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false, unique = true)
    private Pedido pedido;

    /** coluna no BD: forma_pagamento */
    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", nullable = false, length = 30)
    private FormaPagamento forma;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusPagamento status; // PENDENTE, APROVADO, RECUSADO, CANCELADO

    // --- Snapshot financeiro do pedido no momento do pagamento ---
    @NotNull
    @Column(name = "total_produtos", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalProdutos;

    @NotNull
    @Column(name = "desconto", nullable = false, precision = 15, scale = 2)
    private BigDecimal desconto;

    @NotNull
    @Column(name = "frete", nullable = false, precision = 15, scale = 2)
    private BigDecimal frete;

    @NotNull
    @Column(name = "valor_final", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorFinal;

    @Column(name = "referencia_externa", length = 120)
    private String referenciaExterna;

    @Builder.Default
    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "detalhes", length = 2000)
    private String detalhes;

    /** ⭐ novo mapeamento: coluna NOT NULL no BD */
    @Builder.Default
    @Column(name = "pago", nullable = false)
    private Boolean pago = Boolean.FALSE;

    /** Copia (fotografa) os totais do Pedido atual para o Pagamento */
    public void snapshotFrom(Pedido pedido) {
        this.pedido = pedido;
        this.totalProdutos = safe(pedido.getTotalProdutos());
        this.desconto      = safe(pedido.getDesconto());
        this.frete         = safe(pedido.getFrete());
        this.valorFinal    = safe(pedido.getValorTotal());
        normalizeScale();
    }

    private BigDecimal safe(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v);
    }

    public void normalizeScale() {
        this.totalProdutos = this.totalProdutos.setScale(SCALE, RoundingMode.HALF_UP);
        this.desconto      = this.desconto.setScale(SCALE, RoundingMode.HALF_UP);
        this.frete         = this.frete.setScale(SCALE, RoundingMode.HALF_UP);
        this.valorFinal    = this.valorFinal.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /** Garante NOT NULLs e consistência antes do INSERT */
    @PrePersist
    public void prePersist() {
        if (criadoEm == null) criadoEm = LocalDateTime.now();
        if (status == null)   status = StatusPagamento.PENDENTE;
        if (forma == null)    throw new IllegalStateException("Forma de pagamento obrigatória");

        if (totalProdutos == null) totalProdutos = BigDecimal.ZERO;
        if (desconto == null)      desconto = BigDecimal.ZERO;
        if (frete == null)         frete = BigDecimal.ZERO;
        if (valorFinal == null)    valorFinal = BigDecimal.ZERO;

        if (pago == null)          pago = Boolean.FALSE;

        normalizeScale();
    }
}
