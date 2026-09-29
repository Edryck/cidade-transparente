import { itensHal, requisitar } from './cliente'

export type Contagem = { item: string; total: number }

export type Estatisticas = {
  calculadoEm: string
  recebidas: number
  anonimas: number
  prorrogadas: number
  porTipo: Contagem[]
  porSecretaria: Contagem[]
  porStatus: Contagem[]
  prazos: {
    respondidas: number
    respondidasNoPrazo: number
    respondidasForaDoPrazo: number
    percentualNoPrazo: number | null
    pendentesVencidas: number
    tempoMedioRespostaDias: number | null
  }
  lai: {
    pedidosRecebidos: number
    solicitantesDistintos: number
    atendidos: number
    parcialmenteAtendidos: number
    indeferidos: number
    informacaoInexistente: number
    semResposta: number
    recursosInterpostos: number
    recursosDeferidos: number
    recursosIndeferidos: number
  }
}

export type RelatorioGestao = {
  municipio: string
  ano: number
  situacao: 'RASCUNHO' | 'PUBLICADO'
  estatisticas: Estatisticas
  analisePontosRecorrentes: string | null
  providenciasAdotadas: string | null
  publicadoEm: string | null
}

export type RelatorioPublicado = { ano: number; publicadoEm: string }

export const relatoriosApi = {
  listarAnos: async (municipioId: number) =>
    itensHal<RelatorioPublicado>(
      await requisitar(`/municipios/${municipioId}/relatorios-gestao`, { semToken: true }), 'relatorios'),
  obterPorAno: (municipioId: number, ano: number) =>
    requisitar<RelatorioGestao>(`/municipios/${municipioId}/relatorios-gestao/${ano}`, { semToken: true }),
}
