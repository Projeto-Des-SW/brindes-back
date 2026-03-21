package br.ed.ufape.bahiabrindes.dto.avaliacao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CriarAvaliacaoRequest {
    private Long produtoId;
    private Integer nota;
    private String comentario;
}
