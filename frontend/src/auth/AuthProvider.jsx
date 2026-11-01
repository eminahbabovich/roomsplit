import { useCallback, useEffect, useMemo, useState } from 'react'
import { authApi } from '../api/endpoints.js'
import { setUnauthorizedHandler, tokenStore } from '../api/client.js'
import { AuthContext } from './authContext.js'

// Čuva ko je prijavljen. status: 'loading' (provjeravamo token), 'anon' ili 'authed'.
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [status, setStatus] = useState(() => (tokenStore.get() ? 'loading' : 'anon'))

  // Pri pokretanju: ako imamo token, pitamo server ko smo. Istekao token = 401 = odjava.
  useEffect(() => {
    if (!tokenStore.get()) return
    authApi
      .me()
      .then((me) => {
        setUser(me)
        setStatus('authed')
      })
      .catch(() => {
        tokenStore.clear()
        setStatus('anon')
      })
  }, [])

  // Bilo koji zahtjev koji vrati 401 odjavljuje korisnika
  useEffect(() => {
    setUnauthorizedHandler(() => {
      setUser(null)
      setStatus('anon')
    })
    return () => setUnauthorizedHandler(null)
  }, [])

  const login = useCallback(async (email, password) => {
    const response = await authApi.login({ email, password })
    tokenStore.set(response.accessToken)
    setUser(response.user)
    setStatus('authed')
  }, [])

  const register = useCallback(
    async ({ email, password, displayName }) => {
      await authApi.register({ email, password, displayName })
      await login(email, password)
    },
    [login],
  )

  const logout = useCallback(() => {
    tokenStore.clear()
    setUser(null)
    setStatus('anon')
  }, [])

  const value = useMemo(
    () => ({ user, status, login, register, logout }),
    [user, status, login, register, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
