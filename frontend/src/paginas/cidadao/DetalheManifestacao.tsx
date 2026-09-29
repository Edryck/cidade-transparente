import { Download, Paperclip, Printer } from 'lucide-react'
import { useRef, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { FalhaApi } from '../../api/cliente'
import { manifestacoesApi, type Anexo, type ManifestacaoDetalhe } from '../../api/manifestacoes'
import type { Resposta, Tramite } from '../../api/protocolos'
import { Alerta } from '../../componentes/Alerta'
import { BadgeSituacao } from '../../componentes/BadgeSituacao'
import { Botao } from '../../componentes/Botao'
import { CampoFormulario } from '../../componentes/CampoFormulario'
import { Carregando, ErroCarregamento, EstadoVazio } from '../../componentes/Estados'
import { LinhaTramitacao, RespostaOficial } from '../../componentes/LinhaTramitacao'
import { ReguaPrazo } from '../../componentes/ReguaPrazo'
import { dataCurta, dataHora, diaDoInstante } from '../../util/datas'
import { useCarregar } from '../../util/useCarregar'
import { useEnvioUnico } from '../../util/useEnvioUnico'
import { useTitulo } from '../../util/useTitulo'

type Tudo = { m: ManifestacaoDetalhe; tramites: Tramite[]; respostas: Resposta[]; anexos: Anexo[] }

const RECURSO = { PENDENTE: 'Aguardando julgamento', DEFERIDO: 'Deferido', INDEFERIDO: 'Indeferido' }
const tamanho = (bytes: number) => (bytes < 1024 * 1024 ? `${Math.ceil(bytes / 1024)} KB` : `${(bytes / 1024 / 1024).toFixed(1)} MB`)

/**
 * Tela 11. Quem pode ver e o que pode fazer é decidido pela API: outra manifestação responde 404, e as ações
 * (anexar, recorrer) só aparecem se o link correspondente vier em _links.
 */
export default function DetalheManifestacao() {
  const { id } = useParams()
  const numero = Number(id)
  const { estado, tentarDeNovo } = useCarregar<Tudo>(async () => {
    const [m, tramites, respostas, anexos] = await Promise.all([
      manifestacoesApi.obter(numero), manifestacoesApi.tramites(numero), manifestacoesApi.respostas(numero), manifestacoesApi.anexos(numero),
    ])
    return { m, tramites, respostas, anexos }
  }, [numero])
  useTitulo(estado.fase === 'pronto' ? `Manifestação ${estado.dados.m.protocolo}` : 'Manifestação')

  if (estado.fase === 'carregando') return <><h1>Manifestação</h1><Carregando /></>
  if (estado.fase === 'erro') {
    return estado.erro.status === 404 ? (
      <>
        <h1>Manifestação não encontrada</h1>
        <EstadoVazio titulo="Não encontramos esta manifestação na sua conta">
          <p>Confira o endereço. Manifestações enviadas sem identificação não aparecem na conta: acompanhe-as pelo protocolo e pela chave.</p>
          <p><Link to="/minhas-manifestacoes">Minhas manifestações</Link> · <Link to="/acompanhar">Acompanhar protocolo</Link></p>
        </EstadoVazio>
      </>
    ) : (
      <><h1>Manifestação</h1><ErroCarregamento erro={estado.erro} tentarDeNovo={tentarDeNovo} /></>
    )
  }

  return <Conteudo {...estado.dados} recarregar={tentarDeNovo} />
}

function Conteudo({ m, tramites, respostas, anexos, recarregar }: Tudo & { recarregar: () => void }) {
  const prorrogacao = tramites.find((t) => t.statusAnterior === t.statusNovo && /prorrogado/i.test(t.descricao))
  const ultimaResposta = respostas.at(-1)
  const linkRecurso = m._links.recurso?.href

  return (
    <article className="detalhe-manifestacao">
      <p className="protocolo mono">{m.protocolo}</p>
      <h1>{m.assunto}</h1>
      <p className="detalhe-meta">
        <span>{m.tipo.nome}</span>
        <BadgeSituacao status={m.status} />
      </p>
      <dl className="dados">
        <dt>Registrada em</dt><dd className="mono">{dataHora(m.dataAbertura)}</dd>
        <dt>Secretaria</dt><dd>{m.secretaria?.nome ?? 'Ainda na ouvidoria, em triagem'}</dd>
        {m.dataEncerramento && <><dt>Concluída em</dt><dd className="mono">{dataCurta(diaDoInstante(m.dataEncerramento))}</dd></>}
      </dl>

      <ReguaPrazo dataAbertura={m.dataAbertura} dataLimite={m.dataLimite} prorrogada={m.prorrogada} status={m.status}
        respondidaEm={respostas[0]?.respondidaEm ?? null} />
      {prorrogacao && (
        <Alerta tipo="atencao" titulo="Prorrogação">
          <p>{prorrogacao.descricao}</p>
        </Alerta>
      )}

      <section className="secao">
        <h2>O que você escreveu</h2>
        <p className="texto-longo">{m.descricao}</p>
      </section>

      <SecaoAnexos manifestacaoId={m.id} anexos={anexos} podeAnexar={Boolean(m._links.anexar)} aoEnviar={recarregar} />

      {respostas.length > 0 && (
        <section className="secao">
          <h2>{respostas.length === 1 ? 'Resposta da prefeitura' : 'Respostas da prefeitura'}</h2>
          {respostas.map((r) => (
            <RespostaOficial key={r.id} resposta={r} recursoJaApresentado={m.recurso !== null}
              acaoRecurso={r.id === ultimaResposta?.id
                ? linkRecurso
                  ? <FormRecurso link={linkRecurso} aoEnviar={recarregar} />
                  : <p>O prazo para recurso terminou ou o recurso não está disponível para esta resposta.</p>
                : undefined} />
          ))}
        </section>
      )}

      {m.recurso && (
        <section className="secao">
          <h2>Recurso</h2>
          <dl className="dados">
            <dt>Situação</dt><dd>{RECURSO[m.recurso.status]}</dd>
            <dt>Apresentado em</dt><dd className="mono">{dataHora(m.recurso.interpostoEm)}</dd>
            {m.recurso.status === 'PENDENTE' && <><dt>Julgamento até</dt><dd className="mono">{dataCurta(m.recurso.dataLimiteJulgamento)}</dd></>}
            <dt>Sua justificativa</dt><dd className="texto-longo">{m.recurso.justificativa}</dd>
            {m.recurso.decisao && <><dt>Decisão</dt><dd className="texto-longo">{m.recurso.decisao}</dd></>}
          </dl>
        </section>
      )}

      <section className="secao">
        <h2>Histórico</h2>
        <LinhaTramitacao tramites={tramites} />
      </section>

      <p className="nao-imprimir secao">
        <Botao variante="secundario" type="button" onClick={() => window.print()} icone={<Printer aria-hidden size={20} strokeWidth={1.5} />}>
          Imprimir
        </Botao>
      </p>
      <p className="nao-imprimir"><Link to="/minhas-manifestacoes">Minhas manifestações</Link></p>
    </article>
  )
}

function SecaoAnexos({ manifestacaoId, anexos, podeAnexar, aoEnviar }: {
  manifestacaoId: number; anexos: Anexo[]; podeAnexar: boolean; aoEnviar: () => void
}) {
  const [enviando, setEnviando] = useState(false)
  const [falha, setFalha] = useState('')

  async function baixar(a: Anexo) {
    try {
      const arquivo = await manifestacoesApi.baixarAnexo(manifestacaoId, a.id)
      const url = URL.createObjectURL(arquivo)
      const link = Object.assign(document.createElement('a'), { href: url, download: a.nomeArquivo })
      link.click()
      URL.revokeObjectURL(url)
    } catch (erro) {
      setFalha(erro instanceof FalhaApi ? erro.message : 'Não foi possível baixar o arquivo.')
    }
  }

  async function enviar(lista: FileList | null) {
    const arquivo = lista?.[0]
    if (!arquivo) return
    setFalha('')
    setEnviando(true)
    try {
      await manifestacoesApi.enviarAnexo(manifestacaoId, arquivo)
      aoEnviar()
    } catch (erro) {
      setFalha(`Não foi possível anexar ${arquivo.name}. ${erro instanceof FalhaApi ? erro.message : ''}`)
    } finally {
      setEnviando(false)
    }
  }

  if (anexos.length === 0 && !podeAnexar) return null
  return (
    <section className="secao">
      <h2>Anexos</h2>
      {anexos.length === 0 ? <p className="metadado">Nenhum anexo.</p> : (
        <ul className="lista-anexos">
          {anexos.map((a) => (
            <li key={a.id}>
              <span>{a.nomeArquivo} <span className="mono metadado">{tamanho(a.tamanho)}</span></span>
              <button type="button" className="botao-texto nao-imprimir" onClick={() => baixar(a)}>
                <Download aria-hidden size={18} strokeWidth={1.5} /> Baixar<span className="so-leitor"> {a.nomeArquivo}</span>
              </button>
            </li>
          ))}
        </ul>
      )}
      {falha && <Alerta tipo="erro"><p>{falha}</p></Alerta>}
      {podeAnexar && (
        <div className="nao-imprimir">
          <p className="metadado" id="ajuda-anexo">PDF, PNG ou JPEG, até 5 MB.</p>
          <label className={`botao botao-secundario botao-arquivo${enviando ? ' desabilitado' : ''}`}>
            <Paperclip aria-hidden size={20} strokeWidth={1.5} />
            <span>{enviando ? 'Enviando…' : 'Anexar documento'}</span>
            <input type="file" accept="application/pdf,image/png,image/jpeg" className="so-leitor" disabled={enviando}
              aria-describedby="ajuda-anexo" onChange={(e) => { enviar(e.target.files); e.target.value = '' }} />
          </label>
        </div>
      )}
    </section>
  )
}

function FormRecurso({ link, aoEnviar }: { link: string; aoEnviar: () => void }) {
  const [aberto, setAberto] = useState(false)
  const [justificativa, setJustificativa] = useState('')
  const [erro, setErro] = useState('')
  const { enviando, executar } = useEnvioUnico()
  const refCampo = useRef<HTMLTextAreaElement>(null)

  function enviar(evento: FormEvent) {
    evento.preventDefault()
    if (!justificativa.trim()) {
      setErro('Explique por que a resposta deve ser revista')
      refCampo.current?.focus()
      return
    }
    executar(async () => {
      try {
        await manifestacoesApi.recorrer(link, justificativa.trim())
        aoEnviar()
      } catch (e) {
        setErro(e instanceof FalhaApi ? e.message : 'Não foi possível apresentar o recurso agora.')
      }
    })
  }

  if (!aberto) {
    return <Botao type="button" onClick={() => setAberto(true)}>Apresentar recurso</Botao>
  }
  return (
    <form onSubmit={enviar} noValidate className="formulario">
      <p>O recurso é julgado pela ouvidoria, e só há uma instância: depois do julgamento não cabe novo recurso no sistema.</p>
      <CampoFormulario id="justificativa-recurso" rotulo="Por que a resposta deve ser revista" obrigatorio erro={erro}>
        {(p) => <textarea {...p} ref={refCampo} rows={6} maxLength={2000} value={justificativa} onChange={(e) => setJustificativa(e.target.value)} />}
      </CampoFormulario>
      <div className="acoes-passo">
        <Botao variante="secundario" type="button" onClick={() => setAberto(false)} disabled={enviando}>Cancelar</Botao>
        <Botao type="submit" enviando={enviando}>Enviar recurso</Botao>
      </div>
    </form>
  )
}
