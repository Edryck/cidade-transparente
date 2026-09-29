import { useRef, useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { authApi } from '../api/auth'
import { FalhaApi } from '../api/cliente'
import { municipiosApi } from '../api/municipios'
import { Alerta } from '../componentes/Alerta'
import { Botao } from '../componentes/Botao'
import { CabecalhoPagina } from '../componentes/CabecalhoPagina'
import { CampoFormulario } from '../componentes/CampoFormulario'
import { useMunicipio } from '../contextos/MunicipioContext'
import { iniciarSessao, ROTA_INICIAL } from '../contextos/sessao'
import { useTitulo } from '../util/useTitulo'
import { EMAIL } from '../util/validacao'

// Mensagens fixas para 401 e 403: não revelam se o e-mail existe
const MENSAGENS: Record<number, string> = {
  401: 'E-mail ou senha inválidos.',
  403: 'Usuário ou município desativado.',
}

export default function Login() {
  useTitulo('Acesse sua conta')
  const navegar = useNavigate()
  const [parametros] = useSearchParams()
  const { municipio, escolher } = useMunicipio()
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erros, setErros] = useState<Record<string, string>>({})
  const [falha, setFalha] = useState('')
  const [enviando, setEnviando] = useState(false)
  const refFalha = useRef<HTMLDivElement>(null)

  async function entrar(evento: FormEvent) {
    evento.preventDefault()
    const novos: Record<string, string> = {}
    if (!EMAIL.test(email.trim())) novos.email = 'Informe um e-mail no formato nome@exemplo.com'
    if (!senha) novos.senha = 'Informe a senha'
    setErros(novos)
    setFalha('')
    if (Object.keys(novos).length) return

    setEnviando(true)
    try {
      const resposta = await authApi.login(email.trim(), senha)
      const sessao = iniciarSessao(resposta)
      // O cabeçalho passa a mostrar o município da conta, não o escolhido antes do login
      if (sessao.municipioId && sessao.municipioId !== municipio?.id) {
        const lista = await municipiosApi.listarAtivos().catch(() => [])
        const daConta = lista.find((m) => m.id === sessao.municipioId)
        if (daConta) escolher(daConta)
      }
      // Volta para a página protegida que pediu o login, se for um caminho interno
      const voltar = parametros.get('voltar')
      const destino = voltar && voltar.startsWith('/') && !voltar.startsWith('//') ? voltar : ROTA_INICIAL[sessao.perfil]
      navegar(destino, { replace: true })
    } catch (erro) {
      const f = erro instanceof FalhaApi ? erro : new FalhaApi(500, 'Não foi possível entrar agora.')
      setErros(f.campos)
      setFalha(MENSAGENS[f.status] ?? f.message)
      setSenha('')
      requestAnimationFrame(() => refFalha.current?.focus())
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="coluna-estreita">
      <CabecalhoPagina titulo="Acesse sua conta" descricao="Entre para registrar e acompanhar suas manifestações."
        trilha={[{ texto: 'Início', para: '/' }, { texto: 'Acesse sua conta' }]} />

      {parametros.get('registrar') && (
        <Alerta tipo="info">
          <p>
            Reclamação, sugestão, elogio e pedido de informação exigem identificação (Lei 13.460, art. 10). Denúncias
            podem ser feitas <Link to="/denuncia-anonima">sem conta</Link>.
          </p>
        </Alerta>
      )}

      <form onSubmit={entrar} noValidate className="formulario superficie">
        {falha && (
          <div ref={refFalha} tabIndex={-1}><Alerta tipo="erro"><p>{falha}</p></Alerta></div>
        )}
        <CampoFormulario id="email" rotulo="E-mail" obrigatorio erro={erros.email}>
          {(p) => <input {...p} type="email" autoComplete="username" inputMode="email" value={email} onChange={(e) => setEmail(e.target.value)} />}
        </CampoFormulario>
        <CampoFormulario id="senha" rotulo="Senha" obrigatorio erro={erros.senha}>
          {(p) => <input {...p} type="password" autoComplete="current-password" value={senha} onChange={(e) => setSenha(e.target.value)} />}
        </CampoFormulario>
        <Botao type="submit" enviando={enviando} textoEnviando="Entrando…" className="botao-bloco">Entrar</Botao>
        <p style={{ margin: 0 }}>Ainda não tem conta? <Link to="/cadastro">Criar conta</Link></p>
      </form>

      <nav aria-labelledby="sem-conta" className="links-auxiliares">
        <h2 id="sem-conta">Sem conta você também pode</h2>
        <ul className="lista-links">
          <li><Link to="/denuncia-anonima">Registrar denúncia anônima</Link></li>
          <li><Link to="/acompanhar">Acompanhar protocolo</Link></li>
          <li><Link to="/privacidade">Ler o aviso de privacidade</Link></li>
        </ul>
      </nav>
    </div>
  )
}
