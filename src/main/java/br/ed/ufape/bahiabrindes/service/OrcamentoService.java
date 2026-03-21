package br.ed.ufape.bahiabrindes.service;

import br.ed.ufape.bahiabrindes.dto.common.PageResponse;
import br.ed.ufape.bahiabrindes.dto.orcamento.*;
import br.ed.ufape.bahiabrindes.model.entity.*;
import br.ed.ufape.bahiabrindes.model.enums.StatusArte;
import br.ed.ufape.bahiabrindes.model.enums.StatusOrcamento;
import br.ed.ufape.bahiabrindes.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class OrcamentoService {

    private final OrcamentoRepository orcamentoRepository;
    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;
    private final HistoricoStatusOrcamentoRepository historicoRepository;
    private final ArteOrcamentoRepository arteRepository;
    private final ComentarioOrcamentoRepository comentarioRepository;
    private final AvaliacaoProdutoRepository avaliacaoRepository;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final PasswordEncoder passwordEncoder;
    private final MateriaPrimaEstoqueRepository materiaPrimaEstoqueRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final OrcamentoItemRepository orcamentoItemRepository;

    @Value("${app.mail.from:}")
    private String mailFrom;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.upload.dir:uploads/artes}")
    private String uploadDir;

    // Formatador para exibição de datas
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Títulos legíveis para cada status na timeline
    private static final Map<String, String> STATUS_TITULO = Map.of(
            "ORCAMENTO_SOLICITADO", "Orçamento Solicitado",
            "PAGAMENTO_APROVADO",   "Pagamento Aprovado",
            "ARTE_PENDENTE",        "Artes Enviadas para Aprovação",
            "ARTES_APROVADAS",      "Artes Aprovadas",
            "EM_PRODUCAO",          "Em Produção",
            "CONCLUIDO",            "Pedido Concluído",
            "CANCELADO",            "Pedido Cancelado"
    );

    // Descrições padrão para o histórico automático
    private static final Map<String, String> STATUS_DESCRICAO = Map.of(
            "ORCAMENTO_SOLICITADO", "Seu orçamento foi recebido e está sendo analisado pela nossa equipe.",
            "PAGAMENTO_APROVADO",   "O pagamento foi confirmado.",
            "ARTE_PENDENTE",        "As artes do seu pedido foram enviadas. Por favor, revise e aprove para darmos continuidade.",
            "ARTES_APROVADAS",      "Todas as artes foram aprovadas. Seu pedido entrará em produção em breve.",
            "EM_PRODUCAO",          "As artes foram aprovadas e seu pedido entrou na fila de produção.",
            "CONCLUIDO",            "Seu pedido foi concluído e está pronto para entrega.",
            "CANCELADO",            "O pedido foi cancelado."
    );

    @Autowired
    public OrcamentoService(
            OrcamentoRepository orcamentoRepository,
            ProdutoRepository produtoRepository,
            ClienteRepository clienteRepository,
            HistoricoStatusOrcamentoRepository historicoRepository,
            ArteOrcamentoRepository arteRepository,
            ComentarioOrcamentoRepository comentarioRepository,
            AvaliacaoProdutoRepository avaliacaoRepository,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            PasswordEncoder passwordEncoder,
            MateriaPrimaEstoqueRepository materiaPrimaEstoqueRepository,
            MovimentacaoEstoqueRepository movimentacaoRepository,
            FuncionarioRepository funcionarioRepository,
            OrcamentoItemRepository orcamentoItemRepository
    ) {
        this.orcamentoRepository = orcamentoRepository;
        this.produtoRepository = produtoRepository;
        this.clienteRepository = clienteRepository;
        this.historicoRepository = historicoRepository;
        this.arteRepository = arteRepository;
        this.comentarioRepository = comentarioRepository;
        this.avaliacaoRepository = avaliacaoRepository;
        this.mailSenderProvider = mailSenderProvider;
        this.passwordEncoder = passwordEncoder;
        this.materiaPrimaEstoqueRepository = materiaPrimaEstoqueRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.orcamentoItemRepository = orcamentoItemRepository;
    }

    // ─────────────────────────── Listagem (admin) ────────────────────────────

    public PageResponse<AdminOrcamentoListItemDTO> listarTodos(int page, int pageSize, String statusFiltro) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), pageSize, Sort.by("dataCriacao").descending());

        Page<Orcamento> result;
        if (statusFiltro != null && !statusFiltro.isBlank()) {
            try {
                StatusOrcamento status = StatusOrcamento.valueOf(statusFiltro.toUpperCase());
                result = orcamentoRepository.findByStatusOrderByDataCriacaoDesc(status, pageable);
            } catch (IllegalArgumentException e) {
                result = orcamentoRepository.findAllByOrderByDataCriacaoDesc(pageable);
            }
        } else {
            result = orcamentoRepository.findAllByOrderByDataCriacaoDesc(pageable);
        }

        return PageResponse.<AdminOrcamentoListItemDTO>builder()
                .items(result.getContent().stream().map(this::toAdminListItem).toList())
                .page(page)
                .pageSize(pageSize)
                .total(result.getTotalElements())
                .build();
    }

    // ─────────────────────────── Listagem ────────────────────────────────────

    public PageResponse<MeusOrcamentosItemResponseDTO> listarMeusOrcamentos(Long clienteId, int page, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), pageSize, Sort.by("dataCriacao").descending());
        Page<Orcamento> result = orcamentoRepository.findByClienteIdOrderByDataCriacaoDesc(clienteId, pageable);

        return PageResponse.<MeusOrcamentosItemResponseDTO>builder()
                .items(result.getContent().stream().map(this::toMeusOrcamentosItem).toList())
                .page(page)
                .pageSize(pageSize)
                .total(result.getTotalElements())
                .build();
    }

    // ─────────────────────────── Detalhe ────────────────────────────────────

    public OrcamentoDetalheResponseDTO buscarDetalhe(Long clienteId, Long orcamentoId) {
        Orcamento orcamento = orcamentoRepository.findById(orcamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Orçamento não encontrado"));

        if (!orcamento.getCliente().getId().equals(clienteId)) {
            throw new IllegalArgumentException("Orçamento não encontrado");
        }

        return toDetalhe(orcamento);
    }

    /** Detalhe completo para uso administrativo (inclui dados do cliente, pagamento e comentários). */
    public OrcamentoDetalheResponseDTO buscarDetalheAdmin(Long orcamentoId) {
        Orcamento orcamento = orcamentoRepository.findById(orcamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Orçamento não encontrado"));
        return toDetalhe(orcamento);
    }

    /** Atualiza método de pagamento e valor pago de um orçamento. */
    @Transactional
    public OrcamentoDetalheResponseDTO atualizarPagamento(Long orcamentoId, String metodoPagamento, java.math.BigDecimal valorPago) {
        Orcamento orcamento = orcamentoRepository.findById(orcamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Orçamento não encontrado"));
        if (metodoPagamento != null) orcamento.setMetodoPagamento(metodoPagamento);
        if (valorPago != null) orcamento.setValorPago(valorPago);
        orcamentoRepository.save(orcamento);
        return toDetalhe(orcamento);
    }

    /** Atualiza o desconto de um item de orçamento e recalcula o precoTotal do item. */
    @Transactional
    public OrcamentoDetalheResponseDTO atualizarDescontoItem(Long orcamentoId, Long itemId, java.math.BigDecimal desconto) {
        Orcamento orcamento = orcamentoRepository.findById(orcamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Orçamento não encontrado"));
        OrcamentoItem item = orcamentoItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item não encontrado"));
        if (!item.getOrcamento().getId().equals(orcamentoId)) {
            throw new IllegalArgumentException("Item não pertence ao orçamento informado");
        }
        java.math.BigDecimal novoDesconto = desconto != null && desconto.compareTo(java.math.BigDecimal.ZERO) >= 0
                ? desconto : java.math.BigDecimal.ZERO;
        item.setDesconto(novoDesconto);
        java.math.BigDecimal qty = java.math.BigDecimal.valueOf(item.getQuantidade() != null ? item.getQuantidade() : 1);
        java.math.BigDecimal pu = item.getPrecoUnitario() != null ? item.getPrecoUnitario() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal novoTotal = qty.multiply(pu).subtract(novoDesconto);
        if (novoTotal.compareTo(java.math.BigDecimal.ZERO) < 0) novoTotal = java.math.BigDecimal.ZERO;
        item.setPrecoTotal(novoTotal);
        orcamentoItemRepository.save(item);
        return toDetalhe(orcamento);
    }

    // ─────────────────────────── Criar ───────────────────────────────────────

    @Transactional
    public OrcamentoDetalheResponseDTO criarOrcamento(Long clienteId, CriarOrcamentoRequest request) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));

        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new IllegalArgumentException("Orçamento deve conter ao menos um item");
        }

        Orcamento orcamento = Orcamento.builder()
                .cliente(cliente)
                .status(StatusOrcamento.ORCAMENTO_SOLICITADO)
                .valorTotal(BigDecimal.ZERO)
                .itens(new ArrayList<>())
                .historico(new ArrayList<>())
                .artes(new ArrayList<>())
                .build();

        BigDecimal total = BigDecimal.ZERO;
        List<OrcamentoItem> itens = new ArrayList<>();

        for (CriarOrcamentoItemRequest itemReq : request.getItens()) {
            Produto produto = produtoRepository.findById(itemReq.getProdutoId())
                    .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado: " + itemReq.getProdutoId()));

            int quantidade = itemReq.getQuantidade() != null ? itemReq.getQuantidade() : 0;
            if (quantidade <= 0) {
                throw new IllegalArgumentException("Quantidade inválida para o produto: " + produto.getNome());
            }

            BigDecimal precoUnitario = itemReq.getPrecoUnitario() != null
                    ? itemReq.getPrecoUnitario()
                    : (produto.getPrecoVenda() != null ? produto.getPrecoVenda() : BigDecimal.ZERO);
            BigDecimal desconto = itemReq.getDesconto() != null ? itemReq.getDesconto() : BigDecimal.ZERO;
            BigDecimal precoTotal = precoUnitario.multiply(BigDecimal.valueOf(quantidade)).subtract(desconto);
            if (precoTotal.compareTo(BigDecimal.ZERO) < 0) precoTotal = BigDecimal.ZERO;
            total = total.add(precoTotal);

            String imagemUrl = null;
            if (produto.getImagens() != null && !produto.getImagens().isEmpty()) {
                imagemUrl = produto.getImagens().get(0).getUrl();
            }

            OrcamentoItem item = OrcamentoItem.builder()
                    .orcamento(orcamento)
                    .produto(produto)
                    .quantidade(quantidade)
                    .cor(itemReq.getCor())
                    .variacao(itemReq.getImpressao())
                    .precoUnitario(precoUnitario)
                    .desconto(desconto)
                    .precoTotal(precoTotal)
                    .imagemUrl(imagemUrl)
                    .build();

            itens.add(item);
        }

        orcamento.setValorTotal(total);
        orcamento.setItens(itens);

        // Salvar e gerar código amigável
        Orcamento salvo = orcamentoRepository.save(orcamento);

        if (salvo.getCodigo() == null) {
            String codigo = gerarCodigoOrcamento(salvo.getId());
            salvo.setCodigo(codigo);
            salvo = orcamentoRepository.save(salvo);
        }

        // Criar entrada inicial no histórico (ORCAMENTO_SOLICITADO)
        criarEntradaHistorico(salvo, StatusOrcamento.ORCAMENTO_SOLICITADO, "Sistema");

        return toDetalhe(salvo);
    }

        @Transactional
        public OrcamentoDetalheResponseDTO criarOrcamentoAdmin(CriarOrcamentoAdminRequest request, String emailFuncionario) { 
                
                if (request.getItens() == null || request.getItens().isEmpty()) {
                    throw new IllegalArgumentException("Orçamento deve conter ao menos um item");
                }

                if (request.getEmailCliente() == null || request.getEmailCliente().isBlank()) {
                    throw new IllegalArgumentException("O e-mail do cliente é obrigatório para registar a venda.");
                }

                if (funcionarioRepository.findByEmail(request.getEmailCliente()).isPresent()) {
                    throw new IllegalArgumentException("Email já cadastrado como funcionário");
                }

                // Procura o cliente pelo e-mail. Se não existir, cria na hora
                Cliente cliente = clienteRepository.findByEmailAndAtivoTrue(request.getEmailCliente())
                        .orElseGet(() -> {
                            String senhaTemporaria = java.util.UUID.randomUUID().toString().substring(0, 8);

                            Cliente novoCliente = Cliente.builder()
                                    .nome(request.getNomeCliente() != null && !request.getNomeCliente().isBlank() 
                                            ? request.getNomeCliente() 
                                            : "Cliente não identificado")
                                    .email(request.getEmailCliente())
                                    .telefone(request.getTelefoneCliente())
                                    .senha(passwordEncoder.encode(senhaTemporaria))
                                    .ativo(true)
                                    .build();
                            
                            Cliente clienteSalvo = clienteRepository.save(novoCliente);
                        
                            enviarEmailBoasVindas(clienteSalvo.getEmail(), clienteSalvo.getNome(), senhaTemporaria);
                            
                            return clienteSalvo;
                        });

                Orcamento orcamento = Orcamento.builder()
                        .cliente(cliente)
                        .status(StatusOrcamento.ORCAMENTO_SOLICITADO)
                        .valorTotal(BigDecimal.ZERO)
                        .observacoes(request.getObservacoes())
                        .itens(new ArrayList<>())
                        .historico(new ArrayList<>())
                        .artes(new ArrayList<>())
                        .build();

                BigDecimal total = BigDecimal.ZERO;
                List<OrcamentoItem> itens = new ArrayList<>();

                for (CriarOrcamentoItemRequest itemReq : request.getItens()) {
                    Produto produto = produtoRepository.findById(itemReq.getProdutoId())
                            .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado: " + itemReq.getProdutoId()));

                    int quantidade = itemReq.getQuantidade() != null ? itemReq.getQuantidade() : 0;
                    if (quantidade <= 0) {
                            throw new IllegalArgumentException("Quantidade inválida para o produto: " + produto.getNome());
                    }

                    BigDecimal precoUnitario = itemReq.getPrecoUnitario() != null
                            ? itemReq.getPrecoUnitario()
                            : (produto.getPrecoVenda() != null ? produto.getPrecoVenda() : BigDecimal.ZERO);
                    BigDecimal desconto = itemReq.getDesconto() != null ? itemReq.getDesconto() : BigDecimal.ZERO;
                    BigDecimal precoTotal = precoUnitario.multiply(BigDecimal.valueOf(quantidade)).subtract(desconto);
                    if (precoTotal.compareTo(BigDecimal.ZERO) < 0) precoTotal = BigDecimal.ZERO;
                    total = total.add(precoTotal);

                    String imagemUrl = null;
                    if (produto.getImagens() != null && !produto.getImagens().isEmpty()) {
                            imagemUrl = produto.getImagens().get(0).getUrl();
                    }

                    OrcamentoItem item = OrcamentoItem.builder()
                            .orcamento(orcamento)
                            .produto(produto)
                            .quantidade(quantidade)
                            .cor(itemReq.getCor())
                            .variacao(itemReq.getImpressao())
                            .precoUnitario(precoUnitario)
                            .desconto(desconto)
                            .precoTotal(precoTotal)
                            .imagemUrl(imagemUrl)
                            .build();

                    itens.add(item);
                }

                orcamento.setValorTotal(total);
                orcamento.setItens(itens);

                Orcamento salvo = orcamentoRepository.save(orcamento);

                if (salvo.getCodigo() == null) {
                    String codigo = gerarCodigoOrcamento(salvo.getId());
                    salvo.setCodigo(codigo);
                    salvo = orcamentoRepository.save(salvo);
                }

                criarEntradaHistorico(salvo, StatusOrcamento.ORCAMENTO_SOLICITADO, emailFuncionario);

                return toDetalhe(salvo);
        }

        private void enviarEmailBoasVindas(String email, String nome, String senhaTemporaria) {
                JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
                if (mailSender == null) {
                System.err.println("Aviso: MailSender não configurado. Email não enviado.");
                return;
                }

                SimpleMailMessage message = new SimpleMailMessage();
                if (mailFrom != null && !mailFrom.isBlank()) {
                message.setFrom(mailFrom);
                }
                
                message.setTo(email);
                message.setSubject("Bem-vindo(a) à Bahia Brindes! Sua conta foi criada.");
                message.setText("Olá " + nome + ",\n\n" +
                        "Um orçamento foi registrado para você em nossa loja e criamos uma conta " +
                        "para que você possa acompanhar o andamento do seu pedido e aprovar artes!\n\n" +
                        "Sua senha temporária de acesso é: " + senhaTemporaria + "\n\n" +
                        "Recomendamos que acesse o nosso sistema e altere sua senha em 'Esqueceu sua senha?' " +
                        "na aba de Login.\n\n" +
                        "Atenciosamente,\nEquipe Bahia Brindes");

                try {
                mailSender.send(message);
                System.out.println("E-mail de boas vindas enviado para: " + email);
                } catch (Exception ex) {
                System.err.println("Erro ao enviar email de boas-vindas: " + ex.getMessage());
                }
        }

    // ─────────────────────────── Atualizar status ────────────────────────────

    /**
     * Atualiza o status de um orçamento e registra automaticamente no histórico.
     * Pode ser chamado por um controller administrativo no futuro.
     */
    @Transactional
    public OrcamentoDetalheResponseDTO atualizarStatus(Long orcamentoId, StatusOrcamento novoStatus, String responsavel) {
        Orcamento orcamento = orcamentoRepository.findById(orcamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Orçamento não encontrado"));

        orcamento.setStatus(novoStatus);
        if (novoStatus == StatusOrcamento.PAGAMENTO_APROVADO) {
            registrarSaidaEstoqueOrcamento(orcamento, responsavel);
        }
        orcamentoRepository.save(orcamento);

        criarEntradaHistorico(orcamento, novoStatus, responsavel != null ? responsavel : "Sistema");

        return toDetalhe(orcamento);
    }

    // ─────────────────────────── Helpers privados ────────────────────────────

    private void registrarSaidaEstoqueOrcamento(Orcamento orcamento, String responsavelNome) {
        if (orcamento.getItens() == null) return;
        Funcionario funcionario = (responsavelNome != null && !responsavelNome.isBlank())
                ? funcionarioRepository.findByNome(responsavelNome).orElse(null)
                : null;
        for (OrcamentoItem item : orcamento.getItens()) {
            Produto produto = item.getProduto();
            if (produto == null || produto.getItensFichaTecnica() == null) continue;
            int qtdPedido = item.getQuantidade() != null ? item.getQuantidade() : 1;
            for (MateriaPrimaProduto mp : produto.getItensFichaTecnica()) {
                java.math.BigDecimal qtdNecessaria = mp.getQuantidadeNecessaria()
                        .multiply(java.math.BigDecimal.valueOf(qtdPedido));
                MovimentacaoEstoque mov = MovimentacaoEstoque.builder()
                        .tipo("Saída")
                        .tipoItem("MATERIA_PRIMA")
                        .itemId(mp.getMateriaPrima().getId())
                        .materiaPrima(mp.getMateriaPrima())
                        .quantidade(qtdNecessaria)
                        .motivo("Baixa automática - Pagamento aprovado do orçamento #" + orcamento.getCodigo())
                        .dataMovimentacao(java.time.LocalDateTime.now())
                        .usuario(funcionario)
                        .build();
                movimentacaoRepository.save(mov);
                debitarEstoqueMateriaPrima(mp.getMateriaPrima().getId(), qtdNecessaria);
            }
        }
    }

    private void debitarEstoqueMateriaPrima(Long materiaPrimaId, java.math.BigDecimal quantidade) {
        List<MateriaPrimaEstoque> estoques = materiaPrimaEstoqueRepository
                .findByMateriaPrimaIdOrderByEstoqueAtualDesc(materiaPrimaId);
        java.math.BigDecimal restante = quantidade;
        for (MateriaPrimaEstoque estoque : estoques) {
            if (restante.compareTo(java.math.BigDecimal.ZERO) <= 0) break;
            java.math.BigDecimal atual = estoque.getEstoqueAtual() != null ? estoque.getEstoqueAtual() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal deducao = restante.min(atual);
            estoque.setEstoqueAtual(atual.subtract(deducao));
            materiaPrimaEstoqueRepository.save(estoque);
            restante = restante.subtract(deducao);
        }
    }

    private void criarEntradaHistorico(Orcamento orcamento, StatusOrcamento status, String responsavel) {
        String statusStr = status.name();
        String titulo = STATUS_TITULO.getOrDefault(statusStr, statusStr);
        String descricao = STATUS_DESCRICAO.getOrDefault(statusStr, "Status atualizado.");

        HistoricoStatusOrcamento entrada = HistoricoStatusOrcamento.builder()
                .orcamento(orcamento)
                .status(statusStr)
                .titulo(titulo)
                .descricao(descricao)
                .responsavel(responsavel)
                .build();

        historicoRepository.save(entrada);
    }

    private AdminOrcamentoListItemDTO toAdminListItem(Orcamento orcamento) {
        Cliente c = orcamento.getCliente();
        String dataCriacao = orcamento.getDataCriacao() != null
                ? DATE_FORMAT.format(orcamento.getDataCriacao())
                : null;
        int numProdutos = orcamento.getItens() != null ? orcamento.getItens().size() : 0;

        return AdminOrcamentoListItemDTO.builder()
                .id(orcamento.getId())
                .codigo(orcamento.getCodigo())
                .status(orcamento.getStatus() != null ? orcamento.getStatus().name() : null)
                .dataCriacao(dataCriacao)
                .valorTotal(orcamento.getValorTotal())
                .numProdutos(numProdutos)
                .nomeCliente(c != null ? c.getNome() : null)
                .telefoneCliente(c != null ? c.getTelefone() : null)
                .build();
    }

    private MeusOrcamentosItemResponseDTO toMeusOrcamentosItem(Orcamento orcamento) {
        List<ProdutoResumoDTO> produtos = orcamento.getItens() == null ? List.of() :
                orcamento.getItens().stream()
                        .limit(3)
                        .map(item -> {
                            String detalhes = item.getQuantidade() + " unidades";
                            if (item.getCor() != null && !item.getCor().isBlank()) {
                                detalhes += " • " + item.getCor();
                            }
                            return ProdutoResumoDTO.builder()
                                    .id(item.getProduto().getId())
                                    .nome(item.getProduto().getNome())
                                    .detalhesResumo(detalhes)
                                    .imagemUrl(item.getImagemUrl())
                                    .build();
                        })
                        .toList();

        String dataCriacao = orcamento.getDataCriacao() != null
                ? DATE_FORMAT.format(orcamento.getDataCriacao())
                : null;
        String dataPrevista = orcamento.getDataPrevisaoEntrega() != null
                ? DATE_FORMAT.format(orcamento.getDataPrevisaoEntrega())
                : null;

        return MeusOrcamentosItemResponseDTO.builder()
                .id(orcamento.getId())
                .codigo(orcamento.getCodigo())
                .status(orcamento.getStatus() != null ? orcamento.getStatus().name() : null)
                .dataCriacao(dataCriacao)
                .dataPrevisaoEntrega(dataPrevista)
                .valorTotal(orcamento.getValorTotal())
                .produtos(produtos)
                .build();
    }

    private OrcamentoDetalheResponseDTO toDetalhe(Orcamento orcamento) {
        Cliente c = orcamento.getCliente();

        // Histórico
        List<HistoricoStatusOrcamento> historicoEntidades =
                historicoRepository.findByOrcamentoIdOrderByDataAsc(orcamento.getId());

        List<HistoricoStatusItemDTO> historicoDTOs = historicoEntidades.stream()
                .map(h -> HistoricoStatusItemDTO.builder()
                        .id(h.getId())
                        .status(h.getStatus())
                        .titulo(h.getTitulo())
                        .descricao(h.getDescricao())
                        .data(h.getData() != null ? DATE_TIME_FORMAT.format(h.getData()) : null)
                        .responsavel(h.getResponsavel())
                        .build())
                .toList();

        // Artes
        List<ArteOrcamento> arteEntidades =
                arteRepository.findByOrcamentoIdOrderByEnviadoEmAsc(orcamento.getId());

        List<ArteOrcamentoDTO> arteDTOs = arteEntidades.stream()
                .map(a -> construirArteDTO(a, true)) // Incluir imagemData na lista de artes
                .toList();

        // Produtos
        List<OrcamentoProdutoDetalheDTO> produtosDTOs = orcamento.getItens() == null ? List.of() :
                orcamento.getItens().stream()
                        .map(item -> {
                            var avaliacao = avaliacaoRepository
                                    .findByOrcamentoIdAndProdutoId(orcamento.getId(), item.getProduto().getId())
                                    .orElse(null);
                            return OrcamentoProdutoDetalheDTO.builder()
                                    .id(item.getId())
                                    .produtoId(item.getProduto().getId())
                                    .nome(item.getProduto().getNome())
                                    .quantidade(item.getQuantidade())
                                    .cor(item.getCor())
                                    .tamanho(null)
                                    .impressao(item.getVariacao())
                                    .imagemUrl(item.getImagemUrl())
                                    .precoUnitario(item.getPrecoUnitario())
                                    .desconto(item.getDesconto() != null ? item.getDesconto() : BigDecimal.ZERO)
                                    .precoTotal(item.getPrecoTotal())
                                    .jaAvaliado(avaliacao != null)
                                    .notaAvaliacao(avaliacao != null ? avaliacao.getNota() : null)
                                    .comentarioAvaliacao(avaliacao != null ? avaliacao.getComentario() : null)
                                    .build();
                        })
                        .toList();

        // Desconto total = soma dos descontos por item
        BigDecimal descontoTotal = produtosDTOs.stream()
                .map(p -> p.getDesconto() != null ? p.getDesconto() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Subtotal bruto (antes dos descontos) e frete
        BigDecimal subtotalBruto = produtosDTOs.stream()
                .map(p -> {
                    BigDecimal qty = BigDecimal.valueOf(p.getQuantidade() != null ? p.getQuantidade() : 0);
                    BigDecimal pu = p.getPrecoUnitario() != null ? p.getPrecoUnitario() : BigDecimal.ZERO;
                    return qty.multiply(pu);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal frete = BigDecimal.ZERO;
        BigDecimal subtotal = subtotalBruto;

        String dataCriacao = orcamento.getDataCriacao() != null
                ? DATE_FORMAT.format(orcamento.getDataCriacao())
                : null;
        String dataPrevista = orcamento.getDataPrevisaoEntrega() != null
                ? DATE_FORMAT.format(orcamento.getDataPrevisaoEntrega())
                : null;

        // Comentários
        List<ComentarioOrcamento> comentarioEntidades =
                comentarioRepository.findByOrcamentoIdOrderByCriadoEmAsc(orcamento.getId());
        List<ComentarioOrcamentoDTO> comentariosDTOs = comentarioEntidades.stream()
                .map(cm -> ComentarioOrcamentoDTO.builder()
                        .id(cm.getId())
                        .autor(cm.getAutor())
                        .mensagem(cm.getMensagem())
                        .produtoNome(cm.getProdutoNome())
                        .criadoEm(cm.getCriadoEm() != null ? DATE_TIME_FORMAT.format(cm.getCriadoEm()) : null)
                        .build())
                .toList();

        return OrcamentoDetalheResponseDTO.builder()
                .id(orcamento.getId())
                .codigo(orcamento.getCodigo())
                .status(orcamento.getStatus() != null ? orcamento.getStatus().name() : null)
                .dataCriacao(dataCriacao)
                .dataPrevisaoEntrega(dataPrevista)
                .subtotal(subtotal)
                .descontoTotal(descontoTotal)
                .frete(frete)
                .valorTotal(subtotal.subtract(descontoTotal).add(frete))
                .nomeCliente(c != null ? c.getNome() : null)
                .emailCliente(c != null ? c.getEmail() : null)
                .telefoneCliente(c != null ? c.getTelefone() : null)
                .metodoPagamento(orcamento.getMetodoPagamento())
                .valorPago(orcamento.getValorPago() != null ? orcamento.getValorPago() : BigDecimal.ZERO)
                .historico(historicoDTOs)
                .artes(arteDTOs)
                .produtos(produtosDTOs)
                .comentarios(comentariosDTOs)
                .build();
    }

    // ─────────────────────────── Upload de Arte ──────────────────────────────

    @Transactional
    public ArteOrcamentoDTO uploadArte(Long orcamentoId, String produtoNome, MultipartFile arquivo) throws IOException {
        Orcamento orcamento = orcamentoRepository.findById(orcamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Orçamento não encontrado"));

        // Salva apenas no banco de dados (sem pasta de disco)
        String original = arquivo.getOriginalFilename();
        byte[] conteudo = arquivo.getBytes();

        // Cria ou atualiza a ArteOrcamento para este produto
        List<ArteOrcamento> existentes = arteRepository.findByOrcamentoIdOrderByEnviadoEmAsc(orcamentoId);
        ArteOrcamento arte = existentes.stream()
                .filter(a -> produtoNome.equals(a.getProdutoNome()))
                .findFirst()
                .orElse(null);

        if (arte == null) {
            arte = ArteOrcamento.builder()
                    .orcamento(orcamento)
                    .produtoNome(produtoNome)
                    .imagemUrl(null) // Não usa mais URL de arquivo
                    .nomeArquivo(original)
                    .conteudoArquivo(conteudo)
                    .status(StatusArte.PENDENTE)
                    .build();
        } else {
            arte.setImagemUrl(null);
            arte.setNomeArquivo(original);
            arte.setConteudoArquivo(conteudo);
            arte.setStatus(StatusArte.PENDENTE);
        }
        arte = arteRepository.save(arte);

        // Retorna DTO com imagem em base64
        return construirArteDTO(arte, true);
    }

    /**
     * Constrói ArteOrcamentoDTO com opção de incluir a imagem em base64
     */
    private ArteOrcamentoDTO construirArteDTO(ArteOrcamento arte, boolean incluirImagemData) {
        String imagemData = null;

        if (incluirImagemData) {
            // Preferência: conteúdo binário armazenado no banco
            if (arte.getConteudoArquivo() != null && arte.getConteudoArquivo().length > 0) {
                String mimeType = detectarMimeType(arte.getNomeArquivo());
                String base64 = java.util.Base64.getEncoder().encodeToString(arte.getConteudoArquivo());
                imagemData = "data:" + mimeType + ";base64," + base64;
            }
            // Fallback: arquivo salvo em disco (artes legadas)
            else if (arte.getImagemUrl() != null && !arte.getImagemUrl().isBlank()) {
                try {
                    Path filePath = Paths.get(arte.getImagemUrl().startsWith("/")
                            ? arte.getImagemUrl()
                            : System.getProperty("user.dir") + "/" + arte.getImagemUrl());
                    if (Files.exists(filePath)) {
                        byte[] bytes = Files.readAllBytes(filePath);
                        String mimeType = detectarMimeType(filePath.getFileName().toString());
                        String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
                        imagemData = "data:" + mimeType + ";base64," + base64;
                    }
                } catch (IOException ignored) {
                    // Se não conseguir ler, imagemData fica null; front usa imagemUrl como fallback
                }
            }
        }

        // nomeArquivo: usa o campo direto ou extrai do path legado
        String nomeArquivo = arte.getNomeArquivo();
        if (nomeArquivo == null && arte.getImagemUrl() != null) {
            nomeArquivo = Paths.get(arte.getImagemUrl()).getFileName().toString();
        }

        return ArteOrcamentoDTO.builder()
                .id(arte.getId())
                .produtoNome(arte.getProdutoNome())
                .imagemUrl(arte.getImagemUrl())
                .nomeArquivo(nomeArquivo)
                .imagemData(imagemData)
                .status(arte.getStatus() != null ? arte.getStatus().name() : "PENDENTE")
                .enviadoEm(arte.getEnviadoEm() != null ? DATE_FORMAT.format(arte.getEnviadoEm()) : null)
                .build();
    }

    /**
     * Detecta o tipo MIME baseado na extensão do arquivo
     */
    private String detectarMimeType(String nomeArquivo) {
        if (nomeArquivo == null) return "application/octet-stream";

        String extensao = nomeArquivo.toLowerCase();
        if (extensao.endsWith(".png")) return "image/png";
        if (extensao.endsWith(".jpg") || extensao.endsWith(".jpeg")) return "image/jpeg";
        if (extensao.endsWith(".gif")) return "image/gif";
        if (extensao.endsWith(".webp")) return "image/webp";
        if (extensao.endsWith(".svg")) return "image/svg+xml";
        if (extensao.endsWith(".bmp")) return "image/bmp";

        return "application/octet-stream";
    }

    // ─────────────────────────── Avaliação de Arte (cliente) ────────────────

    @Transactional
    public OrcamentoDetalheResponseDTO avaliarArte(Long clienteId, Long orcamentoId, Long arteId, String acao, String comentario) {
        Orcamento orcamento = orcamentoRepository.findById(orcamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Orçamento não encontrado"));

        if (!orcamento.getCliente().getId().equals(clienteId)) {
            throw new IllegalArgumentException("Orçamento não encontrado");
        }

        ArteOrcamento arte = arteRepository.findById(arteId)
                .orElseThrow(() -> new IllegalArgumentException("Arte não encontrada"));

        StatusArte novoStatus = "APROVAR".equalsIgnoreCase(acao)
                ? StatusArte.APROVADA
                : StatusArte.AJUSTE_SOLICITADO;

        arte.setStatus(novoStatus);
        arteRepository.save(arte);

        // Salva comentário vinculado ao produto (se informado)
        if (comentario != null && !comentario.isBlank()) {
            ComentarioOrcamento cm = ComentarioOrcamento.builder()
                    .orcamento(orcamento)
                    .autor(orcamento.getCliente().getNome())
                    .produtoNome(arte.getProdutoNome())
                    .mensagem(comentario)
                    .build();
            comentarioRepository.save(cm);
        }

        // Se todas as artes estiverem aprovadas → ARTES_APROVADAS
        List<ArteOrcamento> todasArtes = arteRepository.findByOrcamentoIdOrderByEnviadoEmAsc(orcamentoId);
        boolean todasAprovadas = !todasArtes.isEmpty() &&
                todasArtes.stream().allMatch(a -> StatusArte.APROVADA.equals(a.getStatus()));

        if (todasAprovadas) {
            orcamento.setStatus(StatusOrcamento.ARTES_APROVADAS);
            orcamentoRepository.save(orcamento);
            criarEntradaHistorico(orcamento, StatusOrcamento.ARTES_APROVADAS, orcamento.getCliente().getNome());
        }

        return toDetalhe(orcamento);
    }

    // ─────────────────────────── Admin — Arte / Comentário ──────────────────

    @Transactional
    public OrcamentoDetalheResponseDTO avaliarArteAdmin(Long orcamentoId, Long arteId, String novoStatusStr, String comentario, String responsavel) {
        Orcamento orcamento = orcamentoRepository.findById(orcamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Orçamento não encontrado"));

        ArteOrcamento arte = arteRepository.findById(arteId)
                .orElseThrow(() -> new IllegalArgumentException("Arte não encontrada"));

        StatusArte novoStatus;
        try {
            novoStatus = StatusArte.valueOf(novoStatusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Status inválido: " + novoStatusStr);
        }

        arte.setStatus(novoStatus);
        arteRepository.save(arte);

        if (comentario != null && !comentario.isBlank()) {
            ComentarioOrcamento cm = ComentarioOrcamento.builder()
                    .orcamento(orcamento)
                    .autor(responsavel != null ? responsavel : "Sistema")
                    .produtoNome(arte.getProdutoNome())
                    .mensagem(comentario)
                    .build();
            comentarioRepository.save(cm);
        }

        return toDetalhe(orcamento);
    }

    @Transactional
    public OrcamentoDetalheResponseDTO adicionarComentario(Long orcamentoId, String mensagem, String produtoNome, String autor) {
        Orcamento orcamento = orcamentoRepository.findById(orcamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Orçamento não encontrado"));

        if (mensagem == null || mensagem.isBlank()) {
            throw new IllegalArgumentException("Mensagem não pode ser vazia");
        }

        ComentarioOrcamento cm = ComentarioOrcamento.builder()
                .orcamento(orcamento)
                .autor(autor != null ? autor : "Sistema")
                .produtoNome(produtoNome != null && !produtoNome.isBlank() ? produtoNome : null)
                .mensagem(mensagem)
                .build();
        comentarioRepository.save(cm);

        return toDetalhe(orcamento);
    }

    // ─────────────────────────── Notificações ────────────────────────────────

    public void notificarStatus(Long orcamentoId) {
        Orcamento orc = orcamentoRepository.findById(orcamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Orçamento não encontrado"));
        Cliente c = orc.getCliente();
        if (c == null || c.getEmail() == null) return;

        String statusLabel = STATUS_TITULO.getOrDefault(
                orc.getStatus() != null ? orc.getStatus().name() : "",
                orc.getStatus() != null ? orc.getStatus().name() : "");
        String link = frontendUrl + "/meus-orcamentos/" + orcamentoId;

        enviarEmail(
                c.getEmail(),
                "Atualização do seu pedido #" + orc.getCodigo() + " — Bahia Brindes",
                "Olá " + c.getNome() + ",\n\n" +
                "O status do seu pedido foi atualizado para: " + statusLabel + "\n\n" +
                "Acesse seu pedido para mais detalhes:\n" + link + "\n\n" +
                "Atenciosamente,\nEquipe Bahia Brindes"
        );
    }

    public void notificarArte(Long orcamentoId) {
        Orcamento orc = orcamentoRepository.findById(orcamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Orçamento não encontrado"));
        Cliente c = orc.getCliente();
        if (c == null || c.getEmail() == null) return;

        String link = frontendUrl + "/meus-orcamentos/" + orcamentoId;

        enviarEmail(
                c.getEmail(),
                "Arte disponível para aprovação — Pedido #" + orc.getCodigo(),
                "Olá " + c.getNome() + ",\n\n" +
                "A arte do seu pedido foi atualizada e está aguardando a sua aprovação.\n\n" +
                "Acesse seu pedido para visualizar e aprovar:\n" + link + "\n\n" +
                "Atenciosamente,\nEquipe Bahia Brindes"
        );
    }

    private void enviarEmail(String to, String subject, String text) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            System.err.println("MailSender não configurado. Email não enviado para: " + to);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        if (mailFrom != null && !mailFrom.isBlank()) message.setFrom(mailFrom);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        try {
            mailSender.send(message);
        } catch (Exception ex) {
            System.err.println("Erro ao enviar email: " + ex.getMessage());
        }
    }

    private String gerarCodigoOrcamento(Long id) {
        int year = LocalDate.now().getYear();
        return String.format("BB-%d-%03d", year, id);
    }

    /**
     * Recupera o arquivo binário da arte para download
     */
    public byte[] obterConteudoArte(Long arteId) {
        ArteOrcamento arte = arteRepository.findById(arteId)
                .orElseThrow(() -> new IllegalArgumentException("Arte não encontrada"));
        
        if (arte.getConteudoArquivo() == null || arte.getConteudoArquivo().length == 0) {
            throw new IllegalArgumentException("Arquivo não disponível para download");
        }
        
        return arte.getConteudoArquivo();
    }

    /**
     * Obtém o nome original do arquivo da arte
     */
    public String obterNomeArquivoArte(Long arteId) {
        ArteOrcamento arte = arteRepository.findById(arteId)
                .orElseThrow(() -> new IllegalArgumentException("Arte não encontrada"));
        return arte.getNomeArquivo() != null ? arte.getNomeArquivo() : "arte.jpg";
    }
}
