import { Paperclip, Trash2 } from 'lucide-react'
import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { FalhaApi } from '../../api/cliente'
import { contaApi } from '../../api/conta'
import { manifestacoesApi } from '../../api/manifestacoes'
import { tiposApi, type TipoManifestacao } from '../../api/tipos'
import { Alerta } from '../../componentes/Alerta'
import { Botao } from '../../componentes/Botao'
import { CampoFormulario } from '../../componentes/CampoFormulario'
import { Carregando, ErroCarregamento } from '../../componentes/Estados'
import { comprovanteTemporario } from '../../contextos/comprovante'
import { useMunicipio } from '../../contextos/MunicipioContext'
import { useCarregar } from '../../util/useCarregar'
import { useEnvioUnico } from '../../util/useEnvioUnico'
import { useTitulo } from '../../util/useTitulo'

const PASSOS = ['Escolha o tipo', 'Descreva o que aconteceu', 'Identificação', 'Revise e envie']
const RASCUNHO = 'ct.rascunho.manifestacao'
const LIMITE_DESCRICAO = 5000
const LIMITE_ANEXO = 5 * 1024 * 1024
const FORMATOS = ['application/pdf', 'image/png', 'image/jpeg']

/** Quando usar cada tipo, em linguagem simples. Textos de interface; as regras vêm da API. */
const EXPLICACAO: Record<string, string> = {
  RECLAMACAO: 'Um serviço público não funcionou como deveria ou foi mal prestado.',
  DENUNCIA: 'Uma irregularidade que precisa ser apurada, como uso indevido de dinheiro ou bens públicos. Pode ser enviada sem identificação.',
  SUGESTAO: 'Uma ideia para melhorar um serviço público.',
  ELOGIO: 'Reconhecer um bom atendimento ou um serviço bem prestado.',
  PEDIDO_INFORMACAO: 'Pedido LAI: solicitação de informação pública, como documentos, contratos ou dados da prefeitura. Não é preciso dizer por que você quer a informação (Lei 12.527, art. 10, § 3º).',
}

type Rascunho = { tipoId: string; assunto: string; descricao: string; anonima: boolean }

function lerRascunho(): Rascunho {
  try {
    return { tipoId: '', assunto: '', descricao: '', anonima: false, ...JSON.parse(sessionStorage.getItem(RASCUNHO) ?? '{}') }
  } catch {
    return { tipoId: '', assunto: '', descricao: '', anonima: false }
  }
}

function textoPrazo(t: TipoManifestacao): string {
  const p = t.prazo
  return `Resposta em até ${p.diasResposta} dias corridos` +
    (p.permiteProrrogacao && p.diasProrrogacao ? `, prorrogáveis uma vez por mais ${p.diasProrrogacao}.` : '.')
}

const tamanho = (bytes: number) => (bytes < 1024 * 1024 ? `${Math.ceil(bytes / 1024)} KB` : `${(bytes / 1024 / 1024).toFixed(1)} MB`)

/**
 * Tela 10: um passo por tela (DESIGN.md). O passo fica na URL, então o botão Voltar do navegador funciona e
 * nada se perde: o texto é salvo como rascunho nesta aba a cada alteração. Anexos ficam só na memória.
 */
export default function NovaManifestacao() {
  useTitulo('Nova manifestação')
  const navegar = useNavigate()
  const { municipio } = useMunicipio()
  const [parametros, setParametros] = useSearchParams()
  const passo = Math.min(4, Math.max(1, Number(parametros.get('passo')) || 1))
  const [dados, setDados] = useState<Rascunho>(lerRascunho)
  const [arquivos, setArquivos] = useState<File[]>([])
  const [erros, setErros] = useState<Record<string, string>>({})
  const [falha, setFalha] = useState('')
  const { enviando, executar } = useEnvioUnico()
  const refTitulo = useRef<HTMLHeadingElement>(null)
  const tipos = useCarregar(tiposApi.listar, [])
  const conta = useCarregar(passo >= 3 ? contaApi.obter : null, [passo >= 3])

  useEffect(() => {
    try {
      sessionStorage.setItem(RASCUNHO, JSON.stringify(dados))
    } catch {
      // sem armazenamento: os dados ficam só na tela
    }
  }, [dados])

  // A cada passo, o foco vai para o título: o leitor de tela anuncia onde a pessoa está
  useEffect(() => {
    refTitulo.current?.focus()
  }, [passo])

  const tipo = tipos.estado.fase === 'pronto' ? tipos.estado.dados.find((t) => String(t.id) === dados.tipoId) : undefined
  const anonima = Boolean(tipo?.permiteAnonimo && dados.anonima)

  function irPara(n: number) {
    setErros({})
    setFalha('')
    setParametros({ passo: String(n) })
  }

  function validar(n: number): Record<string, string> {
    const e: Record<string, string> = {}
    if (n >= 1 && !tipo) e.tipo = 'Escolha o tipo de manifestação'
    if (n >= 2) {
      if (!dados.assunto.trim()) e.assunto = 'Escreva em poucas palavras sobre o que é a manifestação'
      if (!dados.descricao.trim()) e.descricao = 'Descreva o que aconteceu'
    }
    return e
  }

  function continuar(evento: FormEvent) {
    evento.preventDefault()
    const e = validar(passo)
    setErros(e)
    if (Object.keys(e).length === 0) irPara(passo + 1)
  }

  function escolherArquivos(lista: FileList | null) {
    if (!lista) return
    const aceitos: File[] = []
    const recusados: string[] = []
    Array.from(lista).forEach((f) => {
      if (!FORMATOS.includes(f.type)) recusados.push(`${f.name}: envie PDF, PNG ou JPEG`)
      else if (f.size > LIMITE_ANEXO) recusados.push(`${f.name}: o limite é 5 MB`)
      else aceitos.push(f)
    })
    setArquivos((atuais) => [...atuais, ...aceitos])
    setErros((e) => ({ ...e, anexos: recusados.join('. ') }))
  }

  const enviar = () => executar(async () => {
    const e = validar(2)
    if (Object.keys(e).length) {
      setErros(e)
      irPara(e.tipo ? 1 : 2)
      return
    }
    if (!tipo || !municipio) return
    setFalha('')
    try {
      const corpo = { tipoId: tipo.id, assunto: dados.assunto.trim(), descricao: dados.descricao.trim() }
      const criada = anonima
        ? await manifestacoesApi.criarAnonima({ ...corpo, municipioId: municipio.id })
        : await manifestacoesApi.criarIdentificada(corpo)

      // Anexos só depois da criação e só na identificada: o envio de arquivo exige conta
      const anexos = []
      if (!anonima) {
        for (const arquivo of arquivos) {
          try {
            await manifestacoesApi.enviarAnexo(criada.id, arquivo)
            anexos.push({ nome: arquivo.name, enviado: true })
          } catch (erro) {
            anexos.push({ nome: arquivo.name, enviado: false, motivo: erro instanceof FalhaApi ? erro.message : 'falha no envio' })
          }
        }
      }
      comprovanteTemporario.guardar({
        ...criada,
        municipio: `${municipio.nome} (${municipio.uf})`,
        registradoEm: new Date().toISOString(),
        anexos,
      })
      try {
        sessionStorage.removeItem(RASCUNHO)
      } catch {
        // nada a limpar
      }
      navegar('/comprovante', { replace: true })
    } catch (erro) {
      const f = erro instanceof FalhaApi ? erro : new FalhaApi(500, 'Não foi possível enviar agora.')
      setErros(f.campos)
      setFalha(Object.keys(f.campos).length
        ? 'Não foi possível salvar sua manifestação. Verifique os campos indicados.'
        : `Não foi possível salvar sua manifestação. ${f.message}`)
    }
  })

  const cabecalho = (
    <>
      <h1>Nova manifestação</h1>
      <p className="passo-indicador" aria-live="polite">Passo {passo} de 4</p>
      <h2 ref={refTitulo} tabIndex={-1}>{PASSOS[passo - 1]}</h2>
    </>
  )

  if (tipos.estado.fase === 'carregando') return <>{cabecalho}<Carregando texto="Carregando os tipos de manifestação…" /></>
  if (tipos.estado.fase === 'erro') return <>{cabecalho}<ErroCarregamento erro={tipos.estado.erro} tentarDeNovo={tipos.tentarDeNovo} /></>

  return (
    <>
      {cabecalho}

      {passo === 1 && (
        <form onSubmit={continuar} noValidate className="formulario">
          <fieldset className="opcoes" aria-describedby={erros.tipo ? 'erro-tipo' : undefined}>
            <legend className="campo-rotulo">Tipo de manifestação <span className="campo-exigencia">(obrigatório)</span></legend>
            {tipos.estado.dados.map((t) => (
              <label key={t.id} className="opcao">
                <input type="radio" name="tipo" value={t.id} checked={dados.tipoId === String(t.id)}
                  onChange={() => setDados((d) => ({ ...d, tipoId: String(t.id) }))} />
                <span>
                  <span className="opcao-titulo">{t.nome}</span>
                  <span className="opcao-texto">{EXPLICACAO[t.codigo] ?? ''}</span>
                  <span className="opcao-texto metadado">{textoPrazo(t)}{t.permiteRecurso ? ' Se o acesso for negado, cabe recurso.' : ''}</span>
                </span>
              </label>
            ))}
          </fieldset>
          {erros.tipo && <p id="erro-tipo" className="campo-erro" role="alert">{erros.tipo}</p>}
          {tipo?.codigo === 'DENUNCIA' && (
            <Alerta tipo="sigilo" titulo="Denúncia e identidade">
              <p>No passo 3 você escolhe se quer se identificar. Mesmo identificada, a denúncia não revela seu nome à secretaria denunciada: só a ouvidoria vê quem enviou (Lei 13.608, art. 4º-B).</p>
            </Alerta>
          )}
          {tipo?.categoria === 'LAI' && (
            <Alerta tipo="info" titulo="Pedido de acesso à informação">
              <p>O pedido LAI exige identificação (Lei 12.527, art. 10). Se o acesso for negado, total ou parcialmente, você pode recorrer à ouvidoria.</p>
            </Alerta>
          )}
          <div className="acoes-passo">
            <Botao type="submit">Continuar</Botao>
          </div>
        </form>
      )}

      {passo === 2 && (
        <form onSubmit={continuar} noValidate className="formulario">
          <p>Tipo escolhido: <strong>{tipo?.nome}</strong>. <button type="button" className="botao-texto" onClick={() => irPara(1)}>Alterar</button></p>
          <CampoFormulario id="assunto" rotulo="Assunto" obrigatorio erro={erros.assunto} ajuda="Em poucas palavras. Exemplo: lâmpada queimada na Praça Central.">
            {(p) => <input {...p} type="text" maxLength={200} value={dados.assunto} onChange={(e) => setDados((d) => ({ ...d, assunto: e.target.value }))} />}
          </CampoFormulario>
          <CampoFormulario id="descricao" rotulo="Descrição" obrigatorio erro={erros.descricao}
            ajuda="Informe o que aconteceu, onde ocorreu e, se souber, quando aconteceu. Ajuda também citar a secretaria envolvida ou um número de atendimento anterior.">
            {(p) => (
              <>
                <textarea {...p} rows={10} maxLength={LIMITE_DESCRICAO} value={dados.descricao}
                  onChange={(e) => setDados((d) => ({ ...d, descricao: e.target.value }))} />
                <p className="metadado contador">{dados.descricao.length} de {LIMITE_DESCRICAO} caracteres</p>
              </>
            )}
          </CampoFormulario>
          <div className="campo">
            <p className="campo-rotulo" id="anexos-rotulo">Anexos <span className="campo-exigencia">(opcional)</span></p>
            <p className="campo-ajuda" id="anexos-ajuda">PDF, PNG ou JPEG, até 5 MB cada. Fotos e documentos ajudam a ouvidoria a entender o caso.</p>
            <label className="botao botao-secundario botao-arquivo">
              <Paperclip aria-hidden size={20} strokeWidth={1.5} />
              <span>Escolher arquivo</span>
              <input type="file" multiple accept={FORMATOS.join(',')} className="so-leitor" aria-describedby="anexos-ajuda"
                onChange={(e) => { escolherArquivos(e.target.files); e.target.value = '' }} />
            </label>
            {erros.anexos && <p className="campo-erro" role="alert">{erros.anexos}</p>}
            {arquivos.length > 0 && (
              <ul className="lista-anexos">
                {arquivos.map((f, i) => (
                  <li key={`${f.name}-${i}`}>
                    <span>{f.name} <span className="mono metadado">{tamanho(f.size)}</span></span>
                    <button type="button" className="botao-texto" onClick={() => setArquivos((a) => a.filter((_, j) => j !== i))}>
                      <Trash2 aria-hidden size={18} strokeWidth={1.5} /> Remover<span className="so-leitor"> {f.name}</span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
          <div className="acoes-passo">
            <Botao variante="secundario" type="button" onClick={() => irPara(1)}>Voltar</Botao>
            <Botao type="submit">Continuar</Botao>
          </div>
        </form>
      )}

      {passo === 3 && (
        <form onSubmit={continuar} noValidate className="formulario">
          {conta.estado.fase === 'carregando' && <Carregando texto="Carregando seus dados…" />}
          {conta.estado.fase === 'erro' && <ErroCarregamento erro={conta.estado.erro} tentarDeNovo={conta.tentarDeNovo} />}
          {conta.estado.fase === 'pronto' && (
            <>
              <dl className="dados dados-formulario">
                <dt>Nome</dt><dd>{conta.estado.dados.nome}</dd>
                <dt>E-mail</dt><dd>{conta.estado.dados.email}</dd>
                <dt>Telefone</dt><dd>{conta.estado.dados.telefone ?? 'não informado'}</dd>
              </dl>
              <p className="metadado">Dados da sua conta. Para corrigir, use <Link to="/minha-conta">Minha conta</Link>; o texto da manifestação fica salvo.</p>
            </>
          )}
          {tipo?.permiteAnonimo ? (
            <fieldset className="opcoes">
              <legend className="campo-rotulo">Como enviar <span className="campo-exigencia">(obrigatório)</span></legend>
              <label className="opcao">
                <input type="radio" name="identificacao" checked={!dados.anonima} onChange={() => setDados((d) => ({ ...d, anonima: false }))} />
                <span>
                  <span className="opcao-titulo">Com identificação</span>
                  <span className="opcao-texto">Seu nome e contato ficam visíveis só para a ouvidoria; a secretaria que responder não vê quem você é (Lei 13.460, art. 10, § 7º). A manifestação aparece em Minhas manifestações.</span>
                </span>
              </label>
              <label className="opcao">
                <input type="radio" name="identificacao" checked={dados.anonima} onChange={() => setDados((d) => ({ ...d, anonima: true }))} />
                <span>
                  <span className="opcao-titulo">Sem identificação</span>
                  <span className="opcao-texto">A prefeitura não saberá quem enviou. A manifestação não aparece em Minhas manifestações: acompanhe só pelo protocolo e pela chave. Não é possível anexar arquivos. Por lei, o endereço de rede do envio fica guardado por 6 meses e só é entregue com ordem judicial (Marco Civil, art. 15).</span>
                </span>
              </label>
            </fieldset>
          ) : (
            <Alerta tipo="sigilo" titulo="Identidade protegida">
              <p>Este tipo exige identificação. Seu nome e contato ficam visíveis só para a ouvidoria; a secretaria que responder não vê quem você é (Lei 13.460, art. 10, § 7º).</p>
            </Alerta>
          )}
          <div className="acoes-passo">
            <Botao variante="secundario" type="button" onClick={() => irPara(2)}>Voltar</Botao>
            <Botao type="submit">Continuar</Botao>
          </div>
        </form>
      )}

      {passo === 4 && (
        <div className="formulario">
          {falha && <Alerta tipo="erro"><p>{falha}</p></Alerta>}
          <dl className="revisao">
            <div>
              <dt>Tipo</dt>
              <dd>{tipo?.nome}<br /><span className="metadado">{tipo && textoPrazo(tipo)}</span></dd>
              <dd><button type="button" className="botao-texto" onClick={() => irPara(1)}>Alterar tipo</button></dd>
            </div>
            <div>
              <dt>Assunto</dt>
              <dd>{dados.assunto}{erros.assunto && <span className="campo-erro"> {erros.assunto}</span>}</dd>
              <dd><button type="button" className="botao-texto" onClick={() => irPara(2)}>Alterar assunto</button></dd>
            </div>
            <div>
              <dt>Descrição</dt>
              <dd className="texto-longo">{dados.descricao}{erros.descricao && <span className="campo-erro"> {erros.descricao}</span>}</dd>
              <dd><button type="button" className="botao-texto" onClick={() => irPara(2)}>Alterar descrição</button></dd>
            </div>
            <div>
              <dt>Anexos</dt>
              <dd>{arquivos.length === 0 ? 'Nenhum' : arquivos.map((f) => f.name).join(', ')}</dd>
              <dd><button type="button" className="botao-texto" onClick={() => irPara(2)}>Alterar anexos</button></dd>
            </div>
            <div>
              <dt>Identificação</dt>
              <dd>{anonima ? 'Sem identificação' : 'Com identificação, visível só para a ouvidoria'}</dd>
              <dd><button type="button" className="botao-texto" onClick={() => irPara(3)}>Alterar identificação</button></dd>
            </div>
          </dl>
          {anonima && arquivos.length > 0 && (
            <Alerta tipo="atencao"><p>Sem identificação não é possível anexar arquivos: os {arquivos.length} anexos escolhidos não serão enviados.</p></Alerta>
          )}
          <div className="acoes-passo">
            <Botao variante="secundario" type="button" onClick={() => irPara(3)} disabled={enviando}>Voltar</Botao>
            <Botao type="button" onClick={enviar} enviando={enviando}>Enviar manifestação</Botao>
          </div>
        </div>
      )}
    </>
  )
}
