import { requisitar } from './cliente'

export type StatusManifestacao =
  | 'RECEBIDA' | 'EM_ANALISE' | 'ENCAMINHADA' | 'RESPONDIDA' | 'EM_RECURSO' | 'ENCERRADA' | 'ARQUIVADA'

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

export const manifestacoesApi = {
  /** Sem token: a API trata como anônima, e só aceita tipo que admite anonimato (denúncia). */
  criarAnonima: (dados: NovaManifestacao) =>
    requisitar<ManifestacaoCriada>('/manifestacoes', { metodo: 'POST', corpo: dados, semToken: true }),
}
