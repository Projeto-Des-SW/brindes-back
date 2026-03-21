package br.ed.ufape.bahiabrindes.dto.avaliacao;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvaliacaoProdutoDTO {
    private Long id;
    private String nomeCliente;
    private Integer nota;
    private String comentario;
    private String criadoEm;
}
