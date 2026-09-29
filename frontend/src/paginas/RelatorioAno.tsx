import { Link, useParams } from 'react-router-dom'
import { relatoriosApi } from '../api/relatorios'
import { Carregando, ErroCarregamento, EstadoVazio } from '../componentes/Estados'
import { GraficoBarras } from '../componentes/GraficoBarras'
import { useMunicipio } from '../contextos/MunicipioContext'
import { dataCurta, diaDoInstante } from '../util/datas'
import { useCarregar } from '../util/useCarregar'
import { useTitulo } from '../util/useTitulo'

// Número em Plex Mono; a frase de ausência fica na fonte do texto
const numero = (n: number | null, sufixo = '') =>
  n === null ? 'sem respostas no período' : <><span className="mono">{n.toLocaleString('pt-BR')}</span>{sufixo}</>

export default function RelatorioAno() {
  const { ano } = useParams()
  useTitulo(`Relatório de gestão ${ano}`)
  const { municipio } = useMunicipio()
  const anoNumero = Number(ano)
  const { estado, tentarDeNovo } = useCarregar(
    Number.isInteger(anoNumero) ? () => relatoriosApi.obterPorAno(municipio!.id, anoNumero) : null,
    [municipio?.id, anoNumero])

  const voltar = <p><Link to="/relatorios">Todos os relatórios</Link></p>
  if (!Number.isInteger(anoNumero) || (estado.fase === 'erro' && estado.erro.status === 404)) {
    return (
      <>
        <h1>Relatório de gestão {ano}</h1>
        <EstadoVazio titulo={`Não há relatório publicado para ${ano}`}>
          <p>Confira o ano na lista de relatórios disponíveis.</p>
          {voltar}
        </EstadoVazio>
      </>
    )
  }
  if (estado.fase === 'carregando') return <><h1>Relatório de gestão {ano}</h1><Carregando /></>
  if (estado.fase === 'erro') return <><h1>Relatório de gestão {ano}</h1><ErroCarregamento erro={estado.erro} tentarDeNovo={tentarDeNovo} /></>

  const r = estado.dados
  const e = r.estatisticas
  return (
    <article className="documento">
      <h1>Relatório de gestão {r.ano}</h1>
      <p>
        {r.municipio}. Período de 01/01/{r.ano} a 31/12/{r.ano}.
        {r.publicadoEm && <> Publicado em <span className="mono">{dataCurta(diaDoInstante(r.publicadoEm))}</span>; os números não mudam depois da publicação.</>}
      </p>

      <h2>Resumo</h2>
      <dl className="dados resumo">
        <dt>Manifestações recebidas</dt><dd>{numero(e.recebidas)}</dd>
        <dt>Anônimas</dt><dd>{numero(e.anonimas)}</dd>
        <dt>Com prazo prorrogado</dt><dd>{numero(e.prorrogadas)}</dd>
        <dt>Respondidas no prazo</dt>
        <dd>
          {e.prazos.percentualNoPrazo === null ? 'sem respostas no período' : <>
            <span className="mono">{e.prazos.percentualNoPrazo.toLocaleString('pt-BR')}%</span>
            {' '}({e.prazos.respondidasNoPrazo} de {e.prazos.respondidas} respondidas)</>}
        </dd>
        <dt>Tempo médio de resposta</dt><dd>{numero(e.prazos.tempoMedioRespostaDias, ' dias corridos')}</dd>
      </dl>

      <GraficoBarras titulo="Manifestações por tipo" legenda={`Quantidade recebida em ${r.ano}, por tipo (os motivos das manifestações).`} dados={e.porTipo} />
      <GraficoBarras titulo="Manifestações por secretaria" legenda={`Quantidade encaminhada em ${r.ano} a cada secretaria.`} dados={e.porSecretaria} />

      <h2>Pedidos de acesso à informação</h2>
      <p>Estatística anual exigida pela Lei 12.527 (art. 30, III). Sobre os solicitantes, só a contagem: o sistema não coleta perfil de quem pede.</p>
      <table className="tabela">
        <caption className="so-leitor">Pedidos de acesso à informação em {r.ano}</caption>
        <thead><tr><th scope="col">Situação</th><th scope="col">Pedidos</th></tr></thead>
        <tbody>
          <tr><th scope="row">Recebidos</th><td className="mono">{e.lai.pedidosRecebidos}</td></tr>
          <tr><th scope="row">Pessoas diferentes que pediram</th><td className="mono">{e.lai.solicitantesDistintos}</td></tr>
          <tr><th scope="row">Acesso concedido</th><td className="mono">{e.lai.atendidos}</td></tr>
          <tr><th scope="row">Acesso parcialmente concedido</th><td className="mono">{e.lai.parcialmenteAtendidos}</td></tr>
          <tr><th scope="row">Acesso negado</th><td className="mono">{e.lai.indeferidos}</td></tr>
          <tr><th scope="row">Informação inexistente</th><td className="mono">{e.lai.informacaoInexistente}</td></tr>
          <tr><th scope="row">Sem resposta</th><td className="mono">{e.lai.semResposta}</td></tr>
          <tr><th scope="row">Recursos apresentados</th><td className="mono">{e.lai.recursosInterpostos}</td></tr>
          <tr><th scope="row">Recursos deferidos</th><td className="mono">{e.lai.recursosDeferidos}</td></tr>
          <tr><th scope="row">Recursos indeferidos</th><td className="mono">{e.lai.recursosIndeferidos}</td></tr>
        </tbody>
      </table>

      <h2>Pontos recorrentes</h2>
      <p className="texto-longo">{r.analisePontosRecorrentes}</p>
      <h2>Providências adotadas</h2>
      <p className="texto-longo">{r.providenciasAdotadas}</p>
      {voltar}
    </article>
  )
}
