import { CampoFormulario } from './CampoFormulario'

type Props = { valor: string; onChange: (valor: string) => void; erro?: string }

/** Protocolo em fonte monoespaçada, com o formato de exemplo. Aceita colar com espaços. */
export function CampoProtocolo({ valor, onChange, erro }: Props) {
  return (
    <CampoFormulario id="protocolo" rotulo="Protocolo" obrigatorio erro={erro} classe="campo-codigo" ajuda="Exemplo: 2026-9999901-000004">
      {(p) => <input {...p} type="text" inputMode="numeric" autoComplete="off" spellCheck={false}
        value={valor} onChange={(e) => onChange(e.target.value)} />}
    </CampoFormulario>
  )
}

/** Chave de acesso: maiúsculas automáticas; nunca vai para a URL nem para o armazenamento. */
export function CampoChaveAcesso({ valor, onChange, erro }: Props) {
  return (
    <CampoFormulario id="chave" rotulo="Chave de acesso" obrigatorio erro={erro} classe="campo-codigo"
      ajuda="No formato XXXX-XXXX-XXXX, recebida ao registrar. A chave não pode ser recuperada.">
      {(p) => <input {...p} type="text" autoComplete="off" autoCapitalize="characters" spellCheck={false}
        value={valor} onChange={(e) => onChange(e.target.value.toUpperCase())} />}
    </CampoFormulario>
  )
}
