package br.ed.ufape.bahiabrindes.controller;

import br.ed.ufape.bahiabrindes.dto.auth.LoginRequest;
import br.ed.ufape.bahiabrindes.dto.auth.LoginResponse;
import br.ed.ufape.bahiabrindes.service.AuthService;
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

    private LoginRequest loginRequest;
    private LoginResponse loginResponse;

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
        loginResponse.setPerfis(perfis);
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
}
