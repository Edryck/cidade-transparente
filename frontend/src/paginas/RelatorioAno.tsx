import { Link, useParams } from 'react-router-dom'
import { relatoriosApi } from '../api/relatorios'
import { CabecalhoPagina } from '../componentes/CabecalhoPagina'
import { Carregando, ErroCarregamento, EstadoVazio } from '../componentes/Estados'
import { GraficoBarras } from '../componentes/GraficoBarras'
import { TabelaResponsiva } from '../componentes/TabelaResponsiva'
import { useMunicipio } from '../contextos/MunicipioContext'
import { dataCurta, diaDoInstante } from '../util/datas'
import { useCarregar } from '../util/useCarregar'
import { useTitulo } from '../util/useTitulo'

const formatar = (n: number) => n.toLocaleString('pt-BR')

/** Relatório como documento informativo (DESIGN.md 26): período, indicadores com rótulo, gráficos com tabela, texto. */
export default function RelatorioAno() {
  const { ano } = useParams()
  useTitulo(`Relatório de gestão ${ano}`)
  const { municipio } = useMunicipio()
  const anoNumero = Number(ano)
  const { estado, tentarDeNovo } = useCarregar(
    Number.isInteger(anoNumero) ? () => relatoriosApi.obterPorAno(municipio!.id, anoNumero) : null,
    [municipio?.id, anoNumero])

  const trilha = [{ texto: 'Início', para: '/' }, { texto: 'Relatórios de gestão', para: '/relatorios' }, { texto: String(ano) }]
  if (!Number.isInteger(anoNumero) || (estado.fase === 'erro' && estado.erro.status === 404)) {
    return (
      <>
        <CabecalhoPagina titulo="Relatório não encontrado" trilha={trilha} />
        <EstadoVazio titulo={`Não há relatório publicado para ${ano}`}>
          <p>Confira o ano na lista de relatórios disponíveis.</p>
          <p><Link to="/relatorios">Ver relatórios disponíveis</Link></p>
        </EstadoVazio>
      </>
    )
  }
  if (estado.fase === 'carregando') return <><CabecalhoPagina titulo={`Relatório de gestão ${ano}`} trilha={trilha} /><Carregando /></>
  if (estado.fase === 'erro') return <><CabecalhoPagina titulo={`Relatório de gestão ${ano}`} trilha={trilha} /><ErroCarregamento erro={estado.erro} tentarDeNovo={tentarDeNovo} /></>

  const r = estado.dados
  const e = r.estatisticas
  const periodo = `De 01/01/${r.ano} a 31/12/${r.ano}`
  return (
    <div className="coluna-leitura">
      <CabecalhoPagina titulo={`Relatório de gestão ${r.ano}`} trilha={trilha}
        descricao={<>{r.municipio}. {r.publicadoEm && <>Publicado em <span className="mono">{dataCurta(diaDoInstante(r.publicadoEm))}</span>; os números não mudam depois da publicação.</>}</>} />

      <article className="documento">
        <section aria-labelledby="indicadores">
          <h2 id="indicadores">Indicadores do ano</h2>
          <p className="metadado">{periodo}.</p>
          <dl className="indicadores">
            <div className="indicador"><dt>Manifestações recebidas</dt><dd className="mono">{formatar(e.recebidas)}<small>{periodo}</small></dd></div>
            <div className="indicador"><dt>Anônimas</dt><dd className="mono">{formatar(e.anonimas)}<small>do total recebido</small></dd></div>
            <div className="indicador"><dt>Com prazo prorrogado</dt><dd className="mono">{formatar(e.prorrogadas)}<small>prorrogação única, com justificativa</small></dd></div>
            <div className="indicador">
              <dt>Respondidas no prazo</dt>
              <dd className="mono">
                {e.prazos.percentualNoPrazo === null ? '—' : `${e.prazos.percentualNoPrazo.toLocaleString('pt-BR')}%`}
                <small>{e.prazos.percentualNoPrazo === null ? 'sem respostas no período' : `${e.prazos.respondidasNoPrazo} de ${e.prazos.respondidas} respondidas`}</small>
              </dd>
            </div>
            <div className="indicador">
              <dt>Tempo médio de resposta</dt>
              <dd className="mono">
                {e.prazos.tempoMedioRespostaDias === null ? '—' : e.prazos.tempoMedioRespostaDias.toLocaleString('pt-BR')}
                <small>{e.prazos.tempoMedioRespostaDias === null ? 'sem respostas no período' : 'dias corridos'}</small>
              </dd>
            </div>
          </dl>
        </section>

        <section aria-labelledby="por-tipo">
          <h2 id="por-tipo">Por tipo</h2>
          <GraficoBarras titulo="Manifestações por tipo" legenda={`Quantidade recebida em ${r.ano}. Os tipos indicam os motivos das manifestações.`} dados={e.porTipo} />
        </section>

        <section aria-labelledby="por-secretaria">
          <h2 id="por-secretaria">Por secretaria</h2>
          <GraficoBarras titulo="Manifestações por secretaria" legenda={`Quantidade encaminhada em ${r.ano} a cada secretaria.`} dados={e.porSecretaria} />
        </section>

        <section aria-labelledby="lai">
          <h2 id="lai">Pedidos de acesso à informação</h2>
          <p>Estatística anual exigida pela Lei 12.527 (art. 30, III). Sobre os solicitantes, só a contagem: o sistema não coleta perfil de quem pede.</p>
          <TabelaResponsiva legenda={`Pedidos de acesso à informação em ${r.ano}`}>
            <thead><tr><th scope="col">Situação</th><th scope="col" className="numero">Pedidos</th></tr></thead>
            <tbody>
              <tr><th scope="row">Recebidos</th><td className="numero mono">{e.lai.pedidosRecebidos}</td></tr>
              <tr><th scope="row">Pessoas diferentes que pediram</th><td className="numero mono">{e.lai.solicitantesDistintos}</td></tr>
              <tr><th scope="row">Acesso concedido</th><td className="numero mono">{e.lai.atendidos}</td></tr>
              <tr><th scope="row">Acesso parcialmente concedido</th><td className="numero mono">{e.lai.parcialmenteAtendidos}</td></tr>
              <tr><th scope="row">Acesso negado</th><td className="numero mono">{e.lai.indeferidos}</td></tr>
              <tr><th scope="row">Informação inexistente</th><td className="numero mono">{e.lai.informacaoInexistente}</td></tr>
              <tr><th scope="row">Sem resposta</th><td className="numero mono">{e.lai.semResposta}</td></tr>
            </tbody>
          </TabelaResponsiva>
          <TabelaResponsiva legenda={`Recursos contra respostas a pedidos de informação em ${r.ano}`}>
            <thead><tr><th scope="col">Recursos</th><th scope="col" className="numero">Quantidade</th></tr></thead>
            <tbody>
              <tr><th scope="row">Apresentados</th><td className="numero mono">{e.lai.recursosInterpostos}</td></tr>
              <tr><th scope="row">Deferidos</th><td className="numero mono">{e.lai.recursosDeferidos}</td></tr>
              <tr><th scope="row">Indeferidos</th><td className="numero mono">{e.lai.recursosIndeferidos}</td></tr>
            </tbody>
          </TabelaResponsiva>
        </section>

        <section aria-labelledby="pontos">
          <h2 id="pontos">Pontos recorrentes</h2>
          <p className="texto-longo">{r.analisePontosRecorrentes}</p>
        </section>
        <section aria-labelledby="medidas">
          <h2 id="medidas">Medidas tomadas</h2>
          <p className="texto-longo">{r.providenciasAdotadas}</p>
        </section>
      </article>
      <p className="secao"><Link to="/relatorios">Ver todos os relatórios</Link></p>
    </div>
  )
}
