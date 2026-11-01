import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { PageLoader } from '../components/Feedback.jsx'
import { useAuth } from './useAuth.js'

// Zaštićene rute: neprijavljen korisnik ide na prijavu, a poslije prijave se vraća ovdje.
export function RequireAuth() {
  const { status } = useAuth()
  const location = useLocation()

  if (status === 'loading') return <PageLoader label="Provjeravam prijavu…" />
  if (status === 'anon') return <Navigate to="/prijava" replace state={{ from: location.pathname + location.search }} />
  return <Outlet />
}
