package com.miri.wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.miri.wallet.common.AccountNotFoundException;
import com.miri.wallet.common.InsufficientFundsException;
import com.miri.wallet.fx.FxRateService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {
    public static final String FX_CLEARING_EMAIL = "fx-clearing@wallet.internal";
    public static final String FEE_REVENUE_EMAIL = "fee-revenue@wallet.internal";

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final FxRateService fxRateService;
    private final ApplicationEventPublisher eventPublisher;

    public TransferService(AccountRepository accountRepository,
                           TransferRepository transferRepository,
                           LedgerEntryRepository ledgerEntryRepository,
                           FxRateService fxRateService,
                           ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.fxRateService = fxRateService;
    }

    @Transactional
    public Transfer transfer(String idemKey, Long fromId, Long toId, BigDecimal amount) {
        if (idemKey == null || idemKey.isBlank() || idemKey.length() > 100) {
            throw new IllegalArgumentException("Idempotency key must contain 1 to 100 characters");
        }
        if (fromId == null || toId == null) {
            throw new IllegalArgumentException("Account IDs must not be null");
        }
        if (Objects.equals(fromId, toId)) {
            throw new IllegalArgumentException("Source and destination accounts must be different");
        }
        if (amount == null || amount.signum() <= 0 || amount.scale() > 4
                || amount.precision() > 19) {
            throw new IllegalArgumentException("Amount must be positive and fit NUMERIC(19,4)");
        }

        var existingTransfer = transferRepository.findByIdempotencyKey(idemKey);
        if (existingTransfer.isPresent()) {
            return existingTransfer.get();
        }

        var firstId = fromId < toId ? fromId : toId;
        var secondId = fromId < toId ? toId : fromId;
        var firstAccount = accountRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + firstId));
        var secondAccount = accountRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + secondId));

        var source = Objects.equals(fromId, firstId) ? firstAccount : secondAccount;
        var destination = Objects.equals(toId, firstId) ? firstAccount : secondAccount;

        var crossCurrency = !Objects.equals(source.getCurrency(), destination.getCurrency());
        var rate = BigDecimal.ONE;
        var toAmount = amount;
        var fee = BigDecimal.ZERO;
        if (crossCurrency) {
            rate = fxRateService.rate(source.getCurrency(), destination.getCurrency());
            toAmount = fxRateService.convert(amount, rate, destination.getCurrency());
            fee = fxRateService.fee(amount, source.getCurrency());
            if (toAmount.signum() <= 0) {
                throw new IllegalArgumentException("Amount is too small to convert");
            }
        }

        // The ledger is the source of truth; accounts.balance is a cached projection of it.
        var sourceBalance = ledgerEntryRepository.balanceOf(fromId);
        var destinationBalance = ledgerEntryRepository.balanceOf(toId);
        var totalDebit = amount.add(fee);
        if (sourceBalance.compareTo(totalDebit) < 0) {
            throw new InsufficientFundsException("Insufficient funds in source account");
        }

        var accountsToSave = new ArrayList<>(List.of(source, destination));
        var legs = new ArrayList<Leg>();
        legs.add(new Leg(fromId, "DEBIT", amount));
        if (crossCurrency) {
            // Locked after the user wallets and always in id order, so no lock cycles are possible.
            var clearingIn = clearingAccount(source.getCurrency());
            var clearingOut = clearingAccount(destination.getCurrency());
            var firstClearing = clearingIn.getId() < clearingOut.getId() ? clearingIn : clearingOut;
            var secondClearing = firstClearing == clearingIn ? clearingOut : clearingIn;
            lockClearing(firstClearing.getId());
            lockClearing(secondClearing.getId());
            var inBalance = ledgerEntryRepository.balanceOf(clearingIn.getId());
            var outBalance = ledgerEntryRepository.balanceOf(clearingOut.getId());
            if (outBalance.compareTo(toAmount) < 0) {
                throw new InsufficientFundsException(
                        "FX liquidity unavailable for " + destination.getCurrency());
            }
            clearingIn.setBalance(inBalance.add(amount));
            clearingOut.setBalance(outBalance.subtract(toAmount));
            accountsToSave.add(clearingIn);
            accountsToSave.add(clearingOut);
            legs.add(new Leg(clearingIn.getId(), "CREDIT", amount));
            legs.add(new Leg(clearingOut.getId(), "DEBIT", toAmount));
            if (fee.signum() > 0) {
                var feeAccount = systemAccount(FEE_REVENUE_EMAIL, source.getCurrency());
                lockClearing(feeAccount.getId());
                feeAccount.setBalance(ledgerEntryRepository.balanceOf(feeAccount.getId()).add(fee));
                accountsToSave.add(feeAccount);
                legs.add(new Leg(fromId, "DEBIT", fee));
                legs.add(new Leg(feeAccount.getId(), "CREDIT", fee));
            }
        }
        legs.add(new Leg(toId, "CREDIT", toAmount));

        source.setBalance(sourceBalance.subtract(totalDebit));
        destination.setBalance(destinationBalance.add(toAmount));
        accountRepository.saveAll(accountsToSave);

        var now = LocalDateTime.now();
        var transfer = transferRepository.save(
                new Transfer(null, idemKey, fromId, toId, amount, toAmount, rate, fee, now));
        ledgerEntryRepository.saveAll(legs.stream()
                .map(leg -> new LedgerEntry(null, transfer.getId(), leg.accountId(), leg.direction(),
                        leg.amount(), now))
                .toList());
        eventPublisher.publishEvent(new TransferCompleted(transfer.getId(), fromId, toId,
                source.getCurrency(), amount, destination.getCurrency(), toAmount, rate, now));
        return transfer;
    }

    private record Leg(Long accountId, String direction, BigDecimal amount) {
    }

    private Account clearingAccount(String currency) {
        return systemAccount(FX_CLEARING_EMAIL, currency);
    }

    private Account systemAccount(String email, String currency) {
        return accountRepository.findByUserEmailAndCurrency(email, currency)
                .orElseThrow(() -> new IllegalStateException("Missing " + email + " wallet for " + currency));
    }

    private void lockClearing(Long id) {
        accountRepository.findByIdForUpdate(id).orElseThrow();
    }
}