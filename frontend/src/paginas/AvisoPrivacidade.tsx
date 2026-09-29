import { Link } from 'react-router-dom'
import { privacidadeApi } from '../api/privacidade'
import { Carregando, ErroCarregamento } from '../componentes/Estados'
import { useMunicipio } from '../contextos/MunicipioContext'
import { dataCurta } from '../util/datas'
import { useCarregar } from '../util/useCarregar'
import { useTitulo } from '../util/useTitulo'

/** Tela 7: o aviso do município escolhido, lido como documento (títulos, listas, divisores). */
export default function AvisoPrivacidade() {
  useTitulo('Aviso de privacidade')
  const { municipio } = useMunicipio()
  const { estado, tentarDeNovo } = useCarregar(() => privacidadeApi.obter(municipio!.id), [municipio?.id])

  if (estado.fase === 'carregando') return <><h1>Aviso de privacidade</h1><Carregando /></>
  if (estado.fase === 'erro') return <><h1>Aviso de privacidade</h1><ErroCarregamento erro={estado.erro} tentarDeNovo={tentarDeNovo} /></>

  const a = estado.dados
  const versao = /^\d{4}-\d{2}-\d{2}$/.test(a.versao) ? dataCurta(a.versao) : a.versao
  return (
    <article className="documento">
      <h1>Aviso de privacidade</h1>
      <p>Como a prefeitura de {municipio?.nome} trata seus dados pessoais na ouvidoria e nos pedidos de acesso à informação.</p>

      <h2>Controlador</h2>
      <p>{a.controlador}</p>

      <h2>Operador</h2>
      <p>{a.operador}</p>

      <h2>Encarregado</h2>
      {a.encarregado ? (
        <p>{a.encarregado.nome}. Contato: <a href={`mailto:${a.encarregado.email}`}>{a.encarregado.email}</a></p>
      ) : (
        <p>Encarregado ainda não indicado</p>
      )}

      <h2>Tratamentos de dados</h2>
      {a.tratamentos.map((t) => (
        <section key={t.finalidade} className="documento-item">
          <h3>{t.finalidade}</h3>
          <dl className="dados">
            <dt>Dados</dt><dd>{t.dados.join(', ')}</dd>
            <dt>Base legal</dt><dd>{t.baseLegal}</dd>
            <dt>Por quanto tempo</dt><dd>{t.retencao}</dd>
          </dl>
        </section>
      ))}

      <h2>Compartilhamento</h2>
      <ul>{a.compartilhamento.map((c) => <li key={c}>{c}</li>)}</ul>

      <h2>Direitos do titular</h2>
      <ul>{a.direitos.map((d) => <li key={d.direito}>{d.direito}</li>)}</ul>

      <h2>Como exercer os direitos</h2>
      <ul>{a.direitos.map((d) => <li key={d.direito}><strong>{d.direito}:</strong> {d.comoExercer}</li>)}</ul>
      <p>Para consultar ou corrigir seus dados, entre e acesse <Link to="/minha-conta">Minha conta</Link>.</p>

      <h2>Prazo de atendimento</h2>
      <p>{a.prazoAtendimento}</p>

      <h2>Incidentes de segurança</h2>
      <p>{a.incidentes}</p>

      <h2>Versão do aviso</h2>
      <p className="mono">{versao}</p>
    </article>
  )
}
