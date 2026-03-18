package br.ed.ufape.bahiabrindes.repository;

import br.ed.ufape.bahiabrindes.model.entity.MovimentacaoEstoque;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Long> {

    @Query("""
      SELECT m
      FROM MovimentacaoEstoque m
      LEFT JOIN m.fornecedor f
      LEFT JOIN m.usuario u
      LEFT JOIN m.localDestino l
      WHERE m.materiaPrima IS NOT NULL
        AND (:tipo IS NULL OR m.tipo = :tipo)
        AND (
            :search IS NULL
          OR LOWER(m.materiaPrima.nome) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
          OR LOWER(f.razaoSocial)       LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
          OR LOWER(f.nomeFantasia)      LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
          OR LOWER(u.nome)              LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
          OR LOWER(l.nome)              LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
        )
  """)
  Page<MovimentacaoEstoque> search(
          @Param("search") String search,
          @Param("tipo") String tipo,
          Pageable pageable
  );
}

