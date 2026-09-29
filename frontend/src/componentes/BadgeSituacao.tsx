import { CircleCheck, Lock } from 'lucide-react'
import type { StatusManifestacao } from '../api/manifestacoes'

// Cores da tabela de status do DESIGN.md (seção 21). O texto está sempre presente; a cor só reforça
const SITUACOES: Record<StatusManifestacao, { texto: string; tom: string; concluida?: boolean }> = {
  RECEBIDA: { texto: 'Recebida', tom: 'neutro' },
  EM_ANALISE: { texto: 'Em análise pela ouvidoria', tom: 'azul' },
  ENCAMINHADA: { texto: 'Encaminhada à secretaria', tom: 'azul' },
  RESPONDIDA: { texto: 'Respondida', tom: 'verde' },
  EM_RECURSO: { texto: 'Em recurso', tom: 'amarelo' },
  ENCERRADA: { texto: 'Concluída', tom: 'verde', concluida: true },
  ARQUIVADA: { texto: 'Arquivada', tom: 'neutro' },
}

export function textoSituacao(status: StatusManifestacao): string {
  return SITUACOES[status].texto
}

export function BadgeSituacao({ status }: { status: StatusManifestacao }) {
  const s = SITUACOES[status]
  return (
    <span className={`badge badge-${s.tom}`}>
      {s.concluida && <CircleCheck aria-hidden size={16} strokeWidth={2} />}
      {s.texto}
    </span>
  )
}

export function BadgeSigilo() {
  return (
    <span className="badge badge-sigilo">
      <Lock aria-hidden size={16} strokeWidth={2} />
      Identidade preservada
    </span>
  )
}
