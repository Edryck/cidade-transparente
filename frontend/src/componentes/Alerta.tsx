import { CircleAlert, Info, Lock, TriangleAlert, CircleCheck } from 'lucide-react'
import type { ReactNode } from 'react'

type Tipo = 'info' | 'atencao' | 'erro' | 'sucesso' | 'sigilo'

const ICONES = { info: Info, atencao: TriangleAlert, erro: CircleAlert, sucesso: CircleCheck, sigilo: Lock }

/** Faixa de aviso: fundo da cor a 10%, borda a 40%, ícone e texto. Erro é anunciado ao leitor de tela. */
export function Alerta({ tipo = 'info', titulo, children, id }: { tipo?: Tipo; titulo?: string; children: ReactNode; id?: string }) {
  const Icone = ICONES[tipo]
  return (
    <div id={id} className={`alerta alerta-${tipo}`} role={tipo === 'erro' ? 'alert' : undefined}>
      <Icone aria-hidden size={22} strokeWidth={1.5} className="alerta-icone" />
      <div>
        {titulo && <p className="alerta-titulo">{titulo}</p>}
        {children}
      </div>
    </div>
  )
}
