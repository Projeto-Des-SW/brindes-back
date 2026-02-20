package br.ed.ufape.bahiabrindes.dto.estoque;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MateriaPrimaRequest {

    @NotBlank(message = "codigo é obrigatório")
    private String codigo;

    @NotBlank(message = "descricao é obrigatória")
    private String descricao;

    private String unidade;

    /**
     * Categoria pode ser enviada por nome (ex.: "Papel") para manter compatibilidade com o front.
     */
    private String categoria;

    private Long categoriaId;

    private Long fornecedorPrincipalId;

    private BigDecimal estoqueMinimo;
}

