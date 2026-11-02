import { useCallback, useEffect, useState } from 'react'
import { clearToken, getAccounts, getToken, getTransfers } from './api'
import Accounts from './components/Accounts'
import History from './components/History'
import Login from './components/Login'
import TransferForm from './components/TransferForm'
import './App.css'

export default function App() {
  const [loggedIn, setLoggedIn] = useState(() => Boolean(getToken()))
  const [activePage, setActivePage] = useState('accounts')
  const [accounts, setAccounts] = useState([])
  const [transfers, setTransfers] = useState([])
  const [error, setError] = useState('')

  const logout = useCallback(() => {
    clearToken()
    setAccounts([])
    setTransfers([])
    setLoggedIn(false)
  }, [])

  const refresh = useCallback(async () => {
    try {
      const [accountData, transferData] = await Promise.all([getAccounts(), getTransfers()])
      setAccounts(accountData)
      setTransfers(transferData)
      setError('')
    } catch (err) {
      if (err.status === 401) {
        logout()
      } else {
        setError(err.message)
      }
    }
  }, [logout])

  useEffect(() => {
    if (loggedIn) {
      refresh()
    }
  }, [loggedIn, refresh])

  if (!loggedIn) {
    return <Login onLoggedIn={() => setLoggedIn(true)} />
  }

  const pages = [
    { id: 'accounts', label: 'Accounts' },
    { id: 'transfer', label: 'Transfer' },
    { id: 'history', label: 'History' },
  ]

  return (
    <main className="dashboard">
      <header className="topbar">
        <div className="topbar-inner">
          <div className="brand"><span className="brand-mark" aria-hidden="true" />Wallet</div>
          <nav className="main-nav" aria-label="Main navigation">
            {pages.map((page) => (
              <button
                type="button"
                key={page.id}
                className={`nav-button${activePage === page.id ? ' active' : ''}`}
                aria-current={activePage === page.id ? 'page' : undefined}
                onClick={() => setActivePage(page.id)}
              >
                {page.label}
              </button>
            ))}
          </nav>
          <span className="topbar-user">Your wallet</span>
          <button type="button" className="logout-button" onClick={logout}>Log out</button>
        </div>
      </header>
      <div className="demo-banner">Demo mode: simulated money only, no real funds.</div>
      <div className="page-content">
        {error && <p className="error" role="alert">{error}</p>}
        {activePage === 'accounts' && (
          <Accounts accounts={accounts} transfers={transfers} />
        )}
        {activePage === 'transfer' && (
          <TransferForm accounts={accounts} onTransferred={refresh} />
        )}
        {activePage === 'history' && (
          <History accounts={accounts} />
        )}
      </div>
    </main>
  )
}
