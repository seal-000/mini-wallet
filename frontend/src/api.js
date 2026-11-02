const BASE_URL = import.meta.env.VITE_API_URL ?? ''
const TOKEN_KEY = 'wallet.token'

export const getToken = () => sessionStorage.getItem(TOKEN_KEY)
export const setToken = (token) => sessionStorage.setItem(TOKEN_KEY, token)
export const clearToken = () => sessionStorage.removeItem(TOKEN_KEY)

export class ApiError extends Error {
  constructor(status, message) {
    super(message)
    this.status = status
  }
}

async function request(path, { method = 'GET', body, headers = {} } = {}) {
  const token = getToken()
  const response = await fetch(`${BASE_URL}${path}`, {
    method,
    headers: {
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...headers,
    },
    body: body ? JSON.stringify(body) : undefined,
  })

  const text = await response.text()
  const data = text ? JSON.parse(text) : null
  if (!response.ok) {
    throw new ApiError(response.status, data?.message ?? defaultMessage(response.status))
  }
  return data
}

function defaultMessage(status) {
  if (status === 401) return 'Invalid credentials or session expired'
  if (status === 409) return 'That email is already registered'
  return `Request failed (${status})`
}

export const login = (email, password) =>
  request('/api/auth/login', { method: 'POST', body: { email, password } })

export const register = (email, password) =>
  request('/api/auth/register', { method: 'POST', body: { email, password } })

export const getAccounts = () => request('/api/accounts')

export const getTransfers = () => request('/api/transfers')

function statementQuery({ accountId, type, from, to, page, size }) {
  const params = new URLSearchParams()
  if (accountId && accountId !== 'all') params.set('accountId', accountId)
  if (type && type !== 'all') params.set('type', type)
  if (from) params.set('from', from)
  if (to) params.set('to', to)
  if (page !== undefined) params.set('page', page)
  if (size !== undefined) params.set('size', size)
  return params.toString()
}

export const getStatement = (filters) => request(`/api/statements?${statementQuery(filters)}`)

export async function downloadStatementCsv(filters) {
  const response = await fetch(`${BASE_URL}/api/statements/export.csv?${statementQuery(filters)}`, {
    headers: { Authorization: `Bearer ${getToken()}` },
  })
  if (!response.ok) throw new ApiError(response.status, `Export failed (${response.status})`)
  const url = URL.createObjectURL(await response.blob())
  const link = document.createElement('a')
  link.href = url
  link.download = 'wallet-statement.csv'
  link.click()
  URL.revokeObjectURL(url)
}

export const createTransfer = (idempotencyKey, fromAccountId, toEmail, amount) =>
  request('/api/transfers', {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey },
    body: { fromAccountId, toEmail, amount },
  })
