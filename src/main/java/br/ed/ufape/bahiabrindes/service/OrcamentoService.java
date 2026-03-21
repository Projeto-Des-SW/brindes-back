package br.ed.ufape.bahiabrindes.service;

import br.ed.ufape.bahiabrindes.dto.common.PageResponse;
import br.ed.ufape.bahiabrindes.dto.orcamento.*;
import br.ed.ufape.bahiabrindes.model.entity.*;
import br.ed.ufape.bahiabrindes.model.enums.StatusOrcamento;
import br.ed.ufape.bahiabrindes.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.mail.from:}")
    private String mailFrom;

    // Formatador para exibição de datas
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Títulos legíveis para cada status na timeline
    private static final Map<String, String> STATUS_TITULO = Map.of(
            "ORCAMENTO_SOLICITADO", "Orçamento Solicitado",
            "ARTE_PENDENTE",        "Artes Enviadas para Aprovação",
            "EM_PRODUCAO",          "Em Produção",
            "CONCLUIDO",            "Pedido Concluído"
    );

    // Descrições padrão para o histórico automático
    private static final Map<String, String> STATUS_DESCRICAO = Map.of(
            "ORCAMENTO_SOLICITADO", "Seu orçamento foi recebido e está sendo analisado pela nossa equipe.",
            "ARTE_PENDENTE",        "As artes do seu pedido foram enviadas. Por favor, revise e aprove para darmos continuidade.",
            "EM_PRODUCAO",          "As artes foram aprovadas e seu pedido entrou na fila de produção.",
            "CONCLUIDO",            "Seu pedido foi concluído e está pronto para entrega."
    );

    @Autowired
    public OrcamentoService(
            OrcamentoRepository orcamentoRepository,
            ProdutoRepository produtoRepository,
            ClienteRepository clienteRepository,
            HistoricoStatusOrcamentoRepository historicoRepository,
            ArteOrcamentoRepository arteRepository,
            ComentarioOrcamentoRepository comentarioRepository,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            PasswordEncoder passwordEncoder
    ) {
        this.orcamentoRepository = orcamentoRepository;
        this.produtoRepository = produtoRepository;
        this.clienteRepository = clienteRepository;
        this.historicoRepository = historicoRepository;
        this.arteRepository = arteRepository;
        this.comentarioRepository = comentarioRepository;
        this.mailSenderProvider = mailSenderProvider;
        this.passwordEncoder = passwordEncoder;

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

            BigDecimal precoUnitario = produto.getPrecoVenda() != null ? produto.getPrecoVenda() : BigDecimal.ZERO;
            BigDecimal precoTotal = precoUnitario.multiply(BigDecimal.valueOf(quantidade));
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

                    BigDecimal precoUnitario = produto.getPrecoVenda() != null ? produto.getPrecoVenda() : BigDecimal.ZERO;
                    BigDecimal precoTotal = precoUnitario.multiply(BigDecimal.valueOf(quantidade));
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
        orcamentoRepository.save(orcamento);

        criarEntradaHistorico(orcamento, novoStatus, responsavel != null ? responsavel : "Sistema");

        return toDetalhe(orcamento);
    }

    // ─────────────────────────── Helpers privados ────────────────────────────

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
                .map(a -> ArteOrcamentoDTO.builder()
                        .id(a.getId())
                        .produtoNome(a.getProdutoNome())
                        .imagemUrl(a.getImagemUrl())
                        .status(a.getStatus() != null ? a.getStatus().name() : "PENDENTE")
                        .enviadoEm(a.getEnviadoEm() != null ? DATE_FORMAT.format(a.getEnviadoEm()) : null)
                        .build())
                .toList();

        // Produtos
        List<OrcamentoProdutoDetalheDTO> produtosDTOs = orcamento.getItens() == null ? List.of() :
                orcamento.getItens().stream()
                        .map(item -> OrcamentoProdutoDetalheDTO.builder()
                                .id(item.getId())
                                .nome(item.getProduto().getNome())
                                .quantidade(item.getQuantidade())
                                .cor(item.getCor())
                                .tamanho(null) // campo reservado para expansão futura
                                .impressao(item.getVariacao())
                                .imagemUrl(item.getImagemUrl())
                                .precoUnitario(item.getPrecoUnitario())
                                .precoTotal(item.getPrecoTotal())
                                .build())
                        .toList();

        // Subtotal e frete (frete sempre 0 por enquanto — negócio de orçamento)
        BigDecimal subtotal = orcamento.getValorTotal() != null ? orcamento.getValorTotal() : BigDecimal.ZERO;
        BigDecimal frete = BigDecimal.ZERO;

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
                .frete(frete)
                .valorTotal(subtotal.add(frete))
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

    private String gerarCodigoOrcamento(Long id) {
        int year = LocalDate.now().getYear();
        return String.format("BB-%d-%03d", year, id);
    }
}
