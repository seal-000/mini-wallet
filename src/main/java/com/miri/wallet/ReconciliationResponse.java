package com.miri.wallet;

import java.math.BigDecimal;

public record ReconciliationResponse(Long accountId, String currency, BigDecimal accountBalance,
                                     BigDecimal ledgerBalance, boolean balanced) {
}
