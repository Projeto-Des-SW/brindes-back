package br.ed.ufape.bahiabrindes.controller;

import br.ed.ufape.bahiabrindes.dto.auth.LoginRequest;
import br.ed.ufape.bahiabrindes.dto.auth.LoginResponse;
import br.ed.ufape.bahiabrindes.dto.auth.RegisterRequest;
import br.ed.ufape.bahiabrindes.dto.auth.RegisterTokenRequest;
import br.ed.ufape.bahiabrindes.dto.clientes.ClienteRequest;
import br.ed.ufape.bahiabrindes.dto.clientes.ClienteResponse;
import br.ed.ufape.bahiabrindes.model.enums.TipoUsuario;
import br.ed.ufape.bahiabrindes.service.AuthService;
import br.ed.ufape.bahiabrindes.service.ClienteService;
import br.ed.ufape.bahiabrindes.service.PasswordResetService;
import br.ed.ufape.bahiabrindes.service.RegistrationTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;
    @MockBean
    private ClienteService clienteService;
    @MockBean
    private PasswordResetService passwordResetService;
    @MockBean
    private RegistrationTokenService registrationTokenService;

    private LoginRequest loginRequest;
    private LoginResponse loginResponse;
    private RegisterRequest registerRequest;
    private ClienteResponse clienteResponse;
    private RegisterTokenRequest registerTokenRequest;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest();
        loginRequest.setEmail("teste@bahiabrindes.com");
        loginRequest.setSenha("senha123");

        Set<String> perfis = new HashSet<>();
        perfis.add("ADMIN");

        loginResponse = new LoginResponse();
        loginResponse.setToken("token_jwt_mockado");
        loginResponse.setTipo("Bearer");
        loginResponse.setId(1L);
        loginResponse.setNome("Teste User");
        loginResponse.setEmail("teste@bahiabrindes.com");
        loginResponse.setTipoUsuario(TipoUsuario.FUNCIONARIO);
        loginResponse.setPerfis(perfis);

        registerRequest = new RegisterRequest();
        registerRequest.setNome("Cliente Teste");
        registerRequest.setEmail("cliente@bahiabrindes.com");
        registerRequest.setSenha("senha123");
        registerRequest.setToken("token-cadastro");
        registerRequest.setDocumento("12345678901");
        registerRequest.setTelefone("81999999999");

        registerTokenRequest = new RegisterTokenRequest("cliente@bahiabrindes.com");

        clienteResponse = new ClienteResponse();
        clienteResponse.setId(2L);
        clienteResponse.setNome("Cliente Teste");
        clienteResponse.setEmail("cliente@bahiabrindes.com");
        clienteResponse.setDocumento("12345678901");
        clienteResponse.setTelefone("81999999999");
    }

    @Test
    @DisplayName("POST /api/auth/login - Deve realizar login com sucesso")
    void deveRealizarLoginComSucesso() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class))).thenReturn(loginResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("token_jwt_mockado"))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Teste User"))
                .andExpect(jsonPath("$.email").value("teste@bahiabrindes.com"))
                .andExpect(jsonPath("$.tipoUsuario").value("FUNCIONARIO"))
                .andExpect(jsonPath("$.perfis[0]").value("ADMIN"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Deve retornar 401 quando credenciais inválidas")
    void deveRetornar401QuandoCredenciaisInvalidas() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Email ou senha inválidos"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/login - Deve retornar 400 quando email inválido")
    void deveRetornar400QuandoEmailInvalido() throws Exception {
        // Arrange
        loginRequest.setEmail("email-invalido");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/login - Deve retornar 400 quando email em branco")
    void deveRetornar400QuandoEmailEmBranco() throws Exception {
        // Arrange
        loginRequest.setEmail("");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/login - Deve retornar 400 quando senha em branco")
    void deveRetornar400QuandoSenhaEmBranco() throws Exception {
        // Arrange
        loginRequest.setSenha("");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/register - Deve cadastrar cliente com sucesso")
    void deveCadastrarClienteComSucesso() throws Exception {
        when(clienteService.criar(any(ClienteRequest.class))).thenReturn(clienteResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.nome").value("Cliente Teste"))
                .andExpect(jsonPath("$.email").value("cliente@bahiabrindes.com"));
    }

    @Test
    @DisplayName("POST /api/auth/register - Deve retornar 400 quando dados do cliente forem inválidos")
    void deveRetornar400QuandoDadosDoClienteForemInvalidos() throws Exception {
        registerRequest.setEmail("email-invalido");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/register/request-token - Deve solicitar token de cadastro")
    void deveSolicitarTokenDeCadastro() throws Exception {
        mockMvc.perform(post("/api/auth/register/request-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerTokenRequest)))
                .andExpect(status().isOk());
    }
}
