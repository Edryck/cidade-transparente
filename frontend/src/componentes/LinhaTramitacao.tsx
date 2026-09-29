import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import type { Resposta, Tramite } from '../api/protocolos'
import { dataCurta, dataHora } from '../util/datas'
import { Alerta } from './Alerta'
import { textoSituacao } from './BadgeSituacao'

/** Histórico em linha vertical simples: data, o que aconteceu e, quando a API mostra, quem fez. */
export function LinhaTramitacao({ tramites }: { tramites: Tramite[] }) {
  return (
    <ol className="tramitacao">
      {tramites.map((t, i) => {
        const atual = i === tramites.length - 1
        return (
          <li key={i} className={atual ? 'tramitacao-atual' : undefined} aria-current={atual ? 'step' : undefined}>
            <time className="tramitacao-data mono" dateTime={t.registradoEm}>{dataHora(t.registradoEm)}</time>
            <p className="tramitacao-acao">{t.descricao}</p>
            {(t.responsavel || atual) && (
              <p className="tramitacao-detalhe">
                {t.responsavel}
                {t.responsavel && atual && ' · '}
                {atual && <>Situação atual: {textoSituacao(t.statusNovo).toLowerCase()}</>}
              </p>
            )}
          </li>
        )
      })}
    </ol>
  )
}

const RESULTADO: Record<string, string> = {
  CONCEDIDO: 'Acesso concedido',
  PARCIALMENTE_CONCEDIDO: 'Acesso parcialmente concedido',
  NEGADO: 'Acesso negado',
  INEXISTENTE: 'A prefeitura não possui a informação',
}

/** Resposta oficial: um registro administrativo, com órgão e data. Nunca aparência de conversa. */
export function RespostaOficial({ resposta, recursoJaApresentado, acaoRecurso }: {
  resposta: Resposta
  recursoJaApresentado: boolean
  /** Para quem está logado e recebeu da API o link de recurso: o formulário substitui o convite para entrar. */
  acaoRecurso?: ReactNode
}) {
  return (
    <article className="resposta-oficial">
      <header className="resposta-cabecalho">
        <h3>Resposta oficial da {resposta.secretaria}</h3>
        <time className="metadado mono" dateTime={resposta.respondidaEm}>{dataHora(resposta.respondidaEm)}</time>
      </header>
      {resposta.resultadoLai && <p><strong>Resultado do pedido:</strong> {RESULTADO[resposta.resultadoLai]}</p>}
      <p className="resposta-texto">{resposta.texto}</p>
      {resposta.recursoCabivel && !recursoJaApresentado && resposta.prazoRecurso && (
        <Alerta tipo="info" titulo="Você pode apresentar recurso">
          <dl className="dados">
            <dt>Prazo para recurso</dt>
            <dd><span className="mono">até {dataCurta(resposta.prazoRecurso)}</span></dd>
            <dt>Instância recursal</dt>
            <dd>{resposta.instanciaRecursal}</dd>
          </dl>
          {acaoRecurso ?? (
            <p>
              Você pode apresentar recurso dentro do prazo indicado. Para recorrer, é necessário estar autenticado.{' '}
              <Link to="/login">Entrar</Link>
            </p>
          )}
        </Alerta>
      )}
    </article>
  )
}
