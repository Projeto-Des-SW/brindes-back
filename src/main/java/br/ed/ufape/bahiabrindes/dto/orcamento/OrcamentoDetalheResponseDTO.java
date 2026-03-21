package br.ed.ufape.bahiabrindes.dto.orcamento;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrcamentoDetalheResponseDTO {

    private Long id;
    private String codigo;
    private String status;

    /** Data de criação formatada (ex: "12/03/2026") */
    private String dataCriacao;

    /** Data prevista de entrega formatada (pode ser null) */
    private String dataPrevisaoEntrega;

    private BigDecimal subtotal;
    private BigDecimal descontoTotal;
    private BigDecimal frete;
    private BigDecimal valorTotal;

    private String nomeCliente;
    private String emailCliente;
    private String telefoneCliente;

    /** Método de pagamento (PIX, BOLETO, etc.) */
    private String metodoPagamento;

    /** Valor já pago pelo cliente */
    private BigDecimal valorPago;

    /** Timeline de eventos do pedido */
    private List<HistoricoStatusItemDTO> historico;

    /** Artes enviadas para aprovação do cliente */
    private List<ArteOrcamentoDTO> artes;

    /** Produtos/itens do pedido */
    private List<OrcamentoProdutoDetalheDTO> produtos;

    /** Comentários trocados no pedido */
    private List<ComentarioOrcamentoDTO> comentarios;
}
