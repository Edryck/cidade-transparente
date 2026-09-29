import { useEffect } from 'react'

/** Título da aba: o leitor de tela anuncia ao trocar de página. */
export function useTitulo(titulo: string) {
  useEffect(() => {
    document.title = `${titulo} · Ouvidoria e Acesso à Informação`
  }, [titulo])
}
