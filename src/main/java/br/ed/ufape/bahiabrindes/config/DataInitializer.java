package br.ed.ufape.bahiabrindes.config;

import br.ed.ufape.bahiabrindes.model.entity.Funcionario;
import br.ed.ufape.bahiabrindes.model.entity.Perfil;
import br.ed.ufape.bahiabrindes.repository.FuncionarioRepository;
import br.ed.ufape.bahiabrindes.repository.PerfilRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final FuncionarioRepository funcionarioRepository;
    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(String... args) {
        atualizarConstraintStatus();
        atualizarColunasArte();
        atualizarColunasComentario();
        criarPerfisPadrao();
        criarFuncionarioAdmin();
    }

    private void atualizarConstraintStatus() {
        try {
            entityManager.createNativeQuery(
                "ALTER TABLE orcamentos DROP CONSTRAINT IF EXISTS orcamentos_status_check"
            ).executeUpdate();
            entityManager.createNativeQuery(
                "ALTER TABLE orcamentos ADD CONSTRAINT orcamentos_status_check " +
                "CHECK (status IN ('ORCAMENTO_SOLICITADO','PAGAMENTO_APROVADO','ARTE_PENDENTE','ARTES_APROVADAS','EM_PRODUCAO','CONCLUIDO','CANCELADO'))"
            ).executeUpdate();
            log.info("Constraint orcamentos_status_check atualizada.");
        } catch (Exception e) {
            log.warn("Não foi possível atualizar constraint de status: {}", e.getMessage());
        }
    }

    private void atualizarColunasComentario() {
        try {
            entityManager.createNativeQuery(
                "ALTER TABLE comentarios_orcamento ADD COLUMN IF NOT EXISTS produto_nome VARCHAR(500)"
            ).executeUpdate();
            log.info("Coluna produto_nome garantida em comentarios_orcamento.");
        } catch (Exception e) {
            log.warn("Ajuste produto_nome comentario: {}", e.getMessage());
        }
    }

    private void atualizarColunasArte() {
        try {
            entityManager.createNativeQuery(
                "ALTER TABLE artes_orcamento ALTER COLUMN imagem_url DROP NOT NULL"
            ).executeUpdate();
            log.info("Coluna imagem_url tornado nullable em artes_orcamento.");
        } catch (Exception e) {
            log.warn("Ajuste imagem_url: {}", e.getMessage());
        }
        try {
            entityManager.createNativeQuery(
                "ALTER TABLE artes_orcamento ADD COLUMN IF NOT EXISTS conteudo_arquivo bytea"
            ).executeUpdate();
            log.info("Coluna conteudo_arquivo garantida em artes_orcamento.");
        } catch (Exception e) {
            log.warn("Ajuste conteudo_arquivo: {}", e.getMessage());
        }
        try {
            entityManager.createNativeQuery(
                "ALTER TABLE artes_orcamento ADD COLUMN IF NOT EXISTS nome_arquivo VARCHAR(500)"
            ).executeUpdate();
            log.info("Coluna nome_arquivo garantida em artes_orcamento.");
        } catch (Exception e) {
            log.warn("Ajuste nome_arquivo: {}", e.getMessage());
        }
    }

    private void criarPerfisPadrao() {

        if (!perfilRepository.existsByNome("ROLE_FUNCIONARIO")) {
            perfilRepository.save(
                Perfil.builder()
                    .nome("ROLE_FUNCIONARIO")
                    .build()
            );
            log.info("Perfil 'ROLE_FUNCIONARIO' criado.");
        }

        if (!perfilRepository.existsByNome("ROLE_ADMIN")) {
            perfilRepository.save(
                Perfil.builder()
                    .nome("ROLE_ADMIN")
                    .build()
            );
            log.info("Perfil 'ROLE_ADMIN' criado.");
        }
    }

    private void criarFuncionarioAdmin() {
        String email = "admin@gmail.com";

        Funcionario admin = funcionarioRepository.findByEmail(email)
                .orElse(null);

        Perfil perfilFuncionario = perfilRepository.findByNome("ROLE_FUNCIONARIO")
                .orElseThrow();

        Perfil perfilAdmin = perfilRepository.findByNome("ROLE_ADMIN")
                .orElseThrow();

        if (admin == null) {
            admin = Funcionario.builder()
                    .nome("Administrador")
                    .email(email)
                    .senha(passwordEncoder.encode("123456"))
                    .ativo(true)
                    .perfis(Set.of(perfilFuncionario, perfilAdmin))
                    .build();

            funcionarioRepository.save(admin);
            log.info("Admin criado.");
        } else {
            admin.getPerfis().add(perfilAdmin);
            funcionarioRepository.save(admin);

            log.info("Admin atualizado com ROLE_ADMIN.");
        }
    }
}