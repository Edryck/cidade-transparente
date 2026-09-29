import { caminhoDoLink, itensHal, requisitar, requisitarArquivo } from './cliente'
import type { Resposta, StatusRecurso, Tramite } from './protocolos'

export type StatusManifestacao =
  | 'RECEBIDA' | 'EM_ANALISE' | 'ENCAMINHADA' | 'RESPONDIDA' | 'EM_RECURSO' | 'ENCERRADA' | 'ARQUIVADA'

export type Links = Record<string, { href: string }>

export type ManifestacaoCriada = {
  id: number
  protocolo: string
  chaveAcesso: string
  status: StatusManifestacao
  dataLimite: string
  anonima: boolean
  orientacao: string
}

export type NovaManifestacao = { municipioId?: number; tipoId: number; assunto: string; descricao: string }

export type ManifestacaoResumo = {
  id: number
  protocolo: string
  tipo: string
  assunto: string
  status: StatusManifestacao
  dataAbertura: string
  dataLimite: string
  prorrogada: boolean
  secretariaSigla: string | null
  diasRestantes: number | null
  vencida: boolean
}

export type Pagina<T> = { itens: T[]; numero: number; totalPaginas: number; totalItens: number }

export type Recurso = {
  id: number
  respostaId: number
  justificativa: string
  status: StatusRecurso
  interpostoEm: string
  dataLimiteJulgamento: string
  decisao: string | null
  julgadoEm: string | null
}

export type ManifestacaoDetalhe = {
  id: number
  protocolo: string
  tipo: { id: number; codigo: string; nome: string; categoria: 'OUVIDORIA' | 'LAI' }
  assunto: string
  descricao: string
  status: StatusManifestacao
  dataAbertura: string
  dataLimite: string
  prorrogada: boolean
  dataEncerramento: string | null
  secretaria: { id: number; sigla: string; nome: string } | null
  anonima: boolean | null
  manifestante: { nome: string; email: string; cpf: string | null; telefone: string | null } | null
  recurso: Recurso | null
  _links: Links
}

export type Anexo = { id: number; nomeArquivo: string; contentType: string; tamanho: number; enviadoEm: string }

export type FiltrosLista = { status?: string; tipoId?: string; pagina: number }

type PaginaHal<T> = {
  _embedded?: Record<string, T[]>
  page?: { number: number; totalPages: number; totalElements: number }
}

export const manifestacoesApi = {
  /** Sem token: a API trata como anônima, e só aceita tipo que admite anonimato (denúncia). */
  criarAnonima: (dados: NovaManifestacao) =>
    requisitar<ManifestacaoCriada>('/manifestacoes', { metodo: 'POST', corpo: dados, semToken: true }),

  /** Com o token do cidadão: identificada, e o município vem do token (o corpo não o envia). */
  criarIdentificada: (dados: NovaManifestacao) =>
    requisitar<ManifestacaoCriada>('/manifestacoes', { metodo: 'POST', corpo: { ...dados, municipioId: undefined } }),

  /** A API já devolve só as do cidadão logado. Mais recentes primeiro, 10 por página. */
  listar: async ({ status, tipoId, pagina }: FiltrosLista): Promise<Pagina<ManifestacaoResumo>> => {
    const q = new URLSearchParams({ page: String(pagina), size: '10', sort: 'dataAbertura,desc' })
    if (status) q.set('status', status)
    if (tipoId) q.set('tipoId', tipoId)
    const dados = await requisitar<PaginaHal<ManifestacaoResumo>>(`/manifestacoes?${q}`)
    return {
      itens: itensHal(dados, 'manifestacoes'),
      numero: dados.page?.number ?? 0,
      totalPaginas: dados.page?.totalPages ?? 1,
      totalItens: dados.page?.totalElements ?? 0,
    }
  },

  obter: (id: number) => requisitar<ManifestacaoDetalhe>(`/manifestacoes/${id}`),
  tramites: async (id: number) => itensHal<Tramite>(await requisitar(`/manifestacoes/${id}/tramites`), 'tramites'),
  respostas: async (id: number) => itensHal<Resposta>(await requisitar(`/manifestacoes/${id}/respostas`), 'respostas'),
  anexos: async (id: number) => itensHal<Anexo>(await requisitar(`/manifestacoes/${id}/anexos`), 'anexos'),

  enviarAnexo: (id: number, arquivo: File) => {
    const corpo = new FormData()
    corpo.append('arquivo', arquivo)
    return requisitar<Anexo>(`/manifestacoes/${id}/anexos`, { metodo: 'POST', corpo })
  },

  baixarAnexo: (id: number, anexoId: number) => requisitarArquivo(`/manifestacoes/${id}/anexos/${anexoId}`),

  /** Recurso segue o link "recurso" que a API ofereceu no detalhe (HATEOAS). */
  recorrer: (link: string, justificativa: string) =>
    requisitar<Recurso>(caminhoDoLink(link), { metodo: 'POST', corpo: { justificativa } }),
}
