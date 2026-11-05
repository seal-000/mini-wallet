package com.miri.wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferCompleted(
        Long transferId,
        Long fromAccountId,
        Long toAccountId,
        String fromCurrency,
        BigDecimal amount,
        String toCurrency,
        BigDecimal toAmount,
        BigDecimal exchangeRate,
        LocalDateTime occurredAt) {
}
