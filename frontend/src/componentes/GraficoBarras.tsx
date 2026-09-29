import type { Contagem } from '../api/relatorios'

/**
 * Gráfico de barras montado como tabela: o leitor de tela lê nome e valor de cada linha, e a barra é só
 * reforço visual (aria-hidden). Série única, então o título e a legenda dizem o que é medido.
 */
export function GraficoBarras({ titulo, legenda, dados }: { titulo: string; legenda: string; dados: Contagem[] }) {
  const maior = Math.max(1, ...dados.map((d) => d.total))
  const soma = dados.reduce((s, d) => s + d.total, 0)
  return (
    <figure className="grafico">
      <figcaption>
        <span className="grafico-titulo">{titulo}</span>
        <span className="metadado">{legenda}</span>
      </figcaption>
      {dados.length === 0 ? (
        <p className="metadado">Nenhum registro no período.</p>
      ) : (
        <table className="grafico-tabela">
          <thead className="so-leitor">
            <tr><th scope="col">Item</th><th scope="col">Quantidade</th><th scope="col">Percentual</th></tr>
          </thead>
          <tbody>
            {dados.map((d) => (
              <tr key={d.item}>
                <th scope="row">{d.item}</th>
                <td className="grafico-celula">
                  <span className="grafico-barra" aria-hidden style={{ width: `${(d.total / maior) * 100}%` }} />
                  <span className="mono">{d.total}</span>
                </td>
                <td className="mono grafico-percentual">{Math.round((d.total / soma) * 100)}%</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </figure>
  )
}
