import { useState } from 'react'
import { login, register, setToken } from '../api'

export default function Login({ onLoggedIn }) {
  const [mode, setMode] = useState('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  function useDemoAccount() {
    setEmail('demo@wallet.com')
    setPassword('demo1234')
    setError('')
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setBusy(true)
    try {
      const action = mode === 'login' ? login : register
      const { token } = await action(email, password)
      setToken(token)
      onLoggedIn()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="auth-page">
      <aside className="auth-visual" aria-label="Welcome to Wallet">
        <img className="auth-visual-image" src="/login-img.jpg" alt="" />
        <div className="auth-visual-shade" />
        <div className="auth-visual-content">
          <div className="brand brand-light">
            <img className="brand-logo" src="/favicon-32x32.png" alt="" />
            <span>Wallet</span>
          </div>
          <div className="auth-visual-copy">
            <span className="auth-eyebrow">A better way to move money</span>
            <h2>Simple transfers.<br />Thoughtful engineering.</h2>
            <p>Explore a full-stack wallet demo with secure sign-in, a double-entry ledger, and multi-currency transfers.</p>
          </div>
         
        </div>
      </aside>

      <section className="auth-panel">
        <form className="auth-card" onSubmit={handleSubmit}>
          <div className="brand auth-mobile-brand">
            <img className="brand-logo" src="/favicon-32x32.png" alt="" />
            <span>Wallet</span>
          </div>
          <div className="auth-heading">
            <span className="auth-eyebrow">Welcome to Wallet</span>
            <h1>{mode === 'login' ? 'Sign in to your account' : 'Create your account'}</h1>
            <p>{mode === 'login' ? 'Enter your details to explore the demo.' : 'Your demo wallets will be ready as soon as you sign up.'}</p>
          </div>
          <label className="field">
            Email
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)}
                   required autoComplete="email" placeholder="you@example.com" />
          </label>
          <label className="field">
            Password
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)}
                   required minLength={8} maxLength={72}
                   autoComplete={mode === 'login' ? 'current-password' : 'new-password'} />
          </label>
          {error && <p className="error" role="alert">{error}</p>}
          <button type="submit" className="primary-button full-width auth-submit" disabled={busy}>
            {busy ? 'Please wait…' : mode === 'login' ? 'Sign in' : 'Create account'}
            {!busy && <span aria-hidden="true">→</span>}
          </button>
          {mode === 'login' && (
            <section className="demo-credentials" aria-label="Demo account credentials">
              <div>
                <span className="demo-credentials-label">Just want to take a look?</span>
                <p><span>Email</span> <code>demo@wallet.com</code></p>
                <p><span>Password</span> <code>demo1234</code></p>
              </div>
              <button type="button" className="demo-fill-button" onClick={useDemoAccount}>
                Use demo account
              </button>
            </section>
          )}
          <p className="auth-switch">
            {mode === 'login' ? 'New here? ' : 'Already have an account? '}
            <button type="button" className="text-button"
                    onClick={() => { setError(''); setMode(mode === 'login' ? 'register' : 'login') }}>
              {mode === 'login' ? 'Create an account' : 'Sign in'}
            </button>
          </p>
          <p className="auth-note">Demo mode: simulated money only, no real funds.</p>
        </form>
      </section>
    </main>
  )
}
