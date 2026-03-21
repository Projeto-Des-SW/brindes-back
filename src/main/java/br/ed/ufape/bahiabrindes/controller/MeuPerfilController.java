package br.ed.ufape.bahiabrindes.controller;

import br.ed.ufape.bahiabrindes.dto.funcionarios.FuncionarioMeRequest;
import br.ed.ufape.bahiabrindes.dto.funcionarios.FuncionarioResponse;
import br.ed.ufape.bahiabrindes.service.FuncionarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/meu-perfil")
public class MeuPerfilController {

    private final FuncionarioService funcionarioService;

    @Autowired
    public MeuPerfilController(FuncionarioService funcionarioService) {
        this.funcionarioService = funcionarioService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('FUNCIONARIO','ADMIN')")
    public ResponseEntity<FuncionarioResponse> buscarMe() {
        return ResponseEntity.ok(funcionarioService.buscarMe());
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('FUNCIONARIO','ADMIN')")
    public ResponseEntity<FuncionarioResponse> atualizarMe(@Valid @RequestBody FuncionarioMeRequest request) {
        return ResponseEntity.ok(funcionarioService.atualizarMe(request));
    }
}
