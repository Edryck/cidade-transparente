import { useCallback, useRef, useState } from 'react'

/**
 * Garante um envio por vez. O estado "enviando" só desabilita o botão depois de a tela redesenhar; cliques
 * rápidos antes disso passariam. A trava por ref vale no mesmo instante do primeiro clique.
 */
export function useEnvioUnico() {
  const travado = useRef(false)
  const [enviando, setEnviando] = useState(false)
  const executar = useCallback(async (acao: () => Promise<void>) => {
    if (travado.current) return
    travado.current = true
    setEnviando(true)
    try {
      await acao()
    } finally {
      travado.current = false
      setEnviando(false)
    }
  }, [])
  return { enviando, executar }
}
