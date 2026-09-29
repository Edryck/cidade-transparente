import { useCallback, useEffect, useState } from 'react'
import { FalhaApi } from '../api/cliente'

type Estado<T> =
  | { fase: 'carregando' }
  | { fase: 'pronto'; dados: T }
  | { fase: 'erro'; erro: FalhaApi }

/** Carrega dados da API com os estados carregando, pronto e erro, e oferece tentar de novo. */
export function useCarregar<T>(carregar: (() => Promise<T>) | null, dependencias: unknown[]) {
  const [estado, setEstado] = useState<Estado<T>>({ fase: 'carregando' })
  const [tentativa, setTentativa] = useState(0)

  useEffect(() => {
    if (!carregar) return
    let ativo = true
    setEstado({ fase: 'carregando' })
    carregar()
      .then((dados) => ativo && setEstado({ fase: 'pronto', dados }))
      .catch((erro: unknown) =>
        ativo && setEstado({ fase: 'erro', erro: erro instanceof FalhaApi ? erro : new FalhaApi(500, 'Não foi possível carregar.') }))
    return () => {
      ativo = false
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...dependencias, tentativa])

  const tentarDeNovo = useCallback(() => setTentativa((t) => t + 1), [])
  return { estado, tentarDeNovo }
}
