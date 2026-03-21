package br.ed.ufape.bahiabrindes.model.entity;

import br.ed.ufape.bahiabrindes.model.enums.StatusOrcamento;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orcamentos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Orcamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", unique = true)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_previsao_entrega")
    private LocalDateTime dataPrevisaoEntrega;

    @Column(name = "valor_total", precision = 19, scale = 4, nullable = false)
    private BigDecimal valorTotal;

    /** Valor já pago pelo cliente (pode ser parcial) */
    @Column(name = "valor_pago", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal valorPago = BigDecimal.ZERO;

    /** Método de pagamento escolhido (PIX, BOLETO, CARTAO_CREDITO, etc.) */
    @Column(name = "metodo_pagamento", length = 30)
    private String metodoPagamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private StatusOrcamento status;

    @OneToMany(mappedBy = "orcamento", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrcamentoItem> itens = new ArrayList<>();

    @OneToMany(mappedBy = "orcamento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("data ASC")
    @Builder.Default
    private List<HistoricoStatusOrcamento> historico = new ArrayList<>();

    @OneToMany(mappedBy = "orcamento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("enviadoEm ASC")
    @Builder.Default
    private List<ArteOrcamento> artes = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String observacoes;
}
