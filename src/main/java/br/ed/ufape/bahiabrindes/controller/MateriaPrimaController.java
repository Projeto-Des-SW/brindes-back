package br.ed.ufape.bahiabrindes.controller;

import br.ed.ufape.bahiabrindes.dto.common.PageResponse;
import br.ed.ufape.bahiabrindes.dto.estoque.MateriaPrimaRequest;
import br.ed.ufape.bahiabrindes.dto.estoque.MateriaPrimaResponse;
import br.ed.ufape.bahiabrindes.service.MateriaPrimaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/estoque/materias-primas")
public class MateriaPrimaController {

    private final MateriaPrimaService materiaPrimaService;

    @Autowired
    public MateriaPrimaController(MateriaPrimaService materiaPrimaService) {
        this.materiaPrimaService = materiaPrimaService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<MateriaPrimaResponse>> listar(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String categoria,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ResponseEntity.ok(materiaPrimaService.listar(search, categoria, page, pageSize));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MateriaPrimaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(materiaPrimaService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<MateriaPrimaResponse> criar(@Valid @RequestBody MateriaPrimaRequest request) {
        return ResponseEntity.ok(materiaPrimaService.criar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MateriaPrimaResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody MateriaPrimaRequest request
    ) {
        return ResponseEntity.ok(materiaPrimaService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        materiaPrimaService.remover(id);
        return ResponseEntity.noContent().build();
    }
}

