package br.ed.ufape.bahiabrindes.repository;

import br.ed.ufape.bahiabrindes.model.entity.MateriaPrimaProduto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MateriaPrimaProdutoRepository extends JpaRepository<MateriaPrimaProduto, Long> {

    List<MateriaPrimaProduto> findByProdutoId(Long produtoId);
}
