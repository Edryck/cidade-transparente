import { requisitar } from './cliente'

export type MinhaConta = {
  id: number
  nome: string
  email: string
  perfil: string
  municipioId: number | null
  municipioNome: string | null
  ativo: boolean
  criadoEm: string
  cpf: string | null
  telefone: string | null
}

export type DadosConta = { nome: string; email: string; cpf?: string; telefone?: string }

export type Encerramento = { mensagem: string; fundamentacao: string[] }

/** Direitos do titular sobre a própria conta (LGPD art. 18) e troca de senha. */
export const contaApi = {
  obter: () => requisitar<MinhaConta>('/minha-conta'),
  atualizar: (dados: DadosConta) => requisitar<MinhaConta>('/minha-conta', { metodo: 'PUT', corpo: dados }),
  encerrar: () => requisitar<Encerramento>('/minha-conta', { metodo: 'DELETE' }),
  alterarSenha: (senhaAtual: string, novaSenha: string) =>
    requisitar<void>('/minha-conta/senha', { metodo: 'PUT', corpo: { senhaAtual, novaSenha } }),
}
