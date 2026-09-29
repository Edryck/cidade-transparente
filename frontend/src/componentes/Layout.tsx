import { Link, Navigate, Outlet, useLocation } from 'react-router-dom'
import type { Perfil } from '../api/auth'
import { useMunicipio } from '../contextos/MunicipioContext'
import { useSessao } from '../contextos/sessao'
import { CabecalhoMunicipio } from './CabecalhoMunicipio'

export function LayoutPublico() {
  return (
    <>
      {/* Faixa institucional com atalhos de acessibilidade, como nos serviços públicos digitais (DESIGN.md 36) */}
      <div className="faixa">
        <div className="faixa-dentro">
          <span className="faixa-marca">Cidade Transparente</span>
          <nav aria-label="Atalhos de acessibilidade">
            <ul>
              <li><a href="#conteudo">Ir para o conteúdo</a></li>
              <li><a href="#navegacao">Ir para o menu</a></li>
              <li><a href="#rodape">Ir para o rodapé</a></li>
            </ul>
          </nav>
        </div>
      </div>
      <CabecalhoMunicipio />
      <main id="conteudo" className="pagina" tabIndex={-1}>
        <Outlet />
      </main>
      <footer id="rodape" className="rodape">
        <div className="rodape-dentro">
          <p className="rodape-marca">
            <strong>Cidade Transparente</strong>
            <br />
            Ouvidoria e acesso à informação para prefeituras
          </p>
          <nav aria-label="Rodapé">
            <ul>
              <li><Link to="/acompanhar">Acompanhar protocolo</Link></li>
              <li><Link to="/privacidade">Aviso de privacidade</Link></li>
              <li><Link to="/relatorios">Relatórios de gestão</Link></li>
              <li><Link to="/municipios">Trocar município</Link></li>
              <li><a href="#conteudo">Voltar ao topo</a></li>
            </ul>
          </nav>
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
