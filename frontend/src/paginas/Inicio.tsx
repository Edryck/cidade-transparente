import { Link } from 'react-router-dom'
import { BotaoLink } from '../componentes/Botao'
import { useMunicipio } from '../contextos/MunicipioContext'
import { useTitulo } from '../util/useTitulo'

/** Início do DESIGN.md: o município, duas ações principais e o essencial sobre prazos e direitos. */
export default function Inicio() {
  const { municipio } = useMunicipio()
  useTitulo(`Ouvidoria de ${municipio?.nome ?? ''}`)
  return (
    <>
      <h1>Ouvidoria de {municipio?.nome}</h1>
      <p>Registre uma reclamação, denúncia, sugestão, elogio ou pedido de informação e acompanhe a resposta pelo número de protocolo.</p>

      <div className="acoes-principais">
        <BotaoLink to="/login?registrar=1">Registrar manifestação</BotaoLink>
        <BotaoLink to="/acompanhar" variante="secundario">Acompanhar por protocolo</BotaoLink>
      </div>

      <section className="secao">
        <h2>Denúncia sem identificação</h2>
        <p>Denúncias podem ser feitas sem se identificar. Você acompanha pelo protocolo e pela chave de acesso.</p>
        <p><Link to="/denuncia-anonima">Fazer denúncia anônima</Link></p>
      </section>

      <section className="secao">
        <h2>Prazos e direitos</h2>
        <ul>
          <li>Reclamação, denúncia, sugestão e elogio: resposta em até 30 dias corridos, prorrogáveis uma vez por mais 30 com justificativa (Lei 13.460, art. 16).</li>
          <li>Pedido de informação: resposta em até 20 dias corridos, prorrogáveis uma vez por mais 10 (Lei 12.527, art. 11). Se o acesso for negado, cabe recurso.</li>
          <li>Se o prazo vencer em dia sem expediente, ele passa para o próximo dia útil.</li>
          <li>A prefeitura pode adotar prazos menores. O prazo exato aparece no comprovante.</li>
        </ul>
        <p>
          <Link to="/privacidade">Aviso de privacidade</Link> · <Link to="/relatorios">Relatórios de gestão</Link>
        </p>
      </section>
    </>
  )
}
