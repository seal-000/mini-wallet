package com.miri.wallet.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.miri.wallet.common.FxUnavailableException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FxRateServiceTest {
    private final FxRateProvider provider = mock(FxRateProvider.class);
    private final FxRateService service = new FxRateService(provider);

    @Test
    void convertsUsingTargetCurrencyMinorUnits() {
        assertEquals(new BigDecimal("8.50"), service.convert(new BigDecimal("10"), new BigDecimal("0.85"), "EUR"));
        assertEquals(new BigDecimal("1507"),
                service.convert(new BigDecimal("10.01"), new BigDecimal("150.5"), "JPY"));
    }

    @Test
    void usesBankersRoundingOnExactHalves() {
        assertEquals(new BigDecimal("150"), service.convert(BigDecimal.ONE, new BigDecimal("150.5"), "JPY"));
        assertEquals(new BigDecimal("0.04"), service.convert(new BigDecimal("0.05"), new BigDecimal("0.9"), "EUR"));
        assertEquals(new BigDecimal("0.06"), service.convert(new BigDecimal("0.07"), new BigDecimal("0.9"), "EUR"));
    }

    @Test
    void rejectsMissingOrFailedRates() {
        when(provider.fetchRate("USD", "EUR")).thenReturn(null);
        assertThrows(FxUnavailableException.class, () -> service.rate("USD", "EUR"));
        when(provider.fetchRate("USD", "GBP")).thenThrow(new IllegalStateException("down"));
        assertThrows(FxUnavailableException.class, () -> service.rate("USD", "GBP"));
    }

    @Test
    void feeIsHalfPercentRoundedToSourceMinorUnits() {
        assertEquals(new BigDecimal("0.05"), service.fee(new BigDecimal("10"), "USD"));
        assertEquals(new BigDecimal("0.50"), service.fee(new BigDecimal("100.5"), "EUR"));
        assertEquals(new BigDecimal("5"), service.fee(new BigDecimal("1000"), "JPY"));
    }

    @Test
    void sameCurrencyRateIsOne() {
        assertEquals(BigDecimal.ONE, service.rate("USD", "USD"));
    }
}
