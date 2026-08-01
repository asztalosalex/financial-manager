import './App.css'
import { Outlet } from 'react-router-dom'
import AuthProvider from './auth/AuthProvider'
import Header from './components/Header.jsx'
import Footer from './components/Footer.jsx'

function App() {
  return (
    <AuthProvider>
      <div className="app-shell">
        <Header />
        <main className="app-main">
          <Outlet />
        </main>
        <Footer />
      </div>
    </AuthProvider>
  )
}

export default App
