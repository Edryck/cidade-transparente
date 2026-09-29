import { useSyncExternalStore } from 'react'
import type { Perfil, RespostaLogin } from '../api/auth'

// Sessão no sessionStorage: some ao fechar a aba e não é lida por outras abas. A senha nunca é guardada.
// O token é só credencial de acesso: quem decide permissão é sempre o backend.

export type Sessao = {
  token: string
  perfil: Perfil
  nome: string
  usuarioId: number
  municipioId: number | null
  expiraEm: number
}

const CHAVE = 'ct.sessao'
const ouvintes = new Set<() => void>()
let cache: Sessao | null | undefined

function avisar() {
  cache = undefined
  ouvintes.forEach((ouvinte) => ouvinte())
}

export function lerSessao(): Sessao | null {
  if (cache !== undefined) return cache
  try {
    const bruto = sessionStorage.getItem(CHAVE)
    const sessao = bruto ? (JSON.parse(bruto) as Sessao) : null
    cache = sessao && sessao.expiraEm > Date.now() ? sessao : null
  } catch {
    cache = null
  }
  return cache
}

export function iniciarSessao(resposta: RespostaLogin): Sessao {
  const sessao: Sessao = {
    token: resposta.token,
    perfil: resposta.perfil,
    nome: resposta.nome,
    usuarioId: resposta.usuarioId,
    municipioId: resposta.municipioId,
    expiraEm: Date.now() + resposta.expiraEmSegundos * 1000,
  }
  try {
    sessionStorage.setItem(CHAVE, JSON.stringify(sessao))
  } catch {
    // Sem armazenamento (modo privado restrito): a sessão vale só enquanto a página estiver aberta
  }
  avisar()
  cache = sessao
  return sessao
}

/**
 * Remove o token. No "Sair", apaga também os rascunhos desta aba, que podem conter texto da manifestação;
 * quando a sessão só expira (401), o rascunho fica, para a pessoa não perder o que digitou.
 */
export function encerrarSessao(limparRascunhos = false) {
  try {
    Object.keys(sessionStorage)
      .filter((chave) => chave === CHAVE || (limparRascunhos && chave.startsWith('ct.rascunho.')))
      .forEach((chave) => sessionStorage.removeItem(chave))
  } catch {
    // nada a limpar
  }
  avisar()
}

function assinar(ouvinte: () => void) {
  ouvintes.add(ouvinte)
  return () => ouvintes.delete(ouvinte)
}

export function useSessao(): Sessao | null {
  return useSyncExternalStore(assinar, lerSessao, () => null)
}

/**
 * Tela inicial de cada perfil. As áreas autenticadas (telas 9 em diante) ainda não existem no frontend:
 * as rotas já ficam reservadas e mostram esse aviso de forma honesta (paginas/AreaPendente).
 */
export const ROTA_INICIAL: Record<Perfil, string> = {
  CIDADAO: '/minhas-manifestacoes',
  OUVIDOR: '/painel',
  SERVIDOR: '/secretaria',
  ADMIN: '/administracao',
  ADMIN_PLATAFORMA: '/plataforma',
}
