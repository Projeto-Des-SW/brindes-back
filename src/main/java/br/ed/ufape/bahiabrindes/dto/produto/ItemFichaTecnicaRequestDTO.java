package br.ed.ufape.bahiabrindes.dto.produto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemFichaTecnicaRequestDTO {

    private Long id;

    @NotNull(message = "O ID da matéria-prima é obrigatório")
    private Long materiaPrimaId;

    @NotNull(message = "A quantidade é obrigatória")
    @Positive(message = "A quantidade deve ser maior que zero")
    private BigDecimal quantidadeNecessaria;
}
