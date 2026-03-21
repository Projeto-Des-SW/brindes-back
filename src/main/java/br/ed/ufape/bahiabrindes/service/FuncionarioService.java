package br.ed.ufape.bahiabrindes.service;

import br.ed.ufape.bahiabrindes.dto.funcionarios.FuncionarioMeRequest;
import br.ed.ufape.bahiabrindes.dto.funcionarios.FuncionarioRequest;
import br.ed.ufape.bahiabrindes.dto.funcionarios.FuncionarioResponse;
import br.ed.ufape.bahiabrindes.dto.funcionarios.FuncionarioUpdateRequest;
import br.ed.ufape.bahiabrindes.dto.common.PageResponse;
import br.ed.ufape.bahiabrindes.model.entity.Funcionario;
import br.ed.ufape.bahiabrindes.model.entity.Perfil;
import br.ed.ufape.bahiabrindes.repository.ClienteRepository;
import br.ed.ufape.bahiabrindes.repository.FuncionarioRepository;
import br.ed.ufape.bahiabrindes.repository.PerfilRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;
    private final ClienteRepository clienteRepository;
    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public FuncionarioService(FuncionarioRepository funcionarioRepository,
                              ClienteRepository clienteRepository,
                              PerfilRepository perfilRepository,
                              PasswordEncoder passwordEncoder) {
        this.funcionarioRepository = funcionarioRepository;
        this.clienteRepository = clienteRepository;
        this.perfilRepository = perfilRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private void validarEmailDisponivel(String email, Long funcionarioIdAtual) {
        if (funcionarioRepository.existsByEmailAndIdNot(email, funcionarioIdAtual != null ? funcionarioIdAtual : -1L)) {
            throw new IllegalArgumentException("Email já cadastrado por outro funcionário");
        }
        if (clienteRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email já cadastrado como cliente");
        }
    }

    public PageResponse<FuncionarioResponse> listar(String search, int page, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), pageSize, Sort.by("id").descending());
        Page<Funcionario> result = funcionarioRepository.search(blankToNull(search), pageable);

        return PageResponse.<FuncionarioResponse>builder()
                .items(result.getContent().stream().map(this::toResponse).toList())
                .page(page)
                .pageSize(pageSize)
                .total(result.getTotalElements())
                .build();
    }

    public FuncionarioResponse buscarPorId(Long id) {
        Funcionario f = funcionarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado"));
        return toResponse(f);
    }

    public FuncionarioResponse criar(FuncionarioRequest request) {
        validarEmailDisponivel(request.getEmail().trim(), null);

        Perfil perfilPadrao = perfilRepository.findByNome("ROLE_FUNCIONARIO")
                .orElseThrow(() -> new IllegalStateException("Perfil padrão 'ROLE_FUNCIONARIO' não encontrado no banco"));

        Set<Perfil> perfis = new HashSet<>();
        perfis.add(perfilPadrao);

        if (request.getPerfis() != null && !request.getPerfis().isEmpty()) {
            Set<Perfil> perfisAdicionais = request.getPerfis().stream()
                    .filter(nome -> nome != null && !nome.trim().isEmpty())
                    .map(nome -> perfilRepository.findByNome(nome.trim())
                            .orElseThrow(() -> new IllegalArgumentException("Perfil não encontrado: " + nome)))
                    .collect(Collectors.toSet());

            perfis.addAll(perfisAdicionais);
        }

        Funcionario funcionario = Funcionario.builder()
                .nome(request.getNome().trim())
                .email(request.getEmail().trim())
                .senha(passwordEncoder.encode(request.getSenha()))
                .ativo(true)
                .perfis(perfis)
                .build();

        Funcionario salvo = funcionarioRepository.save(funcionario);

        return toResponse(salvo);
    }

    public FuncionarioResponse atualizar(Long id, FuncionarioUpdateRequest request) {
        Funcionario f = funcionarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado"));
        validarEmailDisponivel(request.getEmail().trim(), id);

        f.setNome(request.getNome().trim());
        f.setEmail(request.getEmail().trim());
        if (request.getSenha() != null && !request.getSenha().isBlank()) {
            f.setSenha(passwordEncoder.encode(request.getSenha()));
        }

        if (request.getPerfis() != null) {
            Perfil perfilPadrao = perfilRepository.findByNome("ROLE_FUNCIONARIO")
                    .orElseThrow(() -> new IllegalStateException("Perfil padrão não encontrado"));
            Set<Perfil> perfis = new HashSet<>();
            perfis.add(perfilPadrao);
            request.getPerfis().stream()
                    .filter(nome -> nome != null && !nome.isBlank())
                    .map(nome -> perfilRepository.findByNome(nome.trim())
                            .orElseThrow(() -> new IllegalArgumentException("Perfil não encontrado: " + nome)))
                    .forEach(perfis::add);
            f.setPerfis(perfis);
        }

        return toResponse(funcionarioRepository.save(f));
    }

    public void remover(Long id) {
        Funcionario f = funcionarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado"));

        String emailAtual = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        if (f.getEmail().equalsIgnoreCase(emailAtual)) {
            throw new IllegalArgumentException("Você não pode excluir o próprio usuário.");
        }

        funcionarioRepository.deleteById(id);
    }

    public FuncionarioResponse buscarMe() {
        String email = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        Funcionario f = funcionarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado"));
        return toResponse(f);
    }

    public FuncionarioResponse atualizarMe(FuncionarioMeRequest request) {
        String email = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        Funcionario f = funcionarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado"));
        validarEmailDisponivel(request.getEmail().trim(), f.getId());
        f.setNome(request.getNome().trim());
        f.setEmail(request.getEmail().trim());
        if (request.getSenha() != null && !request.getSenha().isBlank()) {
            f.setSenha(passwordEncoder.encode(request.getSenha()));
        }
        return toResponse(funcionarioRepository.save(f));
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private FuncionarioResponse toResponse(Funcionario f) {
        return FuncionarioResponse.builder()
                .id(f.getId())
                .nome(f.getNome())
                .email(f.getEmail())
                .ativo(f.getAtivo())
                .perfis(f.getPerfis().stream()
                        .map(Perfil::getNome)
                        .collect(Collectors.toSet()))
                .dtCriacao(f.getDtCriacao())
                .build();
    }
}