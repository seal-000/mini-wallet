package com.miri.wallet;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public AccountController(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<AccountResponse> list(Authentication authentication) {
        var user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        return accountRepository.findByUserIdOrderById(user.getId()).stream()
                .map(AccountResponse::from)
                .toList();
    }
}
