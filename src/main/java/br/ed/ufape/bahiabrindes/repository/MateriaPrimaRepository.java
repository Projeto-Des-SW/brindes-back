package br.ed.ufape.bahiabrindes.repository;

import br.ed.ufape.bahiabrindes.model.entity.MateriaPrima;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MateriaPrimaRepository extends JpaRepository<MateriaPrima, Long> {

    @Query(
            value = """
                SELECT
                    mp.id AS id,
                    mp.sku AS codigo,
                    mp.nome AS materiaPrima,
                    COALESCE(SUM(mpe.estoque_atual), 0) AS quantidadeAtual,
                    COALESCE(mp.estoque_minimo, 0) AS estoqueMinimo,
                    MAX(mpe.preco_custo) AS valorUnitario
                FROM materias_primas mp
                LEFT JOIN materias_primas_estoque mpe ON mpe.materias_primas_id = mp.id
                WHERE (
                    :search IS NULL
                    OR LOWER(mp.nome) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(mp.sku)  LIKE LOWER(CONCAT('%', :search, '%'))
                )
                GROUP BY mp.id, mp.sku, mp.nome, mp.estoque_minimo
                HAVING (
                    :status IS NULL
                    OR (:status = 'ABAIXO' AND COALESCE(SUM(mpe.estoque_atual), 0) <  COALESCE(mp.estoque_minimo, 0))
                    OR (:status = 'NORMAL' AND COALESCE(SUM(mpe.estoque_atual), 0) >= COALESCE(mp.estoque_minimo, 0))
                )
            """,
            countQuery = """
                SELECT COUNT(*) FROM (
                    SELECT mp.id
                    FROM materias_primas mp
                    LEFT JOIN materias_primas_estoque mpe ON mpe.materias_primas_id = mp.id
                    WHERE (
                           :search IS NULL
                        OR LOWER(mp.nome) LIKE LOWER(CONCAT('%', :search, '%'))
                        OR LOWER(COALESCE(mp.sku, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                    )
                    GROUP BY mp.id, mp.estoque_minimo
                    HAVING (
                           :status IS NULL
                        OR (
                            :status = 'ABAIXO'
                            AND COALESCE(SUM(mpe.estoque_atual), 0) < COALESCE(mp.estoque_minimo, 0)
                        )
                        OR (
                            :status = 'NORMAL'
                            AND COALESCE(SUM(mpe.estoque_atual), 0) >= COALESCE(mp.estoque_minimo, 0)
                        )
                    )
                ) t
                """,
            nativeQuery = true
    )
    Page<ProdutoEstoqueItemProjection> findEstoqueItems(
            @Param("search") String search,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("""
        SELECT mp
        FROM MateriaPrima mp
        WHERE (
               :search IS NULL
            OR LOWER(mp.nome) LIKE LOWER(CONCAT('%', CAST(:search as string), '%'))
            OR LOWER(COALESCE(mp.sku, '')) LIKE LOWER(CONCAT('%', CAST(:search as string), '%'))
        )
          AND (:categoria IS NULL OR (mp.categoria IS NOT NULL AND mp.categoria.nome = :categoria))
        """)
    Page<MateriaPrima> search(@Param("search") String search, @Param("categoria") String categoria, Pageable pageable);
}

