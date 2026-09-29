import { CircleCheck, Clock, OctagonAlert, TriangleAlert } from 'lucide-react'
import type { StatusManifestacao } from '../api/manifestacoes'
import { dataCurta, diaDaSemana, diaDoInstante, diasEntre, hojeISO, plural } from '../util/datas'

type Props = {
  dataAbertura: string
  dataLimite: string
  prorrogada: boolean
  status: StatusManifestacao
  /** Data da primeira resposta, se houver: o prazo para de correr nela. */
  respondidaEm?: string | null
  compacta?: boolean
}

type Estado = { chave: 'no-prazo' | 'vencendo' | 'vencido' | 'prorrogado' | 'respondida'; rotulo: string; texto: string }

const AGUARDANDO: StatusManifestacao[] = ['RECEBIDA', 'EM_ANALISE', 'ENCAMINHADA']

/**
 * Estado do prazo, derivado só dos dados da API (a regra legal fica no backend). Texto + ícone + cor.
 * A API pública não informa quantos dias vieram da prorrogação, então a régua não inventa a divisão entre
 * prazo inicial e prorrogação: marca o estado "prorrogado" e mostra a data limite nova.
 */
export function estadoDoPrazo(p: Props): Estado | null {
  const hoje = hojeISO()
  if (p.respondidaEm && !AGUARDANDO.includes(p.status)) {
    const dia = diaDoInstante(p.respondidaEm)
    return dia <= p.dataLimite
      ? { chave: 'respondida', rotulo: 'Respondida no prazo', texto: `respondida em ${dataCurta(dia)}` }
      : { chave: 'vencido', rotulo: 'Respondida fora do prazo', texto: `respondida em ${dataCurta(dia)}` }
  }
  if (!AGUARDANDO.includes(p.status)) return null
  const faltam = diasEntre(hoje, p.dataLimite)
  if (faltam < 0) return { chave: 'vencido', rotulo: 'Vencido', texto: `venceu há ${plural(-faltam, 'dia', 'dias')}` }
  if (faltam === 0) return { chave: 'vencendo', rotulo: 'Vencendo', texto: 'vence hoje' }
  const texto = `faltam ${plural(faltam, 'dia corrido', 'dias corridos')}`
  if (faltam <= 5) return { chave: 'vencendo', rotulo: 'Vencendo', texto }
  if (p.prorrogada) return { chave: 'prorrogado', rotulo: 'Prorrogado', texto }
  return { chave: 'no-prazo', rotulo: 'No prazo', texto }
}

const ICONES = { 'no-prazo': Clock, vencendo: TriangleAlert, vencido: OctagonAlert, prorrogado: TriangleAlert, respondida: CircleCheck }

export function ReguaPrazo(props: Props) {
  const estado = estadoDoPrazo(props)
  if (!estado) return null
  const Icone = ICONES[estado.chave]
  const abertura = diaDoInstante(props.dataAbertura)
  const total = Math.max(1, diasEntre(abertura, props.dataLimite))
  const ate = props.respondidaEm && estado.chave !== 'no-prazo' && estado.chave !== 'vencendo' && estado.chave !== 'prorrogado'
    ? diaDoInstante(props.respondidaEm)
    : hojeISO()
  const proporcao = Math.min(1, Math.max(0, diasEntre(abertura, ate) / total))

  const rotulo = (
    <p className={`regua-estado regua-${estado.chave}`}>
      <Icone aria-hidden size={20} strokeWidth={1.5} />
      <span className="regua-rotulo">{estado.rotulo}</span>
      <span className="mono">{estado.texto}</span>
    </p>
  )
  if (props.compacta) return rotulo

  return (
    <div className="regua">
      {rotulo}
      <div className={`regua-barra regua-${estado.chave}`} aria-hidden>
        <span style={{ width: `${Math.round(proporcao * 100)}%` }} />
      </div>
      <dl className="regua-datas">
        <div>
          <dt>Aberta em</dt>
          <dd className="mono">{dataCurta(abertura)}</dd>
        </div>
        <div>
          <dt>Data limite{props.prorrogada ? ' (prorrogada)' : ''}</dt>
          <dd className="mono">
            {dataCurta(props.dataLimite)} <span className="regua-dia">{diaDaSemana(props.dataLimite)}</span>
          </dd>
        </div>
      </dl>
    </div>
  )
}
