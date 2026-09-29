import { requisitar } from './cliente'
import type { StatusManifestacao } from './manifestacoes'

export type ResultadoLai = 'CONCEDIDO' | 'PARCIALMENTE_CONCEDIDO' | 'NEGADO' | 'INEXISTENTE'
export type StatusRecurso = 'PENDENTE' | 'DEFERIDO' | 'INDEFERIDO'

export type Tramite = {
  statusAnterior: StatusManifestacao | null
  statusNovo: StatusManifestacao
  descricao: string
  secretariaDestino: string | null
  responsavel: string | null
  registradoEm: string
}

export type Resposta = {
  id: number
  texto: string
  resultadoLai: ResultadoLai | null
  secretaria: string
  respondidaEm: string
  recursoCabivel: boolean
  prazoRecurso: string | null
  instanciaRecursal: string | null
}

export type ConsultaPublica = {
  protocolo: string
  municipio: string
  tipo: string
  assunto: string
  status: StatusManifestacao
  dataAbertura: string
  dataLimite: string
  prorrogada: boolean
  tramites: Tramite[]
  respostas: Resposta[]
  recurso: StatusRecurso | null
}

export const protocolosApi = {
  /** A chave vai no header, nunca na URL (URLs ficam em logs e no histórico do navegador). */
  consultar: (protocolo: string, chave: string) =>
    requisitar<ConsultaPublica>(`/protocolos/${encodeURIComponent(protocolo.trim())}`, {
      semToken: true,
      cabecalhos: { 'X-Chave-Acesso': chave.trim().toUpperCase() },
    }),
}
