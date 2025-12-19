package br.ed.ufape.bahiabrindes.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    @Builder.Default
    private String tipo = "Bearer";
    private Long id;
    private String nome;
    private String email;
    private Set<String> perfis;
}
