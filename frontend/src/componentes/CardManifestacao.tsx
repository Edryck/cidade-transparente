import { Link } from 'react-router-dom'
import type { ManifestacaoResumo } from '../api/manifestacoes'
import { dataCurta, diaDoInstante } from '../util/datas'
import { BadgeSituacao } from './BadgeSituacao'
import { ReguaPrazo } from './ReguaPrazo'

/**
 * Card do DESIGN.md: protocolo e tipo, título, secretaria e prazo compacto. O título é o link, então o card
 * inteiro não vira um botão gigante e o leitor de tela anuncia o assunto.
 */
export function CardManifestacao({ m }: { m: ManifestacaoResumo }) {
  return (
    <article className="card-manifestacao">
      <p className="card-linha-topo">
        <span className="mono card-protocolo">{m.protocolo}</span>
        <span>{m.tipo}</span>
      </p>
      <h2 className="card-titulo">
        <Link to={`/minhas-manifestacoes/${m.id}`}>{m.assunto}</Link>
      </h2>
      <p className="card-situacao"><BadgeSituacao status={m.status} /></p>
      <dl className="card-dados">
        <div><dt>Secretaria</dt><dd>{m.secretariaSigla ?? 'Ainda na ouvidoria'}</dd></div>
        <div><dt>Registrada em</dt><dd className="mono">{dataCurta(diaDoInstante(m.dataAbertura))}</dd></div>
      </dl>
      <ReguaPrazo compacta dataAbertura={m.dataAbertura} dataLimite={m.dataLimite} prorrogada={m.prorrogada} status={m.status} />
    </article>
  )
}
