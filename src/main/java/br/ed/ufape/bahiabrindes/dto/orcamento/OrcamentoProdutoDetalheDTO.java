package br.ed.ufape.bahiabrindes.dto.orcamento;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrcamentoProdutoDetalheDTO {

    private Long id;
    private Long produtoId;
    private String nome;
    private boolean jaAvaliado;
    private Integer notaAvaliacao;
    private String comentarioAvaliacao;
    private Integer quantidade;
    private String cor;
    private String tamanho;
    private String impressao;
    private String imagemUrl;
    private BigDecimal precoUnitario;
    private BigDecimal desconto;
    private BigDecimal precoTotal;
}
