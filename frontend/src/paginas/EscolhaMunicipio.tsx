import { useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { municipiosApi } from '../api/municipios'
import { Botao } from '../componentes/Botao'
import { CampoBusca } from '../componentes/CampoBusca'
import { Carregando, ErroCarregamento, EstadoVazio } from '../componentes/Estados'
import { useMunicipio } from '../contextos/MunicipioContext'
import { useCarregar } from '../util/useCarregar'
import { useTitulo } from '../util/useTitulo'

const normalizar = (texto: string) => texto.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase()

export default function EscolhaMunicipio() {
  useTitulo('Escolha o município')
  const { escolher } = useMunicipio()
  const navegar = useNavigate()
  const [parametros] = useSearchParams()
  const [busca, setBusca] = useState('')
  const { estado, tentarDeNovo } = useCarregar(municipiosApi.listarAtivos, [])

  const lista = useMemo(() => {
    if (estado.fase !== 'pronto') return []
    const termo = normalizar(busca.trim())
    return estado.dados.filter((m) => normalizar(m.nome).includes(termo))
  }, [estado, busca])

  function selecionar(id: number, nome: string, uf: string) {
    escolher({ id, nome, uf })
    const voltar = parametros.get('voltar')
    // Só caminhos internos: evita redirecionar para outro site
    navegar(voltar && voltar.startsWith('/') && !voltar.startsWith('//') ? voltar : '/')
  }

  return (
    <>
      <h1>Escolha o município</h1>
      <p>A manifestação vai para a prefeitura escolhida, e as informações públicas mostradas são as dela.</p>

      {estado.fase === 'carregando' && <Carregando texto="Carregando municípios…" />}
      {estado.fase === 'erro' && <ErroCarregamento erro={estado.erro} tentarDeNovo={tentarDeNovo} />}
      {estado.fase === 'pronto' && estado.dados.length === 0 && (
        <EstadoVazio titulo="Nenhuma prefeitura disponível">
          <p>Nenhuma prefeitura está usando o serviço no momento. Procure a prefeitura pelos canais presenciais.</p>
        </EstadoVazio>
      )}
      {estado.fase === 'pronto' && estado.dados.length > 0 && (
        <>
          <CampoBusca id="busca-municipio" rotulo="Buscar pelo nome" valor={busca} onChange={setBusca}
            ajuda={`${estado.dados.length} ${estado.dados.length === 1 ? 'município disponível' : 'municípios disponíveis'}`} />
          <p className="so-leitor" role="status" aria-live="polite">
            {busca ? `${lista.length} ${lista.length === 1 ? 'resultado' : 'resultados'}` : ''}
          </p>
          {lista.length === 0 ? (
            <p>Nenhum município encontrado para "{busca}". Confira a grafia ou apague a busca para ver todos.</p>
          ) : (
            <ul className="lista-municipios">
              {lista.map((m) => (
                <li key={m.id}>
                  <span className="lista-municipios-nome">{m.nome} <span className="metadado">{m.uf}</span></span>
                  <Botao variante="secundario" type="button" aria-label={`Selecionar ${m.nome} (${m.uf})`}
                    onClick={() => selecionar(m.id, m.nome, m.uf)}>
                    Selecionar
                  </Botao>
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </>
  )
}
