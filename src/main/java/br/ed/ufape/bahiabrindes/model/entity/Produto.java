package br.ed.ufape.bahiabrindes.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "produtos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "sku", unique = true)
    private String sku;

    @Column(name = "preco_venda", precision = 19, scale = 4)
    private BigDecimal precoVenda;

    @Column(name = "custo_producao", precision = 19, scale = 4)
    private BigDecimal custoProducao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Column(name = "estoque_atual")
    private Integer estoqueAtual;

    @Column(name = "estoque_minimo")
    private Integer estoqueMinimo;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "ATIVO";

    @Column(name = "condicoes_pagamento")
    private String condicoesPagamento;

    @Column(name = "prazo_producao", length = 100)
    private String prazoProducao;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

    @OneToMany(mappedBy = "produto", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MateriaPrimaProduto> itensFichaTecnica = new ArrayList<>();
}
