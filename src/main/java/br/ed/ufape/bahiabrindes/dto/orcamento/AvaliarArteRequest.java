package br.ed.ufape.bahiabrindes.dto.orcamento;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvaliarArteRequest {
    /** "APROVAR" ou "SOLICITAR_AJUSTE" */
    private String acao;
    /** Comentário opcional do cliente */
    private String comentario;
}
