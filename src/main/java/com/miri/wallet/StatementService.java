package com.miri.wallet;

import com.miri.wallet.common.AccountNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatementService {
    static final int MAX_PAGE_SIZE = 100;
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "id");

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public StatementService(AccountRepository accountRepository, TransferRepository transferRepository,
                            LedgerEntryRepository ledgerEntryRepository) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional(readOnly = true)
    public StatementPage page(Long userId, Long accountId, String type, LocalDate from, LocalDate to,
                              int page, int size) {
        var pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), MAX_PAGE_SIZE),
                NEWEST_FIRST);
        var result = query(userId, accountId, type, from, to, pageable);
        return new StatementPage(result.lines(), result.page().getNumber(), result.page().getSize(),
                result.page().getTotalElements(), result.page().getTotalPages());
    }

    @Transactional(readOnly = true)
    public List<StatementLine> all(Long userId, Long accountId, String type, LocalDate from, LocalDate to) {
        return query(userId, accountId, type, from, to, Pageable.unpaged(NEWEST_FIRST)).lines();
    }

    private record Result(org.springframework.data.domain.Page<LedgerEntry> page, List<StatementLine> lines) {
    }

    private Result query(Long userId, Long accountId, String type, LocalDate from, LocalDate to,
                         Pageable pageable) {
        var own = accountRepository.findByUserIdOrderById(userId).stream()
                .collect(Collectors.toMap(Account::getId, a -> a));
        List<Long> ids;
        if (accountId == null) {
            ids = List.copyOf(own.keySet());
        } else if (own.containsKey(accountId)) {
            ids = List.of(accountId);
        } else {
            throw new AccountNotFoundException("Account not found: " + accountId);
        }
        List<String> directions = switch (type == null ? "" : type.toLowerCase()) {
            case "debit" -> List.of("DEBIT");
            case "credit" -> List.of("CREDIT");
            case "", "all" -> List.of("DEBIT", "CREDIT");
            default -> throw new IllegalArgumentException("type must be debit, credit or all");
        };
        var start = from == null ? LocalDateTime.of(1970, 1, 1, 0, 0) : from.atStartOfDay();
        var end = to == null ? LocalDateTime.of(9999, 1, 1, 0, 0) : to.plusDays(1).atStartOfDay();
        if (ids.isEmpty()) {
            ids = List.of(-1L);
        }

        var page = ledgerEntryRepository.search(ids, directions, start, end, pageable);
        var transferIds = page.getContent().stream().map(LedgerEntry::getTransferId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        var transfers = transferRepository.findAllById(transferIds).stream()
                .collect(Collectors.toMap(Transfer::getId, t -> t));
        var otherIds = transfers.values().stream()
                .flatMap(t -> Stream.of(t.getFromAccountId(), t.getToAccountId())).distinct().toList();
        Map<Long, Account> accounts = accountRepository.findAllById(otherIds).stream()
                .collect(Collectors.toMap(Account::getId, a -> a));

        var lines = page.getContent().stream().map(e -> {
            var account = own.get(e.getAccountId());
            var debit = "DEBIT".equals(e.getDirection());
            String description;
            var transfer = e.getTransferId() == null ? null : transfers.get(e.getTransferId());
            if (transfer == null) {
                description = "Opening balance";
            } else {
                var otherId = debit ? transfer.getToAccountId() : transfer.getFromAccountId();
                var isFee = debit && transfer.getFee() != null && transfer.getFee().signum() > 0
                        && e.getAmount().compareTo(transfer.getFee()) == 0;
                description = isFee ? "Transfer fee"
                        : (debit ? "Transfer to" : "Transfer from") + " account ending "
                        + ending(accounts.get(otherId));
            }
            return new StatementLine(e.getId(), e.getCreatedAt(), e.getAccountId(), account.getCurrency(),
                    description, debit ? "Debit" : "Credit", debit ? e.getAmount().negate() : e.getAmount());
        }).toList();
        return new Result(page, lines);
    }

    private static String ending(Account account) {
        var number = account == null ? null : account.getAccountNumber();
        return number == null ? "????" : number.substring(Math.max(0, number.length() - 4));
    }
}
