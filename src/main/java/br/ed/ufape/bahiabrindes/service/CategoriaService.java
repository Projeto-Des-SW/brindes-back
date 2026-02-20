package br.ed.ufape.bahiabrindes.service;

import br.ed.ufape.bahiabrindes.dto.estoque.CategoriaResponse;
import br.ed.ufape.bahiabrindes.dto.estoque.CategoriaRequest;
import br.ed.ufape.bahiabrindes.model.entity.Categoria;
import br.ed.ufape.bahiabrindes.repository.CategoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    @Autowired
    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public List<CategoriaResponse> listarAtivas() {
        return categoriaRepository.findByAtivaTrueOrderByNomeAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    public Categoria buscarPorId(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada"));
    }

    public CategoriaResponse criar(CategoriaRequest request) {
        String nome = normalizeNome(request.getNome());

        Optional<Categoria> existing = categoriaRepository.findByNomeIgnoreCase(nome);
        if (existing.isPresent()) {
            Categoria c = existing.get();
            c.setNome(nome);
            c.setAtiva(true);
            return toResponse(categoriaRepository.save(c));
        }

        Categoria c = Categoria.builder()
                .nome(nome)
                .ativa(true)
                .build();
        return toResponse(categoriaRepository.save(c));
    }

    public CategoriaResponse atualizar(Long id, CategoriaRequest request) {
        Categoria c = buscarPorId(id);
        String nome = normalizeNome(request.getNome());

        Optional<Categoria> other = categoriaRepository.findByNomeIgnoreCase(nome);
        if (other.isPresent() && !other.get().getId().equals(id)) {
            throw new IllegalArgumentException("Já existe uma categoria com este nome");
        }

        c.setNome(nome);
        return toResponse(categoriaRepository.save(c));
    }

    public void remover(Long id) {
        Categoria c = buscarPorId(id);
        c.setAtiva(false);
        categoriaRepository.save(c);
    }

    public Categoria getOrCreateByNome(String nome) {
        if (nome == null || nome.trim().isEmpty()) return null;
        String normalized = nome.trim();
        return categoriaRepository.findByNomeIgnoreCase(normalized)
                .orElseGet(() -> categoriaRepository.save(Categoria.builder().nome(normalized).ativa(true).build()));
    }

    private static String normalizeNome(String nome) {
        if (nome == null) throw new IllegalArgumentException("Nome é obrigatório");
        String normalized = nome.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException("Nome é obrigatório");
        return normalized;
    }

    private CategoriaResponse toResponse(Categoria c) {
        return CategoriaResponse.builder()
                .id(c.getId())
                .nome(c.getNome())
                .build();
    }
}

