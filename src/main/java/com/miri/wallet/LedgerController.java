package com.miri.wallet;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ledger")
public class LedgerController {
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public LedgerController(AccountRepository accountRepository, UserRepository userRepository,
                            LedgerEntryRepository ledgerEntryRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @GetMapping("/reconcile")
    @Transactional(readOnly = true)
    public List<ReconciliationResponse> reconcile(Authentication authentication) {
        var user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        return accountRepository.findByUserIdOrderById(user.getId()).stream()
                .map(a -> {
                    var ledger = ledgerEntryRepository.balanceOf(a.getId());
                    return new ReconciliationResponse(a.getId(), a.getCurrency(), a.getBalance(), ledger,
                            a.getBalance().compareTo(ledger) == 0);
                })
                .toList();
    }
}
