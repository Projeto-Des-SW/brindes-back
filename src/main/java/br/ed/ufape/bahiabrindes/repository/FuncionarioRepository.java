package br.ed.ufape.bahiabrindes.repository;

import br.ed.ufape.bahiabrindes.model.entity.Funcionario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {

    @Query("""
        SELECT f FROM Funcionario f
        WHERE :search IS NULL
           OR LOWER(f.nome)  LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
           OR LOWER(f.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
        """)
    Page<Funcionario> search(@Param("search") String search, Pageable pageable);

    Optional<Funcionario> findByEmail(String email);

    Optional<Funcionario> findByNome(String nome);

    Optional<Funcionario> findByEmailAndAtivoTrue(String email);

    List<Funcionario> findByAtivoTrue();

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);
}
