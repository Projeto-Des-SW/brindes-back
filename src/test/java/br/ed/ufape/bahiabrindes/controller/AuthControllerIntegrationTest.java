package br.ed.ufape.bahiabrindes.controller;

import br.ed.ufape.bahiabrindes.dto.auth.LoginRequest;
import br.ed.ufape.bahiabrindes.dto.auth.RegisterRequest;
import br.ed.ufape.bahiabrindes.dto.auth.RegisterTokenRequest;
import br.ed.ufape.bahiabrindes.dto.clientes.ClienteUpdateRequest;
import br.ed.ufape.bahiabrindes.model.entity.Cliente;
import br.ed.ufape.bahiabrindes.model.entity.Funcionario;
import br.ed.ufape.bahiabrindes.model.entity.Perfil;
import br.ed.ufape.bahiabrindes.repository.ClienteRepository;
import br.ed.ufape.bahiabrindes.repository.FuncionarioRepository;
import br.ed.ufape.bahiabrindes.repository.PerfilRepository;
import br.ed.ufape.bahiabrindes.service.RegistrationTokenService;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
    private ClienteRepository clienteRepository;

    @Autowired
    private PerfilRepository perfilRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RegistrationTokenService registrationTokenService;

    private Funcionario funcionarioTeste;
    private Cliente clienteTeste;

    @BeforeEach
    void setUp() {
        clienteRepository.deleteAll();
        funcionarioRepository.deleteAll();
        perfilRepository.deleteAll();

        Perfil perfilAdmin = new Perfil();
        perfilAdmin.setNome("ADMIN");
        perfilAdmin = perfilRepository.save(perfilAdmin);

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

        clienteTeste = new Cliente();
        clienteTeste.setNome("Cliente Teste");
        clienteTeste.setEmail("cliente@bahiabrindes.com");
        clienteTeste.setSenha(passwordEncoder.encode("senha123"));
        clienteTeste.setAtivo(true);
        clienteTeste = clienteRepository.save(clienteTeste);
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
                .andExpect(jsonPath("$.tipoUsuario").value("FUNCIONARIO"))
                .andExpect(jsonPath("$.perfis").isArray())
                .andExpect(jsonPath("$.perfis[0]").value("ADMIN"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Deve realizar login de cliente com sucesso")
    void deveRealizarLoginDeClienteComSucesso() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("cliente@bahiabrindes.com");
        request.setSenha("senha123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoUsuario").value("CLIENTE"))
                .andExpect(jsonPath("$.perfis").value(containsInAnyOrder("CLIENTE")));
    }

    @Test
    @DisplayName("POST /api/auth/register/request-token - Deve solicitar token de cadastro")
    void deveSolicitarTokenDeCadastro() throws Exception {
        RegisterTokenRequest request = new RegisterTokenRequest("novo.cliente@bahiabrindes.com");

        mockMvc.perform(post("/api/auth/register/request-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/auth/register - Deve cadastrar cliente com sucesso")
    void deveCadastrarClienteComSucesso() throws Exception {
        RegisterTokenRequest tokenRequest = new RegisterTokenRequest("novo.cliente@bahiabrindes.com");
        mockMvc.perform(post("/api/auth/register/request-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tokenRequest)))
                .andExpect(status().isOk());

        String token = ReflectionTestUtils.invokeMethod(
                registrationTokenService,
                "getTokenForEmail",
                "novo.cliente@bahiabrindes.com"
        );

        RegisterRequest request = new RegisterRequest();
        request.setNome("Novo Cliente");
        request.setEmail("novo.cliente@bahiabrindes.com");
        request.setSenha("senha123");
        request.setToken(token);
        request.setDocumento("98765432100");
        request.setTelefone("81988887777");
        request.setEndereco("Rua das Flores, 123");
        request.setSegmentacao("Varejo");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.nome").value("Novo Cliente"))
                .andExpect(jsonPath("$.email").value("novo.cliente@bahiabrindes.com"))
                .andExpect(jsonPath("$.documento").value("98765432100"));
    }

    @Test
    @DisplayName("POST /api/auth/register - Deve retornar 400 para email já cadastrado")
    void deveRetornar400ParaEmailJaCadastradoNoRegister() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setNome("Outro Cliente");
        request.setEmail("cliente@bahiabrindes.com");
        request.setSenha("senha123");
        request.setToken("token-invalido");
        request.setDocumento("11122233344");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/register - Deve retornar 400 para token inválido")
    void deveRetornar400ParaTokenInvalidoNoRegister() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setNome("Outro Cliente");
        request.setEmail("outro@bahiabrindes.com");
        request.setSenha("senha123");
        request.setToken("token-invalido");
        request.setDocumento("11122233344");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
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

    @Test
    @DisplayName("GET /api/clientes/me - Cliente autenticado deve acessar o proprio perfil")
    void clienteAutenticadoDeveAcessarProprioPerfil() throws Exception {
        String token = realizarLogin("cliente@bahiabrindes.com", "senha123");

        mockMvc.perform(get("/api/clientes/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("cliente@bahiabrindes.com"));
    }

    @Test
    @DisplayName("GET /api/estoque/resumo - Cliente autenticado nao deve acessar area interna")
    void clienteAutenticadoNaoDeveAcessarAreaInterna() throws Exception {
        String token = realizarLogin("cliente@bahiabrindes.com", "senha123");

        mockMvc.perform(get("/api/estoque/resumo")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/clientes/me - Funcionario autenticado nao deve acessar area exclusiva de cliente")
    void funcionarioAutenticadoNaoDeveAcessarAreaExclusivaDeCliente() throws Exception {
        String token = realizarLogin("teste@bahiabrindes.com", "senha123");

        mockMvc.perform(get("/api/clientes/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /api/clientes/me - Cliente deve conseguir atualizar o proprio nome")
    void clienteDeveConseguirAtualizarProprioNome() throws Exception {
        String token = realizarLogin("cliente@bahiabrindes.com", "senha123");
        ClienteUpdateRequest request = new ClienteUpdateRequest();
        request.setNome("Cliente Atualizado");

        mockMvc.perform(patch("/api/clientes/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Cliente Atualizado"));
    }

    private String realizarLogin(String email, String senha) throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setSenha(senha);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }
}
