import type { ReactNode } from 'react'

/** Tabela semântica que rola na horizontal dentro da própria caixa em telas estreitas, sem estourar a página. */
export function TabelaResponsiva({ legenda, children }: { legenda: string; children: ReactNode }) {
  return (
    <div className="tabela-responsiva" role="region" aria-label={legenda} tabIndex={0}>
      <table className="tabela">
        <caption>{legenda}</caption>
        {children}
      </table>
    </div>
  )
}
