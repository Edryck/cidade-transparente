import './estilos/tokens.css'
import './estilos/base.css'
import './estilos/componentes.css'
import './estilos/paginas.css'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
