package br.ed.ufape.bahiabrindes.service;

import br.ed.ufape.bahiabrindes.dto.avaliacao.AvaliacaoProdutoDTO;
import br.ed.ufape.bahiabrindes.dto.avaliacao.CriarAvaliacaoRequest;
import br.ed.ufape.bahiabrindes.model.entity.AvaliacaoProduto;
import br.ed.ufape.bahiabrindes.model.entity.Orcamento;
import br.ed.ufape.bahiabrindes.model.entity.Produto;
import br.ed.ufape.bahiabrindes.model.enums.StatusOrcamento;
import br.ed.ufape.bahiabrindes.repository.AvaliacaoProdutoRepository;
import br.ed.ufape.bahiabrindes.repository.OrcamentoRepository;
import br.ed.ufape.bahiabrindes.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AvaliacaoService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AvaliacaoProdutoRepository avaliacaoRepository;
    private final OrcamentoRepository orcamentoRepository;
    private final ProdutoRepository produtoRepository;

    @Autowired
    public AvaliacaoService(
            AvaliacaoProdutoRepository avaliacaoRepository,
            OrcamentoRepository orcamentoRepository,
            ProdutoRepository produtoRepository) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.orcamentoRepository = orcamentoRepository;
        this.produtoRepository = produtoRepository;
    }

    public List<AvaliacaoProdutoDTO> listarPorProduto(Long produtoId) {
        return avaliacaoRepository.findByProdutoIdOrderByCriadoEmDesc(produtoId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public AvaliacaoProdutoDTO criar(Long clienteId, Long orcamentoId, CriarAvaliacaoRequest request) {
        Orcamento orcamento = orcamentoRepository.findById(orcamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Orçamento não encontrado"));

        if (!orcamento.getCliente().getId().equals(clienteId)) {
            throw new IllegalArgumentException("Orçamento não pertence ao cliente");
        }

        if (orcamento.getStatus() != StatusOrcamento.CONCLUIDO) {
            throw new IllegalArgumentException("Só é possível avaliar pedidos concluídos");
        }

        Produto produto = produtoRepository.findById(request.getProdutoId())
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado"));

        boolean produtoNoOrcamento = orcamento.getItens().stream()
                .anyMatch(item -> item.getProduto().getId().equals(produto.getId()));
        if (!produtoNoOrcamento) {
            throw new IllegalArgumentException("Produto não faz parte deste pedido");
        }

        if (avaliacaoRepository.existsByOrcamentoIdAndProdutoId(orcamentoId, produto.getId())) {
            throw new IllegalArgumentException("Produto já foi avaliado neste pedido");
        }

        Integer nota = request.getNota();
        if (nota == null || nota < 1 || nota > 5) {
            throw new IllegalArgumentException("Nota deve ser entre 1 e 5");
        }

        AvaliacaoProduto avaliacao = AvaliacaoProduto.builder()
                .produto(produto)
                .orcamento(orcamento)
                .nomeCliente(orcamento.getCliente().getNome())
                .nota(nota)
                .comentario(request.getComentario())
                .build();

        return toDTO(avaliacaoRepository.save(avaliacao));
    }

    private AvaliacaoProdutoDTO toDTO(AvaliacaoProduto a) {
        return AvaliacaoProdutoDTO.builder()
                .id(a.getId())
                .nomeCliente(a.getNomeCliente())
                .nota(a.getNota())
                .comentario(a.getComentario())
                .criadoEm(a.getCriadoEm() != null ? a.getCriadoEm().format(DATE_FORMAT) : null)
                .build();
    }
}
