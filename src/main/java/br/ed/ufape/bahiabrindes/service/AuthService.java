package br.ed.ufape.bahiabrindes.service;

import br.ed.ufape.bahiabrindes.dto.auth.LoginRequest;
import br.ed.ufape.bahiabrindes.dto.auth.LoginResponse;
import br.ed.ufape.bahiabrindes.model.entity.Cliente;
import br.ed.ufape.bahiabrindes.model.entity.Funcionario;
import br.ed.ufape.bahiabrindes.model.enums.TipoUsuario;
import br.ed.ufape.bahiabrindes.repository.ClienteRepository;
import br.ed.ufape.bahiabrindes.repository.FuncionarioRepository;
import br.ed.ufape.bahiabrindes.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashSet;

@Service
public class AuthService {

    private final FuncionarioRepository funcionarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Autowired
    public AuthService(FuncionarioRepository funcionarioRepository,
                       ClienteRepository clienteRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.funcionarioRepository = funcionarioRepository;
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse login(LoginRequest request) {
        validarEmailUnicoEntreTipos(request.getEmail());

        Funcionario funcionario = funcionarioRepository.findByEmailAndAtivoTrue(request.getEmail())
                .orElse(null);

        if (funcionario != null && passwordEncoder.matches(request.getSenha(), funcionario.getSenha())) {
            Set<String> perfis = funcionario.getPerfis().stream()
                    .map(perfil -> perfil.getNome())
                    .collect(Collectors.toSet());
            String token = jwtUtil.generateToken(
                    funcionario.getEmail(),
                    funcionario.getId(),
                    perfis,
                    TipoUsuario.FUNCIONARIO
            );
            return LoginResponse.builder()
                    .token(token)
                    .tipo("Bearer")
                    .id(funcionario.getId())
                    .nome(funcionario.getNome())
                    .email(funcionario.getEmail())
                    .tipoUsuario(TipoUsuario.FUNCIONARIO)
                    .perfis(perfis)
                    .build();
        }

        Cliente cliente = clienteRepository.findByEmailAndAtivoTrue(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Email ou senha inválidos"));

        if (!passwordEncoder.matches(request.getSenha(), cliente.getSenha())) {
            throw new BadCredentialsException("Email ou senha inválidos");
        }

        Set<String> perfisCliente = new HashSet<>();
        perfisCliente.add("CLIENTE");

        String tokenCliente = jwtUtil.generateToken(
                cliente.getEmail(),
                cliente.getId(),
                perfisCliente,
                TipoUsuario.CLIENTE
        );

        return LoginResponse.builder()
                .token(tokenCliente)
                .tipo("Bearer")
                .id(cliente.getId())
                .nome(cliente.getNome())
                .email(cliente.getEmail())
                .tipoUsuario(TipoUsuario.CLIENTE)
                .perfis(perfisCliente)
                .build();
    }

    private void validarEmailUnicoEntreTipos(String email) {
        boolean existeFuncionario = funcionarioRepository.findByEmail(email).isPresent();
        boolean existeCliente = clienteRepository.findByEmail(email).isPresent();

        if (existeFuncionario && existeCliente) {
            throw new IllegalStateException("Email cadastrado para multiplos tipos de usuario");
        }
    }
}