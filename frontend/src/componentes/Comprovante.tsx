import { Copy, Download, Printer } from 'lucide-react'
import { useId, useState } from 'react'
import { Link } from 'react-router-dom'
import type { ComprovanteTemporario } from '../contextos/comprovante'
import { dataCurta, dataHora, diaDaSemana } from '../util/datas'
import { Alerta } from './Alerta'
import { Botao } from './Botao'

type Props = {
  comprovante: ComprovanteTemporario
  brasao?: string | null
  /** Endereço completo da consulta pública, impresso por extenso. */
  urlAcompanhamento: string
  /** Presente só para quem está logado e pode abrir o detalhe. */
  linkDetalhe?: string
  onConcluir: () => void
}

/**
 * Comprovante de protocolo (componente-assinatura do DESIGN.md). Serve para a denúncia anônima e para a
 * manifestação identificada. A chave vem da API e só existe em memória; o protocolo nunca é gerado aqui.
 */
export function Comprovante({ comprovante, brasao, urlAcompanhamento, linkDetalhe, onConcluir }: Props) {
  const [copiado, setCopiado] = useState<'' | 'protocolo' | 'chave'>('')
  const [guardou, setGuardou] = useState(false)
  const idConfirmacao = useId()

  async function copiar(texto: string, qual: 'protocolo' | 'chave') {
    try {
      await navigator.clipboard.writeText(texto)
      setCopiado(qual)
    } catch {
      setCopiado('')
    }
  }

  return (
    <article className="comprovante" aria-labelledby="comprovante-titulo">
      <header className="comprovante-topo">
        {brasao && <img src={brasao} alt="" width={40} height={40} />}
        <div>
          <p className="comprovante-municipio">{comprovante.municipio}</p>
          <p id="comprovante-titulo" className="comprovante-titulo">Comprovante de manifestação</p>
        </div>
        <div className="selo" aria-hidden>
          <span>Protocolado</span>
          <span className="mono">{dataHora(comprovante.registradoEm)}</span>
        </div>
      </header>

      <section className="comprovante-bloco">
        <h2 className="comprovante-rotulo">Protocolo</h2>
        <div className="comprovante-linha">
          <p className="protocolo mono">{comprovante.protocolo}</p>
          <Botao variante="secundario" type="button" className="nao-imprimir"
            icone={<Copy aria-hidden size={20} strokeWidth={1.5} />}
            onClick={() => copiar(comprovante.protocolo, 'protocolo')}>
            {copiado === 'protocolo' ? 'Protocolo copiado' : 'Copiar protocolo'}
          </Botao>
        </div>
      </section>

      <section className="comprovante-bloco">
        <h2 className="comprovante-rotulo">Chave de acesso</h2>
        <p className="chave mono">{comprovante.chaveAcesso}</p>
        <Alerta tipo="atencao" titulo="Esta chave aparece só agora. Guarde-a para acompanhar sua manifestação.">
          <p>Esta chave será exibida somente agora e não poderá ser recuperada. A prefeitura guarda apenas uma versão cifrada dela.</p>
        </Alerta>
        <div className="comprovante-acoes nao-imprimir">
          <Botao variante="secundario" type="button" icone={<Copy aria-hidden size={20} strokeWidth={1.5} />}
            onClick={() => copiar(comprovante.chaveAcesso, 'chave')}>
            {copiado === 'chave' ? 'Chave copiada' : 'Copiar chave'}
          </Botao>
          <Botao variante="secundario" type="button" icone={<Download aria-hidden size={20} strokeWidth={1.5} />}
            onClick={() => window.print()} aria-describedby="dica-pdf">
            Baixar PDF
          </Botao>
          <Botao variante="secundario" type="button" icone={<Printer aria-hidden size={20} strokeWidth={1.5} />}
            onClick={() => window.print()}>
            Imprimir
          </Botao>
        </div>
        <p id="dica-pdf" className="metadado nao-imprimir">Para baixar em PDF, escolha "Salvar como PDF" na janela de impressão.</p>
        <p className="so-leitor" aria-live="polite">{copiado ? `${copiado === 'chave' ? 'Chave' : 'Protocolo'} copiado.` : ''}</p>
      </section>

      <section className="comprovante-bloco">
        <dl className="dados">
          <dt>Data limite para resposta</dt>
          <dd><span className="mono">{dataCurta(comprovante.dataLimite)}</span> ({diaDaSemana(comprovante.dataLimite)})</dd>
          <dt>Identificação</dt>
          <dd>{comprovante.anonima ? 'Anônima: a prefeitura não sabe quem enviou' : 'Identificada, visível só para a ouvidoria'}</dd>
          <dt>Acompanhe em</dt>
          <dd className="comprovante-url">{urlAcompanhamento}</dd>
        </dl>
        <p>{comprovante.orientacao}</p>
      </section>

      <footer className="comprovante-rodape nao-imprimir">
        <div className="confirmacao-guardou">
          <input id={idConfirmacao} type="checkbox" checked={guardou} onChange={(e) => setGuardou(e.target.checked)} />
          <label htmlFor={idConfirmacao}>Anotei ou salvei meu protocolo e minha chave</label>
        </div>
        <div className="comprovante-acoes">
          <Botao type="button" disabled={!guardou} onClick={onConcluir} aria-describedby={guardou ? undefined : 'dica-concluir'}>
            Concluir
          </Botao>
          <Link to={`/acompanhar?protocolo=${encodeURIComponent(comprovante.protocolo)}`}>Acompanhar protocolo</Link>
          {linkDetalhe && <Link to={linkDetalhe}>Abrir detalhe</Link>}
        </div>
        {!guardou && <p id="dica-concluir" className="metadado">Marque a confirmação acima para concluir.</p>}
      </footer>
    </article>
  )
}
