import { Link } from 'react-router-dom'
import { useTitulo } from '../util/useTitulo'

export default function NaoEncontrada() {
  useTitulo('Página não encontrada')
  return (
    <>
      <h1>Página não encontrada</h1>
      <p>O endereço pode estar incompleto ou ter mudado.</p>
      <p><Link to="/">Início</Link> · <Link to="/acompanhar">Acompanhar protocolo</Link></p>
    </>
  )
}
