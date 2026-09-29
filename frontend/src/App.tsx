import { lazy, Suspense } from 'react'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { Carregando } from './componentes/Estados'
import { LayoutPublico, RequerMunicipio } from './componentes/Layout'
import { MunicipioProvider } from './contextos/MunicipioContext'
import AcompanharProtocolo from './paginas/AcompanharProtocolo'
import AreaPendente from './paginas/AreaPendente'
import CadastroCidadao from './paginas/CadastroCidadao'
import ConfirmacaoRegistro from './paginas/ConfirmacaoRegistro'
import DenunciaAnonima from './paginas/DenunciaAnonima'
import EscolhaMunicipio from './paginas/EscolhaMunicipio'
import Inicio from './paginas/Inicio'
import Login from './paginas/Login'
import NaoEncontrada from './paginas/NaoEncontrada'

// Páginas de leitura longa e com gráficos: carregadas só quando abertas
const AvisoPrivacidade = lazy(() => import('./paginas/AvisoPrivacidade'))
const RelatoriosGestao = lazy(() => import('./paginas/RelatoriosGestao'))
const RelatorioAno = lazy(() => import('./paginas/RelatorioAno'))

export default function App() {
  return (
    <MunicipioProvider>
      <BrowserRouter>
        <Suspense fallback={<Carregando />}>
          <Routes>
            <Route element={<LayoutPublico />}>
              <Route path="/municipios" element={<EscolhaMunicipio />} />
              <Route path="/login" element={<Login />} />
              <Route path="/acompanhar" element={<AcompanharProtocolo />} />
              <Route path="/comprovante" element={<ConfirmacaoRegistro />} />
              <Route element={<RequerMunicipio />}>
                <Route path="/" element={<Inicio />} />
                <Route path="/cadastro" element={<CadastroCidadao />} />
                <Route path="/denuncia-anonima" element={<DenunciaAnonima />} />
                <Route path="/privacidade" element={<AvisoPrivacidade />} />
                <Route path="/relatorios" element={<RelatoriosGestao />} />
                <Route path="/relatorios/:ano" element={<RelatorioAno />} />
              </Route>
              {/* Áreas autenticadas (telas 9 em diante): rotas reservadas, ainda sem telas */}
              {['/minhas-manifestacoes', '/painel', '/secretaria', '/administracao', '/plataforma', '/minha-conta', '/manifestacoes/:id'].map((caminho) => (
                <Route key={caminho} path={caminho} element={<AreaPendente />} />
              ))}
              <Route path="*" element={<NaoEncontrada />} />
            </Route>
          </Routes>
        </Suspense>
      </BrowserRouter>
    </MunicipioProvider>
  )
}
