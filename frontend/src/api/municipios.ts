import { requisitar } from './cliente'

export type MunicipioResumo = { id: number; nome: string; uf: string }

export const municipiosApi = {
  listarAtivos: () => requisitar<MunicipioResumo[]>('/municipios/ativos', { semToken: true }),
}
