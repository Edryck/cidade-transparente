import { useMemo, useState } from 'react'
import type { MunicipioResumo } from '../api/municipios'
import { Botao } from './Botao'
import { CampoBusca } from './CampoBusca'

const normalizar = (texto: string) => texto.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase()

/** Busca por nome (sem acento nem maiúscula) e lista simples com a ação "Selecionar". */
export function SeletorMunicipio({ municipios, atualId, onSelecionar }: {
  municipios: MunicipioResumo[]; atualId?: number; onSelecionar: (m: MunicipioResumo) => void
}) {
  const [busca, setBusca] = useState('')
  const lista = useMemo(() => {
    const termo = normalizar(busca.trim())
    return municipios.filter((m) => normalizar(m.nome).includes(termo))
  }, [municipios, busca])

  return (
    <>
      <CampoBusca id="busca-municipio" rotulo="Buscar município" valor={busca} onChange={setBusca}
        ajuda={`${municipios.length} ${municipios.length === 1 ? 'prefeitura disponível' : 'prefeituras disponíveis'}`} />
      <p className="so-leitor" role="status" aria-live="polite">
        {busca ? `${lista.length} ${lista.length === 1 ? 'resultado' : 'resultados'}` : ''}
      </p>
      {lista.length === 0 ? (
        <p className="estado-vazio" style={{ marginTop: 16 }}>
          Nenhum município encontrado para "{busca}". Confira a grafia ou apague a busca para ver todos.
        </p>
      ) : (
        <ul className="lista-municipios">
          {lista.map((m) => (
            <li key={m.id}>
              <span>
                <span className="lista-municipios-nome">Prefeitura de {m.nome}</span>
                <span className="lista-municipios-uf">{m.uf}</span>
              </span>
              {m.id === atualId ? (
                <Botao variante="secundario" type="button" onClick={() => onSelecionar(m)} aria-label={`Continuar com ${m.nome} (${m.uf}), já selecionado`}>
                  Selecionado
                </Botao>
              ) : (
                <Botao variante="secundario" type="button" onClick={() => onSelecionar(m)} aria-label={`Selecionar ${m.nome} (${m.uf})`}>
                  Selecionar
                </Botao>
              )}
            </li>
          ))}
        </ul>
      )}
    </>
  )
}
