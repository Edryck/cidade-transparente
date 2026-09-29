import { Link, Navigate } from 'react-router-dom'
import type { Perfil } from '../api/auth'
import { Alerta } from '../componentes/Alerta'
import { Botao } from '../componentes/Botao'
import { encerrarSessao, useSessao } from '../contextos/sessao'
import { useTitulo } from '../util/useTitulo'

const NOMES: Record<Perfil, string> = {
  CIDADAO: 'cidadão', OUVIDOR: 'ouvidor', SERVIDOR: 'servidor', ADMIN: 'administrador do município', ADMIN_PLATAFORMA: 'administrador da plataforma',
}

/**
 * Destino das rotas autenticadas (telas 9 em diante), que ainda não existem no frontend. Diz isso com clareza
 * em vez de simular uma tela: a sessão está ativa e a área pública continua disponível.
 */
export default function AreaPendente() {
  useTitulo('Área em desenvolvimento')
  const sessao = useSessao()
  if (!sessao) return <Navigate to="/login" replace />
  return (
    <>
      <h1>Olá, {sessao.nome}</h1>
      <Alerta tipo="info" titulo="Esta área ainda não está disponível">
        <p>Você entrou como {NOMES[sessao.perfil]}. As telas desta área ainda não fazem parte desta versão do sistema.</p>
      </Alerta>
      <p><Link to="/">Início</Link> · <Link to="/acompanhar">Acompanhar protocolo</Link></p>
      <Botao variante="secundario" type="button" onClick={encerrarSessao}>Sair</Botao>
    </>
  )
}
