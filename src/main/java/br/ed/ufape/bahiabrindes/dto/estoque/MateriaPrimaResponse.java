package br.ed.ufape.bahiabrindes.dto.estoque;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MateriaPrimaResponse {
    private Long id;
    private String codigo;
    private String descricao;
    private String unidade;
    private String categoria;
    private String fornecedorPrincipal;
    private BigDecimal estoqueAtual;
    private BigDecimal estoqueMinimo;
}

