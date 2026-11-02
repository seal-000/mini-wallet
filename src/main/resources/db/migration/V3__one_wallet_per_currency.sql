DELETE FROM accounts a
WHERE EXISTS (
    SELECT 1 FROM accounts keep
    WHERE keep.user_id = a.user_id AND keep.currency = a.currency AND keep.id < a.id)
  AND NOT EXISTS (SELECT 1 FROM transfers t WHERE t.from_account_id = a.id OR t.to_account_id = a.id)
  AND NOT EXISTS (SELECT 1 FROM ledger_entries l WHERE l.account_id = a.id);

ALTER TABLE accounts ADD CONSTRAINT accounts_user_currency_key UNIQUE (user_id, currency);

INSERT INTO accounts (user_id, currency, balance, account_number)
SELECT u.id, c.currency, 1000.0000,
       lpad((1000000000 + floor(random() * 9000000000))::bigint::text, 10, '0')
FROM users u
CROSS JOIN (VALUES ('USD'), ('EUR'), ('GBP'), ('JPY')) AS c(currency)
WHERE NOT EXISTS (
    SELECT 1 FROM accounts a WHERE a.user_id = u.id AND a.currency = c.currency);
