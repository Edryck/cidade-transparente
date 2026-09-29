import { FileText } from 'lucide-react'
import type { ReactNode } from 'react'
import type { FalhaApi } from '../api/cliente'
import { Alerta } from './Alerta'
import { Botao } from './Botao'

/** Carregando: texto estático, sem spinner nem skeleton pulsante (DESIGN.md, Movimento). */
export function Carregando({ texto = 'Carregando…' }: { texto?: string }) {
  return (
    <p className="carregando" role="status" aria-live="polite">
      {texto}
    </p>
  )
}

export function ErroCarregamento({ erro, tentarDeNovo }: { erro: FalhaApi; tentarDeNovo?: () => void }) {
  return (
    <Alerta tipo="erro" titulo="Não foi possível carregar">
      <p>{erro.message}</p>
      {tentarDeNovo && (
        <Botao variante="secundario" type="button" onClick={tentarDeNovo}>
          Tentar de novo
        </Botao>
      )}
    </Alerta>
  )
}

/** Estado vazio: diz o que aconteceu e qual é o próximo passo. */
export function EstadoVazio({ titulo, children }: { titulo: string; children?: ReactNode }) {
  return (
    <div className="estado-vazio">
      <FileText aria-hidden size={32} strokeWidth={1.5} />
      <p className="estado-vazio-titulo">{titulo}</p>
      {children}
    </div>
  )
}
