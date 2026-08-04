import { Link, NavLink } from 'react-router-dom'
import { useState, type ReactNode } from 'react'
import Logo from './Logo'
import { useAuth } from '../auth/useAuth'

function Header() {
  const { isAuthenticated, isLoading, logout, clearSession } = useAuth()
  const [menuOpen, setMenuOpen] = useState(false)
  const [loggingOut, setLoggingOut] = useState(false)

  const closeMenu = () => setMenuOpen(false)

  const handleLogout = async (): Promise<void> => {
    closeMenu()
    setLoggingOut(true)
    try {
      await logout()
    } catch {
      clearSession()
    } finally {
      setLoggingOut(false)
    }
  }

  const renderAuthActions = (): ReactNode => {
    if (isLoading) {
      return <span className="nav-link" aria-live="polite">Loading...</span>
    }

    if (!isAuthenticated) {
      return <NavLink to="/login" className="nav-button" onClick={closeMenu}>Get Started</NavLink>
    }

    return (
      <>
        <a href="https://backend.fmanager.local" className="nav-link" onClick={closeMenu}>Docs</a>
        <Link to="/profile" className="nav-button" onClick={closeMenu}>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
            <circle cx="12" cy="7" r="4"></circle>
          </svg>
          Profile
        </Link>
        <button type="button" className="nav-link nav-logout" onClick={handleLogout} disabled={loggingOut}>
          {loggingOut ? 'Logging out...' : 'Log out'}
        </button>
      </>
    )
  }

  return (
    <nav className="navbar">
      <div className="nav-container">
        <div className="nav-logo">
          <Link to="/" onClick={closeMenu}>
            <Logo size="medium" />
          </Link>
        </div>
        <button
          className="nav-toggle"
          aria-label="Toggle navigation menu"
          aria-expanded={menuOpen}
          onClick={() => setMenuOpen((open) => !open)}
        >
          <span className="nav-toggle-bar"></span>
          <span className="nav-toggle-bar"></span>
          <span className="nav-toggle-bar"></span>
        </button>
        <div className={`nav-menu ${menuOpen ? 'open' : ''}`}>
          <a href="#features" className="nav-link" onClick={closeMenu}>Features</a>
          <a href="#about" className="nav-link" onClick={closeMenu}>About</a>
          <a href="#contact" className="nav-link" onClick={closeMenu}>Contact</a>
          {renderAuthActions()}
        </div>
      </div>
    </nav>
  )
}

export default Header
