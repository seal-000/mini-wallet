-- Cross-currency transfers: store the amount received and the rate used.
ALTER TABLE transfers ADD COLUMN to_amount NUMERIC(19,4);
ALTER TABLE transfers ADD COLUMN exchange_rate NUMERIC(19,8) NOT NULL DEFAULT 1;

-- Backfill existing (same-currency) rows; the append-only trigger is paused for this one statement.
ALTER TABLE transfers DISABLE TRIGGER transfers_append_only;
UPDATE transfers SET to_amount = amount;
ALTER TABLE transfers ENABLE TRIGGER transfers_append_only;

ALTER TABLE transfers ALTER COLUMN to_amount SET NOT NULL;
ALTER TABLE transfers ADD CONSTRAINT transfers_to_amount_check CHECK (to_amount > 0);
ALTER TABLE transfers ADD CONSTRAINT transfers_rate_check CHECK (exchange_rate > 0);

-- Internal FX clearing wallets, one per currency. A cross-currency transfer posts four ledger legs:
--   DEBIT sender / CREDIT clearing (source currency), DEBIT clearing / CREDIT recipient (target currency)
-- so every currency's ledger stays balanced. The system user cannot log in.
INSERT INTO users (email, password_hash) VALUES ('fx-clearing@wallet.internal', '!login-disabled');

INSERT INTO accounts (user_id, currency, balance, account_number)
SELECT u.id, c.currency, 1000000000.0000, c.number
FROM users u
CROSS JOIN (VALUES ('USD', '9000000001'), ('EUR', '9000000002'),
                   ('GBP', '9000000003'), ('JPY', '9000000004')) AS c(currency, number)
WHERE u.email = 'fx-clearing@wallet.internal';

-- Opening liquidity as ledger entries, so the ledger still explains every balance.
INSERT INTO ledger_entries (transfer_id, account_id, direction, amount)
SELECT NULL, a.id, 'CREDIT', a.balance
FROM accounts a JOIN users u ON u.id = a.user_id
WHERE u.email = 'fx-clearing@wallet.internal';
