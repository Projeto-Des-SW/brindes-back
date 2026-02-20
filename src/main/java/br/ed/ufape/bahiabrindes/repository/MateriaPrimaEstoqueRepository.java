package br.ed.ufape.bahiabrindes.repository;

import br.ed.ufape.bahiabrindes.model.entity.MateriaPrimaEstoque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface MateriaPrimaEstoqueRepository extends JpaRepository<MateriaPrimaEstoque, Long> {

    Optional<MateriaPrimaEstoque> findByMateriaPrima_IdAndFornecedor_Id(Long materiaPrimaId, Long fornecedorId);

    Optional<MateriaPrimaEstoque> findByMateriaPrima_IdAndFornecedorIsNull(Long materiaPrimaId);

    @Query("SELECT SUM(e.estoqueAtual) FROM MateriaPrimaEstoque e WHERE e.materiaPrima.id = :materiaPrimaId")
    BigDecimal sumEstoqueAtual(@Param("materiaPrimaId") Long materiaPrimaId);
}

