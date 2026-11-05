package com.miri.wallet;

import com.miri.wallet.common.AccountNotFoundException;
import com.miri.wallet.common.ForbiddenOperationException;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {
    private final TransferService transferService;
    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public TransferController(TransferService transferService,
                              TransferRepository transferRepository,
                              AccountRepository accountRepository,
                              UserRepository userRepository) {
        this.transferService = transferService;
        this.transferRepository = transferRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public TransferResponse create(
            Authentication authentication,
            @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody TransferRequest request
    ) {
        var user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        var source = accountRepository.findById(request.fromAccountId())
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found: " + request.fromAccountId()));
        if (!user.getId().equals(source.getUserId())) {
            throw new ForbiddenOperationException("You can only transfer from your own accounts");
        }
        var recipientEmail = request.toEmail().trim().toLowerCase();
        var targetCurrency = request.toCurrency() == null
                ? source.getCurrency()
                : request.toCurrency().toUpperCase();
        if (!RegistrationService.SUPPORTED_CURRENCIES.contains(targetCurrency)) {
            throw new IllegalArgumentException("Unsupported currency: " + targetCurrency);
        }
        var destination = (TransferService.FX_CLEARING_EMAIL.equals(recipientEmail)
                || TransferService.FEE_REVENUE_EMAIL.equals(recipientEmail)
                ? java.util.Optional.<Account>empty()
                : userRepository.findByEmail(recipientEmail)
                .flatMap(recipient -> accountRepository
                        .findFirstByUserIdAndCurrencyOrderById(recipient.getId(), targetCurrency)))
                .orElseThrow(() -> new AccountNotFoundException(
                        "Recipient not found or has no " + targetCurrency + " wallet"));
        var transfer = transferService.transfer(
                key,
                request.fromAccountId(),
                destination.getId(),
                request.amount()
        );
        return TransferResponse.from(transfer, source, destination);
    }

    @GetMapping
    public List<TransferResponse> history(Authentication authentication) {
        var user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        var accountIds = accountRepository.findByUserIdOrderById(user.getId()).stream()
                .map(Account::getId)
                .toList();
        if (accountIds.isEmpty()) {
            return List.of();
        }
        var transfers = transferRepository
                .findByFromAccountIdInOrToAccountIdInOrderByCreatedAtDescIdDesc(accountIds, accountIds);
        var related = transfers.stream()
                .flatMap(t -> java.util.stream.Stream.of(t.getFromAccountId(), t.getToAccountId()))
                .distinct()
                .toList();
        var accountsById = accountRepository.findAllById(related).stream()
                .collect(java.util.stream.Collectors.toMap(Account::getId, a -> a));
        return transfers.stream()
                .map(t -> TransferResponse.from(
                        t, accountsById.get(t.getFromAccountId()), accountsById.get(t.getToAccountId())))
                .toList();
    }
}
