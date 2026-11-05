package com.miri.wallet.fx;

import com.miri.wallet.common.FxUnavailableException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class FxRateService {
    public static final BigDecimal FEE_RATE = new BigDecimal("0.005");

    private final FxRateProvider provider;

    public FxRateService(FxRateProvider provider) {
        this.provider = provider;
    }

    @Cacheable(cacheNames = "fxRates", key = "#from + '-' + #to")
    public BigDecimal rate(String from, String to) {
        if (from.equals(to)) {
            return BigDecimal.ONE;
        }
        BigDecimal rate;
        try {
            rate = provider.fetchRate(from, to);
        } catch (RuntimeException e) {
            throw new FxUnavailableException("Exchange rate service is unavailable, try again shortly", e);
        }
        if (rate == null || rate.signum() <= 0) {
            throw new FxUnavailableException("No exchange rate available for " + from + " to " + to, null);
        }
        return rate;
    }

    /** Fee charged to the sender on top of the amount, in the source currency's minor units. */
    public BigDecimal fee(BigDecimal amount, String sourceCurrency) {
        var digits = Currency.getInstance(sourceCurrency).getDefaultFractionDigits();
        return amount.multiply(FEE_RATE).setScale(digits, RoundingMode.HALF_EVEN);
    }

    /** Converts using the target currency's minor units (e.g. 2 for EUR, 0 for JPY), banker's rounding. */
    public BigDecimal convert(BigDecimal amount, BigDecimal rate, String toCurrency) {
        var digits = Currency.getInstance(toCurrency).getDefaultFractionDigits();
        return amount.multiply(rate).setScale(digits, RoundingMode.HALF_EVEN);
    }
}
