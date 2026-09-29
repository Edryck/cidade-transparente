import type { ButtonHTMLAttributes, ReactNode } from 'react'
import { Link, type LinkProps } from 'react-router-dom'

type Variante = 'primario' | 'secundario' | 'terciario' | 'destrutivo'

type BotaoProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variante?: Variante
  enviando?: boolean
  textoEnviando?: string
  icone?: ReactNode
}

/** Botão do DESIGN.md. Enviando: fica desabilitado e troca o texto, sem animação. */
export function Botao({ variante = 'primario', enviando, textoEnviando, icone, children, className, disabled, ...resto }: BotaoProps) {
  return (
    <button
      className={`botao botao-${variante}${className ? ' ' + className : ''}`}
      disabled={disabled || enviando}
      aria-busy={enviando || undefined}
      {...resto}
    >
      {icone}
      <span>{enviando ? (textoEnviando ?? 'Enviando…') : children}</span>
    </button>
  )
}

export function BotaoLink({ variante = 'primario', icone, children, className, ...resto }: LinkProps & { variante?: Variante; icone?: ReactNode }) {
  return (
    <Link className={`botao botao-${variante}${className ? ' ' + className : ''}`} {...resto}>
      {icone}
      <span>{children}</span>
    </Link>
  )
}
