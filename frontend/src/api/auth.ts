import { requisitar } from './cliente'

export type Perfil = 'ADMIN_PLATAFORMA' | 'ADMIN' | 'OUVIDOR' | 'SERVIDOR' | 'CIDADAO'

export type RespostaLogin = {
  token: string
  tipo: string
  expiraEmSegundos: number
  usuarioId: number
  nome: string
  perfil: Perfil
  municipioId: number | null
}

export type RegistroCidadao = {
  municipioId: number
  nome: string
  email: string
  senha: string
  cpf?: string
  telefone?: string
}

export const authApi = {
  login: (email: string, senha: string) =>
    requisitar<RespostaLogin>('/auth/login', { metodo: 'POST', corpo: { email, senha }, semToken: true }),
  registrarCidadao: (dados: RegistroCidadao) =>
    requisitar<RespostaLogin>('/auth/registro-cidadao', { metodo: 'POST', corpo: dados, semToken: true }),
}
