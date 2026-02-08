package br.ed.ufape.bahiabrindes.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "materias_primas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MateriaPrima {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * No banco chama "nome", mas no front usamos como "descricao".
     */
    @Column(name = "nome", nullable = false)
    private String nome;

    /**
     * No banco chama "sku", mas no front usamos como "codigo".
     */
    @Column(name = "sku")
    private String sku;

    @Column(name = "unidade")
    private String unidade;

    @Column(name = "estoque_minimo", precision = 19, scale = 4)
    private BigDecimal estoqueMinimo;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fornecedor_principal_id")
    private Fornecedor fornecedorPrincipal;
}

