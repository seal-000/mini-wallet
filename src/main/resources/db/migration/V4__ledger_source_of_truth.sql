-- Opening balances are ledger entries not tied to a transfer.
ALTER TABLE ledger_entries ALTER COLUMN transfer_id DROP NOT NULL;
ALTER TABLE ledger_entries ADD CONSTRAINT ledger_entries_direction_check
    CHECK (direction IN ('DEBIT', 'CREDIT'));
ALTER TABLE ledger_entries ADD CONSTRAINT ledger_entries_amount_check CHECK (amount > 0);

-- Backfill: give every account whose balance is not explained by the ledger an opening credit.
INSERT INTO ledger_entries (transfer_id, account_id, direction, amount)
SELECT NULL, a.id, 'CREDIT', a.balance - COALESCE(l.net, 0)
FROM accounts a
LEFT JOIN (
    SELECT account_id,
           SUM(CASE WHEN direction = 'CREDIT' THEN amount ELSE -amount END) AS net
    FROM ledger_entries GROUP BY account_id) l ON l.account_id = a.id
WHERE a.balance - COALESCE(l.net, 0) > 0;

-- Ledger and transfers are append-only.
CREATE FUNCTION forbid_ledger_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION '% on % is not allowed: the ledger is append-only', TG_OP, TG_TABLE_NAME;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER ledger_entries_append_only
    BEFORE UPDATE OR DELETE ON ledger_entries
    FOR EACH ROW EXECUTE FUNCTION forbid_ledger_mutation();

CREATE TRIGGER transfers_append_only
    BEFORE UPDATE OR DELETE ON transfers
    FOR EACH ROW EXECUTE FUNCTION forbid_ledger_mutation();
