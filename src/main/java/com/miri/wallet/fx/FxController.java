package com.miri.wallet.fx;

import com.miri.wallet.RegistrationService;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fx")
public class FxController {
    private final FxRateService fxRateService;

    public FxController(FxRateService fxRateService) {
        this.fxRateService = fxRateService;
    }

    @GetMapping("/quote")
    public FxQuote quote(@RequestParam String from, @RequestParam String to, @RequestParam BigDecimal amount) {
        var source = from.trim().toUpperCase();
        var target = to.trim().toUpperCase();
        if (!RegistrationService.SUPPORTED_CURRENCIES.contains(source)
                || !RegistrationService.SUPPORTED_CURRENCIES.contains(target)) {
            throw new IllegalArgumentException("Unsupported currency");
        }
        if (amount.signum() <= 0 || amount.scale() > 4 || amount.precision() > 19) {
            throw new IllegalArgumentException("Amount must be positive and fit NUMERIC(19,4)");
        }
        var rate = fxRateService.rate(source, target);
        var fee = source.equals(target) ? BigDecimal.ZERO : fxRateService.fee(amount, source);
        return new FxQuote(source, target, rate, amount, fxRateService.convert(amount, rate, target), fee);
    }
}
