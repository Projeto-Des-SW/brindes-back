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
public class EstoqueResumoResponse {
    private BigDecimal valorTotalEmEstoque;
    private long itensAbaixoMinimo;
    private long totalMateriasPrimas;
    private long produtosSemMovimentacao;
}

