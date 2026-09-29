import { relatoriosApi } from '../api/relatorios'
import { BotaoLink } from '../componentes/Botao'
import { CabecalhoPagina } from '../componentes/CabecalhoPagina'
import { Carregando, ErroCarregamento, EstadoVazio } from '../componentes/Estados'
import { useMunicipio } from '../contextos/MunicipioContext'
import { dataCurta, diaDoInstante } from '../util/datas'
import { useCarregar } from '../util/useCarregar'
import { useTitulo } from '../util/useTitulo'

export default function RelatoriosGestao() {
  useTitulo('Relatórios de gestão')
  const { municipio } = useMunicipio()
  const { estado, tentarDeNovo } = useCarregar(() => relatoriosApi.listarAnos(municipio!.id), [municipio?.id])
  return (
    <>
      <CabecalhoPagina titulo="Relatórios de gestão"
        descricao="Consulte os relatórios públicos da ouvidoria e do acesso à informação deste município (Lei 13.460, art. 15)."
        trilha={[{ texto: 'Início', para: '/' }, { texto: 'Relatórios de gestão' }]} />
      <h2>Relatórios disponíveis</h2>
      {estado.fase === 'carregando' && <Carregando texto="Carregando relatórios…" />}
      {estado.fase === 'erro' && <ErroCarregamento erro={estado.erro} tentarDeNovo={tentarDeNovo} />}
      {estado.fase === 'pronto' && estado.dados.length === 0 && (
        <EstadoVazio titulo="Ainda não há relatório publicado">
          <p>O relatório de cada ano é publicado depois que ele termina. Volte depois ou fale com a ouvidoria.</p>
        </EstadoVazio>
      )}
      {estado.fase === 'pronto' && estado.dados.length > 0 && (
        <ul className="lista-anos">
          {estado.dados.map((r) => (
            <li key={r.ano}>
              <span>
                <span className="lista-anos-ano">{r.ano}</span>
                <span className="metadado"> · publicado em <span className="mono">{dataCurta(diaDoInstante(r.publicadoEm))}</span></span>
              </span>
              <BotaoLink to={`/relatorios/${r.ano}`} variante="secundario" aria-label={`Consultar relatório de ${r.ano}`}>Consultar</BotaoLink>
            </li>
          ))}
        </ul>
      )}
    </>
  )
}
