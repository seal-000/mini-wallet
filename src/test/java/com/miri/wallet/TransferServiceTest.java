package com.miri.wallet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.miri.wallet.common.InsufficientFundsException;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TransferServiceTest {
    private AccountRepository accountRepository;
    private TransferRepository transferRepository;
    private LedgerEntryRepository ledgerEntryRepository;
    private TransferService transferService;

    @BeforeEach
    void setUp() {
        accountRepository = Mockito.mock(AccountRepository.class);
        transferRepository = Mockito.mock(TransferRepository.class);
        ledgerEntryRepository = Mockito.mock(LedgerEntryRepository.class);
        transferService = new TransferService(
                accountRepository, transferRepository, ledgerEntryRepository);
    }

    @Test
    void transferMovesFundsAndRecordsLedgerEntries() {
        var source = new Account(1L, 7L, "USD", new BigDecimal("25.0000"), 0L);
        var destination = new Account(2L, 7L, "USD", new BigDecimal("10.0000"), 0L);
        when(transferRepository.findByIdempotencyKey("transfer-1")).thenReturn(Optional.empty());
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(source));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(destination));
        when(ledgerEntryRepository.balanceOf(1L)).thenReturn(source.getBalance());
        when(ledgerEntryRepository.balanceOf(2L)).thenReturn(destination.getBalance());
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> {
            var transfer = invocation.<Transfer>getArgument(0);
            transfer.setId(42L);
            return transfer;
        });

        var transfer = transferService.transfer(
                "transfer-1", 1L, 2L, new BigDecimal("5.0000"));

        assertEquals(new BigDecimal("20.0000"), source.getBalance());
        assertEquals(new BigDecimal("15.0000"), destination.getBalance());
        assertEquals(42L, transfer.getId());
        verify(accountRepository).saveAll(anyList());
        verify(ledgerEntryRepository).saveAll(anyList());
    }

    @Test
    void insufficientFundsDoesNotSaveTransferOrLedgerEntries() {
        var source = new Account(1L, 7L, "USD", new BigDecimal("4.0000"), 0L);
        var destination = new Account(2L, 7L, "USD", BigDecimal.ZERO, 0L);
        when(transferRepository.findByIdempotencyKey("transfer-2")).thenReturn(Optional.empty());
        when(ledgerEntryRepository.balanceOf(1L)).thenReturn(source.getBalance());
        when(ledgerEntryRepository.balanceOf(2L)).thenReturn(destination.getBalance());
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(source));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(destination));

        assertThrows(InsufficientFundsException.class, () -> transferService.transfer(
                "transfer-2", 1L, 2L, new BigDecimal("5.0000")));

        verify(transferRepository, never()).save(any(Transfer.class));
        verify(ledgerEntryRepository, never()).saveAll(anyList());
    }

    @Test
    void repeatedIdempotencyKeyReturnsExistingTransferWithoutChangingAccounts() {
        var existingTransfer = new Transfer(
                42L, "transfer-3", 1L, 2L, new BigDecimal("5.0000"), null);
        when(transferRepository.findByIdempotencyKey("transfer-3"))
                .thenReturn(Optional.of(existingTransfer));

        var result = transferService.transfer(
                "transfer-3", 1L, 2L, new BigDecimal("5.0000"));

        assertSame(existingTransfer, result);
        verifyNoInteractions(accountRepository, ledgerEntryRepository);
    }
}
