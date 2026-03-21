package br.ed.ufape.bahiabrindes.controller;

import br.ed.ufape.bahiabrindes.dto.avaliacao.AvaliacaoProdutoDTO;
import br.ed.ufape.bahiabrindes.dto.common.PageResponse;
import br.ed.ufape.bahiabrindes.dto.produto.ProdutoRequestDTO;
import br.ed.ufape.bahiabrindes.dto.produto.ProdutoResponseDTO;
import br.ed.ufape.bahiabrindes.service.AvaliacaoService;
import br.ed.ufape.bahiabrindes.service.ProdutoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;
    private final AvaliacaoService avaliacaoService;

    @Autowired
    public ProdutoController(ProdutoService produtoService, AvaliacaoService avaliacaoService) {
        this.produtoService = produtoService;
        this.avaliacaoService = avaliacaoService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<ProdutoResponseDTO>> listar(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(name = "status", required = false) String status) {
        return ResponseEntity.ok(produtoService.listar(page, pageSize, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(produtoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<ProdutoResponseDTO> criar(@Valid @RequestBody ProdutoRequestDTO request) {
        return ResponseEntity.ok(produtoService.criar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProdutoRequestDTO request) {
        return ResponseEntity.ok(produtoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        produtoService.remover(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ProdutoResponseDTO> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(produtoService.toggleStatus(id));
    }

    @GetMapping("/{id}/avaliacoes")
    public ResponseEntity<List<AvaliacaoProdutoDTO>> listarAvaliacoes(@PathVariable Long id) {
        return ResponseEntity.ok(avaliacaoService.listarPorProduto(id));
    }
}
