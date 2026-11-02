package com.miri.wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {
    public static final List<String> SUPPORTED_CURRENCIES = List.of("USD", "EUR", "GBP", "JPY");
    private static final BigDecimal DEMO_STARTING_BALANCE = new BigDecimal("1000.0000");

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    private final LedgerEntryRepository ledgerEntryRepository;

    public RegistrationService(UserRepository userRepository, AccountRepository accountRepository,
                               LedgerEntryRepository ledgerEntryRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional
    public User register(String email, String passwordHash) {
        var user = userRepository.save(new User(null, email, passwordHash));
        var accounts = accountRepository.saveAll(SUPPORTED_CURRENCIES.stream()
                .map(currency -> new Account(null, user.getId(), currency, DEMO_STARTING_BALANCE, null))
                .toList());
        var now = LocalDateTime.now();
        ledgerEntryRepository.saveAll(accounts.stream()
                .map(a -> new LedgerEntry(null, null, a.getId(), "CREDIT", DEMO_STARTING_BALANCE, now))
                .toList());
        return user;
    }
}
