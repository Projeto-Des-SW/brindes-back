package br.ed.ufape.bahiabrindes.dto.orcamento;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArteOrcamentoDTO {

    private Long id;

    /** Nome do produto relacionado à arte */
    private String produtoNome;

    /** URL da imagem da arte (para compatibilidade) */
    private String imagemUrl;

    /** Nome original do arquivo com extensão (ex: logo.png) */
    private String nomeArquivo;

    /** Imagem em base64 para renderizar no frontend */
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String imagemData;

    /** Status: PENDENTE | APROVADA | AJUSTE_SOLICITADO */
    private String status;

    /** Data de envio formatada (ex: "12/03/2026") */
    private String enviadoEm;
}
