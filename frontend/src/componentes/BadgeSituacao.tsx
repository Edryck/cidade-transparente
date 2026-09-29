import { CircleCheck, Lock } from 'lucide-react'
import type { StatusManifestacao } from '../api/manifestacoes'

// Situação da manifestação com texto sempre presente; a cor só reforça (DESIGN.md, Badge de situação)
const SITUACOES: Record<StatusManifestacao, { texto: string; tom: string; concluida?: boolean }> = {
  RECEBIDA: { texto: 'Recebida', tom: 'neutro' },
  EM_ANALISE: { texto: 'Em análise pela ouvidoria', tom: 'destaque' },
  ENCAMINHADA: { texto: 'Encaminhada à secretaria', tom: 'destaque' },
  RESPONDIDA: { texto: 'Respondida', tom: 'no-prazo' },
  EM_RECURSO: { texto: 'Em recurso', tom: 'atencao' },
  ENCERRADA: { texto: 'Concluída', tom: 'neutro', concluida: true },
  ARQUIVADA: { texto: 'Arquivada', tom: 'neutro' },
}

export function textoSituacao(status: StatusManifestacao): string {
  return SITUACOES[status].texto
}

export function BadgeSituacao({ status }: { status: StatusManifestacao }) {
  const s = SITUACOES[status]
  return (
    <span className={`badge badge-${s.tom}`}>
      {s.concluida && <CircleCheck aria-hidden size={16} strokeWidth={1.5} />}
      {s.texto}
    </span>
  )
}

export function BadgeSigilo() {
  return (
    <span className="badge badge-sigilo">
      <Lock aria-hidden size={16} strokeWidth={1.5} />
      Identidade preservada
    </span>
  )
}
