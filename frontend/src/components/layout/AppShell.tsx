import { useEffect, useState } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import Sidebar from './Sidebar'
import { useMediaQuery } from '../../hooks/useMediaQuery'
import { MOBILE_NAV_QUERY, NAV_TOGGLE_LABEL, SIDEBAR_PANEL_ID } from './shellNav'

function AppShell() {
  const isMobile = useMediaQuery(MOBILE_NAV_QUERY)
  const [navOpen, setNavOpen] = useState(false)
  const { pathname } = useLocation()

  useEffect(() => {
    setNavOpen(false)
  }, [pathname])

  useEffect(() => {
    if (!isMobile) {
      setNavOpen(false)
    }
  }, [isMobile])

  useEffect(() => {
    if (!navOpen) {
      return
    }

    const handleKeyDown = (event: KeyboardEvent): void => {
      if (event.key === 'Escape') {
        setNavOpen(false)
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('keydown', handleKeyDown)
    }
  }, [navOpen])

  const panelOpen = isMobile && navOpen

  return (
    <div className={panelOpen ? 'shell-layout nav-open' : 'shell-layout'}>
      {isMobile && (
        <button
          type="button"
          className="shell-nav-toggle"
          aria-label={NAV_TOGGLE_LABEL}
          aria-expanded={navOpen}
          aria-controls={SIDEBAR_PANEL_ID}
          onClick={() => {
            setNavOpen((open) => !open)
          }}
        >
          <svg
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth={1.9}
            strokeLinecap="round"
            aria-hidden="true"
          >
            <path d="M4 7h16" />
            <path d="M4 12h16" />
            <path d="M4 17h16" />
          </svg>
        </button>
      )}

      <Sidebar collapsed={isMobile && !navOpen} />

      <main className="shell-main">
        <Outlet />
      </main>
    </div>
  )
}

export default AppShell
