import { FileSearch, FilePen } from 'lucide-react'
import { Link } from 'react-router-dom'
import { BotaoLink } from '../componentes/Botao'
import { CabecalhoPagina } from '../componentes/CabecalhoPagina'
import { useMunicipio } from '../contextos/MunicipioContext'
import { useTitulo } from '../util/useTitulo'

/** Início (DESIGN.md 44): serviço, município, as duas tarefas principais e o essencial sobre prazos. */
export default function Inicio() {
  const { municipio } = useMunicipio()
  useTitulo(`Ouvidoria de ${municipio?.nome ?? ''}`)
  return (
    <>
      <CabecalhoPagina titulo="Ouvidoria e Acesso à Informação"
        descricao={<>Prefeitura Municipal de {municipio?.nome} ({municipio?.uf}). Registre uma reclamação, denúncia, sugestão, elogio ou pedido de informação e acompanhe a resposta pelo protocolo.</>} />

      <div className="tarefas">
        <section className="superficie tarefa" aria-labelledby="tarefa-registrar">
          <FilePen aria-hidden size={28} strokeWidth={1.75} className="tarefa-icone" />
          <h2 id="tarefa-registrar">Registrar manifestação</h2>
          <p>Reclamação, sugestão, elogio e pedido de informação pedem conta. Denúncia pode ser anônima.</p>
          <BotaoLink to="/login?registrar=1">Registrar manifestação</BotaoLink>
          <Link to="/denuncia-anonima">Fazer denúncia anônima</Link>
        </section>
        <section className="superficie tarefa" aria-labelledby="tarefa-acompanhar">
          <FileSearch aria-hidden size={28} strokeWidth={1.75} className="tarefa-icone" />
          <h2 id="tarefa-acompanhar">Acompanhar protocolo</h2>
          <p>Veja a situação, o prazo e as respostas com o protocolo e a chave de acesso. Não é preciso ter conta.</p>
          <BotaoLink to="/acompanhar" variante="secundario">Acompanhar protocolo</BotaoLink>
        </section>
      </div>

      <section aria-labelledby="prazos">
        <h2 id="prazos">Prazos de resposta</h2>
        <ul className="lista-simples">
          <li><strong>Reclamação, denúncia, sugestão e elogio:</strong> até 30 dias corridos, prorrogáveis uma vez por mais 30 com justificativa (Lei 13.460, art. 16).</li>
          <li><strong>Pedido de informação:</strong> até 20 dias corridos, prorrogáveis uma vez por mais 10 (Lei 12.527, art. 11). Se o acesso for negado, cabe recurso.</li>
          <li>Se o prazo vencer em dia sem expediente, passa para o próximo dia útil. A prefeitura pode adotar prazos menores; o prazo exato aparece no comprovante.</li>
        </ul>
      </section>

      <section className="secao" aria-labelledby="mais">
        <h2 id="mais">Transparência e privacidade</h2>
        <ul className="lista-links">
          <li><Link to="/relatorios">Relatórios de gestão da ouvidoria</Link></li>
          <li><Link to="/privacidade">Aviso de privacidade</Link></li>
          <li><Link to="/municipios">Trocar município</Link></li>
        </ul>
      </section>
    </>
  )
}
