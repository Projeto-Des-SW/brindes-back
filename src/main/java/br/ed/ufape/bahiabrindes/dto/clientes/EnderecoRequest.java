package br.ed.ufape.bahiabrindes.dto.clientes;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnderecoRequest {
    private String rua;
    private String numero;
    private String cep;
    private String cidade;
    private String estado;
}

