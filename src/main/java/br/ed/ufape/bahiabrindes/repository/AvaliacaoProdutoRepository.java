package br.ed.ufape.bahiabrindes.repository;

import br.ed.ufape.bahiabrindes.model.entity.AvaliacaoProduto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AvaliacaoProdutoRepository extends JpaRepository<AvaliacaoProduto, Long> {

    List<AvaliacaoProduto> findByProdutoIdOrderByCriadoEmDesc(Long produtoId);

    boolean existsByOrcamentoIdAndProdutoId(Long orcamentoId, Long produtoId);

    Optional<AvaliacaoProduto> findByOrcamentoIdAndProdutoId(Long orcamentoId, Long produtoId);

    @Query("SELECT COALESCE(AVG(a.nota * 1.0), 0.0) FROM AvaliacaoProduto a WHERE a.produto.id = :produtoId")
    Double calcularMediaPorProduto(@Param("produtoId") Long produtoId);

    long countByProdutoId(Long produtoId);
}
