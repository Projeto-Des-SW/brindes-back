package br.ed.ufape.bahiabrindes.service;

import br.ed.ufape.bahiabrindes.dto.clientes.ClienteRequest;
import br.ed.ufape.bahiabrindes.dto.clientes.ClienteResponse;
import br.ed.ufape.bahiabrindes.dto.clientes.ClienteUpdateRequest;
import br.ed.ufape.bahiabrindes.dto.clientes.EnderecoRequest;
import br.ed.ufape.bahiabrindes.dto.clientes.EnderecoResponse;
import br.ed.ufape.bahiabrindes.dto.common.PageResponse;
import br.ed.ufape.bahiabrindes.model.entity.Cliente;
import br.ed.ufape.bahiabrindes.model.entity.Endereco;
import br.ed.ufape.bahiabrindes.repository.ClienteRepository;
import br.ed.ufape.bahiabrindes.repository.FuncionarioRepository;
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
    private final FuncionarioRepository funcionarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public ClienteService(ClienteRepository clienteRepository,
                          FuncionarioRepository funcionarioRepository,
                          PasswordEncoder passwordEncoder) {
        this.clienteRepository = clienteRepository;
        this.funcionarioRepository = funcionarioRepository;
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
        validarEmailDisponivel(blankToNull(request.getEmail()), null);

        String cleanedCpf = request.getDocumento() != null ? request.getDocumento().replaceAll("[^0-9]", "") : null;

        Cliente cliente = Cliente.builder()
                .nome(request.getNome().trim())
                .documento(cleanedCpf)
                .email(blankToNull(request.getEmail()))
                .telefone(blankToNull(request.getTelefone()))
                .endereco(toEnderecoEntityOrNull(request.getEndereco()))
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
            validarEmailDisponivel(email, cliente.getId());
            cliente.setEmail(email);
        }
        if (request.getNome() != null) {
            cliente.setNome(request.getNome().trim());
        }
        if (request.getTelefone() != null) {
            cliente.setTelefone(blankToNull(request.getTelefone()));
        }
        if (request.getEndereco() != null) {
            if (isEnderecoEmpty(request.getEndereco())) {
                cliente.setEndereco(null);
            } else if (cliente.getEndereco() == null) {
                cliente.setEndereco(toEnderecoEntityOrNull(request.getEndereco()));
            } else {
                Endereco e = cliente.getEndereco();
                e.setRua(blankToNull(request.getEndereco().getRua()));
                e.setNumero(blankToNull(request.getEndereco().getNumero()));
                e.setCep(blankToNull(request.getEndereco().getCep()));
                e.setCidade(blankToNull(request.getEndereco().getCidade()));
                e.setEstado(blankToNull(request.getEndereco().getEstado()));
            }
        }
        if (request.getSegmentacao() != null) {
            cliente.setSegmentacao(blankToNull(request.getSegmentacao()));
        }
        if (request.getSenha() != null && !request.getSenha().trim().isEmpty()) {
            cliente.setSenha(passwordEncoder.encode(request.getSenha()));
        }
        if (request.getFotoPerfil() != null) {
            cliente.setFotoPerfil(request.getFotoPerfil().isBlank() ? null : request.getFotoPerfil());
        }

        Cliente clienteAtualizado = clienteRepository.save(cliente);

        return toResponse(clienteAtualizado);
    }

    public ClienteResponse buscarPorEmail(String email) {
        Cliente cliente = clienteRepository.findByEmailAndAtivoTrue(email)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));
        return toResponse(cliente);
    }

    public ClienteResponse atualizarPorEmail(String email, ClienteUpdateRequest request) {
        Cliente cliente = clienteRepository.findByEmailAndAtivoTrue(email)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));
        return atualizar(cliente.getId(), request);
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
                .endereco(toEnderecoResponse(c.getEndereco()))
                .segmentacao(c.getSegmentacao())
                .criadoEm(c.getCriadoEm())
                .fotoPerfil(c.getFotoPerfil())
                .build();
    }

    private void validarEmailDisponivel(String email, Long clienteIdAtual) {
        if (email == null) {
            return;
        }

        clienteRepository.findByEmail(email)
                .filter(cliente -> !cliente.getId().equals(clienteIdAtual))
                .ifPresent(cliente -> {
                    throw new IllegalArgumentException("Email já cadastrado");
                });

        if (funcionarioRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email já cadastrado");
        }
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static boolean isEnderecoEmpty(EnderecoRequest e) {
        if (e == null) return true;
        return blankToNull(e.getRua()) == null
                && blankToNull(e.getNumero()) == null
                && blankToNull(e.getCep()) == null
                && blankToNull(e.getCidade()) == null
                && blankToNull(e.getEstado()) == null;
    }

    private static Endereco toEnderecoEntityOrNull(EnderecoRequest e) {
        if (e == null || isEnderecoEmpty(e)) return null;
        return Endereco.builder()
                .rua(blankToNull(e.getRua()))
                .numero(blankToNull(e.getNumero()))
                .cep(blankToNull(e.getCep()))
                .cidade(blankToNull(e.getCidade()))
                .estado(blankToNull(e.getEstado()))
                .build();
    }

    private static EnderecoResponse toEnderecoResponse(Endereco e) {
        if (e == null) return null;
        return EnderecoResponse.builder()
                .id(e.getId())
                .rua(e.getRua())
                .numero(e.getNumero())
                .cep(e.getCep())
                .cidade(e.getCidade())
                .estado(e.getEstado())
                .build();
    }
    
    private static String cleanCpf(String cpf) {
        if (cpf == null) return null;
        return cpf.replaceAll("[^0-9]", "");
    }
}