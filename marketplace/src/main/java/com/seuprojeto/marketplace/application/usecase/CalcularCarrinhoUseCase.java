package com.seuprojeto.marketplace.application.usecase;

import com.seuprojeto.marketplace.application.dto.SelecaoCarrinho;
import com.seuprojeto.marketplace.domain.model.CategoriaProduto;
import com.seuprojeto.marketplace.domain.model.Produto;
import com.seuprojeto.marketplace.domain.model.ResumoCarrinho;
import com.seuprojeto.marketplace.domain.repository.ProdutoRepositorio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

public class CalcularCarrinhoUseCase {

    private final ProdutoRepositorio produtoRepositorio;

    public CalcularCarrinhoUseCase(ProdutoRepositorio produtoRepositorio) {
        this.produtoRepositorio = produtoRepositorio;
    }

    public ResumoCarrinho executar(List<SelecaoCarrinho> selecaoCarrinhos) {
        // Calcula o subtotal
        BigDecimal subtotal = BigDecimal.ZERO;
        int totalItens = 0;
        BigDecimal percentualDescontoCategoria = BigDecimal.ZERO;

        for (SelecaoCarrinho selecao : selecaoCarrinhos) {
            Optional<Produto> produto = produtoRepositorio.findById(selecao.getIdProduto());
            if (produto.isPresent()) {
                Produto p = produto.get();
                BigDecimal precoItem = p.getPreco().multiply(new BigDecimal(selecao.getQuantidade()));
                subtotal = subtotal.add(precoItem);
                totalItens += selecao.getQuantidade();

                // Calcula desconto por categoria (por item)
                BigDecimal descontoCategoria = obterDescontoCategoria(p.getCategoriaProduto());
                percentualDescontoCategoria = percentualDescontoCategoria.add(
                        descontoCategoria.multiply(new BigDecimal(selecao.getQuantidade()))
                );
            }
        }

        // Calcula desconto por quantidade
        BigDecimal percentualDescontoQuantidade = obterDescontoQuantidade(totalItens);

        // Soma os descontos percentuais
        BigDecimal percentualDescontoTotal = percentualDescontoQuantidade.add(percentualDescontoCategoria);

        // Aplica limite máximo de 25%
        BigDecimal limiteMaximo = new BigDecimal("25");
        if (percentualDescontoTotal.compareTo(limiteMaximo) > 0) {
            percentualDescontoTotal = limiteMaximo;
        }

        // Calcula o valor do desconto em reais
        BigDecimal valorDesconto = subtotal.multiply(percentualDescontoTotal).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        return new ResumoCarrinho(subtotal, valorDesconto);
    }

    private BigDecimal obterDescontoQuantidade(int totalItens) {
        if (totalItens == 1) {
            return BigDecimal.ZERO;
        } else if (totalItens == 2) {
            return new BigDecimal("5");
        } else if (totalItens == 3) {
            return new BigDecimal("7");
        } else {
            return new BigDecimal("10");
        }
    }

    private BigDecimal obterDescontoCategoria(CategoriaProduto categoria) {
        return switch (categoria) {
            case CAPINHA -> new BigDecimal("3");
            case CARREGADOR -> new BigDecimal("5");
            case FONE -> new BigDecimal("3");
            case PELICULA -> new BigDecimal("2");
            case SUPORTE -> new BigDecimal("2");
        };
    }
}