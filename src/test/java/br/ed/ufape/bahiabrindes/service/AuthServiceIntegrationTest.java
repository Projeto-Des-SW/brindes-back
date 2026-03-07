package br.ed.ufape.bahiabrindes.service;

import br.ed.ufape.bahiabrindes.dto.auth.LoginRequest;
import br.ed.ufape.bahiabrindes.dto.auth.LoginResponse;
import br.ed.ufape.bahiabrindes.model.entity.Cliente;
import br.ed.ufape.bahiabrindes.model.entity.Funcionario;
import br.ed.ufape.bahiabrindes.model.entity.Perfil;
import br.ed.ufape.bahiabrindes.model.enums.TipoUsuario;
import br.ed.ufape.bahiabrindes.repository.ClienteRepository;
import br.ed.ufape.bahiabrindes.repository.FuncionarioRepository;
import br.ed.ufape.bahiabrindes.repository.PerfilRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PerfilRepository perfilRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Funcionario funcionarioTeste;
    private Cliente clienteTeste;
    private Perfil perfilAdmin;

    @BeforeEach
    void setUp() {
        clienteRepository.deleteAll();
        funcionarioRepository.deleteAll();
        perfilRepository.deleteAll();

        perfilAdmin = new Perfil();
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
    @DisplayName("Deve realizar login com sucesso usando banco H2")
    void deveRealizarLoginComSucessoComBancoReal() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("teste@bahiabrindes.com");
        request.setSenha("senha123");

        // Act
        LoginResponse response = authService.login(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("Bearer", response.getTipo());
        assertEquals(funcionarioTeste.getId(), response.getId());
        assertEquals("Usuário Teste", response.getNome());
        assertEquals("teste@bahiabrindes.com", response.getEmail());
        assertEquals(TipoUsuario.FUNCIONARIO, response.getTipoUsuario());
        assertTrue(response.getPerfis().contains("ADMIN"));
    }

    @Test
    @DisplayName("Deve realizar login de cliente com sucesso usando banco H2")
    void deveRealizarLoginDeClienteComSucessoComBancoReal() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("cliente@bahiabrindes.com");
        request.setSenha("senha123");

        // Act
        LoginResponse response = authService.login(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals(TipoUsuario.CLIENTE, response.getTipoUsuario());
        assertTrue(response.getPerfis().contains("CLIENTE"));
    }

    @Test
    @DisplayName("Deve lançar exceção para email não cadastrado")
    void deveLancarExcecaoParaEmailNaoCadastrado() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("naoexiste@bahiabrindes.com");
        request.setSenha("senha123");

        // Act & Assert
        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(request)
        );
        assertEquals("Email ou senha inválidos", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar exceção para senha incorreta")
    void deveLancarExcecaoParaSenhaIncorreta() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("teste@bahiabrindes.com");
        request.setSenha("senhaErrada");

        // Act & Assert
        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(request)
        );
        assertEquals("Email ou senha inválidos", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar exceção para usuário inativo")
    void deveLancarExcecaoParaUsuarioInativo() {
        // Arrange
        funcionarioTeste.setAtivo(false);
        funcionarioRepository.save(funcionarioTeste);

        LoginRequest request = new LoginRequest();
        request.setEmail("teste@bahiabrindes.com");
        request.setSenha("senha123");

        // Act & Assert
        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(request)
        );
        assertEquals("Email ou senha inválidos", exception.getMessage());
    }

    @Test
    @DisplayName("Deve bloquear login quando email existir nas duas tabelas")
    void deveBloquearLoginQuandoEmailExistirNasDuasTabelas() {
        // Arrange
        clienteTeste.setEmail("teste@bahiabrindes.com");
        clienteRepository.save(clienteTeste);

        LoginRequest request = new LoginRequest();
        request.setEmail("teste@bahiabrindes.com");
        request.setSenha("senha123");

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> authService.login(request)
        );
        assertEquals("Email cadastrado para multiplos tipos de usuario", exception.getMessage());
    }

    @Test
    @DisplayName("Deve incluir múltiplos perfis no token")
    void deveIncluirMultiplosPerfilsNoToken() {
        // Arrange
        Perfil perfilGerente = new Perfil();
        perfilGerente.setNome("GERENTE");
        perfilGerente = perfilRepository.save(perfilGerente);

        funcionarioTeste.getPerfis().add(perfilGerente);
        funcionarioRepository.save(funcionarioTeste);

        LoginRequest request = new LoginRequest();
        request.setEmail("teste@bahiabrindes.com");
        request.setSenha("senha123");

        // Act
        LoginResponse response = authService.login(request);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getPerfis().size());
        assertTrue(response.getPerfis().contains("ADMIN"));
        assertTrue(response.getPerfis().contains("GERENTE"));
    }
}
