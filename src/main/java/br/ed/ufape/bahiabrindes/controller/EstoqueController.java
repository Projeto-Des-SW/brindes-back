package br.ed.ufape.bahiabrindes.controller;

import br.ed.ufape.bahiabrindes.dto.common.PageResponse;
import br.ed.ufape.bahiabrindes.dto.estoque.*;
import br.ed.ufape.bahiabrindes.service.EstoqueService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/estoque")
public class EstoqueController {

    private final EstoqueService estoqueService;

    @Autowired
    public EstoqueController(EstoqueService estoqueService) {
        this.estoqueService = estoqueService;
    }

    @GetMapping("/resumo")
    public ResponseEntity<EstoqueResumoResponse> resumo() {
        return ResponseEntity.ok(estoqueService.resumo());
    }

    @GetMapping("/itens")
    public ResponseEntity<PageResponse<ProdutoEstoqueItemResponse>> itens(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ResponseEntity.ok(estoqueService.itens(search, status, page, pageSize));
    }

    @GetMapping("/movimentacoes")
    public ResponseEntity<PageResponse<MovimentacaoResponse>> movimentacoes(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ResponseEntity.ok(estoqueService.movimentacoes(search, tipo, from, to, page, pageSize));
    }

    @PostMapping("/movimentacoes")
    public ResponseEntity<MovimentacaoResponse> criarMovimentacao(
            @Valid @RequestBody CriarMovimentacaoRequest request,
            Authentication authentication
    ) {
        String email = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(estoqueService.criarMovimentacao(request, email));
    }
}

