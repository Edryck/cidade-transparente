import { useSearchParams } from 'react-router-dom'
import { manifestacoesApi } from '../../api/manifestacoes'
import { tiposApi } from '../../api/tipos'
import { Botao, BotaoLink } from '../../componentes/Botao'
import { CardManifestacao } from '../../componentes/CardManifestacao'
import { Carregando, ErroCarregamento, EstadoVazio } from '../../componentes/Estados'
import { FalhaApi } from '../../api/cliente'
import { useCarregar } from '../../util/useCarregar'
import { useTitulo } from '../../util/useTitulo'

const SITUACOES = [
  ['', 'Todas'], ['RECEBIDA', 'Recebida'], ['EM_ANALISE', 'Em análise pela ouvidoria'], ['ENCAMINHADA', 'Encaminhada à secretaria'],
  ['RESPONDIDA', 'Respondida'], ['EM_RECURSO', 'Em recurso'], ['ENCERRADA', 'Concluída'], ['ARQUIVADA', 'Arquivada'],
]

/** Tela 9. Filtros e página ficam na URL: voltar do detalhe devolve a mesma lista. */
export default function MinhasManifestacoes() {
  useTitulo('Minhas manifestações')
  const [parametros, setParametros] = useSearchParams()
  const status = parametros.get('situacao') ?? ''
  const tipoId = parametros.get('tipo') ?? ''
  const pagina = Math.max(0, Number(parametros.get('pagina') ?? 0) || 0)
  const lista = useCarregar(() => manifestacoesApi.listar({ status, tipoId, pagina }), [status, tipoId, pagina])
  const tipos = useCarregar(tiposApi.listar, [])
  const filtrando = Boolean(status || tipoId)

  function mudar(chave: string, valor: string) {
    const novos = new URLSearchParams(parametros)
    if (valor) novos.set(chave, valor)
    else novos.delete(chave)
    if (chave !== 'pagina') novos.delete('pagina')
    setParametros(novos)
  }

  return (
    <>
      <div className="titulo-com-acao">
        <h1>Minhas manifestações</h1>
        <BotaoLink to="/nova-manifestacao">Nova manifestação</BotaoLink>
      </div>
      <p>Acompanhe aqui as manifestações que você registrou neste município. As mais recentes aparecem primeiro.</p>

      <form className="filtros-cidadao" onSubmit={(e) => e.preventDefault()} aria-label="Filtrar manifestações">
        <div className="campo">
          <label htmlFor="filtro-situacao" className="campo-rotulo">Situação</label>
          <select id="filtro-situacao" value={status} onChange={(e) => mudar('situacao', e.target.value)}>
            {SITUACOES.map(([valor, texto]) => <option key={valor} value={valor}>{texto}</option>)}
          </select>
        </div>
        <div className="campo">
          <label htmlFor="filtro-tipo" className="campo-rotulo">Tipo</label>
          <select id="filtro-tipo" value={tipoId} onChange={(e) => mudar('tipo', e.target.value)}
            disabled={tipos.estado.fase !== 'pronto'}>
            <option value="">Todos</option>
            {tipos.estado.fase === 'pronto' && tipos.estado.dados.map((t) => <option key={t.id} value={t.id}>{t.nome}</option>)}
          </select>
        </div>
      </form>

      {lista.estado.fase === 'carregando' && <Carregando texto="Carregando suas manifestações…" />}
      {lista.estado.fase === 'erro' && (
        <ErroCarregamento erro={new FalhaApi(lista.estado.erro.status, `Não foi possível carregar suas manifestações. ${lista.estado.erro.message}`)}
          tentarDeNovo={lista.tentarDeNovo} />
      )}
      {lista.estado.fase === 'pronto' && lista.estado.dados.itens.length === 0 && (
        filtrando ? (
          <EstadoVazio titulo="Nenhuma manifestação com esses filtros">
            <p>Escolha outra situação ou outro tipo, ou mostre todas.</p>
            <Botao variante="secundario" type="button" onClick={() => setParametros({})}>Mostrar todas</Botao>
          </EstadoVazio>
        ) : (
          <EstadoVazio titulo="Você ainda não registrou nenhuma manifestação">
            <p>Registre uma manifestação para começar a acompanhar o atendimento da prefeitura.</p>
            <BotaoLink to="/nova-manifestacao">Registrar manifestação</BotaoLink>
          </EstadoVazio>
        )
      )}
      {lista.estado.fase === 'pronto' && lista.estado.dados.itens.length > 0 && (
        <>
          <p className="metadado" role="status">
            {lista.estado.dados.totalItens} {lista.estado.dados.totalItens === 1 ? 'manifestação' : 'manifestações'}
            {filtrando ? ' com esses filtros' : ''}
          </p>
          <ul className="lista-cards">
            {lista.estado.dados.itens.map((m) => <li key={m.id}><CardManifestacao m={m} /></li>)}
          </ul>
          {lista.estado.dados.totalPaginas > 1 && (
            <nav className="paginacao" aria-label="Páginas">
              <Botao variante="secundario" type="button" disabled={pagina === 0} onClick={() => mudar('pagina', String(pagina - 1))}>
                Página anterior
              </Botao>
              <span>Página {pagina + 1} de {lista.estado.dados.totalPaginas}</span>
              <Botao variante="secundario" type="button" disabled={pagina + 1 >= lista.estado.dados.totalPaginas}
                onClick={() => mudar('pagina', String(pagina + 1))}>
                Próxima página
              </Botao>
            </nav>
          )}
        </>
      )}
    </>
  )
}
