package br.ed.ufape.bahiabrindes.service;

import br.ed.ufape.bahiabrindes.dto.auth.LoginRequest;
import br.ed.ufape.bahiabrindes.dto.auth.LoginResponse;
import br.ed.ufape.bahiabrindes.model.entity.Funcionario;
import br.ed.ufape.bahiabrindes.repository.FuncionarioRepository;
import br.ed.ufape.bahiabrindes.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final FuncionarioRepository funcionarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Autowired
    public AuthService(FuncionarioRepository funcionarioRepository, 
                      PasswordEncoder passwordEncoder,
                      JwtUtil jwtUtil) {
        this.funcionarioRepository = funcionarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse login(LoginRequest request) {
        Funcionario funcionario = funcionarioRepository.findByEmailAndAtivoTrue(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Email ou senha inválidos"));

        if (!passwordEncoder.matches(request.getSenha(), funcionario.getSenha())) {
            throw new BadCredentialsException("Email ou senha inválidos");
        }

        Set<String> perfis = funcionario.getPerfis().stream()
                .map(perfil -> perfil.getNome())
                .collect(Collectors.toSet());

        String token = jwtUtil.generateToken(
                funcionario.getEmail(),
                funcionario.getId(),
                perfis
        );

        return LoginResponse.builder()
                .token(token)
                .tipo("Bearer")
                .id(funcionario.getId())
                .nome(funcionario.getNome())
                .email(funcionario.getEmail())
                .perfis(perfis)
                .build();
    }
}
