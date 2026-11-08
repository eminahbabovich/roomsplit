// Jedina tačka kroz koju frontend priča s backendom. Komponente ne zovu fetch direktno:
// ovdje dodajemo token, čitamo JSON i pretvaramo greške u ApiError.

const TOKEN_KEY = 'roomsplit.token'

// Adresa backenda. Lokalno je prazna: zovemo relativno /api, a Vite proxy to šalje na port 8080.
// U produkciji (Vercel) frontend i backend su na različitim domenama, pa se u build-u postavi
// VITE_API_URL (npr. https://roomsplit-api.onrender.com, bez kose crte na kraju).
const API_BASE = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '')

// sessionStorage umjesto localStorage: preživi osvježavanje stranice, ali je odvojen po tabu.
// Kod localStorage-a svi tabovi dijele isti token, pa prijava u jednom tabu prepiše ostale.
// Stari token iz localStorage-a brišemo, da ne ostane viška.
localStorage.removeItem(TOKEN_KEY)

export const tokenStore = {
  get: () => sessionStorage.getItem(TOKEN_KEY),
  set: (token) => sessionStorage.setItem(TOKEN_KEY, token),
  clear: () => sessionStorage.removeItem(TOKEN_KEY),
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
    response = await fetch(`${API_BASE}/api${path}`, {
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
