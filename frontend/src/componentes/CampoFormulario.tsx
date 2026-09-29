import { CircleAlert } from 'lucide-react'
import type { ReactNode } from 'react'

export type PropsControle = {
  id: string
  'aria-describedby'?: string
  'aria-invalid'?: boolean
  required?: boolean
}

type Props = {
  id: string
  rotulo: string
  obrigatorio: boolean
  ajuda?: ReactNode
  erro?: string
  /** Recebe id, aria-describedby e aria-invalid já ligados ao rótulo, à ajuda e ao erro. */
  children: (props: PropsControle) => ReactNode
}

/** Rótulo sempre visível, "(obrigatório)"/"(opcional)" escrito, ajuda e erro ligados ao campo. */
export function CampoFormulario({ id, rotulo, obrigatorio, ajuda, erro, children }: Props) {
  const idAjuda = ajuda ? `${id}-ajuda` : undefined
  const idErro = erro ? `${id}-erro` : undefined
  const descricao = [idAjuda, idErro].filter(Boolean).join(' ') || undefined
  return (
    <div className={`campo${erro ? ' campo-com-erro' : ''}`}>
      <label htmlFor={id} className="campo-rotulo">
        {rotulo} <span className="campo-exigencia">({obrigatorio ? 'obrigatório' : 'opcional'})</span>
      </label>
      {ajuda && <p id={idAjuda} className="campo-ajuda">{ajuda}</p>}
      {children({ id, 'aria-describedby': descricao, 'aria-invalid': erro ? true : undefined, required: obrigatorio })}
      {erro && (
        <p id={idErro} className="campo-erro">
          <CircleAlert aria-hidden size={18} strokeWidth={1.5} />
          <span>{erro}</span>
        </p>
      )}
    </div>
  )
}
