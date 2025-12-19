package br.ed.ufape.bahiabrindes.repository;

import br.ed.ufape.bahiabrindes.model.entity.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {
    
    Optional<Funcionario> findByEmail(String email);
    
    Optional<Funcionario> findByEmailAndAtivoTrue(String email);
    
    List<Funcionario> findByAtivoTrue();
    
    boolean existsByEmail(String email);
}
