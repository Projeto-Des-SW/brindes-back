package br.ed.ufape.bahiabrindes.dto.produto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProdutoResponseDTO {

    private Long id;
    private String nome;
    private String descricao;
    private String sku;
    private BigDecimal precoVenda;
    private BigDecimal custoProducao;
    private Long categoriaId;
    private String categoriaNome;
    private Integer estoqueAtual;
    private Integer estoqueMinimo;
    private String status;
    private String condicoesPagamento;
    private String prazoProducao;
    private String observacoes;

    @Builder.Default
    private List<ItemFichaTecnicaResponseDTO> itensFichaTecnica = new ArrayList<>();
}
