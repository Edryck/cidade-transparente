import { useRef, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { authApi } from '../api/auth'
import { FalhaApi } from '../api/cliente'
import { municipiosApi } from '../api/municipios'
import { Alerta } from '../componentes/Alerta'
import { Botao } from '../componentes/Botao'
import { CampoFormulario } from '../componentes/CampoFormulario'
import { Carregando, ErroCarregamento } from '../componentes/Estados'
import { useMunicipio } from '../contextos/MunicipioContext'
import { iniciarSessao, ROTA_INICIAL } from '../contextos/sessao'
import { useCarregar } from '../util/useCarregar'
import { useTitulo } from '../util/useTitulo'
import { cpfValido, EMAIL } from '../util/validacao'

type Campos = { municipioId: string; nome: string; email: string; senha: string; cpf: string; telefone: string }

/** Liga a mensagem de conflito da API ao campo certo, para o erro aparecer ao lado dele. */
function campoDoConflito(mensagem: string): keyof Campos | null {
  if (/e-mail/i.test(mensagem)) return 'email'
  if (/cpf/i.test(mensagem)) return 'cpf'
  if (/munic/i.test(mensagem)) return 'municipioId'
  return null
}

export default function CadastroCidadao() {
  useTitulo('Criar conta')
  const navegar = useNavigate()
  const { municipio, escolher } = useMunicipio()
  const municipios = useCarregar(municipiosApi.listarAtivos, [])
  const [campos, setCampos] = useState<Campos>({
    municipioId: String(municipio?.id ?? ''), nome: '', email: '', senha: '', cpf: '', telefone: '',
  })
  const [erros, setErros] = useState<Partial<Record<keyof Campos, string>>>({})
  const [falha, setFalha] = useState('')
  const [enviando, setEnviando] = useState(false)
  const refFalha = useRef<HTMLDivElement>(null)
  const alterar = (campo: keyof Campos) => (e: { target: { value: string } }) =>
    setCampos((c) => ({ ...c, [campo]: e.target.value }))

  function validar(): Partial<Record<keyof Campos, string>> {
    const e: Partial<Record<keyof Campos, string>> = {}
    if (!campos.municipioId) e.municipioId = 'Escolha o município em que você quer se manifestar'
    if (!campos.nome.trim()) e.nome = 'Informe seu nome'
    if (!EMAIL.test(campos.email.trim())) e.email = 'Informe um e-mail no formato nome@exemplo.com'
    if (campos.senha.length < 8 || campos.senha.length > 72) e.senha = 'A senha precisa ter de 8 a 72 caracteres'
    if (campos.cpf.trim() && !cpfValido(campos.cpf)) e.cpf = 'Confira o CPF: os números não formam um CPF válido'
    return e
  }

  async function criar(evento: FormEvent) {
    evento.preventDefault()
    const novos = validar()
    setErros(novos)
    setFalha('')
    if (Object.keys(novos).length) return

    setEnviando(true)
    try {
      const resposta = await authApi.registrarCidadao({
        municipioId: Number(campos.municipioId),
        nome: campos.nome.trim(),
        email: campos.email.trim(),
        senha: campos.senha,
        cpf: campos.cpf.trim() || undefined,
        telefone: campos.telefone.trim() || undefined,
      })
      if (municipios.estado.fase === 'pronto') {
        const escolhido = municipios.estado.dados.find((m) => m.id === Number(campos.municipioId))
        if (escolhido) escolher(escolhido)
      }
      iniciarSessao(resposta)
      navegar(ROTA_INICIAL.CIDADAO, { replace: true })
    } catch (erro) {
      const f = erro instanceof FalhaApi ? erro : new FalhaApi(500, 'Não foi possível criar a conta agora.')
      const campo = campoDoConflito(f.message)
      if ((f.status === 409 || f.status === 400) && campo && !Object.keys(f.campos).length) {
        const orientacao = f.status === 409 && campo === 'email' ? ' Se a conta é sua, entre com ela.' : ''
        setErros({ [campo]: f.message + orientacao })
      } else {
        setErros(f.campos as Partial<Record<keyof Campos, string>>)
        setFalha(Object.keys(f.campos).length ? 'Confira os campos marcados.' : f.message)
        requestAnimationFrame(() => refFalha.current?.focus())
      }
    } finally {
      setEnviando(false)
    }
  }

  return (
    <>
      <h1>Criar conta</h1>
      <p>Com a conta, você registra manifestações identificadas, acompanha todas num só lugar e pode recorrer de uma resposta.</p>

      <Alerta tipo="info" titulo="Por que pedimos seus dados">
        <p>
          A prefeitura usa seus dados para cumprir a lei que garante a ouvidoria e o acesso à informação (LGPD, art. 7º,
          II e III). Por isso não há termo para aceitar: o tratamento é uma obrigação legal, não depende de consentimento.
        </p>
        <p><Link to="/privacidade">Consulte o Aviso de Privacidade</Link></p>
      </Alerta>

      <form onSubmit={criar} noValidate className="formulario">
        {falha && (
          <div ref={refFalha} tabIndex={-1}><Alerta tipo="erro"><p>{falha}</p></Alerta></div>
        )}
        <CampoFormulario id="municipio" rotulo="Município" obrigatorio erro={erros.municipioId}>
          {(p) => {
            if (municipios.estado.fase === 'carregando') return <Carregando texto="Carregando municípios…" />
            if (municipios.estado.fase === 'erro') return <ErroCarregamento erro={municipios.estado.erro} tentarDeNovo={municipios.tentarDeNovo} />
            return (
              <select {...p} value={campos.municipioId} onChange={alterar('municipioId')}>
                <option value="">Escolha o município</option>
                {municipios.estado.dados.map((m) => <option key={m.id} value={m.id}>{m.nome} ({m.uf})</option>)}
              </select>
            )
          }}
        </CampoFormulario>
        <CampoFormulario id="nome" rotulo="Nome" obrigatorio erro={erros.nome}>
          {(p) => <input {...p} type="text" autoComplete="name" maxLength={150} value={campos.nome} onChange={alterar('nome')} />}
        </CampoFormulario>
        <CampoFormulario id="email" rotulo="E-mail" obrigatorio erro={erros.email} ajuda="Usado para entrar na conta.">
          {(p) => <input {...p} type="email" autoComplete="email" inputMode="email" maxLength={150} value={campos.email} onChange={alterar('email')} />}
        </CampoFormulario>
        <CampoFormulario id="senha" rotulo="Senha" obrigatorio erro={erros.senha} ajuda="De 8 a 72 caracteres.">
          {(p) => <input {...p} type="password" autoComplete="new-password" maxLength={72} value={campos.senha} onChange={alterar('senha')} />}
        </CampoFormulario>
        <CampoFormulario id="cpf" rotulo="CPF" obrigatorio={false} erro={erros.cpf}
          ajuda="Com ou sem pontuação. Ajuda a ouvidoria a distinguir pessoas com o mesmo nome.">
          {(p) => <input {...p} required={false} type="text" inputMode="numeric" autoComplete="off" maxLength={14} value={campos.cpf} onChange={alterar('cpf')} />}
        </CampoFormulario>
        <CampoFormulario id="telefone" rotulo="Telefone" obrigatorio={false} erro={erros.telefone}
          ajuda="Para a ouvidoria falar com você, se precisar.">
          {(p) => <input {...p} required={false} type="tel" autoComplete="tel" maxLength={20} value={campos.telefone} onChange={alterar('telefone')} />}
        </CampoFormulario>
        <Botao type="submit" enviando={enviando} textoEnviando="Criando conta…">Criar conta</Botao>
      </form>
      <p className="secao">Já tem conta? <Link to="/login">Entrar</Link></p>
    </>
  )
}
