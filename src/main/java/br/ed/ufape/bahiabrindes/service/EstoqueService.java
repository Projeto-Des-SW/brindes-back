package br.ed.ufape.bahiabrindes.service;

import br.ed.ufape.bahiabrindes.dto.common.PageResponse;
import br.ed.ufape.bahiabrindes.dto.estoque.*;
import br.ed.ufape.bahiabrindes.model.entity.*;
import br.ed.ufape.bahiabrindes.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EstoqueService {

    private static final int SEM_MOVIMENTACAO_DIAS_PADRAO = 60;

    private final MateriaPrimaRepository materiaPrimaRepository;
    private final MateriaPrimaEstoqueRepository materiaPrimaEstoqueRepository;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    private final FornecedorRepository fornecedorRepository;
    private final LocalEstoqueRepository localEstoqueRepository;
    private final FuncionarioRepository funcionarioRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public EstoqueService(
            MateriaPrimaRepository materiaPrimaRepository,
            MateriaPrimaEstoqueRepository materiaPrimaEstoqueRepository,
            MovimentacaoEstoqueRepository movimentacaoEstoqueRepository,
            FornecedorRepository fornecedorRepository,
            LocalEstoqueRepository localEstoqueRepository,
            FuncionarioRepository funcionarioRepository
    ) {
        this.materiaPrimaRepository = materiaPrimaRepository;
        this.materiaPrimaEstoqueRepository = materiaPrimaEstoqueRepository;
        this.movimentacaoEstoqueRepository = movimentacaoEstoqueRepository;
        this.fornecedorRepository = fornecedorRepository;
        this.localEstoqueRepository = localEstoqueRepository;
        this.funcionarioRepository = funcionarioRepository;
    }

    public EstoqueResumoResponse resumo() {
        BigDecimal valorTotal = (BigDecimal) entityManager.createNativeQuery("""
                SELECT COALESCE(SUM(estoque_atual * COALESCE(preco_custo, 0)), 0)
                FROM materias_primas_estoque
                """).getSingleResult();

        Number abaixoMinimo = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM (
                    SELECT mp.id
                    FROM materias_primas mp
                    LEFT JOIN materias_primas_estoque mpe ON mpe.materias_primas_id = mp.id
                    GROUP BY mp.id, mp.estoque_minimo
                    HAVING COALESCE(SUM(mpe.estoque_atual), 0) < COALESCE(mp.estoque_minimo, 0)
                ) t
                """).getSingleResult();

        Number totalMp = (Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM materias_primas").getSingleResult();

        LocalDateTime cutoff = LocalDateTime.now().minusDays(SEM_MOVIMENTACAO_DIAS_PADRAO);
        Number semMov = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM materias_primas mp
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM movimentacoes_estoque me
                    WHERE me.materia_prima_id = mp.id
                      AND me.data_movimentacao >= :cutoff
                )
                """)
                .setParameter("cutoff", cutoff)
                .getSingleResult();

        return EstoqueResumoResponse.builder()
                .valorTotalEmEstoque(valorTotal != null ? valorTotal : BigDecimal.ZERO)
                .itensAbaixoMinimo(abaixoMinimo != null ? abaixoMinimo.longValue() : 0L)
                .totalMateriasPrimas(totalMp != null ? totalMp.longValue() : 0L)
                .produtosSemMovimentacao(semMov != null ? semMov.longValue() : 0L)
                .build();
    }

    public PageResponse<ProdutoEstoqueItemResponse> itens(String search, String status, int page, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), pageSize, Sort.by("id").descending());
        Page<ProdutoEstoqueItemProjection> result = materiaPrimaRepository.findEstoqueItems(blankToNull(search), blankToNull(status), pageable);

        return PageResponse.<ProdutoEstoqueItemResponse>builder()
                .items(result.getContent().stream().map(p -> {
                    BigDecimal quantidadeAtual = nvl(p.getQuantidadeAtual());
                    BigDecimal estoqueMinimo = nvl(p.getEstoqueMinimo());
                    String statusProduto = quantidadeAtual.compareTo(estoqueMinimo) < 0 ? "ABAIXO" : "NORMAL";
                    return ProdutoEstoqueItemResponse.builder()
                            .id(p.getId())
                            .codigo(p.getCodigo())
                            .materiaPrima(p.getMateriaPrima())
                            .quantidadeAtual(quantidadeAtual)
                            .estoqueMinimo(estoqueMinimo)
                            .valorUnitario(nvl(p.getValorUnitario()))
                            .status(statusProduto)
                            .build();
                }).toList())
                .page(page)
                .pageSize(pageSize)
                .total(result.getTotalElements())
                .build();
    }

    public PageResponse<MovimentacaoResponse> movimentacoes(String search, String tipo, String from, String to, int page, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), pageSize, Sort.by("dataMovimentacao").descending());
        Page<MovimentacaoEstoque> result = movimentacaoEstoqueRepository.search(blankToNull(search), blankToNull(tipo), pageable);

        return PageResponse.<MovimentacaoResponse>builder()
                .items(result.getContent().stream().map(this::toMovimentacaoResponse).toList())
                .page(page)
                .pageSize(pageSize)
                .total(result.getTotalElements())
                .build();
    }

    @Transactional
    public MovimentacaoResponse criarMovimentacao(CriarMovimentacaoRequest request, String emailUsuario) {
        String tipo = normalizeTipo(request.getTipo());
        if (!"Entrada".equals(tipo) && !"Saída".equals(tipo)) {
            throw new IllegalArgumentException("Tipo inválido. Use 'Entrada' ou 'Saída'.");
        }

        MateriaPrima materiaPrima = materiaPrimaRepository.findById(request.getMateriaPrimaId())
                .orElseThrow(() -> new IllegalArgumentException("Matéria-prima não encontrada"));

        Fornecedor fornecedor = null;
        if (request.getFornecedorId() != null) {
            fornecedor = fornecedorRepository.findById(request.getFornecedorId())
                    .orElseThrow(() -> new IllegalArgumentException("Fornecedor não encontrado"));
        }

        LocalEstoque destino = null;
        if (request.getDestinoId() != null) {
            destino = localEstoqueRepository.findById(request.getDestinoId())
                    .orElseThrow(() -> new IllegalArgumentException("Local de estoque (destino) não encontrado"));
        }

        Funcionario usuario = null;
        if (emailUsuario != null) {
            usuario = funcionarioRepository.findByEmailAndAtivoTrue(emailUsuario).orElse(null);
        }

        BigDecimal quantidade = nvl(request.getQuantidade());
        if (quantidade.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero");
        }

        LocalDateTime dataMov = parseDateTimeOrNow(request.getData());
        BigDecimal valorUnitario = request.getValorUnitario();

        MovimentacaoEstoque mov = MovimentacaoEstoque.builder()
                .tipo(tipo)
                .tipoItem("MATERIA_PRIMA")
                .itemId(materiaPrima.getId())
                .materiaPrima(materiaPrima)
                .quantidade(quantidade)
                .motivo(blankToNull(request.getMotivo()))
                .usuario(usuario)
                .dataMovimentacao(dataMov)
                .fornecedor(fornecedor)
                .localDestino(destino)
                .valorUnitario(valorUnitario)
                .build();

        MovimentacaoEstoque saved = movimentacaoEstoqueRepository.save(mov);

        aplicarDeltaEstoque(
                materiaPrima.getId(),
                fornecedor != null ? fornecedor.getId() : null,
                signedQuantidade(tipo, quantidade),
                "Entrada".equals(tipo) ? valorUnitario : null
        );

        return toMovimentacaoResponse(saved);
    }

    @Transactional
    public MovimentacaoResponse atualizarMovimentacao(Long id, AtualizarMovimentacaoRequest request, String emailUsuario) {
        MovimentacaoEstoque existente = movimentacaoEstoqueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Movimentação não encontrada"));

        String tipoNovo = normalizeTipo(request.getTipo());
        if (!"Entrada".equals(tipoNovo) && !"Saída".equals(tipoNovo)) {
            throw new IllegalArgumentException("Tipo inválido. Use 'Entrada' ou 'Saída'.");
        }

        BigDecimal quantidadeNova = nvl(request.getQuantidade());
        if (quantidadeNova.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero");
        }

        // Reverte efeito antigo no estoque
        Long mpAntigaId = existente.getMateriaPrima() != null ? existente.getMateriaPrima().getId() : existente.getItemId();
        Long fornecedorAntigoId = existente.getFornecedor() != null ? existente.getFornecedor().getId() : null;
        BigDecimal quantidadeAntiga = nvl(existente.getQuantidade());
        String tipoAntigo = normalizeTipo(existente.getTipo());
        aplicarDeltaEstoque(mpAntigaId, fornecedorAntigoId, signedQuantidade(tipoAntigo, quantidadeAntiga).negate(), null);

        // Resolve novas referências e atualiza entidade
        MateriaPrima mpNova = materiaPrimaRepository.findById(request.getMateriaPrimaId())
                .orElseThrow(() -> new IllegalArgumentException("Matéria-prima não encontrada"));

        Fornecedor fornecedorNovo = null;
        if (request.getFornecedorId() != null) {
            fornecedorNovo = fornecedorRepository.findById(request.getFornecedorId())
                    .orElseThrow(() -> new IllegalArgumentException("Fornecedor não encontrado"));
        }

        LocalEstoque destinoNovo = null;
        if (request.getDestinoId() != null) {
            destinoNovo = localEstoqueRepository.findById(request.getDestinoId())
                    .orElseThrow(() -> new IllegalArgumentException("Local de estoque (destino) não encontrado"));
        }

        Funcionario usuario = null;
        if (emailUsuario != null) {
            usuario = funcionarioRepository.findByEmailAndAtivoTrue(emailUsuario).orElse(null);
        }

        existente.setTipo(tipoNovo);
        existente.setTipoItem("MATERIA_PRIMA");
        existente.setItemId(mpNova.getId());
        existente.setMateriaPrima(mpNova);
        existente.setQuantidade(quantidadeNova);
        existente.setMotivo(blankToNull(request.getMotivo()));
        if (usuario != null) {
            existente.setUsuario(usuario);
        }
        existente.setDataMovimentacao(parseDateTimeOrNow(request.getData()));
        existente.setFornecedor(fornecedorNovo);
        existente.setLocalDestino(destinoNovo);
        existente.setValorUnitario(request.getValorUnitario());

        MovimentacaoEstoque saved = movimentacaoEstoqueRepository.save(existente);

        // Aplica novo efeito no estoque
        aplicarDeltaEstoque(
                mpNova.getId(),
                fornecedorNovo != null ? fornecedorNovo.getId() : null,
                signedQuantidade(tipoNovo, quantidadeNova),
                "Entrada".equals(tipoNovo) ? request.getValorUnitario() : null
        );

        return toMovimentacaoResponse(saved);
    }

    @Transactional
    public void excluirMovimentacao(Long id) {
        MovimentacaoEstoque existente = movimentacaoEstoqueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Movimentação não encontrada"));

        Long mpId = existente.getMateriaPrima() != null ? existente.getMateriaPrima().getId() : existente.getItemId();
        Long fornecedorId = existente.getFornecedor() != null ? existente.getFornecedor().getId() : null;
        BigDecimal qtd = nvl(existente.getQuantidade());
        String tipo = normalizeTipo(existente.getTipo());

        aplicarDeltaEstoque(mpId, fornecedorId, signedQuantidade(tipo, qtd).negate(), null);
        movimentacaoEstoqueRepository.delete(existente);
    }

    private MovimentacaoResponse toMovimentacaoResponse(MovimentacaoEstoque m) {
        String fornecedor = "";
        if (m.getFornecedor() != null) {
            fornecedor = blankToNull(m.getFornecedor().getNomeFantasia()) != null
                    ? m.getFornecedor().getNomeFantasia()
                    : (m.getFornecedor().getRazaoSocial() != null ? m.getFornecedor().getRazaoSocial() : "");
        }

        String destino = m.getLocalDestino() != null ? m.getLocalDestino().getNome() : "";
        String responsavel = m.getUsuario() != null ? m.getUsuario().getNome() : "";
        String materiaPrima = m.getMateriaPrima() != null ? m.getMateriaPrima().getNome() : "";

        BigDecimal valorTotal = BigDecimal.ZERO;
        if (m.getValorUnitario() != null) {
            valorTotal = nvl(m.getQuantidade()).multiply(m.getValorUnitario());
        }

        return MovimentacaoResponse.builder()
                .id(m.getId())
                .data(m.getDataMovimentacao() != null ? m.getDataMovimentacao().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .tipo(m.getTipo())
                .materiaPrimaId(m.getMateriaPrima() != null ? m.getMateriaPrima().getId() : m.getItemId())
                .materiaPrima(materiaPrima)
                .quantidade(nvl(m.getQuantidade()))
                .fornecedorId(m.getFornecedor() != null ? m.getFornecedor().getId() : null)
                .fornecedor(fornecedor)
                .responsavel(responsavel)
                .destinoId(m.getLocalDestino() != null ? m.getLocalDestino().getId() : null)
                .destino(destino)
                .valorUnitario(m.getValorUnitario())
                .motivo(m.getMotivo())
                .valorTotal(valorTotal)
                .build();
    }

    private MateriaPrimaEstoque resolveEstoque(Long materiaPrimaId, Long fornecedorId) {
        if (fornecedorId != null) {
            return materiaPrimaEstoqueRepository.findByMateriaPrima_IdAndFornecedor_Id(materiaPrimaId, fornecedorId)
                    .orElseGet(() -> {
                        MateriaPrima mp = materiaPrimaRepository.getReferenceById(materiaPrimaId);
                        Fornecedor f = fornecedorRepository.getReferenceById(fornecedorId);
                        return MateriaPrimaEstoque.builder()
                                .materiaPrima(mp)
                                .fornecedor(f)
                                .estoqueAtual(BigDecimal.ZERO)
                                .build();
                    });
        }

        return materiaPrimaEstoqueRepository.findByMateriaPrima_IdAndFornecedorIsNull(materiaPrimaId)
                .orElseGet(() -> {
                    MateriaPrima mp = materiaPrimaRepository.getReferenceById(materiaPrimaId);
                    return MateriaPrimaEstoque.builder()
                            .materiaPrima(mp)
                            .fornecedor(null)
                            .estoqueAtual(BigDecimal.ZERO)
                            .build();
                });
    }

    private void aplicarDeltaEstoque(Long materiaPrimaId, Long fornecedorId, BigDecimal delta, BigDecimal novoPrecoCustoSeEntrada) {
        if (materiaPrimaId == null) {
            throw new IllegalArgumentException("Matéria-prima inválida para ajuste de estoque");
        }
        BigDecimal d = nvl(delta);
        if (d.compareTo(BigDecimal.ZERO) == 0) return;

        MateriaPrimaEstoque estoque = resolveEstoque(materiaPrimaId, fornecedorId);
        BigDecimal atual = nvl(estoque.getEstoqueAtual());
        BigDecimal novo = atual.add(d);
        if (novo.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Estoque insuficiente para operação");
        }
        estoque.setEstoqueAtual(novo);
        if (novoPrecoCustoSeEntrada != null) {
            estoque.setPrecoCusto(novoPrecoCustoSeEntrada);
        }
        materiaPrimaEstoqueRepository.save(estoque);
    }

    private static BigDecimal signedQuantidade(String tipo, BigDecimal quantidade) {
        BigDecimal q = nvl(quantidade);
        String t = normalizeTipo(tipo);
        if ("Entrada".equals(t)) return q;
        if ("Saída".equals(t)) return q.negate();
        throw new IllegalArgumentException("Tipo inválido. Use 'Entrada' ou 'Saída'.");
    }

    private static String normalizeTipo(String tipo) {
        if (tipo == null) return null;
        String t = tipo.trim();
        // Normaliza "Saida" para "Saída" se vier sem acento
        if ("Saida".equalsIgnoreCase(t)) return "Saída";
        if ("Saída".equalsIgnoreCase(t)) return "Saída";
        if ("Entrada".equalsIgnoreCase(t)) return "Entrada";
        return t;
    }

    private static LocalDateTime parseDateTimeOrNow(String iso) {
        LocalDateTime parsed = parseDateTimeOrNull(iso);
        return parsed != null ? parsed : LocalDateTime.now();
    }

    private static LocalDateTime parseDateTimeOrNull(String iso) {
        String s = blankToNull(iso);
        if (s == null) return null;
        try {
            return OffsetDateTime.parse(s).toLocalDateTime();
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(s);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

