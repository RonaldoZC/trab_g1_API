package br.edu.atitus.currencyapi.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BcbCotacao(
        Double cotacaoCompra,
        Double cotacaoVenda,
        String dataHoraCotacao,
        String tipoBoletim
) {
}
