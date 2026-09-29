import { Link } from 'react-router-dom'
import type { Resposta, Tramite } from '../api/protocolos'
import { dataCurta, dataHora } from '../util/datas'
import { Alerta } from './Alerta'
import { textoSituacao } from './BadgeSituacao'

/** Histórico em ordem: trilho à esquerda, um ponto por movimento, o último em destaque (estado atual). */
export function LinhaTramitacao({ tramites }: { tramites: Tramite[] }) {
  return (
    <ol className="tramitacao">
      {tramites.map((t, i) => {
        const atual = i === tramites.length - 1
        return (
          <li key={i} className={atual ? 'tramitacao-atual' : undefined} aria-current={atual ? 'step' : undefined}>
            <p className="tramitacao-acao">{t.descricao}</p>
            <p className="metadado">
              <time className="mono" dateTime={t.registradoEm}>{dataHora(t.registradoEm)}</time>
              {t.responsavel && <> · {t.responsavel}</>}
              {atual && <> · situação atual: {textoSituacao(t.statusNovo).toLowerCase()}</>}
            </p>
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

/** Resposta oficial: fundo papel-baixo e borda esquerda discreta (DESIGN.md, Linha de tramitação). */
export function RespostaOficial({ resposta, recursoJaApresentado }: { resposta: Resposta; recursoJaApresentado: boolean }) {
  return (
    <article className="resposta-oficial">
      <h3>Resposta da {resposta.secretaria}</h3>
      <p className="metadado">
        <time className="mono" dateTime={resposta.respondidaEm}>{dataHora(resposta.respondidaEm)}</time>
        {resposta.resultadoLai && <> · {RESULTADO[resposta.resultadoLai]}</>}
      </p>
      <p className="resposta-texto">{resposta.texto}</p>
      {resposta.recursoCabivel && !recursoJaApresentado && resposta.prazoRecurso && (
        <Alerta tipo="atencao" titulo="Você pode apresentar recurso">
          <dl className="dados">
            <dt>Prazo para recurso</dt>
            <dd className="mono">até {dataCurta(resposta.prazoRecurso)}</dd>
            <dt>Instância recursal</dt>
            <dd>{resposta.instanciaRecursal}</dd>
          </dl>
          <p>
            Você pode apresentar recurso dentro do prazo indicado. Para recorrer, é necessário estar autenticado.{' '}
            <Link to="/login">Entrar</Link>
          </p>
        </Alerta>
      )}
    </article>
  )
}
