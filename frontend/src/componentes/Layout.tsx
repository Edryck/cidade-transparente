import { Link, Navigate, Outlet, useLocation } from 'react-router-dom'
import type { Perfil } from '../api/auth'
import { useMunicipio } from '../contextos/MunicipioContext'
import { useSessao } from '../contextos/sessao'
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

/**
 * Rotas que exigem login. Sem sessão, vai para o login e volta depois. Isto só organiza a navegação:
 * quem autoriza é o backend, que recusa o token de quem não pode (401/403/404).
 */
export function RequerSessao({ perfis }: { perfis: Perfil[] }) {
  const sessao = useSessao()
  const local = useLocation()
  if (!sessao) return <Navigate to={`/login?voltar=${encodeURIComponent(local.pathname + local.search)}`} replace />
  if (!perfis.includes(sessao.perfil)) return <Navigate to="/area-indisponivel" replace />
  return <Outlet />
}
