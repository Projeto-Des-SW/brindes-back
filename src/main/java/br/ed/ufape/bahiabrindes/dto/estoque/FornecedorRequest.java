package br.ed.ufape.bahiabrindes.dto.estoque;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FornecedorRequest {

    @NotBlank(message = "nome é obrigatório")
    private String nome;

    private String cnpj;
    private String telefone;
    private String email;
    private String prazoEntrega;

    /**
     * ATIVO | INATIVO
     */
    private String status;

    private String condicoesPagamento;
    private String observacoes;
    private EnderecoRequest endereco;
}

