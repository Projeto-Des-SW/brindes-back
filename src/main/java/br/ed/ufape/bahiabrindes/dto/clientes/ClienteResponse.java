package br.ed.ufape.bahiabrindes.dto.clientes;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteResponse {
    private Long id;
    private String nome;
    private String documento;
    private String email;
    private String telefone;
    private String endereco;
    private String segmentacao;
    private LocalDateTime criadoEm;
}