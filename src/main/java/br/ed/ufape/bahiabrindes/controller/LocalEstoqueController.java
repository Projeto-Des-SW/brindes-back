package br.ed.ufape.bahiabrindes.controller;

import br.ed.ufape.bahiabrindes.dto.common.PageResponse;
import br.ed.ufape.bahiabrindes.dto.estoque.LocalEstoqueRequest;
import br.ed.ufape.bahiabrindes.dto.estoque.LocalEstoqueResponse;
import br.ed.ufape.bahiabrindes.service.LocalEstoqueService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/estoque/locais")
public class LocalEstoqueController {

    private final LocalEstoqueService localEstoqueService;

    @Autowired
    public LocalEstoqueController(LocalEstoqueService localEstoqueService) {
        this.localEstoqueService = localEstoqueService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<LocalEstoqueResponse>> listar(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ResponseEntity.ok(localEstoqueService.listar(search, page, pageSize));
    }

    @PostMapping
    public ResponseEntity<LocalEstoqueResponse> criar(@Valid @RequestBody LocalEstoqueRequest request) {
        return ResponseEntity.ok(localEstoqueService.criar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LocalEstoqueResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody LocalEstoqueRequest request
    ) {
        return ResponseEntity.ok(localEstoqueService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        localEstoqueService.remover(id);
        return ResponseEntity.noContent().build();
    }
}

