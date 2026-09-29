import { Link } from 'react-router-dom'
import { relatoriosApi } from '../api/relatorios'
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
      <h1>Relatórios de gestão</h1>
      <p>A cada ano, a ouvidoria publica quantas manifestações recebeu, os motivos, os prazos cumpridos e as providências adotadas (Lei 13.460, art. 15).</p>
      <h2>Anos disponíveis</h2>
      {estado.fase === 'carregando' && <Carregando />}
      {estado.fase === 'erro' && <ErroCarregamento erro={estado.erro} tentarDeNovo={tentarDeNovo} />}
      {estado.fase === 'pronto' && estado.dados.length === 0 && (
        <EstadoVazio titulo="Ainda não há relatório publicado">
          <p>O relatório de um ano é publicado depois que ele termina. Volte depois ou fale com a ouvidoria.</p>
        </EstadoVazio>
      )}
      {estado.fase === 'pronto' && estado.dados.length > 0 && (
        <ul className="lista-anos">
          {estado.dados.map((r) => (
            <li key={r.ano}>
              <Link to={`/relatorios/${r.ano}`}>Relatório de {r.ano}</Link>
              <span className="metadado">publicado em <span className="mono">{dataCurta(diaDoInstante(r.publicadoEm))}</span></span>
            </li>
          ))}
        </ul>
      )}
    </>
  )
}
