import { Search } from 'lucide-react'

/** Busca com rótulo visível; filtra enquanto a pessoa digita. */
export function CampoBusca({ id, rotulo, valor, onChange, ajuda }: {
  id: string; rotulo: string; valor: string; onChange: (valor: string) => void; ajuda?: string
}) {
  return (
    <div className="campo">
      <label htmlFor={id} className="campo-rotulo">{rotulo}</label>
      {ajuda && <p id={`${id}-ajuda`} className="campo-ajuda">{ajuda}</p>}
      <div className="campo-busca">
        <Search aria-hidden size={20} strokeWidth={1.5} />
        <input id={id} type="search" value={valor} onChange={(e) => onChange(e.target.value)}
          autoComplete="off" aria-describedby={ajuda ? `${id}-ajuda` : undefined} />
      </div>
    </div>
  )
}
