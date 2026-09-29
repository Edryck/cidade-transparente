import { lerSessao, encerrarSessao } from '../contextos/sessao'

// Base da API: em desenvolvimento o Vite repassa /api para o backend (vite.config.ts).
const BASE = (import.meta.env.VITE_API_URL ?? '') + '/api/v1'

/** Erro vindo da API (ProblemDetail) ou da rede, já com mensagem que pode ir para a tela. */
export class FalhaApi extends Error {
  status: number
  campos: Record<string, string>

  constructor(status: number, mensagem: string, campos: Record<string, string> = {}) {
    super(mensagem)
    this.status = status
    this.campos = campos
  }
}

type Opcoes = {
  metodo?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  /** Objeto vira JSON; FormData segue como multipart (o navegador define o boundary). */
  corpo?: unknown
  cabecalhos?: Record<string, string>
  /** Não envia o token nem se houver sessão (ex.: denúncia anônima). */
  semToken?: boolean
}

const MENSAGEM_PADRAO: Record<number, string> = {
  0: 'Não foi possível falar com o sistema. Verifique sua conexão e tente de novo.',
  401: 'Sua sessão terminou. Entre de novo para continuar.',
  403: 'Seu perfil não tem acesso a esta área.',
  404: 'Não encontramos o que você procurou.',
  409: 'Não foi possível concluir a ação no estado atual.',
  413: 'O arquivo é maior que o limite permitido.',
  500: 'O sistema está com um problema no momento. Tente de novo em alguns minutos.',
}

async function enviar(caminho: string, opcoes: Opcoes): Promise<Response> {
  const cabecalhos: Record<string, string> = { Accept: 'application/json, application/hal+json', ...opcoes.cabecalhos }
  const multipart = opcoes.corpo instanceof FormData
  if (opcoes.corpo !== undefined && !multipart) cabecalhos['Content-Type'] = 'application/json'
  const sessao = opcoes.semToken ? null : lerSessao()
  if (sessao) cabecalhos.Authorization = `Bearer ${sessao.token}`

  let resposta: Response
  try {
    resposta = await fetch(BASE + caminho, {
      method: opcoes.metodo ?? 'GET',
      headers: cabecalhos,
      body: opcoes.corpo === undefined ? undefined : multipart ? (opcoes.corpo as FormData) : JSON.stringify(opcoes.corpo),
    })
  } catch {
    throw new FalhaApi(0, MENSAGEM_PADRAO[0])
  }
  if (resposta.status === 401 && sessao) encerrarSessao()
  return resposta
}

/** Arquivo protegido (ex.: anexo): baixa com o token, porque um link comum não enviaria o Authorization. */
export async function requisitarArquivo(caminho: string): Promise<Blob> {
  const resposta = await enviar(caminho, {})
  if (!resposta.ok) await lancarFalha(resposta)
  return resposta.blob()
}

/**
 * Caminho da API a partir de um link HATEOAS (href absoluto). As ações seguem o link que a API ofereceu,
 * sem o frontend montar a URL por conta própria.
 */
export function caminhoDoLink(href: string): string {
  return new URL(href, window.location.origin).pathname.replace(/^.*?\/api\/v1/, '')
}

async function lancarFalha(resposta: Response): Promise<never> {
  let problema: { detail?: string; campos?: Record<string, string> } = {}
  try {
    problema = (await resposta.json()) ?? {}
  } catch {
    problema = {}
  }
  const status = resposta.status >= 500 ? 500 : resposta.status
  // Em 5xx a mensagem do servidor não vai para a tela: pode ser técnica demais para o cidadão
  const mensagem = status < 500 && problema.detail ? problema.detail : (MENSAGEM_PADRAO[status] ?? MENSAGEM_PADRAO[500])
  throw new FalhaApi(status, mensagem, problema.campos ?? {})
}

export async function requisitar<T>(caminho: string, opcoes: Opcoes = {}): Promise<T> {
  const resposta = await enviar(caminho, opcoes)
  if (!resposta.ok) await lancarFalha(resposta)
  if (resposta.status === 204) return undefined as T

  const texto = await resposta.text()
  let dados: unknown = null
  try {
    dados = texto ? JSON.parse(texto) : null
  } catch {
    dados = null
  }

  return dados as T
}

/** Lista HAL: os itens vêm em _embedded.<nome>; coleção vazia não traz _embedded. */
export function itensHal<T>(dados: { _embedded?: Record<string, T[]> } | null, nome: string): T[] {
  return dados?._embedded?.[nome] ?? []
}
