import { Link } from 'react-router-dom'
import { privacidadeApi } from '../api/privacidade'
import { CabecalhoPagina } from '../componentes/CabecalhoPagina'
import { Carregando, ErroCarregamento } from '../componentes/Estados'
import { useMunicipio } from '../contextos/MunicipioContext'
import { useSessao } from '../contextos/sessao'
import { dataCurta } from '../util/datas'
import { useCarregar } from '../util/useCarregar'
import { useTitulo } from '../util/useTitulo'

const SECOES = [
  ['controlador', 'Controlador'], ['operador', 'Operador'], ['encarregado', 'Encarregado'],
  ['tratamentos', 'Tratamentos de dados'], ['compartilhamento', 'Compartilhamento'], ['direitos', 'Direitos do titular'],
  ['exercer', 'Como exercer os direitos'], ['prazo', 'Prazo de atendimento'], ['incidentes', 'Incidentes de segurança'],
  ['versao', 'Versão do aviso'],
] as const

/** Tela 7: documento de leitura, com sumário, títulos e divisores; sem um card por parágrafo. */
export default function AvisoPrivacidade() {
  useTitulo('Aviso de privacidade')
  const { municipio } = useMunicipio()
  const sessao = useSessao()
  const { estado, tentarDeNovo } = useCarregar(() => privacidadeApi.obter(municipio!.id), [municipio?.id])
  const cabecalho = (
    <CabecalhoPagina titulo="Aviso de Privacidade"
      descricao={<>Entenda como a Prefeitura Municipal de {municipio?.nome} trata seus dados quando você usa a ouvidoria e o acesso à informação.</>}
      trilha={[{ texto: 'Início', para: '/' }, { texto: 'Aviso de privacidade' }]} />
  )

  if (estado.fase === 'carregando') return <>{cabecalho}<Carregando /></>
  if (estado.fase === 'erro') return <>{cabecalho}<ErroCarregamento erro={estado.erro} tentarDeNovo={tentarDeNovo} /></>

  const a = estado.dados
  const versao = /^\d{4}-\d{2}-\d{2}$/.test(a.versao) ? dataCurta(a.versao) : a.versao
  return (
    <div className="coluna-leitura">
      {cabecalho}
      <nav className="sumario" aria-labelledby="sumario-titulo">
        <h2 id="sumario-titulo">Nesta página</h2>
        <ol>{SECOES.map(([id, texto]) => <li key={id}><a href={`#${id}`}>{texto}</a></li>)}</ol>
      </nav>

      <article className="documento">
        <section id="controlador"><h2>Controlador</h2><p>{a.controlador}</p></section>
        <section id="operador"><h2>Operador</h2><p>{a.operador}</p></section>
        <section id="encarregado">
          <h2>Encarregado</h2>
          {a.encarregado
            ? <p>{a.encarregado.nome}. Contato: <a href={`mailto:${a.encarregado.email}`}>{a.encarregado.email}</a></p>
            : <p>Encarregado ainda não indicado.</p>}
        </section>
        <section id="tratamentos">
          <h2>Tratamentos de dados</h2>
          {a.tratamentos.map((t) => (
            <div key={t.finalidade} className="item-tratamento">
              <h3>{t.finalidade}</h3>
              <dl className="dados">
                <dt>Dados</dt><dd>{t.dados.join(', ')}</dd>
                <dt>Base legal</dt><dd>{t.baseLegal}</dd>
                <dt>Por quanto tempo</dt><dd>{t.retencao}</dd>
              </dl>
            </div>
          ))}
        </section>
        <section id="compartilhamento"><h2>Compartilhamento</h2><ul>{a.compartilhamento.map((c) => <li key={c}>{c}</li>)}</ul></section>
        <section id="direitos"><h2>Direitos do titular</h2><ul>{a.direitos.map((d) => <li key={d.direito}>{d.direito}</li>)}</ul></section>
        <section id="exercer">
          <h2>Como exercer os direitos</h2>
          <ul>{a.direitos.map((d) => <li key={d.direito}><strong>{d.direito}:</strong> {d.comoExercer}</li>)}</ul>
          <p>
            Para consultar ou corrigir seus dados, {sessao ? <Link to="/minha-conta">acesse Minha conta</Link> : <>entre e acesse <Link to="/minha-conta">Minha conta</Link></>}.
          </p>
        </section>
        <section id="prazo"><h2>Prazo de atendimento</h2><p>{a.prazoAtendimento}</p></section>
        <section id="incidentes"><h2>Incidentes de segurança</h2><p>{a.incidentes}</p></section>
        <section id="versao"><h2>Versão do aviso</h2><p className="mono">{versao}</p></section>
      </article>
    </div>
  )
}
