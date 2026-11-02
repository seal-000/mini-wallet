package com.miri.wallet;

import java.math.BigDecimal;

public record AccountResponse(Long id, String currency, BigDecimal balance, String accountNumber) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getId(), account.getCurrency(), account.getBalance(),
                account.getAccountNumber());
    }
}
