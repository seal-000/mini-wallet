package com.miri.wallet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Runs only after the transfer transaction commits, so rolled-back transfers never notify. */
@Component
public class TransferCompletedListener {
    private static final Logger log = LoggerFactory.getLogger(TransferCompletedListener.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(TransferCompleted event) {
        log.info("TransferCompleted id={} {} {} -> {} {}", event.transferId(), event.amount(),
                event.fromCurrency(), event.toAmount(), event.toCurrency());
    }
}
