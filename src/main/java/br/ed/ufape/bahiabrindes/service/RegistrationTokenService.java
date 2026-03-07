package br.ed.ufape.bahiabrindes.service;

import br.ed.ufape.bahiabrindes.dto.auth.RegisterTokenRequest;
import br.ed.ufape.bahiabrindes.repository.ClienteRepository;
import br.ed.ufape.bahiabrindes.repository.FuncionarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RegistrationTokenService {

    private final ClienteRepository clienteRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.from:}")
    private String mailFrom;

    private static final int EXP_MINUTES = 5;
    private final Map<String, RegistrationTokenData> tokensByEmail = new ConcurrentHashMap<>();

    public void requestToken(RegisterTokenRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        validarEmailDisponivel(email);
        limparExpirados();

        String token = UUID.randomUUID().toString();
        tokensByEmail.put(email, new RegistrationTokenData(token, LocalDateTime.now().plusMinutes(EXP_MINUTES)));
        sendRegisterEmail(email, token);
    }

    public void validateAndConsume(String email, String token) {
        limparExpirados();

        RegistrationTokenData tokenData = tokensByEmail.get(normalize(email));
        if (tokenData == null || tokenData.expiresAt().isBefore(LocalDateTime.now())) {
            tokensByEmail.remove(normalize(email));
            throw new IllegalStateException("Token inválido");
        }

        if (!tokenData.token().equals(token)) {
            throw new IllegalStateException("Token inválido");
        }

        tokensByEmail.remove(normalize(email));
    }

    String getTokenForEmail(String email) {
        RegistrationTokenData tokenData = tokensByEmail.get(normalize(email));
        return tokenData != null ? tokenData.token() : null;
    }

    private void validarEmailDisponivel(String email) {
        if (clienteRepository.findByEmail(email).isPresent() || funcionarioRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email já cadastrado");
        }
    }

    private void limparExpirados() {
        LocalDateTime now = LocalDateTime.now();
        tokensByEmail.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private void sendRegisterEmail(String email, String token) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        if (mailFrom != null && !mailFrom.isBlank()) {
            message.setFrom(mailFrom);
        }
        message.setTo(email);
        message.setSubject("Confirmação de cadastro - Bahia Brindes");
        message.setText("Use este token para concluir seu cadastro (5 min): " + token);

        try {
            mailSender.send(message);
        } catch (Exception ex) {
            System.err.println("Erro ao enviar email de cadastro: " + ex.getMessage());
        }
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }

    private record RegistrationTokenData(String token, LocalDateTime expiresAt) {
    }
}
