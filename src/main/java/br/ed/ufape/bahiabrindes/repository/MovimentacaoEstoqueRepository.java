package br.ed.ufape.bahiabrindes.repository;

import br.ed.ufape.bahiabrindes.model.entity.MovimentacaoEstoque;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Long> {

    @Query("""
      SELECT m
      FROM MovimentacaoEstoque m
      LEFT JOIN m.fornecedor f
      LEFT JOIN m.usuario u
      LEFT JOIN m.localDestino l
      WHERE m.materiaPrima IS NOT NULL
        AND (:tipo IS NULL OR m.tipo = :tipo)
        AND (:from IS NULL OR m.dataMovimentacao >= :from)
        AND (:to   IS NULL OR m.dataMovimentacao <= :to)
        AND (
            :search IS NULL
          OR LOWER(m.materiaPrima.nome) LIKE LOWER(CONCAT('%', CAST(:search as string), '%'))
          OR LOWER(f.razaoSocial)       LIKE LOWER(CONCAT('%', CAST(:search as string), '%'))
          OR LOWER(f.nomeFantasia)      LIKE LOWER(CONCAT('%', CAST(:search as string), '%'))
          OR LOWER(u.nome)              LIKE LOWER(CONCAT('%', CAST(:search as string), '%'))
          OR LOWER(l.nome)              LIKE LOWER(CONCAT('%', CAST(:search as string), '%'))
        )
  """)
  Page<MovimentacaoEstoque> search(
          @Param("search") String search,
          @Param("tipo") String tipo,
          @Param("from") LocalDateTime from,
          @Param("to") LocalDateTime to,
          Pageable pageable
  );
}

