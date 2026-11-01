import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth.js'
import { Avatar } from './Avatar.jsx'
import { Button } from './Button.jsx'
import { Logo } from './Logo.jsx'

function SiteHeader() {
  const { user, status, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/')
  }

  return (
    <header className="site-header">
      <div className="container site-header-inner">
        <Logo />

        <nav className="site-nav" aria-label="Glavna navigacija">
          {status === 'authed' ? (
            <>
              <NavLink to="/grupe" className="nav-link">
                Moje grupe
              </NavLink>
              <span className="nav-user">
                <Avatar name={user.displayName} id={user.id} size={32} />
                <span className="nav-user-name">{user.displayName}</span>
              </span>
              <Button variant="ghost" size="sm" onClick={handleLogout}>
                Odjava
              </Button>
            </>
          ) : (
            <>
              <Link to="/prijava" className="btn btn-ghost btn-sm">
                Prijava
              </Link>
              <Link to="/registracija" className="btn btn-primary btn-sm">
                Napravi račun
              </Link>
            </>
          )}
        </nav>
      </div>
    </header>
  )
}

function SiteFooter() {
  return (
    <footer className="site-footer">
      <div className="container site-footer-inner">
        <Logo />
        <p>
          Studentski projekat: Spring Boot, PostgreSQL i React. Novac se računa u feninzima, bez zaokruživanja koje
          krade.
        </p>
      </div>
    </footer>
  )
}

export function Layout() {
  return (
    <>
      <SiteHeader />
      <main className="site-main">
        <Outlet />
      </main>
      <SiteFooter />
    </>
  )
}
