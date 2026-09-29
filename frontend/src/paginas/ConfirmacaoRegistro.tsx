import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Alerta } from '../componentes/Alerta'
import { Comprovante } from '../componentes/Comprovante'
import { EstadoVazio } from '../componentes/Estados'
import { comprovanteTemporario } from '../contextos/comprovante'
import { useMunicipio } from '../contextos/MunicipioContext'
import { useSessao } from '../contextos/sessao'
import { useTitulo } from '../util/useTitulo'

/** Tela 5. Serve à denúncia anônima (tela 4) e à manifestação identificada (tela 10). */
export default function ConfirmacaoRegistro() {
  useTitulo('Manifestação registrada')
  const navegar = useNavigate()
  const sessao = useSessao()
  const { municipio } = useMunicipio()
  const [comprovante] = useState(comprovanteTemporario.obter)

  if (!comprovante) {
    return (
      <>
        <h1>Comprovante indisponível</h1>
        <EstadoVazio titulo="O comprovante não está mais nesta tela">
          <p>
            Por segurança, a chave de acesso aparece uma única vez e não fica guardada no navegador. Se você anotou o
            protocolo e a chave, acompanhe a manifestação por eles.
          </p>
          <p><Link to="/acompanhar">Acompanhar protocolo</Link></p>
        </EstadoVazio>
      </>
    )
  }

  const cidadao = sessao?.perfil === 'CIDADAO'
  const falhas = comprovante.anexos?.filter((a) => !a.enviado) ?? []
  const url = `${window.location.origin}/acompanhar?protocolo=${encodeURIComponent(comprovante.protocolo)}`
  return (
    <>
      <h1>Manifestação registrada</h1>
      <p>Sua manifestação foi registrada. Guarde este protocolo e a chave de acesso{cidadao ? ': mesmo com a conta, eles são o seu comprovante' : ''}.</p>
      {comprovante.anexos && comprovante.anexos.length > 0 && (falhas.length === 0
        ? <Alerta tipo="sucesso"><p>{comprovante.anexos.length === 1 ? 'Anexo enviado' : `${comprovante.anexos.length} anexos enviados`}: {comprovante.anexos.map((a) => a.nome).join(', ')}.</p></Alerta>
        : <Alerta tipo="atencao" titulo="Alguns anexos não foram enviados">
            <ul>{falhas.map((a) => <li key={a.nome}>{a.nome}: {a.motivo}</li>)}</ul>
            <p>A manifestação foi registrada. Envie esses arquivos de novo pelo detalhe da manifestação.</p>
          </Alerta>)}
      <Comprovante
        comprovante={comprovante}
        brasao={municipio?.brasao}
        urlAcompanhamento={url}
        linkDetalhe={cidadao && !comprovante.anonima ? `/minhas-manifestacoes/${comprovante.id}` : undefined}
        onConcluir={() => {
          comprovanteTemporario.descartar()
          navegar(cidadao ? '/minhas-manifestacoes' : '/', { replace: true })
        }}
      />
    </>
  )
}
