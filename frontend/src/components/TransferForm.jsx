import { useEffect, useState } from 'react'
import { createTransfer, getQuote } from '../api'
import { formatMoney } from '../format'

const DEMO_RECIPIENT = 'friend@wallet.com'

export default function TransferForm({ accounts, onTransferred }) {
  const [fromId, setFromId] = useState('')
  const [toEmail, setToEmail] = useState('')
  const [toCurrencyChoice, setToCurrencyChoice] = useState('')
  const [amount, setAmount] = useState('')
  const [quoteState, setQuoteState] = useState(null)
  const [idempotencyKey, setIdempotencyKey] = useState(() => crypto.randomUUID())
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [busy, setBusy] = useState(false)

  const selectedFrom = fromId || accounts[0]?.id || ''
  const selectedAccount = accounts.find((account) => String(account.id) === String(selectedFrom))
  const sourceCurrency = selectedAccount?.currency ?? 'USD'
  const targetCurrency = toCurrencyChoice || sourceCurrency
  const crossCurrency = targetCurrency !== sourceCurrency
  const numericAmount = Number(amount)
  const validAmount = Number.isFinite(numericAmount) && numericAmount > 0
  const formattedAmount = formatMoney(validAmount ? numericAmount : 0, sourceCurrency)
  const quoteKey = `${sourceCurrency}|${targetCurrency}|${amount}`
  const quote = quoteState?.key === quoteKey ? quoteState : null
  const fee = crossCurrency && quote?.data ? Number(quote.data.fee) : 0
  const balanceAfter = selectedAccount
    ? Number(selectedAccount.balance) - (validAmount ? numericAmount : 0) - fee
    : 0

  useEffect(() => {
    if (!crossCurrency || !validAmount) return undefined
    let cancelled = false
    const timer = setTimeout(() => {
      getQuote(sourceCurrency, targetCurrency, amount)
        .then((data) => !cancelled && setQuoteState({ key: quoteKey, data }))
        .catch((err) => !cancelled && setQuoteState({ key: quoteKey, error: err.message }))
    }, 300)
    return () => {
      cancelled = true
      clearTimeout(timer)
    }
  }, [crossCurrency, validAmount, sourceCurrency, targetCurrency, amount, quoteKey])

  const recipientGets = !crossCurrency
    ? formattedAmount
    : quote?.data ? formatMoney(quote.data.convertedAmount, targetCurrency) : '\u2026'

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    setBusy(true)
    try {
      const transfer = await createTransfer(
        idempotencyKey, Number(selectedFrom), toEmail.trim(), amount, targetCurrency)
      setSuccess(transfer.fromCurrency !== transfer.toCurrency
        ? `Transfer #${transfer.id} completed: recipient received ${formatMoney(transfer.toAmount, transfer.toCurrency)} at ${transfer.exchangeRate} (fee ${formatMoney(transfer.fee, transfer.fromCurrency)})`
        : `Transfer #${transfer.id} completed`)
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
          <p>Pick a wallet to send from, and the currency your recipient receives. Different currencies are converted at the current rate.</p>
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
              Recipient receives
              <select value={targetCurrency} onChange={(event) => setToCurrencyChoice(event.target.value)}>
                {accounts.map((account) => (
                  <option key={account.id} value={account.currency}>
                    {account.currency}{account.currency === sourceCurrency ? ' (same currency)' : ''}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              Amount ({sourceCurrency})
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
            {crossCurrency && (
              <div className="summary-line">
                <span>Exchange rate</span>
                <strong>
                  {quote?.data ? `1 ${sourceCurrency} = ${quote.data.rate} ${targetCurrency}` : '\u2026'}
                </strong>
              </div>
            )}
            <div className="summary-line">
              <span>Fee{crossCurrency ? ' (0.5%)' : ''}</span>
              <strong>{formatMoney(fee, sourceCurrency)}</strong>
            </div>
            <div className="summary-line total"><span>Recipient gets</span><strong>{recipientGets}</strong></div>
            {quote?.error && <p className="error" role="alert">{quote.error}</p>}
            {crossCurrency && (
              <p className="summary-foot">Rate is cached for up to an hour; the amount you see is what the recipient receives.</p>
            )}
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
