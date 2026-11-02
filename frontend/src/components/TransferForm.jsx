import { useState } from 'react'
import { createTransfer } from '../api'
import { formatMoney } from '../format'

const DEMO_RECIPIENT = 'friend@wallet.com'

export default function TransferForm({ accounts, onTransferred }) {
  const [fromId, setFromId] = useState('')
  const [toEmail, setToEmail] = useState('')
  const [amount, setAmount] = useState('')
  const [idempotencyKey, setIdempotencyKey] = useState(() => crypto.randomUUID())
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [busy, setBusy] = useState(false)

  const selectedFrom = fromId || accounts[0]?.id || ''
  const selectedAccount = accounts.find((account) => String(account.id) === String(selectedFrom))
  const numericAmount = Number(amount)
  const validAmount = Number.isFinite(numericAmount) && numericAmount > 0
  const formattedAmount = formatMoney(validAmount ? numericAmount : 0, selectedAccount?.currency ?? 'USD')
  const balanceAfter = selectedAccount
    ? Number(selectedAccount.balance) - (validAmount ? numericAmount : 0)
    : 0

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    setBusy(true)
    try {
      const transfer = await createTransfer(
        idempotencyKey, Number(selectedFrom), toEmail.trim(), amount)
      setSuccess(`Transfer #${transfer.id} completed`)
      setAmount('')
      setToEmail('')
      setIdempotencyKey(crypto.randomUUID())
      await onTransferred()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <div className="page-heading">
        <div>
          <h1>Send money</h1>
          <p>Pick a wallet to send from; the recipient receives it in their wallet of the same currency.</p>
        </div>
      </div>
      {accounts.length === 0 ? (
        <section className="card"><p className="empty-state">No wallets found.</p></section>
      ) : (
        <div className="form-layout">
          <form className="card form-card" onSubmit={handleSubmit}>
            <label className="field">
              From
              <select value={selectedFrom} onChange={(event) => setFromId(event.target.value)} required>
                {accounts.map((account) => (
                  <option key={account.id} value={account.id}>
                    {account.currency} wallet · ending {account.accountNumber.slice(-4)} · {formatMoney(account.balance, account.currency)}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              Recipient email
              <input
                type="email"
                value={toEmail}
                onChange={(event) => setToEmail(event.target.value)}
                placeholder="friend@example.com"
                autoComplete="off"
                required
              />
            </label>
            <p className="demo-hint">
              Use this email example for transfer:{' '}
              <button type="button" className="text-button" onClick={() => setToEmail(DEMO_RECIPIENT)}>
                {DEMO_RECIPIENT}
              </button>
            </p>
            <label className="field">
              Amount ({selectedAccount?.currency ?? 'USD'})
              <input
                type="number"
                min="0.0001"
                step="0.0001"
                value={amount}
                onChange={(event) => setAmount(event.target.value)}
                placeholder="0.00"
                required
              />
            </label>
            <p className="safe-retry">Safe to retry: this transfer is applied only once, even if you submit twice.</p>
            {error && <p className="error" role="alert">{error}</p>}
            {success && <p className="success" role="status">{success}</p>}
            <button type="submit" className="primary-button full-width" disabled={busy}>
              {busy ? 'Sending…' : `Send ${validAmount ? formattedAmount : 'money'}`}
            </button>
          </form>

          <aside className="card summary-card">
            <h2 className="section-title">Summary</h2>
            <div className="summary-line"><span>Amount</span><strong>{formattedAmount}</strong></div>
            <div className="summary-line"><span>Fee</span><strong>{formatMoney(0, selectedAccount?.currency ?? 'USD')}</strong></div>
            <div className="summary-line total"><span>Recipient gets</span><strong>{formattedAmount}</strong></div>
            <p className="summary-foot">
              {selectedAccount
                ? `Estimated balance after transfer: ${formatMoney(balanceAfter, selectedAccount.currency)}`
                : 'Select a wallet to see the estimated balance.'}
            </p>
          </aside>
        </div>
      )}
    </>
  )
}
