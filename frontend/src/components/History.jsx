import { useEffect, useState } from 'react'
import { downloadStatementCsv, getStatement } from '../api'
import { formatMoney } from '../format'

const PAGE_SIZE = 10
const DAY_MS = 24 * 60 * 60 * 1000

function periodStart(period) {
  return period === 'all' ? undefined : new Date(Date.now() - Number(period) * DAY_MS).toISOString().slice(0, 10)
}

export default function History({ accounts }) {
  const [accountFilter, setAccountFilter] = useState('all')
  const [typeFilter, setTypeFilter] = useState('all')
  const [periodFilter, setPeriodFilter] = useState('30')
  const [page, setPage] = useState(0)
  const [statement, setStatement] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    getStatement({
      accountId: accountFilter,
      type: typeFilter,
      from: periodStart(periodFilter),
      page,
      size: PAGE_SIZE,
    })
      .then((data) => {
        if (!cancelled) {
          setStatement(data)
          setError('')
        }
      })
      .catch((err) => {
        if (!cancelled) setError(err.message)
      })
    return () => {
      cancelled = true
    }
  }, [accountFilter, typeFilter, periodFilter, page])

  const rows = statement?.items ?? []
  const pageCount = Math.max(1, statement?.totalPages ?? 1)

  function changeFilter(setter) {
    return (event) => {
      setPage(0)
      setter(event.target.value)
    }
  }

  function exportCsv() {
    downloadStatementCsv({
      accountId: accountFilter,
      type: typeFilter,
      from: periodStart(periodFilter),
    }).catch((err) => setError(err.message))
  }

  return (
    <>
      <div className="page-heading">
        <div>
          <h1>Transaction history</h1>
          <p>Review and export your wallet ledger.</p>
        </div>
        <button
          type="button"
          className="secondary-button"
          onClick={exportCsv}
          disabled={!statement || statement.totalItems === 0}
        >
          Export CSV
        </button>
      </div>

      <section className="card">
        <div className="history-toolbar">
          <div className="filters" aria-label="Transaction filters">
            <label className="sr-only" htmlFor="history-account-filter">Wallet</label>
            <select
              id="history-account-filter"
              className="filter-control"
              value={accountFilter}
              onChange={changeFilter(setAccountFilter)}
            >
              <option value="all">All wallets</option>
              {accounts.map((account) => (
                <option key={account.id} value={account.id}>{account.currency} wallet</option>
              ))}
            </select>
            <label className="sr-only" htmlFor="history-type-filter">Transaction type</label>
            <select
              id="history-type-filter"
              className="filter-control"
              value={typeFilter}
              onChange={changeFilter(setTypeFilter)}
            >
              <option value="all">All types</option>
              <option value="debit">Debits</option>
              <option value="credit">Credits</option>
            </select>
            <label className="sr-only" htmlFor="history-period-filter">Date range</label>
            <select
              id="history-period-filter"
              className="filter-control"
              value={periodFilter}
              onChange={changeFilter(setPeriodFilter)}
            >
              <option value="30">Last 30 days</option>
              <option value="90">Last 90 days</option>
              <option value="all">All time</option>
            </select>
          </div>
        </div>

        {error && <p className="empty-state">{error}</p>}

        <div className="table-wrap">
          <table className="transaction-table">
            <thead>
              <tr><th>Date</th><th>Description</th><th>Type</th><th className="amount-cell">Amount</th></tr>
            </thead>
            <tbody>
              {rows.map((line) => (
                <tr key={line.id}>
                  <td>{new Date(line.date).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}</td>
                  <td><strong>{line.description}</strong></td>
                  <td>{line.type}</td>
                  <td className={`amount-cell ${line.amount < 0 ? 'negative' : 'positive'}`}>
                    {line.amount < 0 ? '\u2212' : '+'}{formatMoney(Math.abs(line.amount), line.currency)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {statement && rows.length === 0 && <p className="empty-state">No transactions match these filters.</p>}

        <div className="history-pagination">
          <span>Page {page + 1} of {pageCount}{statement ? ` · ${statement.totalItems} entries` : ''}</span>
          <div className="pagination-actions">
            <button
              type="button"
              className="secondary-button"
              onClick={() => setPage((current) => Math.max(0, current - 1))}
              disabled={page === 0}
            >
              Previous
            </button>
            <button
              type="button"
              className="secondary-button"
              onClick={() => setPage((current) => Math.min(pageCount - 1, current + 1))}
              disabled={page >= pageCount - 1}
            >
              Next
            </button>
          </div>
        </div>
      </section>
    </>
  )
}
