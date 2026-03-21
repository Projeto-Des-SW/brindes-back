package br.ed.ufape.bahiabrindes.dto.orcamento;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminAvaliarArteRequest {
    private String novoStatus; // PENDENTE | APROVADA | AJUSTE_SOLICITADO
    private String comentario;
}
