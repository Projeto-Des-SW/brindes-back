package br.ed.ufape.bahiabrindes.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "materias_primas_estoque")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MateriaPrimaEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fornecedor_id")
    private Fornecedor fornecedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "materias_primas_id", nullable = false)
    private MateriaPrima materiaPrima;

    @Column(name = "preco_custo", precision = 19, scale = 2)
    private BigDecimal precoCusto;

    @Column(name = "estoque_atual", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal estoqueAtual = BigDecimal.ZERO;
}

