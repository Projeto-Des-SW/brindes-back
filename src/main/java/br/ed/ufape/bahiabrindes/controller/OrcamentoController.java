package br.ed.ufape.bahiabrindes.controller;

import br.ed.ufape.bahiabrindes.dto.avaliacao.AvaliacaoProdutoDTO;
import br.ed.ufape.bahiabrindes.dto.avaliacao.CriarAvaliacaoRequest;
import br.ed.ufape.bahiabrindes.dto.common.PageResponse;
import br.ed.ufape.bahiabrindes.dto.orcamento.AdicionarComentarioRequest;
import br.ed.ufape.bahiabrindes.dto.orcamento.AdminAvaliarArteRequest;
import br.ed.ufape.bahiabrindes.dto.orcamento.AdminOrcamentoListItemDTO;
import br.ed.ufape.bahiabrindes.dto.orcamento.ArteOrcamentoDTO;
import br.ed.ufape.bahiabrindes.dto.orcamento.AtualizarItemDescontoRequest;
import br.ed.ufape.bahiabrindes.dto.orcamento.AtualizarPagamentoRequest;
import br.ed.ufape.bahiabrindes.dto.orcamento.AvaliarArteRequest;
import br.ed.ufape.bahiabrindes.dto.orcamento.CriarOrcamentoAdminRequest;
import br.ed.ufape.bahiabrindes.dto.orcamento.CriarOrcamentoRequest;
import br.ed.ufape.bahiabrindes.dto.orcamento.MeusOrcamentosItemResponseDTO;
import br.ed.ufape.bahiabrindes.dto.orcamento.OrcamentoDetalheResponseDTO;
import br.ed.ufape.bahiabrindes.model.entity.Cliente;
import br.ed.ufape.bahiabrindes.model.enums.StatusOrcamento;
import br.ed.ufape.bahiabrindes.repository.ClienteRepository;
import br.ed.ufape.bahiabrindes.repository.FuncionarioRepository;
import br.ed.ufape.bahiabrindes.service.AvaliacaoService;
import br.ed.ufape.bahiabrindes.service.OrcamentoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/orcamentos")
public class OrcamentoController {

    private final OrcamentoService orcamentoService;
    private final ClienteRepository clienteRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final AvaliacaoService avaliacaoService;

    @Autowired
    public OrcamentoController(OrcamentoService orcamentoService, ClienteRepository clienteRepository, FuncionarioRepository funcionarioRepository, AvaliacaoService avaliacaoService) {
        this.orcamentoService = orcamentoService;
        this.clienteRepository = clienteRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.avaliacaoService = avaliacaoService;
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

    /**
     * Cliente avalia uma arte (APROVAR ou SOLICITAR_AJUSTE).
     * Se todas aprovadas → status muda para ARTES_APROVADAS automaticamente.
     */
    @PostMapping("/meus/{orcamentoId}/artes/{arteId}/avaliar")
    public ResponseEntity<OrcamentoDetalheResponseDTO> avaliarArte(
            Authentication authentication,
            @PathVariable Long orcamentoId,
            @PathVariable Long arteId,
            @RequestBody AvaliarArteRequest request
    ) {
        Long clienteId = getClienteIdFromAuth(authentication);
        return ResponseEntity.ok(orcamentoService.avaliarArte(clienteId, orcamentoId, arteId, request.getAcao(), request.getComentario()));
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
            return ResponseEntity.ok(orcamentoService.criarOrcamentoAdmin(request, getNomeFuncionario(authentication)));
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
     * Atualiza o desconto de um item de orçamento.
     * Ex: PATCH /api/orcamentos/admin/{orcamentoId}/itens/{itemId}
     */
    @PatchMapping("/admin/{orcamentoId}/itens/{itemId}")
    public ResponseEntity<OrcamentoDetalheResponseDTO> atualizarDescontoItem(
            @PathVariable Long orcamentoId,
            @PathVariable Long itemId,
            @RequestBody AtualizarItemDescontoRequest request
    ) {
        return ResponseEntity.ok(orcamentoService.atualizarDescontoItem(orcamentoId, itemId, request.getDesconto()));
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

    /**
     * Upload de arte para um produto de um orçamento.
     * Ex: POST /api/orcamentos/42/artes?produtoNome=Caneta
     */
    @PostMapping("/{id}/artes")
    public ResponseEntity<ArteOrcamentoDTO> uploadArte(
            @PathVariable Long id,
            @RequestParam String produtoNome,
            @RequestParam MultipartFile arquivo) throws IOException {
        return ResponseEntity.ok(orcamentoService.uploadArte(id, produtoNome, arquivo));
    }

    /**
     * Notifica o cliente sobre o status atual do pedido por e-mail.
     * Ex: POST /api/orcamentos/42/notificar/status
     */
    @PostMapping("/{id}/notificar/status")
    public ResponseEntity<Void> notificarStatus(@PathVariable Long id) {
        orcamentoService.notificarStatus(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Notifica o cliente sobre nova arte disponível para aprovação.
     * Ex: POST /api/orcamentos/42/notificar/arte
     */
    @PostMapping("/{id}/notificar/arte")
    public ResponseEntity<Void> notificarArte(@PathVariable Long id) {
        orcamentoService.notificarArte(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Download do arquivo original da arte.
     * Ex: GET /api/orcamentos/artes-download/123
     * Retorna o arquivo binário com o nome original.
     */
    @GetMapping("/artes-download/{arteId}")
    public ResponseEntity<byte[]> downloadArte(@PathVariable Long arteId) {
        byte[] conteudo = orcamentoService.obterConteudoArte(arteId);
        String nomeArquivo = orcamentoService.obterNomeArquivoArte(arteId);
        
        // Detectar tipo MIME baseado na extensão
        String mediaType = detectarMediaType(nomeArquivo);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeArquivo + "\"")
                .header(HttpHeaders.CONTENT_TYPE, mediaType)
                .body(conteudo);
    }

    /**
     * Download do arquivo original da arte.
     * Ex: GET /api/orcamentos/artes/123/download
     * Retorna o arquivo binário com o nome original.
     */
    @GetMapping("/artes/{arteId}/download")
    public ResponseEntity<byte[]> downloadArteById(@PathVariable Long arteId) {
        byte[] conteudo = orcamentoService.obterConteudoArte(arteId);
        String nomeArquivo = orcamentoService.obterNomeArquivoArte(arteId);
        
        // Detectar tipo MIME baseado na extensão
        String mediaType = detectarMediaType(nomeArquivo);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeArquivo + "\"")
                .header(HttpHeaders.CONTENT_TYPE, mediaType)
                .body(conteudo);
    }

    /**
     * Visualizar/renderizar a imagem da arte no navegador.
     * Ex: GET /api/orcamentos/artes/123/visualizar
     * Retorna a imagem com o tipo MIME correto para ser exibida inline.
     */
    @GetMapping("/artes/{arteId}/visualizar")
    public ResponseEntity<byte[]> visualizarArte(@PathVariable Long arteId) {
        byte[] conteudo = orcamentoService.obterConteudoArte(arteId);
        String nomeArquivo = orcamentoService.obterNomeArquivoArte(arteId);
        
        // Detectar tipo MIME baseado na extensão
        String mediaType = detectarMediaType(nomeArquivo);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, mediaType)
                .header(HttpHeaders.CACHE_CONTROL, "max-age=86400") // Cache por 24h
                .body(conteudo);
    }

    /**
     * Detecta o tipo MIME baseado na extensão do arquivo
     */
    private String detectarMediaType(String nomeArquivo) {
        if (nomeArquivo == null) return "application/octet-stream";
        
        String extensao = nomeArquivo.toLowerCase();
        if (extensao.endsWith(".png")) return "image/png";
        if (extensao.endsWith(".jpg") || extensao.endsWith(".jpeg")) return "image/jpeg";
        if (extensao.endsWith(".gif")) return "image/gif";
        if (extensao.endsWith(".webp")) return "image/webp";
        if (extensao.endsWith(".svg")) return "image/svg+xml";
        if (extensao.endsWith(".bmp")) return "image/bmp";
        if (extensao.endsWith(".pdf")) return "application/pdf";
        if (extensao.endsWith(".zip")) return "application/zip";
        
        return "application/octet-stream"; // fallback
    }

    /**
     * Admin altera o status de uma arte específica.
     * Ex: PATCH /api/orcamentos/admin/{orcamentoId}/artes/{arteId}/status
     */
    @PatchMapping("/admin/{orcamentoId}/artes/{arteId}/status")
    public ResponseEntity<OrcamentoDetalheResponseDTO> avaliarArteAdmin(
            Authentication authentication,
            @PathVariable Long orcamentoId,
            @PathVariable Long arteId,
            @RequestBody AdminAvaliarArteRequest request
    ) {
        return ResponseEntity.ok(orcamentoService.avaliarArteAdmin(orcamentoId, arteId, request.getNovoStatus(), request.getComentario(), getNomeFuncionario(authentication)));
    }

    /**
     * Admin adiciona um comentário a um orçamento (geral ou por produto).
     * Ex: POST /api/orcamentos/admin/{orcamentoId}/comentario
     */
    @PostMapping("/admin/{orcamentoId}/comentario")
    public ResponseEntity<OrcamentoDetalheResponseDTO> adicionarComentario(
            Authentication authentication,
            @PathVariable Long orcamentoId,
            @RequestBody AdicionarComentarioRequest request
    ) {
        return ResponseEntity.ok(orcamentoService.adicionarComentario(orcamentoId, request.getMensagem(), request.getProdutoNome(), getNomeFuncionario(authentication)));
    }

    @PostMapping("/meus/{orcamentoId}/avaliar")
    public ResponseEntity<AvaliacaoProdutoDTO> avaliarProduto(
            Authentication authentication,
            @PathVariable Long orcamentoId,
            @RequestBody CriarAvaliacaoRequest request
    ) {
        Long clienteId = getClienteIdFromAuth(authentication);
        return ResponseEntity.ok(avaliacaoService.criar(clienteId, orcamentoId, request));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String getNomeFuncionario(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) return "Sistema";
        String email = authentication.getName();
        return funcionarioRepository.findByEmail(email)
                .map(f -> f.getNome())
                .orElse(email);
    }

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
