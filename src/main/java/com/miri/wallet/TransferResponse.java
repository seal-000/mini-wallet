package com.miri.wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferResponse(
        Long id,
        Long fromAccountId,
        Long toAccountId,
        String fromAccountEnding,
        String toAccountEnding,
        BigDecimal amount,
        LocalDateTime createdAt
) {
    public static TransferResponse from(Transfer transfer, Account from, Account to) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getFromAccountId(),
                transfer.getToAccountId(),
                ending(from),
                ending(to),
                transfer.getAmount(),
                transfer.getCreatedAt()
        );
    }

    private static String ending(Account account) {
        var number = account == null ? null : account.getAccountNumber();
        return number == null ? null : number.substring(Math.max(0, number.length() - 4));
    }
}
