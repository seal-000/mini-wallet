# Mini Digital Wallet

A payments-ledger demo: users hold one wallet per currency (USD, EUR, GBP, JPY), and send money to other users by email. Every movement is recorded in an append-only double-entry ledger.

> **Simulation only. No real money is involved.** New accounts are seeded with fake funds.

## Features

- Registration and login with JWT (Spring Security)
- One auto-created wallet per currency, each with a 10-digit account number ("Account ending 3043")
- Transfers by recipient email, in a single `@Transactional` method (all-or-nothing)
- Double-entry ledger: each transfer writes a DEBIT and a CREDIT entry
- **Ledger is the source of truth**: balances are the sum of ledger entries; `accounts.balance` is a cached copy
- **Append-only**: database triggers reject UPDATE/DELETE on `ledger_entries` and `transfers`
- **Reconciliation**: `GET /api/ledger/reconcile` compares each wallet's cached balance to its ledger sum
- Idempotency keys (`Idempotency-Key` header): a retried request moves money once
- Concurrency safety: pessimistic row locks (taken in id order to avoid deadlocks) plus `@Version`
- Cross-currency transfers (e.g. USD→EUR): rates come from the free [Frankfurter](https://frankfurter.dev) API and are cached with Spring Cache (`@Cacheable`, Caffeine, 1 hour). The Transfer page shows the rate and converted amount before you send
- Each transfer stores the rate used and the amount received. Conversion is `BigDecimal` rounded HALF_EVEN to the target currency's minor units (2 for EUR, 0 for JPY)
- A cross-currency transfer posts four ledger legs through internal per-currency FX clearing wallets (`DEBIT` sender / `CREDIT` clearing in the source currency, `DEBIT` clearing / `CREDIT` recipient in the target currency), so each currency's ledger stays balanced
- Server-side paginated statements (built from ledger entries) with wallet/type/date filters and CSV export
- `BigDecimal` / `NUMERIC(19,4)` for all amounts; Flyway migrations

## Stack

Java 21, Spring Boot 4, Spring Data JPA, Spring Security (JWT), PostgreSQL 17, Flyway, JUnit 5 + Testcontainers, React 19 + Vite, Docker.

## Run locally

Prerequisites: JDK 21, Docker, Node.js.

```bash
docker compose up -d          # Postgres on localhost:5432
./mvnw spring-boot:run        # backend on http://localhost:8080
cd frontend
npm install
npm run dev                   # frontend on http://localhost:5173
```

Open the app, register, and you get four wallets with 1000.00 each. To try a transfer, register a second user (e.g. `friend@wallet.com`) and send to that email.

### Configuration

Spring Boot reads environment variables, not `.env` (only Docker Compose reads `.env`).

| Variable | Default |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/wallet` (must start with `jdbc:`) |
| `DB_USERNAME` / `DB_PASSWORD` | `postgres` / `postgres` |
| `JWT_SECRET` | dev-only value (set your own, 32+ bytes) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` |

Example for a hosted Postgres such as Neon:

```bash
export DB_URL="jdbc:postgresql://<host>/<db>?sslmode=require"
export DB_USERNAME="..."
export DB_PASSWORD="..."
```

## API

| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/register` | Create user and wallets, returns a JWT |
| POST | `/api/auth/login` | Returns a JWT |
| GET | `/api/accounts` | Your wallets |
| POST | `/api/transfers` | `{fromAccountId, toEmail, amount, toCurrency?}` with `Idempotency-Key` header. `toCurrency` defaults to the source currency; if different, the transfer is converted |
| GET | `/api/fx/quote` | `?from=USD&to=EUR&amount=10` returns rate and converted amount |
| GET | `/api/transfers` | Transfer list |
| GET | `/api/statements` | Paginated ledger statement. Params: `accountId`, `type` (debit/credit), `from`, `to` (YYYY-MM-DD), `page`, `size` |
| GET | `/api/statements/export.csv` | Same filters, downloads a CSV |
| GET | `/api/ledger/reconcile` | Cached balance vs ledger sum per wallet |

Sample requests are in `src/main/java/com/miri/wallet/request.http`.

## Tests

```bash
./mvnw test
```

Requires Docker (Testcontainers). Includes a test that fires 50 concurrent transfers and verifies totals are conserved, no wallet is overdrawn, ledger sums match balances, and ledger rows cannot be modified.

## Deploy (free tier)

Simulation only, so free hosting is fine.

1. **Database (Neon):** create a project and copy host, user and password. Your JDBC URL is `jdbc:postgresql://<host>/<db>?sslmode=require`.
2. **Backend (Render):** New → Blueprint → select this repo (uses `render.yaml` and the `Dockerfile`). Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`; `JWT_SECRET` is generated. Set `CORS_ALLOWED_ORIGINS` to your Vercel URL after step 3. Flyway creates the schema on first start. The free tier sleeps when idle, so the first request can take ~1 minute.
3. **Frontend (Vercel):** import the repo, set the root directory to `frontend`, and add the env var `VITE_API_URL=https://<your-render-service>.onrender.com`.

CI (`.github/workflows/ci.yml`) runs backend tests, frontend lint and build, and a Docker build on every push and pull request.

## Roadmap

Deposits, a `TransferCompleted` event.
