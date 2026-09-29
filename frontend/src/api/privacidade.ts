import { requisitar } from './cliente'

export type AvisoPrivacidade = {
  versao: string
  controlador: string
  operador: string
  encarregado: { nome: string; email: string } | null
  tratamentos: { finalidade: string; dados: string[]; baseLegal: string; retencao: string }[]
  compartilhamento: string[]
  direitos: { direito: string; comoExercer: string }[]
  prazoAtendimento: string
  incidentes: string
}

export const privacidadeApi = {
  obter: (municipioId: number) => requisitar<AvisoPrivacidade>(`/municipios/${municipioId}/privacidade`, { semToken: true }),
}
