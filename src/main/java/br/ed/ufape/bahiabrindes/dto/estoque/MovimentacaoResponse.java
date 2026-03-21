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
public class MovimentacaoResponse {
    private Long id;
    private String data; // ISO
    private String tipo; // Entrada | Saída
    private Long materiaPrimaId;
    private String materiaPrima;
    private BigDecimal quantidade;
    private Long fornecedorId;
    private String fornecedor;
    private String responsavel;
    private Long destinoId;
    private String destino;
    private BigDecimal valorUnitario;
    private String motivo;
    private BigDecimal valorTotal;
}

