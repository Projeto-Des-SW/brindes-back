package br.ed.ufape.bahiabrindes.dto.orcamento;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdicionarComentarioRequest {
    private String mensagem;
    private String produtoNome; // opcional — null = comentário geral
}
