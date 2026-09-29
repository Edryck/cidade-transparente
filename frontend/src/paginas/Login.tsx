import { useRef, useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { authApi } from '../api/auth'
import { FalhaApi } from '../api/cliente'
import { municipiosApi } from '../api/municipios'
import { Alerta } from '../componentes/Alerta'
import { Botao } from '../componentes/Botao'
import { CampoFormulario } from '../componentes/CampoFormulario'
import { useMunicipio } from '../contextos/MunicipioContext'
import { iniciarSessao, ROTA_INICIAL } from '../contextos/sessao'
import { useTitulo } from '../util/useTitulo'
import { EMAIL } from '../util/validacao'

// Mensagens fixas para 401 e 403: não revelam se o e-mail existe
const MENSAGENS: Record<number, string> = {
  401: 'E-mail ou senha inválidos',
  403: 'Usuário ou município desativado',
}

export default function Login() {
  useTitulo('Entrar')
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
      navegar(ROTA_INICIAL[sessao.perfil], { replace: true })
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
    <>
      <h1>Entrar</h1>
      {parametros.get('registrar') ? (
        <Alerta tipo="info">
          <p>
            Para registrar reclamação, sugestão, elogio ou pedido de informação, entre com sua conta ou crie uma: a
            lei exige identificação nesses casos (Lei 13.460, art. 10). Denúncias podem ser feitas{' '}
            <Link to="/denuncia-anonima">sem identificação</Link>.
          </p>
        </Alerta>
      ) : (
        <p>Com a conta, você acompanha suas manifestações e pode recorrer de uma resposta.</p>
      )}

      <form onSubmit={entrar} noValidate className="formulario">
        {falha && (
          <div ref={refFalha} tabIndex={-1}>
            <Alerta tipo="erro">
              <p>{falha}</p>
            </Alerta>
          </div>
        )}
        <CampoFormulario id="email" rotulo="E-mail" obrigatorio erro={erros.email}>
          {(p) => <input {...p} type="email" autoComplete="username" inputMode="email" value={email} onChange={(e) => setEmail(e.target.value)} />}
        </CampoFormulario>
        <CampoFormulario id="senha" rotulo="Senha" obrigatorio erro={erros.senha}>
          {(p) => <input {...p} type="password" autoComplete="current-password" value={senha} onChange={(e) => setSenha(e.target.value)} />}
        </CampoFormulario>
        <Botao type="submit" enviando={enviando} textoEnviando="Entrando…">Entrar</Botao>
      </form>

      <nav aria-label="Outras opções" className="secao">
        <ul className="lista-links">
          <li><Link to="/cadastro">Criar conta</Link></li>
          <li><Link to="/denuncia-anonima">Denúncia anônima</Link></li>
          <li><Link to="/acompanhar">Acompanhar protocolo</Link></li>
          <li><Link to="/privacidade">Aviso de privacidade</Link></li>
        </ul>
      </nav>
    </>
  )
}
