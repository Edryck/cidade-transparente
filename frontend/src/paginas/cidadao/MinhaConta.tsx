import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { FalhaApi } from '../../api/cliente'
import { contaApi, type Encerramento, type MinhaConta as Conta } from '../../api/conta'
import { Alerta } from '../../componentes/Alerta'
import { Botao } from '../../componentes/Botao'
import { useSair } from '../../componentes/CabecalhoMunicipio'
import { CampoFormulario } from '../../componentes/CampoFormulario'
import { Carregando, ErroCarregamento } from '../../componentes/Estados'
import { encerrarSessao } from '../../contextos/sessao'
import { dataCurta, diaDoInstante } from '../../util/datas'
import { useCarregar } from '../../util/useCarregar'
import { useEnvioUnico } from '../../util/useEnvioUnico'
import { useTitulo } from '../../util/useTitulo'
import { cpfValido, EMAIL } from '../../util/validacao'

/** Tela 13: acesso e correção dos próprios dados (LGPD art. 18, I a III) e encerramento da conta. */
export default function MinhaConta() {
  useTitulo('Minha conta')
  const { estado, tentarDeNovo } = useCarregar(contaApi.obter, [])
  const [encerramento, setEncerramento] = useState<Encerramento | null>(null)

  if (encerramento) return <ContaEncerrada encerramento={encerramento} />
  if (estado.fase === 'carregando') return <><h1>Minha conta</h1><Carregando /></>
  if (estado.fase === 'erro') return <><h1>Minha conta</h1><ErroCarregamento erro={estado.erro} tentarDeNovo={tentarDeNovo} /></>
  return (
    <>
      <h1>Minha conta</h1>
      <p>Seus dados ficam visíveis só para você e para a ouvidoria. <Link to="/privacidade">Aviso de privacidade</Link></p>
      <FormConta conta={estado.dados} />
      <FormSenha />
      <EncerrarConta aoEncerrar={setEncerramento} />
    </>
  )
}

function FormConta({ conta }: { conta: Conta }) {
  const [campos, setCampos] = useState({ nome: conta.nome, email: conta.email, cpf: conta.cpf ?? '', telefone: conta.telefone ?? '' })
  const [erros, setErros] = useState<Record<string, string>>({})
  const [mensagem, setMensagem] = useState<{ tipo: 'sucesso' | 'erro'; texto: string } | null>(null)
  const [salvando, setSalvando] = useState(false)
  const refMensagem = useRef<HTMLDivElement>(null)
  const alterar = (c: keyof typeof campos) => (e: { target: { value: string } }) => setCampos((v) => ({ ...v, [c]: e.target.value }))

  async function salvar(evento: FormEvent) {
    evento.preventDefault()
    const e: Record<string, string> = {}
    if (!campos.nome.trim()) e.nome = 'Informe seu nome'
    if (!EMAIL.test(campos.email.trim())) e.email = 'Informe um e-mail no formato nome@exemplo.com'
    if (campos.cpf.trim() && !cpfValido(campos.cpf)) e.cpf = 'Confira o CPF: os números não formam um CPF válido'
    setErros(e)
    setMensagem(null)
    if (Object.keys(e).length) return
    setSalvando(true)
    try {
      await contaApi.atualizar({
        nome: campos.nome.trim(), email: campos.email.trim(),
        cpf: campos.cpf.trim() || undefined, telefone: campos.telefone.trim() || undefined,
      })
      setMensagem({ tipo: 'sucesso', texto: 'Dados atualizados.' })
    } catch (erro) {
      const f = erro instanceof FalhaApi ? erro : new FalhaApi(500, 'Não foi possível salvar agora.')
      const campo = /e-mail/i.test(f.message) ? 'email' : /cpf/i.test(f.message) ? 'cpf' : null
      if (campo && !Object.keys(f.campos).length) setErros({ [campo]: f.message })
      else {
        setErros(f.campos)
        setMensagem({ tipo: 'erro', texto: Object.keys(f.campos).length ? 'Não foi possível salvar. Verifique os campos indicados.' : f.message })
      }
    } finally {
      setSalvando(false)
      requestAnimationFrame(() => refMensagem.current?.focus())
    }
  }

  return (
    <form onSubmit={salvar} noValidate className="formulario">
      <div ref={refMensagem} tabIndex={-1} aria-live="polite">
        {mensagem && <Alerta tipo={mensagem.tipo}><p>{mensagem.texto}</p></Alerta>}
      </div>
      <dl className="dados dados-formulario">
        <dt>Município</dt><dd>{conta.municipioNome ?? 'não informado'}</dd>
        <dt>Conta criada em</dt><dd className="mono">{dataCurta(diaDoInstante(conta.criadoEm))}</dd>
      </dl>
      <p className="metadado">O município não pode ser alterado: ele define para qual prefeitura suas manifestações vão.</p>
      <CampoFormulario id="nome" rotulo="Nome" obrigatorio erro={erros.nome}>
        {(p) => <input {...p} type="text" autoComplete="name" maxLength={150} value={campos.nome} onChange={alterar('nome')} />}
      </CampoFormulario>
      <CampoFormulario id="email" rotulo="E-mail" obrigatorio erro={erros.email} ajuda="Também é o e-mail para entrar na conta.">
        {(p) => <input {...p} type="email" autoComplete="email" maxLength={150} value={campos.email} onChange={alterar('email')} />}
      </CampoFormulario>
      <CampoFormulario id="cpf" rotulo="CPF" obrigatorio={false} erro={erros.cpf} ajuda="Com ou sem pontuação.">
        {(p) => <input {...p} type="text" inputMode="numeric" autoComplete="off" maxLength={14} value={campos.cpf} onChange={alterar('cpf')} />}
      </CampoFormulario>
      <CampoFormulario id="telefone" rotulo="Telefone" obrigatorio={false} erro={erros.telefone}>
        {(p) => <input {...p} type="tel" autoComplete="tel" maxLength={20} value={campos.telefone} onChange={alterar('telefone')} />}
      </CampoFormulario>
      <Botao type="submit" enviando={salvando} textoEnviando="Salvando…">Salvar alterações</Botao>
    </form>
  )
}

/** Senha atual obrigatória; os campos são limpos depois de salvar e nunca vão para armazenamento. */
function FormSenha() {
  const vazio = { atual: '', nova: '', confirmacao: '' }
  const [campos, setCampos] = useState(vazio)
  const [erros, setErros] = useState<Record<string, string>>({})
  const [mensagem, setMensagem] = useState<{ tipo: 'sucesso' | 'erro'; texto: string } | null>(null)
  const { enviando, executar } = useEnvioUnico()
  const alterar = (c: keyof typeof campos) => (e: { target: { value: string } }) => setCampos((v) => ({ ...v, [c]: e.target.value }))

  function salvar(evento: FormEvent) {
    evento.preventDefault()
    const e: Record<string, string> = {}
    if (!campos.atual) e.atual = 'Informe a senha atual'
    if (campos.nova.length < 8 || campos.nova.length > 72) e.nova = 'A nova senha precisa ter de 8 a 72 caracteres'
    else if (campos.nova === campos.atual) e.nova = 'A nova senha precisa ser diferente da atual'
    if (campos.confirmacao !== campos.nova) e.confirmacao = 'A confirmação precisa ser igual à nova senha'
    setErros(e)
    setMensagem(null)
    if (Object.keys(e).length) return
    executar(async () => {
      try {
        await contaApi.alterarSenha(campos.atual, campos.nova)
        setCampos(vazio)
        setMensagem({ tipo: 'sucesso', texto: 'Senha alterada. Use a nova senha no próximo acesso.' })
      } catch (erro) {
        const f = erro instanceof FalhaApi ? erro : new FalhaApi(500, 'Não foi possível alterar a senha agora.')
        if (/atual/i.test(f.message)) setErros({ atual: f.message })
        else if (f.campos.novaSenha || /nova senha/i.test(f.message)) setErros({ nova: f.campos.novaSenha ?? f.message })
        else setMensagem({ tipo: 'erro', texto: f.message })
      }
    })
  }

  return (
    <section className="secao">
      <h2>Alterar senha</h2>
      <form onSubmit={salvar} noValidate className="formulario">
        <div aria-live="polite">{mensagem && <Alerta tipo={mensagem.tipo}><p>{mensagem.texto}</p></Alerta>}</div>
        <CampoFormulario id="senha-atual" rotulo="Senha atual" obrigatorio erro={erros.atual}>
          {(p) => <input {...p} type="password" autoComplete="current-password" value={campos.atual} onChange={alterar('atual')} />}
        </CampoFormulario>
        <CampoFormulario id="senha-nova" rotulo="Nova senha" obrigatorio erro={erros.nova} ajuda="De 8 a 72 caracteres.">
          {(p) => <input {...p} type="password" autoComplete="new-password" maxLength={72} value={campos.nova} onChange={alterar('nova')} />}
        </CampoFormulario>
        <CampoFormulario id="senha-confirmacao" rotulo="Confirmar nova senha" obrigatorio erro={erros.confirmacao}>
          {(p) => <input {...p} type="password" autoComplete="new-password" maxLength={72} value={campos.confirmacao} onChange={alterar('confirmacao')} />}
        </CampoFormulario>
        <Botao type="submit" enviando={enviando} textoEnviando="Salvando…">Alterar senha</Botao>
      </form>
    </section>
  )
}

function EncerrarConta({ aoEncerrar }: { aoEncerrar: (e: Encerramento) => void }) {
  const refDialogo = useRef<HTMLDialogElement>(null)
  const [enviando, setEnviando] = useState(false)
  const [falha, setFalha] = useState('')

  async function confirmar() {
    setEnviando(true)
    setFalha('')
    try {
      const resposta = await contaApi.encerrar()
      refDialogo.current?.close()
      encerrarSessao(true)
      aoEncerrar(resposta)
    } catch (erro) {
      setFalha(erro instanceof FalhaApi ? erro.message : 'Não foi possível encerrar a conta agora.')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section className="secao">
      <h2>Encerrar conta</h2>
      <p>O acesso com este e-mail é bloqueado. Suas manifestações não são apagadas: são documentos públicos e ficam guardadas com acesso restrito.</p>
      <Botao variante="destrutivo" type="button" onClick={() => refDialogo.current?.showModal()}>Encerrar minha conta</Botao>
      <dialog ref={refDialogo} className="dialogo" aria-labelledby="titulo-encerrar">
        <h2 id="titulo-encerrar">Encerrar sua conta?</h2>
        <p>Você não conseguirá mais entrar com este e-mail. Para acompanhar manifestações já registradas, use o protocolo e a chave de acesso.</p>
        {falha && <Alerta tipo="erro"><p>{falha}</p></Alerta>}
        <div className="acoes-passo">
          <Botao variante="secundario" type="button" onClick={() => refDialogo.current?.close()} disabled={enviando}>Manter conta</Botao>
          <Botao variante="destrutivo" type="button" onClick={confirmar} enviando={enviando} textoEnviando="Encerrando…">Encerrar conta</Botao>
        </div>
      </dialog>
    </section>
  )
}

/** A lei manda explicar por que os dados não são apagados (LGPD art. 18, § 4º, II): a fundamentação vem da API. */
function ContaEncerrada({ encerramento }: { encerramento: Encerramento }) {
  const sair = useSair()
  const refTitulo = useRef<HTMLHeadingElement>(null)
  useEffect(() => refTitulo.current?.focus(), [])
  return (
    <>
      <h1 ref={refTitulo} tabIndex={-1}>Conta encerrada</h1>
      <p>{encerramento.mensagem}</p>
      <h2>Por que os dados continuam guardados</h2>
      <ul>{encerramento.fundamentacao.map((f) => <li key={f}>{f}</li>)}</ul>
      <Botao type="button" onClick={sair}>Voltar ao início</Botao>
    </>
  )
}
