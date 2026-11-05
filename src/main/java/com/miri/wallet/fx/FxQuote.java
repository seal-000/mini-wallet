package com.miri.wallet.fx;

import java.math.BigDecimal;

public record FxQuote(String from, String to, BigDecimal rate, BigDecimal amount, BigDecimal convertedAmount,
                      BigDecimal fee) {
}
