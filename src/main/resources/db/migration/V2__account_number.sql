ALTER TABLE accounts ADD COLUMN account_number VARCHAR(10);

UPDATE accounts SET account_number = lpad((1000000000 + id)::text, 10, '0');

ALTER TABLE accounts ALTER COLUMN account_number SET NOT NULL;
ALTER TABLE accounts ADD CONSTRAINT accounts_account_number_key UNIQUE (account_number);
