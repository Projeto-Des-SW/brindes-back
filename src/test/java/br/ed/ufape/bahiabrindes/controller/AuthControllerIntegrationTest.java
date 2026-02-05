package br.ed.ufape.bahiabrindes.controller;

import br.ed.ufape.bahiabrindes.dto.auth.LoginRequest;
import br.ed.ufape.bahiabrindes.model.entity.Funcionario;
import br.ed.ufape.bahiabrindes.model.entity.Perfil;
import br.ed.ufape.bahiabrindes.repository.FuncionarioRepository;
import br.ed.ufape.bahiabrindes.repository.PerfilRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.matchesPattern;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired
    private PerfilRepository perfilRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Funcionario funcionarioTeste;

    @BeforeEach
    void setUp() {
        // Limpar apenas funcionários (perfis são criados pelo Liquibase)
        funcionarioRepository.deleteAll();

        // Buscar perfil ADMIN criado pelo Liquibase
        Perfil perfilAdmin = perfilRepository.findByNome("ADMIN")
            .orElseThrow(() -> new RuntimeException("Perfil ADMIN não encontrado"));

        // Criar funcionário de teste
        Set<Perfil> perfis = new HashSet<>();
        perfis.add(perfilAdmin);

        funcionarioTeste = new Funcionario();
        funcionarioTeste.setNome("Usuário Teste");
        funcionarioTeste.setEmail("teste@bahiabrindes.com");
        funcionarioTeste.setSenha(passwordEncoder.encode("senha123"));
        funcionarioTeste.setAtivo(true);
        funcionarioTeste.setPerfis(perfis);
        funcionarioTeste = funcionarioRepository.save(funcionarioTeste);
    }

    @Test
    @DisplayName("POST /api/auth/login - Deve realizar login end-to-end com sucesso")
    void deveRealizarLoginEndToEndComSucesso() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("teste@bahiabrindes.com");
        request.setSenha("senha123");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.id").value(funcionarioTeste.getId()))
                .andExpect(jsonPath("$.nome").value("Usuário Teste"))
                .andExpect(jsonPath("$.email").value("teste@bahiabrindes.com"))
                .andExpect(jsonPath("$.perfis").isArray())
                .andExpect(jsonPath("$.perfis[0]").value("ADMIN"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Deve retornar 401 com credenciais inválidas")
    void deveRetornar401ComCredenciaisInvalidas() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("teste@bahiabrindes.com");
        request.setSenha("senhaErrada");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/login - Deve retornar 401 para email não cadastrado")
    void deveRetornar401ParaEmailNaoCadastrado() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("naoexiste@bahiabrindes.com");
        request.setSenha("senha123");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/login - Deve retornar 400 para dados inválidos")
    void deveRetornar400ParaDadosInvalidos() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("email-invalido");
        request.setSenha("");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/login - Deve retornar 401 para usuário inativo")
    void deveRetornar401ParaUsuarioInativo() throws Exception {
        // Arrange
        funcionarioTeste.setAtivo(false);
        funcionarioRepository.save(funcionarioTeste);

        LoginRequest request = new LoginRequest();
        request.setEmail("teste@bahiabrindes.com");
        request.setSenha("senha123");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/login - Token gerado deve ser válido")
    void tokenGeradoDeveSerValido() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("teste@bahiabrindes.com");
        request.setSenha("senha123");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(matchesPattern("^eyJ[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_.+/=]+$")));
    }
}
