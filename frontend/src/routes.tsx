import { Navigate, type RouteObject } from 'react-router-dom'
import App from './App'
import ProtectedRoute from './components/ProtectedRoute'
import AppShell from './components/layout/AppShell'
import PublicLayout from './components/layout/PublicLayout'
import Home from './pages/Home'
import Login from './pages/Login'
import Register from './pages/Register'
import Dashboard from './pages/Dashboard'
import Transactions from './pages/Transactions'
import Categories from './pages/Categories'
import Budgets from './pages/Budgets'
import Reports from './pages/Reports'
import Settings from './pages/Settings'

export const routes: RouteObject[] = [
  {
    path: '/',
    element: <App />,
    children: [
      {
        element: <PublicLayout />,
        children: [
          { index: true, element: <Home /> },
          { path: 'login', element: <Login /> },
          { path: 'register', element: <Register /> }
        ]
      },
      { path: 'profile', element: <Navigate to="/settings" replace /> },
      {
        element: <ProtectedRoute />,
        children: [
          {
            element: <AppShell />,
            children: [
              { path: 'dashboard', element: <Dashboard /> },
              { path: 'transactions', element: <Transactions /> },
              { path: 'categories', element: <Categories /> },
              { path: 'budgets', element: <Budgets /> },
              { path: 'reports', element: <Reports /> },
              { path: 'settings', element: <Settings /> }
            ]
          }
        ]
      }
    ]
  }
]
