package br.ed.ufape.bahiabrindes.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "materias_primas_produtos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MateriaPrimaProduto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "materia_prima_id", nullable = false)
    private MateriaPrima materiaPrima;

    @Column(name = "quantidade_necessaria", precision = 19, scale = 4, nullable = false)
    private BigDecimal quantidadeNecessaria;

    @Column(name = "custo_calculado", precision = 19, scale = 4)
    private BigDecimal custoCalculado;
}
