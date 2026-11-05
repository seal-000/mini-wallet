package com.miri.wallet.fx;

import java.math.BigDecimal;

public interface FxRateProvider {
    /** Units of {@code to} currency per one unit of {@code from} currency. */
    BigDecimal fetchRate(String from, String to);
}
