package br.ed.ufape.bahiabrindes.dto.funcionarios;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FuncionarioUpdateRequest {

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    private String email;

    private String senha; // optional – only updated when non-blank

    private Set<String> perfis;
}
