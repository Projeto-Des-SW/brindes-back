package br.ed.ufape.bahiabrindes.dto.orcamento;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CriarOrcamentoAdminRequest {

    @NotEmpty(message = "O orçamento deve conter ao menos um item")
    @Valid
    private List<CriarOrcamentoItemRequest> itens;

    private String observacoes;

    // campos exclusivos para quando o Admin regista uma venda
    private String nomeCliente;
    private String emailCliente;
    private String telefoneCliente;
}