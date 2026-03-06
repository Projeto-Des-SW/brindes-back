package br.ed.ufape.bahiabrindes.dto.clientes;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteUpdateRequest {

    private String nome;

    @Email(message = "Email inválido")
    private String email;

    private String senha;

    @Pattern(regexp = "\\d{11}", message = "CPF deve conter exatamente 11 dígitos numéricos (sem pontos ou hífen)")
    private String documento;

    @Pattern(regexp = "\\d{10,11}", message = "Telefone deve conter entre 10 e 11 dígitos numéricos (sem espaços, pontos ou hífen)")
    private String telefone;

    private String endereco;
    private String segmentacao;
}