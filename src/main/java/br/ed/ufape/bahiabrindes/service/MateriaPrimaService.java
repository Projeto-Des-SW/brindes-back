package br.ed.ufape.bahiabrindes.service;

import br.ed.ufape.bahiabrindes.dto.common.PageResponse;
import br.ed.ufape.bahiabrindes.dto.estoque.MateriaPrimaRequest;
import br.ed.ufape.bahiabrindes.dto.estoque.MateriaPrimaResponse;
import br.ed.ufape.bahiabrindes.model.entity.Categoria;
import br.ed.ufape.bahiabrindes.model.entity.Fornecedor;
import br.ed.ufape.bahiabrindes.model.entity.LocalEstoque;
import br.ed.ufape.bahiabrindes.model.entity.MateriaPrima;
import br.ed.ufape.bahiabrindes.repository.FornecedorRepository;
import br.ed.ufape.bahiabrindes.repository.LocalEstoqueRepository;
import br.ed.ufape.bahiabrindes.repository.MateriaPrimaEstoqueRepository;
import br.ed.ufape.bahiabrindes.repository.MateriaPrimaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class MateriaPrimaService {

    private final MateriaPrimaRepository materiaPrimaRepository;
    private final MateriaPrimaEstoqueRepository materiaPrimaEstoqueRepository;
    private final CategoriaService categoriaService;
    private final FornecedorRepository fornecedorRepository;
    private final LocalEstoqueRepository localEstoqueRepository;

    @Autowired
    public MateriaPrimaService(
            MateriaPrimaRepository materiaPrimaRepository,
            MateriaPrimaEstoqueRepository materiaPrimaEstoqueRepository,
            CategoriaService categoriaService,
            FornecedorRepository fornecedorRepository,
            LocalEstoqueRepository localEstoqueRepository
    ) {
        this.materiaPrimaRepository = materiaPrimaRepository;
        this.materiaPrimaEstoqueRepository = materiaPrimaEstoqueRepository;
        this.categoriaService = categoriaService;
        this.fornecedorRepository = fornecedorRepository;
        this.localEstoqueRepository = localEstoqueRepository;
    }

    public PageResponse<MateriaPrimaResponse> listar(String search, String categoria, int page, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), pageSize, Sort.by("id").descending());
        Page<MateriaPrima> result = materiaPrimaRepository.search(blankToNull(search), blankToNull(categoria), pageable);

        return PageResponse.<MateriaPrimaResponse>builder()
                .items(result.getContent().stream().map(this::toResponse).toList())
                .page(page)
                .pageSize(pageSize)
                .total(result.getTotalElements())
                .build();
    }

    public MateriaPrimaResponse buscarPorId(Long id) {
        MateriaPrima mp = materiaPrimaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Matéria-prima não encontrada"));
        return toResponse(mp);
    }

    public MateriaPrimaResponse criar(MateriaPrimaRequest request) {
        if (materiaPrimaRepository.existsBySku(request.getCodigo())) {
            throw new IllegalArgumentException("Já existe uma matéria-prima com o código \"" + request.getCodigo() + "\"");
        }
        Categoria categoria = resolveCategoria(request);
        Fornecedor fornecedorPrincipal = resolveFornecedorPrincipal(request.getFornecedorPrincipalId());

        MateriaPrima mp = MateriaPrima.builder()
                .sku(request.getCodigo())
                .nome(request.getDescricao())
                .unidade(request.getUnidade())
                .estoqueMinimo(nvl(request.getEstoqueMinimo()))
                .atualizadoEm(LocalDateTime.now())
                .categoria(categoria)
                .fornecedorPrincipal(fornecedorPrincipal)
                .fornecedorSecundario(resolveFornecedor(request.getFornecedorSecundarioId()))
                .localEstoque(resolveLocalEstoque(request.getLocalEstoqueId()))
                .build();

        return toResponse(materiaPrimaRepository.save(mp));
    }

    public MateriaPrimaResponse atualizar(Long id, MateriaPrimaRequest request) {
        MateriaPrima mp = materiaPrimaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Matéria-prima não encontrada"));
        if (materiaPrimaRepository.existsBySkuAndIdNot(request.getCodigo(), id)) {
            throw new IllegalArgumentException("Já existe uma matéria-prima com o código \"" + request.getCodigo() + "\"");
        }

        mp.setSku(request.getCodigo());
        mp.setNome(request.getDescricao());
        mp.setUnidade(request.getUnidade());
        mp.setEstoqueMinimo(nvl(request.getEstoqueMinimo()));
        mp.setAtualizadoEm(LocalDateTime.now());
        mp.setCategoria(resolveCategoria(request));
        mp.setFornecedorPrincipal(resolveFornecedorPrincipal(request.getFornecedorPrincipalId()));
        mp.setFornecedorSecundario(resolveFornecedor(request.getFornecedorSecundarioId()));
        mp.setLocalEstoque(resolveLocalEstoque(request.getLocalEstoqueId()));

        return toResponse(materiaPrimaRepository.save(mp));
    }

    public void remover(Long id) {
        MateriaPrima mp = materiaPrimaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Matéria-prima não encontrada"));
        materiaPrimaRepository.delete(mp);
    }

    public MateriaPrimaResponse toggleStatus(Long id) {
        MateriaPrima mp = materiaPrimaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Matéria-prima não encontrada"));
        mp.setAtivo(!(Boolean.TRUE.equals(mp.getAtivo())));
        return toResponse(materiaPrimaRepository.save(mp));
    }

    private MateriaPrimaResponse toResponse(MateriaPrima mp) {
        BigDecimal estoqueAtual = materiaPrimaEstoqueRepository.sumEstoqueAtual(mp.getId());
        String categoria = mp.getCategoria() != null ? mp.getCategoria().getNome() : "";
        String fornecedor = "";
        if (mp.getFornecedorPrincipal() != null) {
            fornecedor = blankToNull(mp.getFornecedorPrincipal().getNomeFantasia()) != null
                    ? mp.getFornecedorPrincipal().getNomeFantasia()
                    : mp.getFornecedorPrincipal().getRazaoSocial();
        }

        Long fornSecundarioId = mp.getFornecedorSecundario() != null ? mp.getFornecedorSecundario().getId() : null;
        String fornSecundarioNome = "";
        if (mp.getFornecedorSecundario() != null) {
            fornSecundarioNome = blankToNull(mp.getFornecedorSecundario().getNomeFantasia()) != null
                    ? mp.getFornecedorSecundario().getNomeFantasia()
                    : mp.getFornecedorSecundario().getRazaoSocial();
        }

        Long localEstoqueId = mp.getLocalEstoque() != null ? mp.getLocalEstoque().getId() : null;
        String localEstoqueNome = mp.getLocalEstoque() != null ? mp.getLocalEstoque().getNome() : "";

        return MateriaPrimaResponse.builder()
                .id(mp.getId())
                .codigo(mp.getSku() != null ? mp.getSku() : "")
                .descricao(mp.getNome())
                .unidade(mp.getUnidade())
                .categoria(categoria)
                .fornecedorPrincipal(fornecedor)
                .fornecedorSecundarioId(fornSecundarioId)
                .fornecedorSecundario(fornSecundarioNome)
                .localEstoqueId(localEstoqueId)
                .localEstoque(localEstoqueNome)
                .estoqueAtual(estoqueAtual != null ? estoqueAtual : BigDecimal.ZERO)
                .estoqueMinimo(mp.getEstoqueMinimo() != null ? mp.getEstoqueMinimo() : BigDecimal.ZERO)
                .build();
    }

    private Categoria resolveCategoria(MateriaPrimaRequest request) {
        if (request.getCategoriaId() != null) {
            return categoriaService.buscarPorId(request.getCategoriaId());
        }
        return categoriaService.getOrCreateByNome(request.getCategoria());
    }

    private Fornecedor resolveFornecedorPrincipal(Long fornecedorPrincipalId) {
        if (fornecedorPrincipalId == null) return null;
        return fornecedorRepository.findById(fornecedorPrincipalId)
                .orElseThrow(() -> new IllegalArgumentException("Fornecedor principal não encontrado"));
    }

    private Fornecedor resolveFornecedor(Long id) {
        if (id == null) return null;
        return fornecedorRepository.findById(id).orElse(null);
    }

    private LocalEstoque resolveLocalEstoque(Long id) {
        if (id == null) return null;
        return localEstoqueRepository.findById(id).orElse(null);
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

