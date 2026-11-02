import { formatMoney } from '../format'

function activityFor(transfer, accounts) {
  const source = accounts.find((account) => account.id === transfer.fromAccountId)
  const destination = accounts.find((account) => account.id === transfer.toAccountId)
  const outgoing = Boolean(source)
  const account = source ?? destination
  const otherEnding = outgoing ? transfer.toAccountEnding : transfer.fromAccountEnding
  const label = outgoing ? 'Transfer to' : 'Transfer from'
  const amount = `${outgoing ? '−' : '+'}${formatMoney(transfer.amount, account?.currency ?? 'USD')}`

  return {
    id: transfer.id,
    description: `${label} account ending ${otherEnding}`,
    date: new Date(transfer.createdAt).toLocaleString(),
    amount,
    positive: !outgoing,
  }
}

export default function Accounts({ accounts, transfers }) {
  const recentActivity = [...transfers].slice(0, 4)

  return (
    <>
      <div className="page-heading">
        <div>
          <h1>Your accounts</h1>
          <p>One demo wallet per currency, created automatically when you sign up.</p>
        </div>
      </div>

      {accounts.length === 0 ? (
        <section className="card">
          <p className="empty-state">No wallets found.</p>
        </section>
      ) : (
        <div className="account-grid">
          {accounts.map((account) => (
            <article className="card account-card" key={account.id}>
              <span className="account-currency">{account.currency} wallet</span>
              <strong className="account-balance">{formatMoney(account.balance, account.currency)}</strong>
              <span className="account-number">Account ending {account.accountNumber.slice(-4)}</span>
            </article>
          ))}
        </div>
      )}

      <section className="card activity-card">
        <h2 className="section-title">Recent activity</h2>
        {recentActivity.length === 0 ? (
          <p className="empty-state">Your recent transfers will appear here.</p>
        ) : recentActivity.map((transfer) => {
          const activity = activityFor(transfer, accounts)
          return (
            <div className="activity-row" key={activity.id}>
              <div>
                <div className="activity-description">{activity.description}</div>
                <div className="activity-date">{activity.date}</div>
              </div>
              <div className={`activity-amount ${activity.positive ? 'positive' : 'negative'}`}>
                {activity.amount}
              </div>
            </div>
          )
        })}
      </section>
    </>
  )
}
