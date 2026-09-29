import { CircleUserRound, Menu, X } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom'
import { comprovanteTemporario } from '../contextos/comprovante'
import { useMunicipio } from '../contextos/MunicipioContext'
import { encerrarSessao, useSessao } from '../contextos/sessao'

type Item = { para: string; texto: string; fim?: boolean }

// Navegação por tarefa (DESIGN.md 11): pública e do cidadão, sem opções que a pessoa não pode usar
const PUBLICA: Item[] = [
  { para: '/', texto: 'Início', fim: true },
  { para: '/login?registrar=1', texto: 'Registrar manifestação' },
  { para: '/acompanhar', texto: 'Acompanhar protocolo' },
  { para: '/privacidade', texto: 'Aviso de privacidade' },
  { para: '/relatorios', texto: 'Transparência' },
]
const CIDADAO: Item[] = [
  { para: '/minhas-manifestacoes', texto: 'Minhas manifestações', fim: true },
  { para: '/nova-manifestacao', texto: 'Nova manifestação' },
  { para: '/acompanhar', texto: 'Acompanhar protocolo' },
  { para: '/privacidade', texto: 'Aviso de privacidade' },
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

/** O município fica sempre identificado no topo (PRODUCT.md, princípio 5). */
export function CabecalhoMunicipio() {
  const { municipio } = useMunicipio()
  const sessao = useSessao()
  const sair = useSair()
  const local = useLocation()
  const [menuAberto, setMenuAberto] = useState(false)
  const cidadao = sessao?.perfil === 'CIDADAO'
  const itens = cidadao ? CIDADAO : municipio ? PUBLICA : []

  useEffect(() => setMenuAberto(false), [local.pathname])

  const nome = municipio ? `Prefeitura Municipal de ${municipio.nome}` : 'Cidade Transparente'
  const conta = cidadao
    ? { para: '/minha-conta', texto: 'Minha conta' }
    : sessao ? null : { para: '/login', texto: 'Entrar' }

  const links = (classe?: string) => (
    <ul>
      {itens.map((item) => (
        <li key={item.para}>
          <NavLink to={item.para} end={item.fim} className={classe}>{item.texto}</NavLink>
        </li>
      ))}
    </ul>
  )

  return (
    <header className="cabecalho">
      <div className="cabecalho-dentro">
        {itens.length > 0 && (
          <button type="button" className="botao-menu" aria-expanded={menuAberto} aria-controls="navegacao-movel"
            aria-label={menuAberto ? 'Fechar menu' : 'Abrir menu'} onClick={() => setMenuAberto((a) => !a)}>
            {menuAberto ? <X aria-hidden size={22} strokeWidth={1.75} /> : <Menu aria-hidden size={22} strokeWidth={1.75} />}
          </button>
        )}
        <Link to={cidadao ? '/minhas-manifestacoes' : municipio ? '/' : '/municipios'} className="cabecalho-municipio">
          {municipio?.brasao && <img src={municipio.brasao} alt="" width={40} height={40} className="cabecalho-brasao" />}
          <span>
            <span className="cabecalho-nome">{nome}{municipio && <span className="so-leitor"> ({municipio.uf})</span>}</span>
            <span className="cabecalho-servico">{municipio ? 'Ouvidoria e Acesso à Informação' : 'Escolha o município para começar'}</span>
          </span>
        </Link>

        <nav id="navegacao" aria-label="Principal" className="cabecalho-navegacao">
          <ul>
            {itens.map((item) => (
              <li key={item.para}><NavLink to={item.para} end={item.fim}>{item.texto}</NavLink></li>
            ))}
            {conta && <li><NavLink to={conta.para}>{conta.texto}</NavLink></li>}
            {sessao && <li><button type="button" className="botao-texto" onClick={sair}>Sair</button></li>}
          </ul>
        </nav>

        {conta && (
          <Link to={conta.para} className="botao-conta" aria-label={conta.texto}>
            <CircleUserRound aria-hidden size={22} strokeWidth={1.75} />
          </Link>
        )}
      </div>

      {itens.length > 0 && (
        <nav id="navegacao-movel" aria-label="Menu" className={`navegacao-movel${menuAberto ? ' aberta' : ''}`}>
          {links()}
          <ul>
            {conta && <li><NavLink to={conta.para}>{conta.texto}</NavLink></li>}
            {municipio && !sessao && <li><Link to="/municipios">Trocar município</Link></li>}
            {sessao && <li><button type="button" className="botao-texto" onClick={sair}>Sair</button></li>}
          </ul>
        </nav>
      )}
    </header>
  )
}
