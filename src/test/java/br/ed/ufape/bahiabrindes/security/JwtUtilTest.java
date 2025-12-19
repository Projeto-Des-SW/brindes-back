package br.ed.ufape.bahiabrindes.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String secret = "1234567890123456789012345678901234567890123456789012345678901234";
    private final Long expiration = 3600000L; // 1 hora

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", secret);
        ReflectionTestUtils.setField(jwtUtil, "expiration", expiration);
    }

    @Test
    @DisplayName("Deve gerar token JWT válido")
    void deveGerarTokenValido() {
        // Arrange
        String email = "teste@bahiabrindes.com";
        Long userId = 1L;
        Set<String> perfis = new HashSet<>();
        perfis.add("ADMIN");

        // Act
        String token = jwtUtil.generateToken(email, userId, perfis);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.startsWith("eyJ")); // JWT começa com eyJ
    }

    @Test
    @DisplayName("Deve extrair email do token")
    void deveExtrairEmailDoToken() {
        // Arrange
        String email = "teste@bahiabrindes.com";
        Long userId = 1L;
        Set<String> perfis = new HashSet<>();
        perfis.add("ADMIN");
        String token = jwtUtil.generateToken(email, userId, perfis);

        // Act
        String extractedEmail = jwtUtil.extractEmail(token);

        // Assert
        assertEquals(email, extractedEmail);
    }

    @Test
    @DisplayName("Deve extrair userId do token")
    void deveExtrairUserIdDoToken() {
        // Arrange
        String email = "teste@bahiabrindes.com";
        Long userId = 1L;
        Set<String> perfis = new HashSet<>();
        perfis.add("ADMIN");
        String token = jwtUtil.generateToken(email, userId, perfis);

        // Act
        Long extractedUserId = jwtUtil.extractUserId(token);

        // Assert
        assertEquals(userId, extractedUserId);
    }

    @Test
    @DisplayName("Deve validar token corretamente")
    void deveValidarTokenCorretamente() {
        // Arrange
        String email = "teste@bahiabrindes.com";
        Long userId = 1L;
        Set<String> perfis = new HashSet<>();
        perfis.add("ADMIN");
        String token = jwtUtil.generateToken(email, userId, perfis);

        // Act
        Boolean isValid = jwtUtil.validateToken(token, email);

        // Assert
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Deve invalidar token com email diferente")
    void deveInvalidarTokenComEmailDiferente() {
        // Arrange
        String email = "teste@bahiabrindes.com";
        Long userId = 1L;
        Set<String> perfis = new HashSet<>();
        perfis.add("ADMIN");
        String token = jwtUtil.generateToken(email, userId, perfis);

        // Act
        Boolean isValid = jwtUtil.validateToken(token, "outro@bahiabrindes.com");

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Deve extrair data de expiração do token")
    void deveExtrairDataExpiracao() {
        // Arrange
        String email = "teste@bahiabrindes.com";
        Long userId = 1L;
        Set<String> perfis = new HashSet<>();
        perfis.add("ADMIN");
        String token = jwtUtil.generateToken(email, userId, perfis);

        // Act
        Date expirationDate = jwtUtil.extractExpiration(token);

        // Assert
        assertNotNull(expirationDate);
        assertTrue(expirationDate.after(new Date()));
    }
}
