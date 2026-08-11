import './App.css'
import { Outlet } from 'react-router-dom'
import AuthProvider from './auth/AuthProvider'

function App() {
  return (
    <AuthProvider>
      <Outlet />
    </AuthProvider>
  )
}

export default App
