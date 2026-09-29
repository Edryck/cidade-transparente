import type { ManifestacaoCriada } from '../api/manifestacoes'

// Comprovante recém-criado, só em memória: a chave de acesso não vai para URL, histórico nem armazenamento.
// Se a página for recarregada, o comprovante some, e a tela de confirmação explica o que fazer.

export type ComprovanteTemporario = ManifestacaoCriada & { municipio: string; registradoEm: string }

let atual: ComprovanteTemporario | null = null

export const comprovanteTemporario = {
  guardar: (comprovante: ComprovanteTemporario) => {
    atual = comprovante
  },
  obter: () => atual,
  descartar: () => {
    atual = null
  },
}
