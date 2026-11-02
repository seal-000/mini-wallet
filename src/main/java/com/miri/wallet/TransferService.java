package com.miri.wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import com.miri.wallet.common.AccountNotFoundException;
import com.miri.wallet.common.InsufficientFundsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public TransferService(AccountRepository accountRepository,
                           TransferRepository transferRepository,
                           LedgerEntryRepository ledgerEntryRepository) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
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

        if (!Objects.equals(source.getCurrency(), destination.getCurrency())) {
            throw new IllegalArgumentException("Accounts must use the same currency");
        }
        // The ledger is the source of truth; accounts.balance is a cached projection of it.
        var sourceBalance = ledgerEntryRepository.balanceOf(fromId);
        var destinationBalance = ledgerEntryRepository.balanceOf(toId);
        if (sourceBalance.compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds in source account");
        }

        source.setBalance(sourceBalance.subtract(amount));
        destination.setBalance(destinationBalance.add(amount));
        accountRepository.saveAll(List.of(source, destination));

        var now = LocalDateTime.now();
        var transfer = new Transfer(null, idemKey, fromId, toId, amount, now);
        transfer = transferRepository.save(transfer);
        ledgerEntryRepository.saveAll(List.of(
                new LedgerEntry(null, transfer.getId(), fromId, "DEBIT", amount, now),
                new LedgerEntry(null, transfer.getId(), toId, "CREDIT", amount, now)
        ));
        return transfer;
    }
}