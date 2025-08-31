package cass.service;

import cass.client.CurrencyPriceClient;
import cass.dto.CurrencyPriceDTO;
import cass.dto.QuotationDTO;
import cass.entity.QuotationEntity;
import cass.message.KafkaEvents;
import cass.repository.QuotationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@ApplicationScoped
public class QuotationService {

    private static final Logger LOG = LoggerFactory.getLogger(QuotationService.class);

    @Inject
    QuotationRepository quotationRepository;

    @Inject
    KafkaEvents kafkaEvents;

    @Inject
    @RestClient
    CurrencyPriceClient currencyPriceClient;

    public void getCurrencyPrice() {
        try {
            CurrencyPriceDTO currencyPriceInfo = currencyPriceClient.getPriceByPair("USD-BRL");
            LOG.debug("O valor da variavelImportante é: {}", currencyPriceInfo);
            //Verifica se a resposta e o objeto interno não são nulos
            if (currencyPriceInfo != null && currencyPriceInfo.getUsdBrl() != null) {
                if (updateCurrencyInfoPrice(currencyPriceInfo)) {
                    kafkaEvents.sendNewKafkaEvent(QuotationDTO
                            .builder()
                            .currencyPrice(new BigDecimal(currencyPriceInfo.getUsdBrl().getBid()))
                            .date(new Date())
                            .build());
                }
            } else {
                LOG.warn("Resposta da API de cotação foi nula ou inválida.");
            }
            // Captura exceções caso a API falhe (fique offline, etc.)
        } catch (Exception e) {
            LOG.error("Falha ao se comunicar com a API de cotação.", e);
        }
    }

    // O resto da sua classe continua igual
    private boolean updateCurrencyInfoPrice(CurrencyPriceDTO currencyPriceInfo) {
        BigDecimal currencyPrice = new BigDecimal(currencyPriceInfo.getUsdBrl().getBid());
        boolean updatePrice = false;

        List<QuotationEntity> quotationList = quotationRepository.findAll().list();

        if (quotationList.isEmpty()) {
            saveQuotation(currencyPriceInfo);
            updatePrice = true;
        } else {
            QuotationEntity lastDollarPrice = quotationList.get(quotationList.size() - 1);

            if (currencyPrice.floatValue() > lastDollarPrice.getCurrencyPrice().floatValue()) {
                saveQuotation(currencyPriceInfo);
                updatePrice = true;
            }
        }
        return updatePrice;
    }

    private void saveQuotation(CurrencyPriceDTO currencyInfo) {
        QuotationEntity quotation = new QuotationEntity();

        quotation.setDate(new Date());
        quotation.setCurrencyPrice(new BigDecimal(currencyInfo.getUsdBrl().getBid()));
        quotation.setPctChange(currencyInfo.getUsdBrl().getPctChange());
        quotation.setPair("USD-BRL");

        quotationRepository.persist(quotation);
    }
}