import { Link, NavLink } from 'react-router-dom'
import { useState, useEffect } from 'react'
import Logo from './Logo'

function Header() {
  const [isLoggedIn, setIsLoggedIn] = useState(false)
  const [menuOpen, setMenuOpen] = useState(false)

  useEffect(() => {
    const checkAuthStatus = () => {
      const token = localStorage.getItem('authToken')
      setIsLoggedIn(!!token)
    }

    // Check auth status on mount
    checkAuthStatus()

    // Listen for storage changes (when user logs in/out in another tab)
    window.addEventListener('storage', checkAuthStatus)

    // Listen for custom auth events
    window.addEventListener('authStateChanged', checkAuthStatus)

    return () => {
      window.removeEventListener('storage', checkAuthStatus)
      window.removeEventListener('authStateChanged', checkAuthStatus)
    }
  }, [])

  const closeMenu = () => setMenuOpen(false)

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
          {isLoggedIn ? (
            <>
              <Link to="https://backend.fmanager.local" className="nav-link" onClick={closeMenu}>Docs</Link>
              <Link to="/profile" className="nav-button" onClick={closeMenu}>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                  <circle cx="12" cy="7" r="4"></circle>
                </svg>
                Profile
              </Link>
            </>
          ) : (
            <NavLink to="/login" className="nav-button" onClick={closeMenu}>Get Started</NavLink>
          )}
        </div>
      </div>
    </nav>
  )
}

export default Header
