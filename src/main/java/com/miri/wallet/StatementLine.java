package com.miri.wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StatementLine(Long id, LocalDateTime date, Long accountId, String currency,
                            String description, String type, BigDecimal amount) {
}
