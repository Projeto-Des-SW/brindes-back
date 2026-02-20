package br.ed.ufape.bahiabrindes.repository;

import br.ed.ufape.bahiabrindes.model.entity.LocalEstoque;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LocalEstoqueRepository extends JpaRepository<LocalEstoque, Long> {

    @Query("""
        SELECT l
        FROM LocalEstoque l
        WHERE l.ativo = true
          AND (
               :search IS NULL
            OR LOWER(l.nome) LIKE LOWER(CONCAT('%', CAST(:search as string), '%'))
            OR LOWER(COALESCE(l.descricao, '')) LIKE LOWER(CONCAT('%', CAST(:search as string), '%'))
          )
        """)
    Page<LocalEstoque> search(@Param("search") String search, Pageable pageable);
}

