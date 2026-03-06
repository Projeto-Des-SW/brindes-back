package br.ed.ufape.bahiabrindes.service;

import br.ed.ufape.bahiabrindes.dto.clientes.ClienteRequest;
import br.ed.ufape.bahiabrindes.dto.clientes.ClienteResponse;
import br.ed.ufape.bahiabrindes.dto.clientes.ClienteUpdateRequest;
import br.ed.ufape.bahiabrindes.dto.common.PageResponse;
import br.ed.ufape.bahiabrindes.model.entity.Cliente;
import br.ed.ufape.bahiabrindes.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public ClienteService(ClienteRepository clienteRepository, PasswordEncoder passwordEncoder) {
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public PageResponse<ClienteResponse> listar(String search, int page, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), pageSize, Sort.by("id").descending());
        Page<Cliente> result = clienteRepository.search(blankToNull(search), pageable);
        return PageResponse.<ClienteResponse>builder()
                .items(result.getContent().stream().map(this::toResponse).toList())
                .page(page)
                .pageSize(pageSize)
                .total(result.getTotalElements())
                .build();
    }

    public ClienteResponse buscarPorId(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));
        return toResponse(cliente);
    }

    public ClienteResponse criar(ClienteRequest request) {
        if (request.getDocumento() != null) {
            String cleaned = cleanCpf(request.getDocumento());
            if (cleaned.length() != 11) {
                throw new IllegalArgumentException("CPF deve conter exatamente 11 números");
            }
            if (clienteRepository.findByDocumento(cleaned).isPresent()) {
                throw new IllegalArgumentException("Documento já cadastrado");
            }
        }
        if (clienteRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email já cadastrado");
        }

        String cleanedCpf = request.getDocumento() != null ? request.getDocumento().replaceAll("[^0-9]", "") : null;

        Cliente cliente = Cliente.builder()
                .nome(request.getNome().trim())
                .documento(cleanedCpf)
                .email(blankToNull(request.getEmail()))
                .telefone(blankToNull(request.getTelefone()))
                .endereco(blankToNull(request.getEndereco()))
                .segmentacao(blankToNull(request.getSegmentacao()))
                .senha(passwordEncoder.encode(request.getSenha()))
                .ativo(true)
                .build();

        return toResponse(clienteRepository.save(cliente));
    }

    public ClienteResponse atualizar(Long id, ClienteUpdateRequest request) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));

        String cleanedCpf = null;
        if (request.getDocumento() != null) {
            cleanedCpf = cleanCpf(request.getDocumento());
            if (cleanedCpf.length() != 11) {
                throw new IllegalArgumentException("CPF deve conter exatamente 11 números");
            }
            if (!cleanedCpf.equals(cliente.getDocumento()) &&
                    clienteRepository.findByDocumento(cleanedCpf).isPresent()) {
                throw new IllegalArgumentException("Documento já cadastrado");
            }
            cliente.setDocumento(cleanedCpf);
        }
        if (request.getEmail() != null) {
            String email = request.getEmail().trim();
            if (!email.equals(cliente.getEmail()) &&
                    clienteRepository.findByEmail(email).isPresent()) {
                throw new IllegalArgumentException("Email já cadastrado");
            }
            cliente.setEmail(email);
        }
        if (request.getNome() != null) {
            cliente.setNome(request.getNome().trim());
        }
        if (request.getTelefone() != null) {
            cliente.setTelefone(blankToNull(request.getTelefone()));
        }
        if (request.getEndereco() != null) {
            cliente.setEndereco(blankToNull(request.getEndereco()));
        }
        if (request.getSegmentacao() != null) {
            cliente.setSegmentacao(blankToNull(request.getSegmentacao()));
        }
        if (request.getSenha() != null && !request.getSenha().trim().isEmpty()) {
            cliente.setSenha(passwordEncoder.encode(request.getSenha()));
        }

        Cliente clienteAtualizado = clienteRepository.save(cliente);

        return toResponse(clienteAtualizado);
    }


    public void remover(Long id) {
        if (!clienteRepository.existsById(id)) {
            throw new IllegalArgumentException("Cliente não encontrado");
        }
        clienteRepository.deleteById(id);
    }

    private ClienteResponse toResponse(Cliente c) {
        return ClienteResponse.builder()
                .id(c.getId())
                .nome(c.getNome())
                .documento(c.getDocumento())
                .email(c.getEmail())
                .telefone(c.getTelefone())
                .endereco(c.getEndereco())
                .segmentacao(c.getSegmentacao())
                .criadoEm(c.getCriadoEm())
                .build();
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
    
    private static String cleanCpf(String cpf) {
        if (cpf == null) return null;
        return cpf.replaceAll("[^0-9]", "");
    }
}