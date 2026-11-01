import { useContext } from 'react'
import { AuthContext } from './authContext.js'

export function useAuth() {
  const value = useContext(AuthContext)
  if (!value) throw new Error('useAuth se mora koristiti unutar AuthProvider-a')
  return value
}
