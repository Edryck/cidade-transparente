import { useRef, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { FalhaApi } from '../api/cliente'
import { protocolosApi, type ConsultaPublica } from '../api/protocolos'
import { Alerta } from '../componentes/Alerta'
import { BadgeSituacao } from '../componentes/BadgeSituacao'
import { Botao } from '../componentes/Botao'
import { CabecalhoPagina } from '../componentes/CabecalhoPagina'
import { CampoChaveAcesso, CampoProtocolo } from '../componentes/CamposCodigo'
import { LinhaTramitacao, RespostaOficial } from '../componentes/LinhaTramitacao'
import { estadoDoPrazo, ReguaPrazo } from '../componentes/ReguaPrazo'
import { dataCurta, diaDoInstante } from '../util/datas'
import { useEnvioUnico } from '../util/useEnvioUnico'
import { useTitulo } from '../util/useTitulo'
import { PROTOCOLO } from '../util/validacao'

const RECURSO = { PENDENTE: 'Recurso apresentado, aguardando julgamento', DEFERIDO: 'Recurso deferido', INDEFERIDO: 'Recurso indeferido' }

export default function AcompanharProtocolo() {
  useTitulo('Acompanhar manifestação')
  const [parametros] = useSearchParams()
  const [protocolo, setProtocolo] = useState(parametros.get('protocolo') ?? '')
  const [chave, setChave] = useState('')
  const [erros, setErros] = useState<Record<string, string>>({})
  const [falha, setFalha] = useState('')
  const [consulta, setConsulta] = useState<ConsultaPublica | null>(null)
  const { enviando, executar } = useEnvioUnico()
  const refResultado = useRef<HTMLElement>(null)
  const refFalha = useRef<HTMLDivElement>(null)

  function consultar(evento: FormEvent) {
    evento.preventDefault()
    const novos: Record<string, string> = {}
    if (!PROTOCOLO.test(protocolo.trim())) novos.protocolo = 'Informe o protocolo no formato 2026-9999901-000004'
    if (!chave.trim()) novos.chave = 'Informe a chave de acesso recebida ao registrar'
    setErros(novos)
    setFalha('')
    setConsulta(null)
    if (Object.keys(novos).length) return
    executar(async () => {
      try {
        setConsulta(await protocolosApi.consultar(protocolo, chave))
        requestAnimationFrame(() => refResultado.current?.focus())
      } catch (erro) {
        const f = erro instanceof FalhaApi ? erro : new FalhaApi(500, 'Não foi possível consultar agora.')
        if (f.status === 400) setErros({ chave: 'Informe a chave de acesso recebida ao registrar' })
        // 404: mesma mensagem para protocolo inexistente e chave errada; não revela qual dos dois falhou
        else setFalha(f.status === 404 ? 'Protocolo ou chave de acesso inválidos.' : f.message)
        requestAnimationFrame(() => refFalha.current?.focus())
      }
    })
  }

  const primeiraResposta = consulta?.respostas[0]?.respondidaEm ?? null
  const prazo = consulta && estadoDoPrazo({
    dataAbertura: consulta.dataAbertura, dataLimite: consulta.dataLimite, prorrogada: consulta.prorrogada,
    status: consulta.status, respondidaEm: primeiraResposta,
  })
  return (
    <>
      <CabecalhoPagina titulo="Acompanhar manifestação"
        descricao="Informe seu protocolo e sua chave de acesso. Não é necessário ter conta."
        trilha={[{ texto: 'Início', para: '/' }, { texto: 'Acompanhar manifestação' }]} />

      <form onSubmit={consultar} noValidate className="formulario superficie">
        {falha && (
          <div ref={refFalha} tabIndex={-1}>
            <Alerta tipo="erro"><p>{falha} Confira os dois dados e tente de novo.</p></Alerta>
          </div>
        )}
        <CampoProtocolo valor={protocolo} onChange={setProtocolo} erro={erros.protocolo} />
        <CampoChaveAcesso valor={chave} onChange={setChave} erro={erros.chave} />
        <Botao type="submit" enviando={enviando} textoEnviando="Consultando…">Consultar</Botao>
      </form>

      {consulta && (
        <section ref={refResultado} tabIndex={-1} aria-labelledby="resultado-titulo" className="resultado">
          <div className="superficie resultado-resumo">
            <h2 id="resultado-titulo" className="so-leitor">Manifestação {consulta.protocolo}</h2>
            <p className="comprovante-rotulo" style={{ margin: 0 }}>Manifestação</p>
            <p className="protocolo">{consulta.protocolo}</p>
            <p style={{ margin: 0, fontSize: 20, fontWeight: 600 }}>{consulta.assunto}</p>
            <p className="resultado-linha">
              <span>{consulta.tipo}</span>
              <BadgeSituacao status={consulta.status} />
              {prazo && <strong>{prazo.texto}</strong>}
            </p>
          </div>

          <ReguaPrazo dataAbertura={consulta.dataAbertura} dataLimite={consulta.dataLimite}
            prorrogada={consulta.prorrogada} status={consulta.status} respondidaEm={primeiraResposta} />

          <h2>Dados da manifestação</h2>
          <dl className="dados superficie">
            <dt>Município</dt><dd>{consulta.municipio}</dd>
            <dt>Tipo</dt><dd>{consulta.tipo}</dd>
            <dt>Assunto</dt><dd>{consulta.assunto}</dd>
            <dt>Data de abertura</dt><dd className="mono">{dataCurta(diaDoInstante(consulta.dataAbertura))}</dd>
            <dt>Data limite</dt><dd className="mono">{dataCurta(consulta.dataLimite)}</dd>
            <dt>Prorrogação</dt><dd>{consulta.prorrogada ? 'Prazo prorrogado uma vez. A justificativa está no histórico.' : 'Sem prorrogação'}</dd>
            {consulta.recurso && <><dt>Recurso</dt><dd>{RECURSO[consulta.recurso]}</dd></>}
          </dl>

          {consulta.respostas.length > 0 && (
            <>
              <h2>{consulta.respostas.length === 1 ? 'Resposta' : 'Respostas'}</h2>
              {consulta.respostas.map((r) => (
                <RespostaOficial key={r.id} resposta={r} recursoJaApresentado={consulta.recurso !== null} />
              ))}
            </>
          )}

          <h2>Histórico</h2>
          <div className="superficie">
            <LinhaTramitacao tramites={consulta.tramites} />
          </div>
          <p className="metadado" style={{ marginTop: 16 }}>Nomes de pessoas não aparecem nesta consulta pública.</p>
          <p><Link to="/">Voltar ao início</Link></p>
        </section>
      )}
    </>
  )
}
