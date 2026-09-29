import { Copy, Download, KeyRound, Printer } from 'lucide-react'
import { useId, useState } from 'react'
import { Link } from 'react-router-dom'
import type { ComprovanteTemporario } from '../contextos/comprovante'
import { dataCurta, dataHora, diaDaSemana, diaDoInstante, diasEntre, plural } from '../util/datas'
import { Botao, BotaoLink } from './Botao'

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
 * Comprovante de protocolo (DESIGN.md 19). Serve à denúncia anônima e à manifestação identificada. A chave vem
 * da API e só existe em memória; o protocolo nunca é gerado aqui. "Baixar comprovante" usa a impressão do
 * navegador (Salvar como PDF), sem biblioteca extra.
 */
export function Comprovante({ comprovante, brasao, urlAcompanhamento, linkDetalhe, onConcluir }: Props) {
  const [copiado, setCopiado] = useState<'' | 'protocolo' | 'chave'>('')
  const [guardou, setGuardou] = useState(false)
  const idConfirmacao = useId()
  const registro = diaDoInstante(comprovante.registradoEm)
  const prazo = diasEntre(registro, comprovante.dataLimite)

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
          <p className="comprovante-municipio">Prefeitura Municipal de {comprovante.municipio}</p>
          <p id="comprovante-titulo" className="comprovante-titulo">Comprovante de manifestação · Ouvidoria e Acesso à Informação</p>
        </div>
      </header>

      <div className="comprovante-corpo">
        <section>
          <h2 className="comprovante-rotulo">Protocolo</h2>
          <div className="comprovante-linha">
            <p className="protocolo">{comprovante.protocolo}</p>
            <Botao variante="secundario" type="button" className="nao-imprimir"
              icone={<Copy aria-hidden size={18} strokeWidth={2} />} onClick={() => copiar(comprovante.protocolo, 'protocolo')}>
              {copiado === 'protocolo' ? 'Protocolo copiado' : 'Copiar protocolo'}
            </Botao>
          </div>
        </section>

        <section className="chave-bloco">
          <h2 className="comprovante-rotulo">Chave de acesso</h2>
          <div className="comprovante-linha">
            <p className="chave">{comprovante.chaveAcesso}</p>
            <Botao variante="secundario" type="button" className="nao-imprimir"
              icone={<Copy aria-hidden size={18} strokeWidth={2} />} onClick={() => copiar(comprovante.chaveAcesso, 'chave')}>
              {copiado === 'chave' ? 'Chave copiada' : 'Copiar chave'}
            </Botao>
          </div>
          <p className="chave-aviso">
            <KeyRound aria-hidden size={18} strokeWidth={2} />
            <span>Esta chave será exibida somente agora e não poderá ser recuperada.</span>
          </p>
          <p className="metadado" style={{ margin: 0 }}>Guarde sua chave de acesso. Ela será necessária para acompanhar sua manifestação.</p>
        </section>

        <dl className="dados">
          <dt>Data do registro</dt>
          <dd className="mono">{dataHora(comprovante.registradoEm)}</dd>
          <dt>Prazo previsto</dt>
          <dd>{plural(prazo, 'dia corrido', 'dias corridos')}, até <span className="mono">{dataCurta(comprovante.dataLimite)}</span> ({diaDaSemana(comprovante.dataLimite)})</dd>
          <dt>Identificação</dt>
          <dd>{comprovante.anonima ? 'Anônima: a prefeitura não sabe quem enviou' : 'Identificada, visível só para a ouvidoria'}</dd>
          <dt>Acompanhe em</dt>
          <dd className="comprovante-url">{urlAcompanhamento}</dd>
        </dl>
        <p style={{ margin: 0 }}>{comprovante.orientacao}</p>

        <div className="comprovante-acoes nao-imprimir">
          <BotaoLink to={`/acompanhar?protocolo=${encodeURIComponent(comprovante.protocolo)}`}>Acompanhar manifestação</BotaoLink>
          <Botao variante="secundario" type="button" icone={<Printer aria-hidden size={18} strokeWidth={2} />} onClick={() => window.print()}>
            Imprimir
          </Botao>
          <Botao variante="secundario" type="button" icone={<Download aria-hidden size={18} strokeWidth={2} />}
            onClick={() => window.print()} aria-describedby="dica-pdf">
            Baixar comprovante
          </Botao>
        </div>
        <p id="dica-pdf" className="metadado nao-imprimir" style={{ margin: 0 }}>Para baixar em PDF, escolha "Salvar como PDF" na janela de impressão.</p>
        <p className="so-leitor" aria-live="polite">{copiado ? `${copiado === 'chave' ? 'Chave' : 'Protocolo'} copiado.` : ''}</p>
      </div>

      <footer className="comprovante-rodape nao-imprimir">
        <div className="confirmacao-guardou">
          <input id={idConfirmacao} type="checkbox" checked={guardou} onChange={(e) => setGuardou(e.target.checked)} />
          <label htmlFor={idConfirmacao}>Anotei ou salvei meu protocolo e minha chave</label>
        </div>
        <div className="comprovante-acoes">
          <Botao variante="secundario" type="button" disabled={!guardou} onClick={onConcluir}
            aria-describedby={guardou ? undefined : 'dica-concluir'}>
            Concluir
          </Botao>
          {linkDetalhe && <Link to={linkDetalhe}>Abrir detalhe da manifestação</Link>}
        </div>
        {!guardou && <p id="dica-concluir" className="metadado" style={{ margin: 0 }}>Marque a confirmação acima para concluir.</p>}
      </footer>
    </article>
  )
}
