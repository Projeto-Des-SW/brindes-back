package br.ed.ufape.bahiabrindes.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "clientes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(unique = true)
    private String documento;
    
    private String telefone;
    private String segmentacao;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "endereco_id")
    private Endereco endereco;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;
    
    @Column(name = "senha", nullable = false, length = 255)
    private String senha;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    /** Foto de perfil em Base64 (ex: "data:image/jpeg;base64,...") */
    @Column(name = "foto_perfil", columnDefinition = "TEXT")
    private String fotoPerfil;
}