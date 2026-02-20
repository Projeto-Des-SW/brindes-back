package br.ed.ufape.bahiabrindes.dto.estoque;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FornecedorResponse {
    private Long id;
    private String nome;
    private String cnpj;
    private String telefone;
    private String email;
    private String prazoEntrega;
    private String status; // ATIVO | INATIVO
    private String condicoesPagamento;
    private String observacoes;
    private EnderecoResponse endereco;
}

