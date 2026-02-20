package br.ed.ufape.bahiabrindes.dto.estoque;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocalEstoqueRequest {
    @NotBlank(message = "nome é obrigatório")
    private String nome;
    private String descricao;
}

