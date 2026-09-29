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
      ? { chave: 'respondida', rotulo: 'Respondida no prazo', texto: `Respondida em ${dataCurta(dia)}` }
      : { chave: 'vencido', rotulo: 'Respondida fora do prazo', texto: `Respondida em ${dataCurta(dia)}` }
  }
  if (!AGUARDANDO.includes(p.status)) return null
  const faltam = diasEntre(hoje, p.dataLimite)
  if (faltam < 0) return { chave: 'vencido', rotulo: 'Prazo vencido', texto: `Venceu há ${plural(-faltam, 'dia', 'dias')}` }
  if (faltam === 0) return { chave: 'vencendo', rotulo: 'Vence hoje', texto: 'Vence hoje' }
  const texto = `Faltam ${plural(faltam, 'dia', 'dias')}`
  if (faltam <= 5) return { chave: 'vencendo', rotulo: 'Perto do vencimento', texto }
  if (p.prorrogada) return { chave: 'prorrogado', rotulo: 'Prazo prorrogado', texto }
  return { chave: 'no-prazo', rotulo: 'Dentro do prazo', texto }
}

const ICONES = { 'no-prazo': Clock, vencendo: TriangleAlert, vencido: OctagonAlert, prorrogado: TriangleAlert, respondida: CircleCheck }

export function ReguaPrazo(props: Props) {
  const estado = estadoDoPrazo(props)
  if (!estado) return null
  const Icone = ICONES[estado.chave]

  if (props.compacta) {
    return (
      <p className={`regua-compacta regua-${estado.chave}`}>
        <Icone aria-hidden size={18} strokeWidth={2} />
        <strong>{estado.texto}</strong>
        {estado.rotulo !== estado.texto && <span className="metadado">{estado.rotulo}</span>}
      </p>
    )
  }

  const abertura = diaDoInstante(props.dataAbertura)
  const total = Math.max(1, diasEntre(abertura, props.dataLimite))
  const parouEm = estado.chave === 'respondida' || (estado.chave === 'vencido' && props.respondidaEm)
    ? diaDoInstante(props.respondidaEm!) : hojeISO()
  const proporcao = Math.min(1, Math.max(0, diasEntre(abertura, parouEm) / total))

  return (
    <section className={`regua regua-${estado.chave}`} aria-labelledby="regua-titulo">
      <h2 id="regua-titulo" className="regua-titulo">Prazo da manifestação</h2>
      <p className="regua-estado">
        <Icone aria-hidden size={22} strokeWidth={2} />
        <span className="regua-texto">{estado.texto}</span>
        <span className="regua-rotulo">{estado.rotulo}</span>
      </p>
      <div className="regua-barra" aria-hidden><span style={{ width: `${Math.round(proporcao * 100)}%` }} /></div>
      <dl className="regua-datas">
        <div><dt>Prazo total</dt><dd>{plural(total, 'dia corrido', 'dias corridos')}{props.prorrogada ? ', com prorrogação' : ''}</dd></div>
        <div><dt>Aberta em</dt><dd className="mono">{dataCurta(abertura)}</dd></div>
        <div>
          <dt>Data limite</dt>
          <dd><span className="mono">{dataCurta(props.dataLimite)}</span> <span className="regua-dia">({diaDaSemana(props.dataLimite)})</span></dd>
        </div>
      </dl>
    </section>
  )
}
