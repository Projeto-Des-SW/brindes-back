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
public class ProdutoEstoqueItemResponse {
    private Long id;
    private String codigo;
    private String materiaPrima;
    private BigDecimal quantidadeAtual;
    private BigDecimal estoqueMinimo;
    private BigDecimal valorUnitario;
    private String status; // NORMAL | ABAIXO
}

