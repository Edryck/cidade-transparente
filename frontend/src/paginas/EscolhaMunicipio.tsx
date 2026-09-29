import { useNavigate, useSearchParams } from 'react-router-dom'
import { municipiosApi } from '../api/municipios'
import { CabecalhoPagina } from '../componentes/CabecalhoPagina'
import { Carregando, ErroCarregamento, EstadoVazio } from '../componentes/Estados'
import { SeletorMunicipio } from '../componentes/SeletorMunicipio'
import { useMunicipio } from '../contextos/MunicipioContext'
import { useCarregar } from '../util/useCarregar'
import { useTitulo } from '../util/useTitulo'

export default function EscolhaMunicipio() {
  useTitulo('Escolha o município')
  const { municipio, escolher } = useMunicipio()
  const navegar = useNavigate()
  const [parametros] = useSearchParams()
  const { estado, tentarDeNovo } = useCarregar(municipiosApi.listarAtivos, [])

  return (
    <>
      <CabecalhoPagina titulo="Escolha o município"
        descricao="Selecione a prefeitura para continuar. A manifestação vai para ela, e as informações públicas mostradas são as dela." />
      {municipio && (
        <p className="municipio-atual">
          <span>Prefeitura selecionada agora: <strong>{municipio.nome} ({municipio.uf})</strong></span>
        </p>
      )}
      {estado.fase === 'carregando' && <Carregando texto="Carregando municípios…" />}
      {estado.fase === 'erro' && <ErroCarregamento erro={estado.erro} tentarDeNovo={tentarDeNovo} />}
      {estado.fase === 'pronto' && estado.dados.length === 0 && (
        <EstadoVazio titulo="Nenhuma prefeitura disponível">
          <p>Nenhuma prefeitura está usando o serviço no momento. Procure a prefeitura pelos canais presenciais.</p>
        </EstadoVazio>
      )}
      {estado.fase === 'pronto' && estado.dados.length > 0 && (
        <div>
          <SeletorMunicipio municipios={estado.dados} atualId={municipio?.id} onSelecionar={(m) => {
            escolher(m)
            const voltar = parametros.get('voltar')
            // Só caminhos internos: evita redirecionar para outro site
            navegar(voltar && voltar.startsWith('/') && !voltar.startsWith('//') ? voltar : '/')
          }} />
        </div>
      )}
    </>
  )
}
