package br.ed.ufape.bahiabrindes.repository;

import br.ed.ufape.bahiabrindes.model.entity.Fornecedor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {

    @Query("""
      SELECT f
      FROM Fornecedor f
      WHERE (:status IS NULL OR f.status = :status)
        AND (
            :search IS NULL
          OR LOWER(f.razaoSocial) LIKE LOWER(CONCAT('%', CAST(:search as string), '%'))
          OR LOWER(f.nomeFantasia) LIKE LOWER(CONCAT('%', CAST(:search as string), '%'))
          OR f.cnpj LIKE (CONCAT('%', CAST(:search as string), '%'))
        )
  """)
  Page<Fornecedor> search(@Param("search") String search, @Param("status") String status, Pageable pageable);
}

