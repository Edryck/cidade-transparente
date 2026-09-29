import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'

type Trilha = { texto: string; para?: string }[]

/** Trilha ("onde estou"), título e explicação curta. Um por página, com o único h1. */
export function CabecalhoPagina({ titulo, descricao, trilha, children }: {
  titulo: string; descricao?: ReactNode; trilha?: Trilha; children?: ReactNode
}) {
  return (
    <div className="cabecalho-pagina">
      {trilha && trilha.length > 0 && (
        <nav aria-label="Você está em" className="trilha">
          <ol>
            {trilha.map((t, i) => (
              <li key={t.texto}>
                {t.para && i < trilha.length - 1 ? <Link to={t.para}>{t.texto}</Link> : <span aria-current="page">{t.texto}</span>}
              </li>
            ))}
          </ol>
        </nav>
      )}
      <h1>{titulo}</h1>
      {descricao && <p>{descricao}</p>}
      {children}
    </div>
  )
}
