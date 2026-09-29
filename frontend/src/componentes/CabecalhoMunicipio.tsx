import { Menu, X } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom'
import { comprovanteTemporario } from '../contextos/comprovante'
import { useMunicipio } from '../contextos/MunicipioContext'
import { encerrarSessao, useSessao } from '../contextos/sessao'

// No desktop, "Minha conta" já está no cabeçalho e o aviso no rodapé: aparecem só no menu do celular
const ITENS_CIDADAO = [
  { para: '/minhas-manifestacoes', texto: 'Minhas manifestações' },
  { para: '/nova-manifestacao', texto: 'Nova manifestação' },
  { para: '/acompanhar', texto: 'Acompanhar protocolo' },
  { para: '/minha-conta', texto: 'Minha conta', soCelular: true },
  { para: '/privacidade', texto: 'Aviso de privacidade', soCelular: true },
]

/** Sair: apaga token, rascunhos e o comprovante em memória, e volta para a área pública. */
export function useSair() {
  const navegar = useNavigate()
  return () => {
    encerrarSessao(true)
    comprovanteTemporario.descartar()
    navegar('/', { replace: true })
  }
}

/** O município está sempre no topo: ninguém deve ter dúvida de em qual prefeitura está (PRODUCT.md, princípio 5). */
export function CabecalhoMunicipio() {
  const { municipio } = useMunicipio()
  const sessao = useSessao()
  const sair = useSair()
  const local = useLocation()
  const [menuAberto, setMenuAberto] = useState(false)
  const cidadao = sessao?.perfil === 'CIDADAO'

  // Fecha o menu do celular ao trocar de página
  useEffect(() => setMenuAberto(false), [local.pathname])

  return (
    <header className="cabecalho">
      <div className="cabecalho-dentro">
        <Link to={cidadao ? '/minhas-manifestacoes' : municipio ? '/' : '/municipios'} className="cabecalho-municipio">
          {municipio?.brasao && <img src={municipio.brasao} alt="" width={40} height={40} className="cabecalho-brasao" />}
          <span>
            <span className="cabecalho-nome">{municipio ? `${municipio.nome} (${municipio.uf})` : 'Escolha o município'}</span>
            <span className="cabecalho-servico">Ouvidoria e Acesso à Informação</span>
          </span>
        </Link>
        <div className="cabecalho-acoes">
          {cidadao ? (
            <>
              <Link to="/minha-conta" className="so-largo">Minha conta</Link>
              <button type="button" className="botao-texto so-largo" onClick={sair}>Sair</button>
              <button type="button" className="botao-menu" aria-expanded={menuAberto} aria-controls="navegacao-cidadao"
                onClick={() => setMenuAberto((a) => !a)}>
                {menuAberto ? <X aria-hidden size={22} strokeWidth={1.5} /> : <Menu aria-hidden size={22} strokeWidth={1.5} />}
                Menu
              </button>
            </>
          ) : (
            <>
              {municipio && <Link to="/municipios">Trocar município</Link>}
              {sessao ? <button type="button" className="botao-texto" onClick={sair}>Sair</button> : <Link to="/login">Entrar</Link>}
            </>
          )}
        </div>
      </div>
      {cidadao && (
        <nav id="navegacao-cidadao" aria-label="Área do cidadão" className={`navegacao-cidadao${menuAberto ? ' aberta' : ''}`}>
          <ul>
            {ITENS_CIDADAO.map((item) => (
              <li key={item.para} className={item.soCelular ? 'so-estreito' : undefined}>
                <NavLink to={item.para} end={item.para === '/minhas-manifestacoes'}>{item.texto}</NavLink>
              </li>
            ))}
            <li className="so-estreito"><button type="button" className="botao-texto" onClick={sair}>Sair</button></li>
          </ul>
        </nav>
      )}
    </header>
  )
}
