package br.ed.ufape.bahiabrindes.dto.produto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemFichaTecnicaResponseDTO {

    private Long id;
    private Long materiaPrimaId;
    private String materiaPrimaNome;
    private String materiaPrimaUnidade;
    private BigDecimal quantidadeNecessaria;
    private BigDecimal custoCalculado;
}
