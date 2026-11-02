package com.miri.wallet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TransferServiceConcurrencyTest {
    private static final int THREAD_COUNT = 50;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferService transferService;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Test
    void concurrentTransfersPreserveTotalBalanceAndNeverOverdraw() throws Exception {
        var sourceUser = userRepository.saveAndFlush(
                new User(null, "concurrency-a-" + UUID.randomUUID() + "@wallet.test", "unused"));
        var destinationUser = userRepository.saveAndFlush(
                new User(null, "concurrency-b-" + UUID.randomUUID() + "@wallet.test", "unused"));
        var source = accountRepository.saveAndFlush(
                new Account(null, sourceUser.getId(), "USD", new BigDecimal("100.0000"), 0L));
        var destination = accountRepository.saveAndFlush(
                new Account(null, destinationUser.getId(), "USD", new BigDecimal("0.0000"), 0L));
        var initialTotal = source.getBalance().add(destination.getBalance());
        ledgerEntryRepository.save(new LedgerEntry(null, null, source.getId(), "CREDIT",
                source.getBalance(), java.time.LocalDateTime.now()));

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        var ready = new CountDownLatch(THREAD_COUNT);
        var start = new CountDownLatch(1);
        var futures = new ArrayList<Future<Transfer>>(THREAD_COUNT);
        var transferIds = new HashSet<Long>();
        try {
            for (int i = 0; i < THREAD_COUNT; i++) {
                var index = i;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting to start transfers");
                    }
                    return transferService.transfer(
                            "concurrent-" + UUID.randomUUID() + "-" + index,
                            source.getId(),
                            destination.getId(),
                            new BigDecimal("1.0000"));
                }));
            }

            assertTrue(ready.await(10, TimeUnit.SECONDS), "All transfer threads should be ready");
            start.countDown();

            for (var future : futures) {
                assertTrue(transferIds.add(future.get(60, TimeUnit.SECONDS).getId()),
                        "Each request should create a distinct transfer");
            }
        } finally {
            start.countDown();
            executor.shutdownNow();
            executor.awaitTermination(30, TimeUnit.SECONDS);
        }

        var finalSource = accountRepository.findById(source.getId()).orElseThrow();
        var finalDestination = accountRepository.findById(destination.getId()).orElseThrow();
        var finalTotal = finalSource.getBalance().add(finalDestination.getBalance());

        assertEquals(THREAD_COUNT, transferIds.size());
        assertEquals(0, initialTotal.compareTo(finalTotal), "Total wallet balance must be conserved");
        assertTrue(finalSource.getBalance().signum() >= 0, "Source balance must not be negative");
        assertTrue(finalDestination.getBalance().signum() >= 0, "Destination balance must not be negative");
        assertEquals(new BigDecimal("50.0000"), finalSource.getBalance());
        assertEquals(new BigDecimal("50.0000"), finalDestination.getBalance());
        assertEquals(0, ledgerEntryRepository.balanceOf(source.getId()).compareTo(finalSource.getBalance()));
        assertEquals(0, ledgerEntryRepository.balanceOf(destination.getId())
                .compareTo(finalDestination.getBalance()));
        var entry = ledgerEntryRepository.findAll().get(0);
        assertThrows(Exception.class, () -> jdbcTemplate.update(
                "UPDATE ledger_entries SET amount = 1 WHERE id = ?", entry.getId()));
        assertThrows(Exception.class, () -> jdbcTemplate.update(
                "DELETE FROM ledger_entries WHERE id = ?", entry.getId()));
    }

}
