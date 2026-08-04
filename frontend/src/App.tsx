import './App.css'
import { Outlet } from 'react-router-dom'
import AuthProvider from './auth/AuthProvider'
import Header from './components/Header'
import Footer from './components/Footer'

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
