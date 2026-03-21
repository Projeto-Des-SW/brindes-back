package br.ed.ufape.bahiabrindes.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "orcamento_itens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrcamentoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orcamento_id", nullable = false)
    private Orcamento orcamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(name = "quantidade", nullable = false)
    private Integer quantidade;

    @Column(name = "cor")
    private String cor;

    @Column(name = "variacao")
    private String variacao;

    @Column(name = "preco_unitario", precision = 19, scale = 4, nullable = false)
    private BigDecimal precoUnitario;

    @Column(name = "preco_total", precision = 19, scale = 4, nullable = false)
    private BigDecimal precoTotal;

    @Column(name = "desconto", precision = 19, scale = 4)
    private BigDecimal desconto;

    @Column(name = "imagem_url")
    private String imagemUrl;
}

