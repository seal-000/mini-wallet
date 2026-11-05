-- 0.5% fee on cross-currency transfers. Fee revenue is booked on internal per-currency wallets
-- (opening balance 0), so every currency's ledger still balances:
--   DEBIT sender / CREDIT fee-revenue (source currency)
ALTER TABLE transfers ADD COLUMN fee NUMERIC(19,4) NOT NULL DEFAULT 0;
ALTER TABLE transfers ADD CONSTRAINT transfers_fee_check CHECK (fee >= 0);

INSERT INTO users (email, password_hash) VALUES ('fee-revenue@wallet.internal', '!login-disabled');

INSERT INTO accounts (user_id, currency, balance, account_number)
SELECT u.id, c.currency, 0, c.number
FROM users u
CROSS JOIN (VALUES ('USD', '9100000001'), ('EUR', '9100000002'),
                   ('GBP', '9100000003'), ('JPY', '9100000004')) AS c(currency, number)
WHERE u.email = 'fee-revenue@wallet.internal';
