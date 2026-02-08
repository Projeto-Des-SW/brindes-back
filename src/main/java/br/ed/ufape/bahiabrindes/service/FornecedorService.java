package br.ed.ufape.bahiabrindes.service;

import br.ed.ufape.bahiabrindes.dto.common.PageResponse;
import br.ed.ufape.bahiabrindes.dto.estoque.EnderecoRequest;
import br.ed.ufape.bahiabrindes.dto.estoque.EnderecoResponse;
import br.ed.ufape.bahiabrindes.dto.estoque.FornecedorRequest;
import br.ed.ufape.bahiabrindes.dto.estoque.FornecedorResponse;
import br.ed.ufape.bahiabrindes.model.entity.Endereco;
import br.ed.ufape.bahiabrindes.model.entity.Fornecedor;
import br.ed.ufape.bahiabrindes.repository.FornecedorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FornecedorService {

    private final FornecedorRepository fornecedorRepository;

    @Autowired
    public FornecedorService(FornecedorRepository fornecedorRepository) {
        this.fornecedorRepository = fornecedorRepository;
    }

    public PageResponse<FornecedorResponse> listar(String search, String status, int page, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), pageSize, Sort.by("id").descending());
        Page<Fornecedor> result = fornecedorRepository.search(blankToNull(search), blankToNull(status), pageable);

        return PageResponse.<FornecedorResponse>builder()
                .items(result.getContent().stream().map(this::toResponse).toList())
                .page(page)
                .pageSize(pageSize)
                .total(result.getTotalElements())
                .build();
    }

    public FornecedorResponse criar(FornecedorRequest request) {
        Fornecedor fornecedor = Fornecedor.builder()
                .razaoSocial(request.getNome())
                .nomeFantasia(request.getNome())
                .cnpj(request.getCnpj())
                .emailContato(request.getEmail())
                .telefoneContato(request.getTelefone())
                .status(blankToNull(request.getStatus()) != null ? request.getStatus() : "ATIVO")
                .prazoEntregaDias(parsePrazoEntregaDias(request.getPrazoEntrega()))
                .condicoesPagamento(blankToNull(request.getCondicoesPagamento()))
                .observacoes(blankToNull(request.getObservacoes()))
                .endereco(toEnderecoEntityOrNull(request.getEndereco()))
                .build();

        return toResponse(fornecedorRepository.save(fornecedor));
    }

    public FornecedorResponse atualizar(Long id, FornecedorRequest request) {
        Fornecedor fornecedor = fornecedorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fornecedor não encontrado"));

        fornecedor.setRazaoSocial(request.getNome());
        fornecedor.setNomeFantasia(request.getNome());
        fornecedor.setCnpj(request.getCnpj());
        fornecedor.setEmailContato(request.getEmail());
        fornecedor.setTelefoneContato(request.getTelefone());
        if (blankToNull(request.getStatus()) != null) {
            fornecedor.setStatus(request.getStatus());
        }
        fornecedor.setPrazoEntregaDias(parsePrazoEntregaDias(request.getPrazoEntrega()));
        fornecedor.setCondicoesPagamento(blankToNull(request.getCondicoesPagamento()));
        fornecedor.setObservacoes(blankToNull(request.getObservacoes()));

        if (request.getEndereco() != null) {
            if (isEnderecoEmpty(request.getEndereco())) {
                fornecedor.setEndereco(null);
            } else if (fornecedor.getEndereco() == null) {
                fornecedor.setEndereco(toEnderecoEntityOrNull(request.getEndereco()));
            } else {
                Endereco e = fornecedor.getEndereco();
                e.setRua(blankToNull(request.getEndereco().getRua()));
                e.setNumero(blankToNull(request.getEndereco().getNumero()));
                e.setCep(blankToNull(request.getEndereco().getCep()));
                e.setCidade(blankToNull(request.getEndereco().getCidade()));
                e.setEstado(blankToNull(request.getEndereco().getEstado()));
            }
        }

        return toResponse(fornecedorRepository.save(fornecedor));
    }

    public void remover(Long id) {
        Fornecedor fornecedor = fornecedorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fornecedor não encontrado"));
        fornecedor.setStatus("INATIVO");
        fornecedorRepository.save(fornecedor);
    }

    private FornecedorResponse toResponse(Fornecedor f) {
        String nome = blankToNull(f.getNomeFantasia()) != null ? f.getNomeFantasia() : f.getRazaoSocial();
        String prazo = f.getPrazoEntregaDias() != null ? (f.getPrazoEntregaDias() + " dias") : "";

        return FornecedorResponse.builder()
                .id(f.getId())
                .nome(nome)
                .cnpj(f.getCnpj())
                .telefone(f.getTelefoneContato())
                .email(f.getEmailContato())
                .prazoEntrega(prazo)
                .status(f.getStatus())
                .condicoesPagamento(f.getCondicoesPagamento())
                .observacoes(f.getObservacoes())
                .endereco(toEnderecoResponse(f.getEndereco()))
                .build();
    }

    private static EnderecoResponse toEnderecoResponse(Endereco e) {
        if (e == null) return null;
        return EnderecoResponse.builder()
                .rua(e.getRua())
                .numero(e.getNumero())
                .cep(e.getCep())
                .cidade(e.getCidade())
                .estado(e.getEstado())
                .build();
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

    private static Integer parsePrazoEntregaDias(String prazoEntrega) {
        if (prazoEntrega == null) return null;
        String s = prazoEntrega.trim();
        if (s.isEmpty()) return null;

        Pattern p = Pattern.compile("(\\d+)");
        Matcher m = p.matcher(s);
        if (!m.find()) return null;
        try {
            return Integer.parseInt(m.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

