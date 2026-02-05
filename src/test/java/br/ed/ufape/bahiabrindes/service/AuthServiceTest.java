package br.ed.ufape.bahiabrindes.service;

import br.ed.ufape.bahiabrindes.dto.auth.LoginRequest;
import br.ed.ufape.bahiabrindes.dto.auth.LoginResponse;
import br.ed.ufape.bahiabrindes.model.entity.Funcionario;
import br.ed.ufape.bahiabrindes.model.entity.Perfil;
import br.ed.ufape.bahiabrindes.repository.FuncionarioRepository;
import br.ed.ufape.bahiabrindes.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private FuncionarioRepository funcionarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private Funcionario funcionario;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        Perfil perfil = new Perfil();
        perfil.setId(1L);
        perfil.setNome("ADMIN");

        Set<Perfil> perfis = new HashSet<>();
        perfis.add(perfil);

        funcionario = new Funcionario();
        funcionario.setId(1L);
        funcionario.setNome("Teste User");
        funcionario.setEmail("teste@bahiabrindes.com");
        funcionario.setSenha("$2a$10$encoded_password");
        funcionario.setAtivo(true);
        funcionario.setPerfis(perfis);

        loginRequest = new LoginRequest();
        loginRequest.setEmail("teste@bahiabrindes.com");
        loginRequest.setSenha("senha123");
    }

    @Test
    @DisplayName("Deve realizar login com sucesso")
    void deveRealizarLoginComSucesso() {
        // Arrange
        when(funcionarioRepository.findByEmailAndAtivoTrue(loginRequest.getEmail()))
                .thenReturn(Optional.of(funcionario));
        when(passwordEncoder.matches(loginRequest.getSenha(), funcionario.getSenha()))
                .thenReturn(true);
        when(jwtUtil.generateToken(anyString(), any(), any()))
                .thenReturn("token_jwt_mockado");

        // Act
        LoginResponse response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("token_jwt_mockado", response.getToken());
        assertEquals("Bearer", response.getTipo());
        assertEquals(funcionario.getId(), response.getId());
        assertEquals(funcionario.getNome(), response.getNome());
        assertEquals(funcionario.getEmail(), response.getEmail());
        assertTrue(response.getPerfis().contains("ADMIN"));
    }

    @Test
    @DisplayName("Deve lançar exceção quando email não encontrado")
    void deveLancarExcecaoQuandoEmailNaoEncontrado() {
        // Arrange
        when(funcionarioRepository.findByEmailAndAtivoTrue(loginRequest.getEmail()))
                .thenReturn(Optional.empty());

        // Act & Assert
        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(loginRequest)
        );
        
        assertEquals("Email ou senha inválidos", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar exceção quando senha incorreta")
    void deveLancarExcecaoQuandoSenhaIncorreta() {
        // Arrange
        when(funcionarioRepository.findByEmailAndAtivoTrue(loginRequest.getEmail()))
                .thenReturn(Optional.of(funcionario));
        when(passwordEncoder.matches(loginRequest.getSenha(), funcionario.getSenha()))
                .thenReturn(false);

        // Act & Assert
        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(loginRequest)
        );
        
        assertEquals("Email ou senha inválidos", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar exceção quando usuário inativo")
    void deveLancarExcecaoQuandoUsuarioInativo() {
        // Arrange
        loginRequest.setEmail("inativo@bahiabrindes.com");
        when(funcionarioRepository.findByEmailAndAtivoTrue(loginRequest.getEmail()))
                .thenReturn(Optional.empty());

        // Act & Assert
        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(loginRequest)
        );
        
        assertEquals("Email ou senha inválidos", exception.getMessage());
    }
}
