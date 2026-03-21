package br.ed.ufape.bahiabrindes.controller;

import br.ed.ufape.bahiabrindes.dto.common.PageResponse;
import br.ed.ufape.bahiabrindes.dto.orcamento.AdminOrcamentoListItemDTO;
import br.ed.ufape.bahiabrindes.dto.orcamento.AtualizarPagamentoRequest;
import br.ed.ufape.bahiabrindes.dto.orcamento.CriarOrcamentoAdminRequest;
import br.ed.ufape.bahiabrindes.dto.orcamento.CriarOrcamentoRequest;
import br.ed.ufape.bahiabrindes.dto.orcamento.MeusOrcamentosItemResponseDTO;
import br.ed.ufape.bahiabrindes.dto.orcamento.OrcamentoDetalheResponseDTO;
import br.ed.ufape.bahiabrindes.model.entity.Cliente;
import br.ed.ufape.bahiabrindes.model.enums.StatusOrcamento;
import br.ed.ufape.bahiabrindes.repository.ClienteRepository;
import br.ed.ufape.bahiabrindes.service.OrcamentoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orcamentos")
public class OrcamentoController {

    private final OrcamentoService orcamentoService;
    private final ClienteRepository clienteRepository;

    @Autowired
    public OrcamentoController(OrcamentoService orcamentoService, ClienteRepository clienteRepository) {
        this.orcamentoService = orcamentoService;
        this.clienteRepository = clienteRepository;
    }

    // ── Endpoints do cliente ──────────────────────────────────────────────────

    @GetMapping("/meus")
    public ResponseEntity<PageResponse<MeusOrcamentosItemResponseDTO>> listarMeus(
            Authentication authentication,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize
    ) {
        Long clienteId = getClienteIdFromAuth(authentication);
        return ResponseEntity.ok(orcamentoService.listarMeusOrcamentos(clienteId, page, pageSize));
    }

    @GetMapping("/meus/{id}")
    public ResponseEntity<OrcamentoDetalheResponseDTO> detalharMeuOrcamento(
            Authentication authentication,
            @PathVariable Long id
    ) {
        Long clienteId = getClienteIdFromAuth(authentication);
        return ResponseEntity.ok(orcamentoService.buscarDetalhe(clienteId, id));
    }

    @PostMapping
    public ResponseEntity<OrcamentoDetalheResponseDTO> criar(
            Authentication authentication,
            @Valid @RequestBody CriarOrcamentoRequest request
    ) {
        Long clienteId = getClienteIdFromAuth(authentication);
        return ResponseEntity.ok(orcamentoService.criarOrcamento(clienteId, request));
    }

    // ── Endpoints administrativos ─────────────────────────────────────────────

    @PostMapping("/admin")
        public ResponseEntity<OrcamentoDetalheResponseDTO> criarAdmin(
                org.springframework.security.core.Authentication authentication, // 1. Recebe o crachá de quem está logado
                @Valid @RequestBody CriarOrcamentoAdminRequest request
        ) {
            // 2. Extrai o e-mail do funcionário
            String emailFuncionario = (authentication != null && authentication.getName() != null) 
                                    ? authentication.getName() 
                                    : "Sistema";

            // 3. Envia OS DOIS parâmetros para o Serviço (o request e o e-mail)
            return ResponseEntity.ok(orcamentoService.criarOrcamentoAdmin(request, emailFuncionario));
        }

    /**
     * Lista todos os orçamentos (acesso restrito a FUNCIONARIO/ADMIN).
     * Ex: GET /api/orcamentos/admin?page=1&pageSize=20&status=ORCAMENTO_SOLICITADO
     */
    @GetMapping("/admin")
    public ResponseEntity<PageResponse<AdminOrcamentoListItemDTO>> listarTodos(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(name = "status", required = false) String status
    ) {
        return ResponseEntity.ok(orcamentoService.listarTodos(page, pageSize, status));
    }

    /**
     * Detalha um orçamento específico para o admin.
     * Ex: GET /api/orcamentos/admin/42
     */
    @GetMapping("/admin/{id}")
    public ResponseEntity<OrcamentoDetalheResponseDTO> detalharAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(orcamentoService.buscarDetalheAdmin(id));
    }

    /**
     * Atualiza método de pagamento e valor pago de um orçamento.
     * Ex: PATCH /api/orcamentos/{id}/pagamento
     */
    @PatchMapping("/{id}/pagamento")
    public ResponseEntity<OrcamentoDetalheResponseDTO> atualizarPagamento(
            @PathVariable Long id,
            @RequestBody AtualizarPagamentoRequest request
    ) {
        return ResponseEntity.ok(orcamentoService.atualizarPagamento(id, request.getMetodoPagamento(), request.getValorPago()));
    }

    /**
     * Atualiza o status de um orçamento e registra automaticamente no histórico.
     * Ex: PATCH /api/orcamentos/{id}/status?novoStatus=ARTE_PENDENTE&responsavel=João
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrcamentoDetalheResponseDTO> atualizarStatus(
            @PathVariable Long id,
            @RequestParam String novoStatus,
            @RequestParam(required = false, defaultValue = "Sistema") String responsavel
    ) {
        StatusOrcamento status;
        try {
            status = StatusOrcamento.valueOf(novoStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(orcamentoService.atualizarStatus(id, status, responsavel));
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private Long getClienteIdFromAuth(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Usuário não autenticado");
        }
        String email = authentication.getName();
        Cliente cliente = clienteRepository.findByEmailAndAtivoTrue(email)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));
        return cliente.getId();
    }
}
