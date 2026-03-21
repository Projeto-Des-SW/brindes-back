package br.ed.ufape.bahiabrindes.dto.produto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
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
public class ProdutoRequestDTO {

    @NotBlank(message = "O nome é obrigatório")
    private String nome;

    private String descricao;

    private String sku;

    @PositiveOrZero(message = "O preço de venda não pode ser negativo")
    private BigDecimal precoVenda;

    @PositiveOrZero(message = "O custo de produção não pode ser negativo")
    private BigDecimal custoProducao;

    private Long categoriaId;

    @PositiveOrZero(message = "O estoque mínimo não pode ser negativo")
    private Integer estoqueMinimo;

    @PositiveOrZero(message = "A quantidade disponível não pode ser negativa")
    private Integer estoqueAtual;

    private String status;

    private String condicoesPagamento;

    @NotBlank(message = "O prazo de produção é obrigatório")
    private String prazoProducao;

    private String observacoes;

    @Builder.Default
    private List<ItemFichaTecnicaRequestDTO> itensFichaTecnica = new ArrayList<>();

    @Valid
    @NotEmpty(message = "É obrigatório informar ao menos uma imagem")
    @Size(max = 4, message = "O produto pode ter no máximo 4 imagens")
    @Builder.Default
    private List<ProdutoImagemRequest> imagens = new ArrayList<>();
}
