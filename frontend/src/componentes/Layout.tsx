import { Link, Navigate, Outlet, useLocation } from 'react-router-dom'
import { useMunicipio } from '../contextos/MunicipioContext'
import { CabecalhoMunicipio } from './CabecalhoMunicipio'

export function LayoutPublico() {
  return (
    <>
      <a className="pular" href="#conteudo">Pular para o conteúdo</a>
      <CabecalhoMunicipio />
      <main id="conteudo" className="pagina" tabIndex={-1}>
        <Outlet />
      </main>
      <footer className="rodape">
        <div className="rodape-dentro">
          <nav aria-label="Rodapé">
            <Link to="/acompanhar">Acompanhar protocolo</Link>
            <Link to="/privacidade">Aviso de privacidade</Link>
            <Link to="/relatorios">Relatórios de gestão</Link>
          </nav>
          <p className="metadado">Cidade Transparente</p>
        </div>
      </footer>
    </>
  )
}

/** Telas que dependem do município (cadastro, denúncia, privacidade, relatórios): sem ele, vai escolher primeiro. */
export function RequerMunicipio() {
  const { municipio } = useMunicipio()
  const local = useLocation()
  if (!municipio) return <Navigate to={`/municipios?voltar=${encodeURIComponent(local.pathname + local.search)}`} replace />
  return <Outlet />
}
