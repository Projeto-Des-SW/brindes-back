package br.ed.ufape.bahiabrindes.repository;

import java.math.BigDecimal;

public interface ProdutoEstoqueItemProjection {
    Long getId();
    String getCodigo();
    String getMateriaPrima();
    BigDecimal getQuantidadeAtual();
    BigDecimal getEstoqueMinimo();
    BigDecimal getValorUnitario();
}

