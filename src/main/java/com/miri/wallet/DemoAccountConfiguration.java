package com.miri.wallet;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@ConditionalOnProperty(prefix = "app.demo-account", name = "enabled", havingValue = "true")
public class DemoAccountConfiguration {
    private static final String EMAIL = "demo@wallet.com";
    private static final String PASSWORD = "demo1234";

    @Bean
    CommandLineRunner createDemoAccount(UserRepository userRepository,
                                        RegistrationService registrationService,
                                        PasswordEncoder passwordEncoder) {
        return args -> {
            var user = userRepository.findByEmail(EMAIL);
            if (user.isEmpty()) {
                registrationService.register(EMAIL, passwordEncoder.encode(PASSWORD));
            } else if (!passwordEncoder.matches(PASSWORD, user.get().getPasswordHash())) {
                user.get().setPasswordHash(passwordEncoder.encode(PASSWORD));
                userRepository.save(user.get());
            }
        };
    }
}
