import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { FalhaApi } from '../api/cliente'
import { manifestacoesApi } from '../api/manifestacoes'
import { Alerta } from '../componentes/Alerta'
import { Botao } from '../componentes/Botao'
import { CabecalhoPagina } from '../componentes/CabecalhoPagina'
import { CampoFormulario } from '../componentes/CampoFormulario'
import { comprovanteTemporario } from '../contextos/comprovante'
import { useMunicipio } from '../contextos/MunicipioContext'
import { useEnvioUnico } from '../util/useEnvioUnico'
import { useTitulo } from '../util/useTitulo'

/**
 * GET /tipos-manifestacao exige login, então esta tela não tem como descobrir o id da denúncia pela API.
 * O id vem da configuração (o seed cria DENUNCIA com id 2). É a limitação atual, mostrada na própria tela.
 */
const TIPO_DENUNCIA_ID = Number(import.meta.env.VITE_TIPO_DENUNCIA_ID ?? 2)
const RASCUNHO = 'ct.rascunho.denuncia'
const LIMITE_DESCRICAO = 5000

function lerRascunho(): { assunto: string; descricao: string } {
  try {
    return JSON.parse(sessionStorage.getItem(RASCUNHO) ?? '') as { assunto: string; descricao: string }
  } catch {
    return { assunto: '', descricao: '' }
  }
}

export default function DenunciaAnonima() {
  useTitulo('Denúncia anônima')
  const navegar = useNavigate()
  const { municipio } = useMunicipio()
  const [campos, setCampos] = useState(lerRascunho)
  const [erros, setErros] = useState<Record<string, string>>({})
  const [falha, setFalha] = useState('')
  const { enviando, executar } = useEnvioUnico()
  const refFalha = useRef<HTMLDivElement>(null)

  // Rascunho salvo a cada alteração, só nesta aba (sessionStorage): recarregar a página não perde o texto
  useEffect(() => {
    try {
      sessionStorage.setItem(RASCUNHO, JSON.stringify(campos))
    } catch {
      // sem armazenamento: o texto fica só na tela
    }
  }, [campos])

  function enviar(evento: FormEvent) {
    evento.preventDefault()
    executar(async () => {
    const novos: Record<string, string> = {}
    if (!campos.assunto.trim()) novos.assunto = 'Escreva em poucas palavras sobre o que é a denúncia'
    if (!campos.descricao.trim()) novos.descricao = 'Descreva o que aconteceu'
    setErros(novos)
    setFalha('')
    if (Object.keys(novos).length || !municipio) return

    try {
      const criada = await manifestacoesApi.criarAnonima({
        municipioId: municipio.id,
        tipoId: TIPO_DENUNCIA_ID,
        assunto: campos.assunto.trim(),
        descricao: campos.descricao.trim(),
      })
      comprovanteTemporario.guardar({
        ...criada,
        municipio: `${municipio.nome} (${municipio.uf})`,
        registradoEm: new Date().toISOString(),
      })
      try {
        sessionStorage.removeItem(RASCUNHO)
      } catch {
        // nada a limpar
      }
      navegar('/comprovante')
    } catch (erro) {
      const f = erro instanceof FalhaApi ? erro : new FalhaApi(500, 'Não foi possível enviar agora.')
      setErros(f.campos)
      setFalha(Object.keys(f.campos).length ? 'Confira os campos marcados.' : f.message)
      requestAnimationFrame(() => refFalha.current?.focus())
    }
    })
  }

  return (
    <>
      <CabecalhoPagina titulo="Registrar denúncia anônima" descricao="Você pode registrar uma denúncia sem criar uma conta."
        trilha={[{ texto: 'Início', para: '/' }, { texto: 'Denúncia anônima' }]} />

      <Alerta tipo="sigilo" titulo="Sua identidade não é registrada">
        <p>
          A prefeitura não saberá quem você é. Por lei, o endereço de rede do envio fica guardado por 6 meses e só é
          entregue com ordem judicial (Marco Civil, art. 15).
        </p>
      </Alerta>

      <form onSubmit={enviar} noValidate className="formulario superficie">
        {falha && (
          <div ref={refFalha} tabIndex={-1}><Alerta tipo="erro"><p>{falha}</p></Alerta></div>
        )}
        <dl className="dados dados-formulario">
          <dt>Município</dt>
          <dd>Prefeitura de {municipio?.nome} ({municipio?.uf}) <Link to="/municipios?voltar=/denuncia-anonima">Trocar</Link></dd>
          <dt>Tipo</dt>
          <dd>Denúncia</dd>
        </dl>

        <CampoFormulario id="assunto" rotulo="Assunto" obrigatorio erro={erros.assunto}
          ajuda="Em poucas palavras. Exemplo: obra sem placa na Rua 3.">
          {(p) => <input {...p} type="text" maxLength={200} value={campos.assunto}
            onChange={(e) => setCampos((c) => ({ ...c, assunto: e.target.value }))} />}
        </CampoFormulario>

        <CampoFormulario id="descricao" rotulo="Descreva o que aconteceu" obrigatorio erro={erros.descricao}
          ajuda="Onde, quando e o que você viu. Evite dados de outras pessoas que não sejam necessários para apurar.">
          {(p) => (
            <>
              <textarea {...p} rows={8} maxLength={LIMITE_DESCRICAO} value={campos.descricao}
                onChange={(e) => setCampos((c) => ({ ...c, descricao: e.target.value }))} />
              <p className="metadado contador" aria-live="off">
                {campos.descricao.length} de {LIMITE_DESCRICAO} caracteres
              </p>
            </>
          )}
        </CampoFormulario>

        <p className="metadado" style={{ margin: 0 }}>
          O texto fica salvo neste navegador até o envio. A denúncia anônima não aceita anexos: o envio de arquivos exige conta.
          Para outros tipos de manifestação, <Link to="/login?registrar=1">entre com sua conta</Link>.
        </p>

        <Botao type="submit" enviando={enviando}>Registrar denúncia</Botao>
      </form>

      <Alerta tipo="discreto" titulo="Limitação atual">
        <p>
          Neste protótipo, o tipo de denúncia é previamente identificado como DENUNCIA porque a API pública ainda não
          disponibiliza a lista de tipos.
        </p>
      </Alerta>
    </>
  )
}
