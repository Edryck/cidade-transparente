import { useRef, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { FalhaApi } from '../api/cliente'
import { protocolosApi, type ConsultaPublica } from '../api/protocolos'
import { Alerta } from '../componentes/Alerta'
import { BadgeSituacao } from '../componentes/BadgeSituacao'
import { Botao } from '../componentes/Botao'
import { CampoFormulario } from '../componentes/CampoFormulario'
import { LinhaTramitacao, RespostaOficial } from '../componentes/LinhaTramitacao'
import { ReguaPrazo } from '../componentes/ReguaPrazo'
import { dataCurta, diaDoInstante } from '../util/datas'
import { useTitulo } from '../util/useTitulo'
import { PROTOCOLO } from '../util/validacao'

const RECURSO = { PENDENTE: 'Recurso apresentado, aguardando julgamento', DEFERIDO: 'Recurso deferido', INDEFERIDO: 'Recurso indeferido' }

export default function AcompanharProtocolo() {
  useTitulo('Acompanhar protocolo')
  const [parametros] = useSearchParams()
  const [protocolo, setProtocolo] = useState(parametros.get('protocolo') ?? '')
  const [chave, setChave] = useState('')
  const [erros, setErros] = useState<Record<string, string>>({})
  const [falha, setFalha] = useState('')
  const [consulta, setConsulta] = useState<ConsultaPublica | null>(null)
  const [enviando, setEnviando] = useState(false)
  const refResultado = useRef<HTMLElement>(null)
  const refFalha = useRef<HTMLDivElement>(null)

  async function consultar(evento: FormEvent) {
    evento.preventDefault()
    const novos: Record<string, string> = {}
    if (!PROTOCOLO.test(protocolo.trim())) novos.protocolo = 'Informe o protocolo no formato 2026-9999901-000004'
    if (!chave.trim()) novos.chave = 'Informe a chave de acesso recebida ao registrar'
    setErros(novos)
    setFalha('')
    setConsulta(null)
    if (Object.keys(novos).length) return

    setEnviando(true)
    try {
      setConsulta(await protocolosApi.consultar(protocolo, chave))
      requestAnimationFrame(() => refResultado.current?.focus())
    } catch (erro) {
      const f = erro instanceof FalhaApi ? erro : new FalhaApi(500, 'Não foi possível consultar agora.')
      if (f.status === 400) setErros({ chave: 'Informe a chave de acesso recebida ao registrar' })
      // 404: mesma mensagem para protocolo inexistente e chave errada; não revela qual dos dois falhou
      else setFalha(f.status === 404 ? 'Protocolo ou chave de acesso inválidos' : f.message)
      requestAnimationFrame(() => refFalha.current?.focus())
    } finally {
      setEnviando(false)
    }
  }

  const primeiraResposta = consulta?.respostas[0]?.respondidaEm ?? null
  return (
    <>
      <h1>Acompanhar protocolo</h1>
      <p>Use o protocolo e a chave de acesso recebidos ao registrar. Não é preciso ter conta.</p>

      <form onSubmit={consultar} noValidate className="formulario">
        {falha && <div ref={refFalha} tabIndex={-1}><Alerta tipo="erro"><p>{falha}</p><p>Confira os dois dados e tente de novo.</p></Alerta></div>}
        <CampoFormulario id="protocolo" rotulo="Protocolo" obrigatorio erro={erros.protocolo} ajuda="Exemplo: 2026-9999901-000004">
          {(p) => <input {...p} className="mono" type="text" inputMode="numeric" autoComplete="off" spellCheck={false}
            value={protocolo} onChange={(e) => setProtocolo(e.target.value)} />}
        </CampoFormulario>
        <CampoFormulario id="chave" rotulo="Chave de acesso" obrigatorio erro={erros.chave}
          ajuda="No formato XXXX-XXXX-XXXX. A chave não pode ser recuperada; quem tem conta também acompanha pelo login.">
          {(p) => <input {...p} className="mono" type="text" autoComplete="off" autoCapitalize="characters" spellCheck={false}
            value={chave} onChange={(e) => setChave(e.target.value)} />}
        </CampoFormulario>
        <Botao type="submit" enviando={enviando} textoEnviando="Consultando…">Acompanhar protocolo</Botao>
      </form>

      {consulta && (
        <section ref={refResultado} tabIndex={-1} aria-labelledby="resultado-titulo" className="secao resultado">
          <p className="protocolo mono">{consulta.protocolo}</p>
          <h2 id="resultado-titulo">{consulta.assunto}</h2>
          <p><BadgeSituacao status={consulta.status} /></p>
          <dl className="dados">
            <dt>Município</dt><dd>{consulta.municipio}</dd>
            <dt>Tipo</dt><dd>{consulta.tipo}</dd>
            <dt>Abertura</dt><dd className="mono">{dataCurta(diaDoInstante(consulta.dataAbertura))}</dd>
            <dt>Data limite</dt><dd className="mono">{dataCurta(consulta.dataLimite)}</dd>
            <dt>Prorrogação</dt><dd>{consulta.prorrogada ? 'Prazo prorrogado uma vez. A justificativa está no histórico.' : 'Sem prorrogação'}</dd>
            {consulta.recurso && <><dt>Recurso</dt><dd>{RECURSO[consulta.recurso]}</dd></>}
          </dl>

          <ReguaPrazo dataAbertura={consulta.dataAbertura} dataLimite={consulta.dataLimite}
            prorrogada={consulta.prorrogada} status={consulta.status} respondidaEm={primeiraResposta} />

          {consulta.respostas.length > 0 && (
            <section className="secao">
              <h2>{consulta.respostas.length === 1 ? 'Resposta' : 'Respostas'}</h2>
              {consulta.respostas.map((r) => (
                <RespostaOficial key={r.id} resposta={r} recursoJaApresentado={consulta.recurso !== null} />
              ))}
            </section>
          )}

          <section className="secao">
            <h2>Histórico</h2>
            <LinhaTramitacao tramites={consulta.tramites} />
          </section>

          <p className="metadado">Nomes de pessoas não aparecem nesta consulta pública.</p>
          <p><Link to="/">Início</Link></p>
        </section>
      )}
    </>
  )
}
