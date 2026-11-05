package com.miri.wallet.fx;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class FrankfurterRateProvider implements FxRateProvider {
    private final RestClient client;

    public FrankfurterRateProvider(@Value("${app.fx-api-url}") String baseUrl) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    record Response(Map<String, BigDecimal> rates) {
    }

    @Override
    public BigDecimal fetchRate(String from, String to) {
        var response = client.get()
                .uri("/latest?base={from}&symbols={to}", from, to)
                .retrieve()
                .body(Response.class);
        return response == null || response.rates() == null ? null : response.rates().get(to);
    }
}
