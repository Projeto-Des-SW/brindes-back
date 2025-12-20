package br.ed.ufape.bahiabrindes.service;

import br.ed.ufape.bahiabrindes.dto.auth.ForgotPasswordRequest;
import br.ed.ufape.bahiabrindes.dto.auth.ResetPasswordRequest;
import br.ed.ufape.bahiabrindes.model.entity.Funcionario;
import br.ed.ufape.bahiabrindes.model.entity.PasswordResetToken;
import br.ed.ufape.bahiabrindes.repository.FuncionarioRepository;
import br.ed.ufape.bahiabrindes.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final FuncionarioRepository funcionarioRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final PasswordEncoder passwordEncoder;
    @Value("${app.mail.from:}")
    private String mailFrom;

    private static final int EXP_MINUTES = 5;

    @Transactional
    public void requestReset(ForgotPasswordRequest request) {
        Optional<Funcionario> opt = funcionarioRepository.findByEmailAndAtivoTrue(request.getEmail());
        if (opt.isEmpty()) {
            return;
        }
        Funcionario funcionario = opt.get();

        tokenRepository.deleteByFuncionarioId(funcionario.getId());

        String tokenStr = UUID.randomUUID().toString();
        PasswordResetToken token = PasswordResetToken.builder()
                .funcionario(funcionario)
                .token(tokenStr)
                .expiresAt(LocalDateTime.now().plusMinutes(EXP_MINUTES))
                .used(false)
                .build();
        tokenRepository.save(token);

        sendResetEmail(funcionario.getEmail(), tokenStr);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Token inválido"));

        if (token.isUsed()) {
            throw new IllegalArgumentException("Token inválido");
        }
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token inválido");
        }

        Funcionario funcionario = token.getFuncionario();
        funcionario.setSenha(passwordEncoder.encode(request.getNovaSenha()));

        token.setUsed(true);
        tokenRepository.save(token);
    }

    private void sendResetEmail(String email, String token) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        if (mailFrom != null && !mailFrom.isBlank()) {
            message.setFrom(mailFrom);
        }
        message.setTo(email);
        message.setSubject("Recuperação de senha - Bahia Brindes");
        message.setText("Use este token para resetar sua senha (5 min): " + token);
        try {
            mailSender.send(message);
        } catch (Exception ex) {
            System.err.println("Erro ao enviar email de recuperação de senha: " + ex.getMessage());
        }
    }
}
