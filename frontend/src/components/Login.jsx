import { useState } from 'react'
import { login, register, setToken } from '../api'

export default function Login({ onLoggedIn }) {
  const [mode, setMode] = useState('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

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
      <form className="card auth-card" onSubmit={handleSubmit}>
        <div className="brand"><span className="brand-mark" aria-hidden="true" />Wallet</div>
        <h1>{mode === 'login' ? 'Sign in' : 'Create an account'}</h1>
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
        <button type="submit" className="primary-button full-width" disabled={busy}>
          {busy ? 'Please wait…' : mode === 'login' ? 'Sign in' : 'Create account'}
        </button>
        <p className="auth-switch">
          {mode === 'login' ? 'New here? ' : 'Already have an account? '}
          <button type="button" className="text-button"
                  onClick={() => { setError(''); setMode(mode === 'login' ? 'register' : 'login') }}>
            {mode === 'login' ? 'Create an account' : 'Sign in'}
          </button>
        </p>
        <p className="auth-note">Demo mode: simulated money only, no real funds.</p>
      </form>
    </main>
  )
}
