import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import './index.css'
import { routes } from './routes'

const router = createBrowserRouter(routes)

const rootElement = document.getElementById('root')

if (rootElement === null) {
  throw new Error('The #root element is missing from index.html')
}

createRoot(rootElement).render(
  <StrictMode>
    <RouterProvider router={router} />
  </StrictMode>,
)
