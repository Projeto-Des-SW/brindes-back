package br.ed.ufape.bahiabrindes.service;

import br.ed.ufape.bahiabrindes.dto.common.PageResponse;
import br.ed.ufape.bahiabrindes.dto.estoque.LocalEstoqueRequest;
import br.ed.ufape.bahiabrindes.dto.estoque.LocalEstoqueResponse;
import br.ed.ufape.bahiabrindes.model.entity.LocalEstoque;
import br.ed.ufape.bahiabrindes.repository.LocalEstoqueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class LocalEstoqueService {

    private final LocalEstoqueRepository localEstoqueRepository;

    @Autowired
    public LocalEstoqueService(LocalEstoqueRepository localEstoqueRepository) {
        this.localEstoqueRepository = localEstoqueRepository;
    }

    public PageResponse<LocalEstoqueResponse> listar(String search, int page, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), pageSize, Sort.by("id").descending());
        Page<LocalEstoque> result = localEstoqueRepository.search(blankToNull(search), pageable);

        return PageResponse.<LocalEstoqueResponse>builder()
                .items(result.getContent().stream().map(this::toResponse).toList())
                .page(page)
                .pageSize(pageSize)
                .total(result.getTotalElements())
                .build();
    }

    public LocalEstoqueResponse criar(LocalEstoqueRequest request) {
        LocalEstoque local = LocalEstoque.builder()
                .nome(request.getNome())
                .descricao(request.getDescricao())
                .ativo(true)
                .build();
        return toResponse(localEstoqueRepository.save(local));
    }

    public LocalEstoqueResponse atualizar(Long id, LocalEstoqueRequest request) {
        LocalEstoque local = localEstoqueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Local de estoque não encontrado"));
        local.setNome(request.getNome());
        local.setDescricao(request.getDescricao());
        return toResponse(localEstoqueRepository.save(local));
    }

    public void remover(Long id) {
        LocalEstoque local = localEstoqueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Local de estoque não encontrado"));
        local.setAtivo(false);
        localEstoqueRepository.save(local);
    }

    private LocalEstoqueResponse toResponse(LocalEstoque l) {
        return LocalEstoqueResponse.builder()
                .id(l.getId())
                .nome(l.getNome())
                .descricao(l.getDescricao())
                .build();
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

