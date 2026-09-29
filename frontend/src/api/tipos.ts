import { itensHal, requisitar } from './cliente'

export type TipoManifestacao = {
  id: number
  codigo: 'RECLAMACAO' | 'DENUNCIA' | 'SUGESTAO' | 'ELOGIO' | 'PEDIDO_INFORMACAO' | string
  nome: string
  categoria: 'OUVIDORIA' | 'LAI'
  permiteAnonimo: boolean
  permiteRecurso: boolean
  prazo: {
    origem: 'FEDERAL' | 'MUNICIPAL'
    diasResposta: number
    permiteProrrogacao: boolean
    diasProrrogacao: number | null
    diasInterposicaoRecurso: number | null
    diasJulgamentoRecurso: number | null
  }
}

export const tiposApi = {
  /** Exige login: cada tipo vem com o prazo vigente no município do token. */
  listar: async () => itensHal<TipoManifestacao>(await requisitar('/tipos-manifestacao'), 'tipos'),
}
