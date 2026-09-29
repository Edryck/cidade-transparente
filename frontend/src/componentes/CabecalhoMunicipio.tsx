import { Link } from 'react-router-dom'
import { useMunicipio } from '../contextos/MunicipioContext'
import { encerrarSessao, useSessao } from '../contextos/sessao'

/** O município está sempre no topo: ninguém deve ter dúvida de em qual prefeitura está (PRODUCT.md, princípio 5). */
export function CabecalhoMunicipio() {
  const { municipio } = useMunicipio()
  const sessao = useSessao()
  return (
    <header className="cabecalho">
      <div className="cabecalho-dentro">
        <Link to={municipio ? '/' : '/municipios'} className="cabecalho-municipio">
          {municipio?.brasao && <img src={municipio.brasao} alt="" width={40} height={40} className="cabecalho-brasao" />}
          <span>
            <span className="cabecalho-nome">{municipio ? `${municipio.nome} (${municipio.uf})` : 'Escolha o município'}</span>
            <span className="cabecalho-servico">Ouvidoria e Acesso à Informação</span>
          </span>
        </Link>
        <div className="cabecalho-acoes">
          {municipio && <Link to="/municipios">Trocar município</Link>}
          {sessao ? (
            <button type="button" className="botao-texto" onClick={encerrarSessao}>
              Sair
            </button>
          ) : (
            <Link to="/login">Entrar</Link>
          )}
        </div>
      </div>
    </header>
  )
}
