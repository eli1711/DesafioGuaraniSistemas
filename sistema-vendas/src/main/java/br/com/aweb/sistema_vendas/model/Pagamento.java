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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FormaPagamento forma;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPagamento status; // PENDENTE, APROVADO, RECUSADO, CANCELADO

    // --- Snapshot financeiro do pedido no momento do pagamento ---
    @NotNull
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalProdutos;

    @NotNull
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal desconto;

    @NotNull
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal frete;

    @NotNull
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valorFinal;

    // Dados auxiliares
    @Column(length = 120)
    private String referenciaExterna;

    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(length = 2000)
    private String detalhes;

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
}
