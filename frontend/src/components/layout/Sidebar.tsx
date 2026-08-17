import type { ReactNode } from 'react'
import { Link, NavLink } from 'react-router-dom'
import Logo from '../Logo'
import { useAuth } from '../../auth/useAuth'
import { SIDEBAR_PANEL_ID } from './shellNav'

interface NavItem {
  to: string
  label: string
  icon: ReactNode
}

interface SidebarProps {
  collapsed?: boolean
}

const ICON_PROPS = {
  width: 17,
  height: 17,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.9,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  'aria-hidden': true,
  className: 'shell-nav-icon'
} as const

const NAV_ITEMS: NavItem[] = [
  {
    to: '/dashboard',
    label: 'Overview',
    icon: (
      <svg {...ICON_PROPS}>
        <rect x="3" y="3" width="7" height="7" rx="1.5" />
        <rect x="14" y="3" width="7" height="7" rx="1.5" />
        <rect x="14" y="14" width="7" height="7" rx="1.5" />
        <rect x="3" y="14" width="7" height="7" rx="1.5" />
      </svg>
    )
  },
  {
    to: '/transactions',
    label: 'Transactions',
    icon: (
      <svg {...ICON_PROPS}>
        <path d="M4 8h13" />
        <path d="M13 4l4 4-4 4" />
        <path d="M20 16H7" />
        <path d="M11 12l-4 4 4 4" />
      </svg>
    )
  },
  {
    to: '/categories',
    label: 'Categories',
    icon: (
      <svg {...ICON_PROPS}>
        <path d="M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
      </svg>
    )
  },
  {
    to: '/budgets',
    label: 'Budgets',
    icon: (
      <svg {...ICON_PROPS}>
        <rect x="3" y="6" width="18" height="13" rx="2" />
        <path d="M3 10h18" />
        <rect x="14.5" y="12.5" width="3.5" height="3" rx="0.5" />
      </svg>
    )
  },
  {
    to: '/reports',
    label: 'Reports',
    icon: (
      <svg {...ICON_PROPS}>
        <path d="M3 3v18h18" />
        <path d="M7 15l4-5 3 3 5-7" />
      </svg>
    )
  },
  {
    to: '/settings',
    label: 'Settings',
    icon: (
      <svg {...ICON_PROPS}>
        <circle cx="12" cy="12" r="3" />
        <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.6a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z" />
      </svg>
    )
  }
]

function Sidebar({ collapsed = false }: SidebarProps) {
  const { user, logout, clearSession } = useAuth()

  const handleLogout = async (): Promise<void> => {
    try {
      await logout()
    } catch {
      clearSession()
    }
  }

  return (
    <aside id={SIDEBAR_PANEL_ID} className="shell-sidebar" inert={collapsed}>
      <Link to="/dashboard" className="shell-logo">
        <Logo size="medium" />
      </Link>

      <nav aria-label="Main navigation">
        <ul className="shell-nav-list">
          {NAV_ITEMS.map((item) => (
            <li key={item.to}>
              <NavLink
                to={item.to}
                aria-current="page"
                className={({ isActive }) =>
                  isActive ? 'shell-nav-link active' : 'shell-nav-link'
                }
              >
                {item.icon}
                <span className="shell-nav-label">{item.label}</span>
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>

      <div className="shell-user">
        <div className="shell-user-identity">
          <span className="shell-avatar" aria-hidden="true"></span>
          <span className="shell-user-text">
            <span className="shell-user-name">{user?.username ?? ''}</span>
            <span className="shell-user-email">{user?.email ?? ''}</span>
          </span>
        </div>
        <button
          type="button"
          className="shell-logout"
          onClick={() => {
            void handleLogout()
          }}
        >
          Log out
        </button>
      </div>
    </aside>
  )
}

export default Sidebar
