// Jedina tačka kroz koju frontend priča s backendom. Komponente ne zovu fetch direktno:
// ovdje dodajemo token, čitamo JSON i pretvaramo greške u ApiError.

const TOKEN_KEY = 'roomsplit.token'

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

// Greška s API-ja. fieldErrors je mapa { polje: poruka } koju vraća Bean Validation.
export class ApiError extends Error {
  constructor(status, message, fieldErrors = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

let unauthorizedHandler = null

// AuthProvider se ovdje prijavi da bude obaviješten kad token istekne (401)
export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler
}

export async function api(path, { method = 'GET', body, auth = true } = {}) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  const token = tokenStore.get()
  if (auth && token) headers.Authorization = `Bearer ${token}`

  let response
  try {
    response = await fetch(`/api${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    })
  } catch {
    throw new ApiError(0, 'Server nije dostupan. Provjeri da je backend pokrenut.')
  }

  if (response.status === 204) return null

  const data = await response.json().catch(() => null)

  if (!response.ok) {
    if (response.status === 401 && auth) {
      tokenStore.clear()
      unauthorizedHandler?.()
    }
    throw new ApiError(
      response.status,
      data?.message ?? 'Nešto je pošlo po zlu. Pokušaj ponovo.',
      data?.fieldErrors ?? {},
    )
  }

  return data
}
