package br.ed.ufape.bahiabrindes.repository;

import br.ed.ufape.bahiabrindes.model.entity.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByDocumento(String documento);

    Optional<Cliente> findByEmail(String email);
    
    Optional<Cliente> findByEmailAndAtivoTrue(String email);

    @Query("""
        SELECT c FROM Cliente c
        WHERE (
               :search IS NULL
            OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(COALESCE(c.documento, '')) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(COALESCE(c.email, '')) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(COALESCE(c.telefone, '')) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(COALESCE(c.segmentacao, '')) LIKE LOWER(CONCAT('%', :search, '%'))
        )
        """)
    Page<Cliente> search(@Param("search") String search, Pageable pageable);
}