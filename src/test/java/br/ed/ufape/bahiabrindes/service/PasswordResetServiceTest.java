package br.ed.ufape.bahiabrindes.service;

import br.ed.ufape.bahiabrindes.dto.auth.ForgotPasswordRequest;
import br.ed.ufape.bahiabrindes.dto.auth.ResetPasswordRequest;
import br.ed.ufape.bahiabrindes.model.entity.Funcionario;
import br.ed.ufape.bahiabrindes.model.entity.PasswordResetToken;
import br.ed.ufape.bahiabrindes.repository.FuncionarioRepository;
import br.ed.ufape.bahiabrindes.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PasswordResetServiceTest {

    @Mock
    private FuncionarioRepository funcionarioRepository;
    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService service;

    private Funcionario funcionario;
    private JavaMailSender mailSender;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        mailSender = mock(JavaMailSender.class);
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        funcionario = Funcionario.builder()
                .id(1L)
                .email("user@bahiabrindes.com")
                .ativo(true)
                .senha("hash")
                .build();
    }

    @Test
    void requestReset_shouldCreateTokenAndSendEmail_whenUserExists() {
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail(funcionario.getEmail());
        when(funcionarioRepository.findByEmailAndAtivoTrue(funcionario.getEmail())).thenReturn(Optional.of(funcionario));

        service.requestReset(req);

        verify(tokenRepository).deleteByFuncionarioId(funcionario.getId());
        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(captor.capture());
        PasswordResetToken saved = captor.getValue();
        assertEquals(funcionario, saved.getFuncionario());
        assertNotNull(saved.getToken());
        assertTrue(saved.getExpiresAt().isAfter(LocalDateTime.now()));
        verify(mailSender).send(any(org.springframework.mail.SimpleMailMessage.class));
    }

    @Test
    void requestReset_shouldDoNothing_whenUserNotFound() {
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("unknown@bahiabrindes.com");
        when(funcionarioRepository.findByEmailAndAtivoTrue(req.getEmail())).thenReturn(Optional.empty());

        service.requestReset(req);

        verify(tokenRepository, never()).save(any());
        verify(mailSender, never()).send(any(org.springframework.mail.SimpleMailMessage.class));
    }

    @Test
    void resetPassword_shouldSetNewPassword_whenTokenValid() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("tok");
        req.setNovaSenha("newPass");

        PasswordResetToken token = PasswordResetToken.builder()
                .id(1L)
                .funcionario(funcionario)
                .token("tok")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .build();
        when(tokenRepository.findByToken("tok")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newPass")).thenReturn("newHash");

        service.resetPassword(req);

        assertEquals("newHash", funcionario.getSenha());
        assertTrue(token.isUsed());
        verify(tokenRepository).save(token);
    }

    @Test
    void resetPassword_shouldFail_whenTokenExpired() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("tok");
        req.setNovaSenha("newPass");
        PasswordResetToken token = PasswordResetToken.builder()
                .id(1L)
                .funcionario(funcionario)
                .token("tok")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .used(false)
                .build();
        when(tokenRepository.findByToken("tok")).thenReturn(Optional.of(token));

        assertThrows(IllegalStateException.class, () -> service.resetPassword(req));
    }
}
