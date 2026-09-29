import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { contraste, hexValido } from '../util/contraste'

// Município escolhido na tela 1. Não é dado pessoal: fica no localStorage para sobreviver a recarregamentos.
// brasao e corDestaque ainda não vêm da API; o cabeçalho e o tema já sabem usá-los quando vierem.

export type MunicipioEscolhido = {
  id: number
  nome: string
  uf: string
  brasao?: string | null
  corDestaque?: string | null
}

type Contexto = {
  municipio: MunicipioEscolhido | null
  escolher: (municipio: MunicipioEscolhido) => void
  limpar: () => void
}

const CHAVE = 'ct.municipio'
// Cor da prefeitura só entra se o texto branco dos botões continuar legível (4.5:1)
const BRANCO = '#FFFFFF'
const MunicipioCtx = createContext<Contexto | null>(null)

function ler(): MunicipioEscolhido | null {
  try {
    const bruto = localStorage.getItem(CHAVE)
    return bruto ? (JSON.parse(bruto) as MunicipioEscolhido) : null
  } catch {
    return null
  }
}

/** Escurece a cor em 20% para o hover (--azul-escuro). */
function escurecer(hex: string): string {
  const canais = [1, 3, 5].map((i) => Math.round(parseInt(hex.slice(i, i + 2), 16) * 0.8))
  return '#' + canais.map((c) => c.toString(16).padStart(2, '0')).join('')
}

export function MunicipioProvider({ children }: { children: ReactNode }) {
  const [municipio, setMunicipio] = useState<MunicipioEscolhido | null>(ler)

  // O município troca só a cor de ação (--azul), e só se ela contrastar 4.5:1 com o branco; senão fica o padrão
  useEffect(() => {
    const raiz = document.documentElement.style
    const cor = municipio?.corDestaque
    if (hexValido(cor) && contraste(cor, BRANCO) >= 4.5) {
      raiz.setProperty('--azul', cor)
      raiz.setProperty('--azul-escuro', escurecer(cor))
    } else {
      raiz.removeProperty('--azul')
      raiz.removeProperty('--azul-escuro')
    }
  }, [municipio])

  const escolher = useCallback((novo: MunicipioEscolhido) => {
    setMunicipio(novo)
    try {
      localStorage.setItem(CHAVE, JSON.stringify(novo))
    } catch {
      // sem armazenamento: a escolha vale até recarregar a página
    }
  }, [])

  const limpar = useCallback(() => {
    setMunicipio(null)
    try {
      localStorage.removeItem(CHAVE)
    } catch {
      // nada a limpar
    }
  }, [])

  const valor = useMemo(() => ({ municipio, escolher, limpar }), [municipio, escolher, limpar])
  return <MunicipioCtx.Provider value={valor}>{children}</MunicipioCtx.Provider>
}

export function useMunicipio(): Contexto {
  const ctx = useContext(MunicipioCtx)
  if (!ctx) throw new Error('useMunicipio fora do MunicipioProvider')
  return ctx
}
