package br.edu.atitus.currencyapi.servicesimpl;

import br.edu.atitus.currencyapi.dtos.CurrencyResponse;
import br.edu.atitus.currencyapi.repositories.CurrencyRepository;
import br.edu.atitus.currencyapi.services.CurrencyService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CurrencyServiceJpa implements CurrencyService {

    private static final Logger log = LoggerFactory.getLogger(CurrencyServiceJpa.class);

    private final CurrencyRepository repository;
    private final BcbClientService bcbClientService;

    @Value("${server.port:8080}")
    private String serverPort;

    // Data fixa (formato MM-dd-yyyy) de um dia útil com cotação disponível no BCB
    @Value("${app.bcb.data-cotacao:06-10-2026}")
    private String bcbDataCotacao;

    public CurrencyServiceJpa(CurrencyRepository repository, BcbClientService bcbClientService) {
        this.repository = repository;
        this.bcbClientService = bcbClientService;
    }

    @Override
    public CurrencyResponse findBySourceCurrencyAndTargetCurrency(String sourceCurrency, String targetCurrency) throws Exception {
        String source = sourceCurrency.toUpperCase();
        String target = targetCurrency.toUpperCase();
        String environment = "Currency API running in port " + serverPort;

        Double conversionRate = getRateFromBcb(source, target);
        if (conversionRate == null) {
            // Fallback: cotação cadastrada no banco de dados local
            var currency = repository.findBySourceCurrencyAndTargetCurrency(source, target)
                    .orElseThrow(() -> new EntityNotFoundException("Currency não encontrado"));
            conversionRate = currency.getConversionRate();
            log.info("Cotação {}->{} obtida do banco de dados local", source, target);
        }
        return new CurrencyResponse(source, target, conversionRate, environment);
    }

    private Double getRateFromBcb(String source, String target) {
        // O BCB informa a cotação de moedas estrangeiras em relação ao real (BRL)
        if (!"BRL".equals(target) || "BRL".equals(source)) {
            return null;
        }
        try {
            Double rate = bcbClientService.getCotacaoVenda(source, bcbDataCotacao);
            if (rate == null) {
                log.warn("BCB sem cotação para {} em {}. Usando fallback local", source, bcbDataCotacao);
            } else {
                log.info("Cotação {}->{} obtida do BCB", source, target);
            }
            return rate;
        } catch (Exception e) {
            log.warn("Falha ao consultar o BCB para {}: {}. Usando fallback local", source, e.toString());
            return null;
        }
    }
}
