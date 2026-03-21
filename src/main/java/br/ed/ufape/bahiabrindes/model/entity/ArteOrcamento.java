package br.ed.ufape.bahiabrindes.model.entity;

import br.ed.ufape.bahiabrindes.model.enums.StatusArte;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "artes_orcamento")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArteOrcamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orcamento_id", nullable = false)
    private Orcamento orcamento;

    /** Nome do produto ao qual esta arte se refere */
    @Column(name = "produto_nome", nullable = false)
    private String produtoNome;

    /** URL da imagem/arquivo da arte (legado — novos uploads usam conteudoArquivo) */
    @Column(name = "imagem_url")
    private String imagemUrl;

    /** Nome original do arquivo com extensão (ex: logo.png, design.jpeg) */
    @Column(name = "nome_arquivo")
    private String nomeArquivo;

    /** Conteúdo binário do arquivo armazenado no banco */
    @Column(name = "conteudo_arquivo", columnDefinition = "bytea")
    private byte[] conteudoArquivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private StatusArte status = StatusArte.PENDENTE;

    /** Comentário do cliente ao solicitar ajuste */
    @Column(name = "comentario", columnDefinition = "TEXT")
    private String comentario;

    @CreationTimestamp
    @Column(name = "enviado_em", nullable = false, updatable = false)
    private LocalDateTime enviadoEm;
}
