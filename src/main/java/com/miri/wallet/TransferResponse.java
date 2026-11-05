package com.miri.wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferResponse(
        Long id,
        Long fromAccountId,
        Long toAccountId,
        String fromAccountEnding,
        String toAccountEnding,
        String fromCurrency,
        String toCurrency,
        BigDecimal amount,
        BigDecimal toAmount,
        BigDecimal exchangeRate,
        BigDecimal fee,
        LocalDateTime createdAt
) {
    public static TransferResponse from(Transfer transfer, Account from, Account to) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getFromAccountId(),
                transfer.getToAccountId(),
                ending(from),
                ending(to),
                from == null ? null : from.getCurrency(),
                to == null ? null : to.getCurrency(),
                transfer.getAmount(),
                transfer.getToAmount(),
                transfer.getExchangeRate(),
                transfer.getFee(),
                transfer.getCreatedAt()
        );
    }

    private static String ending(Account account) {
        var number = account == null ? null : account.getAccountNumber();
        return number == null ? null : number.substring(Math.max(0, number.length() - 4));
    }
}
