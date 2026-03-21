package br.ed.ufape.bahiabrindes.service;

import br.ed.ufape.bahiabrindes.dto.common.PageResponse;
import br.ed.ufape.bahiabrindes.dto.produto.ItemFichaTecnicaRequestDTO;
import br.ed.ufape.bahiabrindes.dto.produto.ItemFichaTecnicaResponseDTO;
import br.ed.ufape.bahiabrindes.dto.produto.ProdutoImagemRequest;
import br.ed.ufape.bahiabrindes.dto.produto.ProdutoImagemResponse;
import br.ed.ufape.bahiabrindes.dto.produto.ProdutoRequestDTO;
import br.ed.ufape.bahiabrindes.dto.produto.ProdutoResponseDTO;
import br.ed.ufape.bahiabrindes.model.entity.Categoria;
import br.ed.ufape.bahiabrindes.model.entity.MateriaPrima;
import br.ed.ufape.bahiabrindes.model.entity.MateriaPrimaProduto;
import br.ed.ufape.bahiabrindes.model.entity.Produto;
import br.ed.ufape.bahiabrindes.model.entity.ProdutoImagem;
import br.ed.ufape.bahiabrindes.repository.AvaliacaoProdutoRepository;
import br.ed.ufape.bahiabrindes.repository.MateriaPrimaEstoqueRepository;
import br.ed.ufape.bahiabrindes.repository.MateriaPrimaRepository;
import br.ed.ufape.bahiabrindes.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final MateriaPrimaRepository materiaPrimaRepository;
    private final CategoriaService categoriaService;
    private final AvaliacaoProdutoRepository avaliacaoRepository;
    private final MateriaPrimaEstoqueRepository materiaPrimaEstoqueRepository;

    @Autowired
    public ProdutoService(
            ProdutoRepository produtoRepository,
            MateriaPrimaRepository materiaPrimaRepository,
            CategoriaService categoriaService,
            AvaliacaoProdutoRepository avaliacaoRepository,
            MateriaPrimaEstoqueRepository materiaPrimaEstoqueRepository) {
        this.produtoRepository = produtoRepository;
        this.materiaPrimaRepository = materiaPrimaRepository;
        this.categoriaService = categoriaService;
        this.avaliacaoRepository = avaliacaoRepository;
        this.materiaPrimaEstoqueRepository = materiaPrimaEstoqueRepository;
    }

    public PageResponse<ProdutoResponseDTO> listar(int page, int pageSize, String status) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), pageSize, Sort.by("id").descending());
        Page<Produto> result = (status != null && !status.isBlank())
                ? produtoRepository.findByStatus(status.toUpperCase(), pageable)
                : produtoRepository.findAll(pageable);

        return PageResponse.<ProdutoResponseDTO>builder()
                .items(result.getContent().stream().map(this::toResponse).toList())
                .page(page)
                .pageSize(pageSize)
                .total(result.getTotalElements())
                .build();
    }

    public ProdutoResponseDTO buscarPorId(Long id) {
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado"));
        return toResponse(produto);
    }

    @Transactional
    public ProdutoResponseDTO criar(ProdutoRequestDTO request) {
        Produto produto = Produto.builder()
                .nome(request.getNome())
                .descricao(request.getDescricao())
                .sku(request.getSku())
                .precoVenda(request.getPrecoVenda())
                .custoProducao(request.getCustoProducao())
                .categoria(resolveCategoria(request.getCategoriaId()))
                .estoqueAtual(0)
                .estoqueMinimo(request.getEstoqueMinimo() != null ? request.getEstoqueMinimo() : 0)
                .status(request.getStatus() != null ? request.getStatus() : "ATIVO")
                .condicoesPagamento(request.getCondicoesPagamento())
                .prazoProducao(request.getPrazoProducao())
                .observacoes(request.getObservacoes())
                .itensFichaTecnica(new ArrayList<>())
                .build();

        syncFichaTecnica(produto, request.getItensFichaTecnica());
        syncImagens(produto, request.getImagens());

        return toResponse(produtoRepository.save(produto));
    }

    @Transactional
    public ProdutoResponseDTO atualizar(Long id, ProdutoRequestDTO request) {
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado"));

        produto.setNome(request.getNome());
        produto.setDescricao(request.getDescricao());
        produto.setSku(request.getSku());
        produto.setPrecoVenda(request.getPrecoVenda());
        produto.setCustoProducao(request.getCustoProducao());
        produto.setCategoria(resolveCategoria(request.getCategoriaId()));
        produto.setEstoqueMinimo(request.getEstoqueMinimo() != null ? request.getEstoqueMinimo() : 0);
        produto.setStatus(request.getStatus() != null ? request.getStatus() : produto.getStatus());
        produto.setCondicoesPagamento(request.getCondicoesPagamento());
        produto.setPrazoProducao(request.getPrazoProducao());
        produto.setObservacoes(request.getObservacoes());

        syncFichaTecnica(produto, request.getItensFichaTecnica());
        syncImagens(produto, request.getImagens());

        return toResponse(produtoRepository.save(produto));
    }

    @Transactional
    public void remover(Long id) {
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado"));
        String novoStatus = "ATIVO".equals(produto.getStatus()) ? "INATIVO" : "ATIVO";
        produto.setStatus(novoStatus);
        produtoRepository.save(produto);
    }

    @Transactional
    public ProdutoResponseDTO toggleStatus(Long id) {
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado"));
        produto.setStatus("ATIVO".equals(produto.getStatus()) ? "INATIVO" : "ATIVO");
        return toResponse(produtoRepository.save(produto));
    }

    // ── Helpers ──────────────────────────────────────────────

    private void syncImagens(Produto produto, List<ProdutoImagemRequest> imagensRequest) {
        if (imagensRequest == null) return;

        produto.getImagens().clear();

        int ordem = 1;
        for (ProdutoImagemRequest req : imagensRequest) {
            ProdutoImagem imagem = ProdutoImagem.builder()
                    .produto(produto)
                    .url(req.getUrl().trim())
                    .ordem(req.getOrdem() != null ? req.getOrdem() : ordem)
                    .build();
            produto.getImagens().add(imagem);
            ordem++;
        }
    }

    private void syncFichaTecnica(Produto produto, List<ItemFichaTecnicaRequestDTO> itensRequest) {
        if (itensRequest == null)
            return;

        produto.getItensFichaTecnica().clear();

        for (ItemFichaTecnicaRequestDTO itemReq : itensRequest) {
            MateriaPrima mp = materiaPrimaRepository.findById(itemReq.getMateriaPrimaId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Matéria-prima não encontrada: " + itemReq.getMateriaPrimaId()));

            MateriaPrimaProduto item = MateriaPrimaProduto.builder()
                    .produto(produto)
                    .materiaPrima(mp)
                    .quantidadeNecessaria(itemReq.getQuantidadeNecessaria())
                    .build();

            produto.getItensFichaTecnica().add(item);
        }
    }

    private Categoria resolveCategoria(Long categoriaId) {
        if (categoriaId == null)
            return null;
        return categoriaService.buscarPorId(categoriaId);
    }

    private ProdutoResponseDTO toResponse(Produto produto) {
        List<ItemFichaTecnicaResponseDTO> itens = produto.getItensFichaTecnica() != null
                ? produto.getItensFichaTecnica().stream().map(this::toItemResponse).toList()
                : List.of();

        List<ProdutoImagemResponse> imagens = produto.getImagens() != null
                ? produto.getImagens().stream()
                        .sorted(java.util.Comparator.comparingInt(ProdutoImagem::getOrdem))
                        .map(img -> ProdutoImagemResponse.builder()
                                .id(img.getId())
                                .url(img.getUrl())
                                .ordem(img.getOrdem())
                                .build())
                        .toList()
                : List.of();

        Double media = avaliacaoRepository.calcularMediaPorProduto(produto.getId());
        long total = avaliacaoRepository.countByProdutoId(produto.getId());

        return ProdutoResponseDTO.builder()
                .id(produto.getId())
                .nome(produto.getNome())
                .descricao(produto.getDescricao())
                .sku(produto.getSku())
                .precoVenda(produto.getPrecoVenda())
                .custoProducao(produto.getCustoProducao())
                .categoriaId(produto.getCategoria() != null ? produto.getCategoria().getId() : null)
                .categoriaNome(produto.getCategoria() != null ? produto.getCategoria().getNome() : null)
                .estoqueAtual(calcularEstoqueDisponivel(produto))
                .estoqueMinimo(produto.getEstoqueMinimo())
                .status(produto.getStatus())
                .condicoesPagamento(produto.getCondicoesPagamento())
                .prazoProducao(produto.getPrazoProducao())
                .observacoes(produto.getObservacoes())
                .mediaAvaliacao(media != null ? media : 0.0)
                .totalAvaliacoes(total)
                .itensFichaTecnica(itens)
                .imagens(imagens)
                .build();
    }

    private int calcularEstoqueDisponivel(Produto produto) {
        List<MateriaPrimaProduto> itens = produto.getItensFichaTecnica();
        if (itens == null || itens.isEmpty()) return 0;
        int min = Integer.MAX_VALUE;
        for (MateriaPrimaProduto item : itens) {
            java.math.BigDecimal estoque = materiaPrimaEstoqueRepository.sumEstoqueAtual(item.getMateriaPrima().getId());
            if (estoque == null) estoque = java.math.BigDecimal.ZERO;
            java.math.BigDecimal qtd = item.getQuantidadeNecessaria();
            if (qtd == null || qtd.compareTo(java.math.BigDecimal.ZERO) <= 0) continue;
            int disponivel = estoque.divide(qtd, 0, java.math.RoundingMode.FLOOR).intValue();
            min = Math.min(min, disponivel);
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    private ItemFichaTecnicaResponseDTO toItemResponse(MateriaPrimaProduto item) {
        return ItemFichaTecnicaResponseDTO.builder()
                .id(item.getId())
                .materiaPrimaId(item.getMateriaPrima().getId())
                .materiaPrimaNome(item.getMateriaPrima().getNome())
                .materiaPrimaUnidade(item.getMateriaPrima().getUnidade())
                .quantidadeNecessaria(item.getQuantidadeNecessaria())
                .custoCalculado(item.getCustoCalculado())
                .build();
    }
}
