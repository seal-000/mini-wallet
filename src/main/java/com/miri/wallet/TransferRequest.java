package com.miri.wallet;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record TransferRequest(
        @NotNull @Positive Long fromAccountId,
        @NotBlank @Email String toEmail,
        @NotNull @Positive @Digits(integer = 15, fraction = 4) BigDecimal amount,
        @Pattern(regexp = "^[A-Za-z]{3}$") String toCurrency
) {
}
