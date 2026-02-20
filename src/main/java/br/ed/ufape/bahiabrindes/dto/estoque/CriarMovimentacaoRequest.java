package br.ed.ufape.bahiabrindes.dto.estoque;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CriarMovimentacaoRequest {

    @NotBlank(message = "Tipo é obrigatório (Entrada/Saída)")
    private String tipo;

    @NotNull(message = "materiaPrimaId é obrigatório")
    private Long materiaPrimaId;

    @NotNull(message = "quantidade é obrigatória")
    private BigDecimal quantidade;

    private Long fornecedorId;
    private Long destinoId; // local de estoque
    private BigDecimal valorUnitario;

    private String data;

    private String motivo;
}

