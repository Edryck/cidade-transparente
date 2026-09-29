# Design

## Ideia central

O visual parte de um objeto que todo brasileiro já conhece: o **comprovante de protocolo em papel**. Papel levemente quente, tinta azul-escura, número em fonte monoespaçada, carimbo em vermelho-tijolo. Não é nostalgia nem skeuomorfismo; é vocabulário. Quem já protocolou algo numa repartição reconhece a linguagem e confia nela. A interface é sóbria como um documento e ágil como uma ferramenta.

Duas partes do sistema, dois registros:

- **Cidadão** (público): largo, calmo, tipografia grande, um passo por vez. Parece um formulário bem diagramado.
- **Painel** (ouvidor, servidor, admin): compacto, tabular, teclado primeiro. Parece uma mesa de trabalho.

Os dois compartilham tokens, cor, tipografia e os mesmos componentes-assinatura (comprovante, régua de prazo, linha de tramitação).

## Tema

Modo claro como principal. Muitos usuários acessam de celular ao ar livre ou de computadores antigos de repartição; papel claro com tinta escura é o que melhor sobrevive a tela ruim. Modo escuro previsto nos tokens, fora do MVP.

O município aparece sempre no cabeçalho com nome e brasão (SVG, quando houver). O município pode definir uma **cor de destaque** própria, que substitui apenas `--cor-destaque`. Se não contrastar 4.5:1 com o fundo, o sistema cai para o padrão. Todo o resto da paleta é fixo.

## Cores

Tokens definidos como custom properties em `:root`. Nomes em português de propósito: são os mesmos usados no código.

| Token | Valor | Papel |
|-------|-------|-------|
| `--papel` | `#F5F0E4` | Fundo de página. Papel quente, não branco |
| `--papel-alto` | `#FBF8F0` | Superfície de card, campo, comprovante |
| `--papel-baixo` | `#E9E2D0` | Faixas, cabeçalho de tabela, área rebaixada |
| `--tinta` | `#17202E` | Texto principal, títulos |
| `--tinta-suave` | `#4A5567` | Texto secundário, metadados (contraste 7:1 sobre `--papel`) |
| `--linha` | `#17202E` a 14% | Divisores e bordas |
| `--destaque` | `#1F4E79` | Azul-tinta institucional. Ações primárias, links, foco. **Substituível por município** |
| `--destaque-forte` | `#163A5B` | Hover/pressed do destaque |
| `--carimbo` | `#A8341F` | Vermelho-tijolo. Prazo vencido, erro, marca do protocolo. Único vermelho do sistema |
| `--no-prazo` | `#2F6B4A` | Verde-musgo. Dentro do prazo, respondida |
| `--atencao` | `#8A5A00` | Âmbar escuro. Prazo vencendo (≤ 5 dias), prorrogado |
| `--sigilo` | `#5A3E7A` | Roxo apagado, usado **só** para indicar manifestação com identidade preservada |

Fundos de badge usam a cor semântica a 12% sobre `--papel-alto`; o texto usa a cor cheia. Nunca cor pura como fundo grande.

Regras:
- Vermelho é o carimbo. Aparece em prazo vencido, protocolo com pendência crítica e erros de validação. Não decora nada.
- Azul age. Se é clicável ou primário, é `--destaque`.
- Verde afirma uma coisa só: cumprido.
- Sem gradientes. Sem sombras coloridas. Sem branco puro (`#fff`) e sem preto puro (`#000`).

## Tipografia

Três famílias, cada uma com função.

| Família | Uso |
|---------|-----|
| **Newsreader** (serifada, texto e título) | Títulos de página, nome do município, título de manifestação, cabeçalho do comprovante. Dá o tom de documento |
| **Atkinson Hyperlegible Next** | Corpo, formulários, tabelas, botões. Desenhada para leitura em baixa visão e diferencia bem `I l 1` e `O 0` |
| **IBM Plex Mono** | Protocolo, chave de acesso, datas em tabela, contagem de dias, IDs |

Carregar apenas os pesos usados, self-hosted, `font-display: swap`, subset latino. Fallback: `Georgia, serif` / `system-ui, sans-serif` / `ui-monospace, monospace`.

| Estilo | Família | Peso | Tamanho (mobile → desktop) | Uso |
|--------|---------|------|-----------------|-----|
| Título 1 | Newsreader | 600 | 30 → 40px, entrelinha 1.15 | Título de página |
| Título 2 | Newsreader | 600 | 22 → 28px | Seções |
| Título 3 | Atkinson | 700 | 18 → 20px | Grupos de formulário, cards |
| Corpo | Atkinson | 400 | **18px** no fluxo do cidadão, 15–16px no painel | Texto corrido, entrelinha 1.55 |
| Rótulo | Atkinson | 700 | 14px, sem caixa alta | Label de campo |
| Metadado | Atkinson | 400 | 14px, `--tinta-suave` | Datas, autores |
| Protocolo | Plex Mono | 500 | 20 → 28px, `letter-spacing: 0.02em` | Número de protocolo em destaque |
| Dado | Plex Mono | 400 | 14px, `tabular-nums` | Colunas de data e de prazo |

Nada de caixa alta corrida em título ou botão. Nada de tracking largo em texto pequeno. Comprimento de linha de corpo entre 60 e 72 caracteres.

## Iconografia

Ícones de traço, 1.5px, cantos levemente arredondados, `currentColor`. Conjunto único (Lucide ou Phosphor Regular), nunca misturar. Ícone sempre acompanha texto quando o significado é crítico (prazo, sigilo, erro); sozinho só em ações universais (fechar, anexar, imprimir).

Sem emoji na interface. Sem ilustração de "pessoas simpáticas". Se precisar de ilustração vazia, um desenho de traço simples de papel, carimbo ou envelope, monocromático.

## Componentes

### Cabeçalho do município
- Faixa com brasão (40px, se houver), nome do município em Newsreader 600 e "Ouvidoria e Acesso à Informação" em metadado
- Fundo `--papel-alto`, borda inferior `--linha`
- Sem barra colorida de topo. Sem logo da plataforma em destaque: "Cidade Transparente" aparece só no rodapé, discreto

### Comprovante de protocolo (componente-assinatura)
- Cartão de papel (`--papel-alto`) com borda tracejada fina em cima e embaixo, evocando picote
- Topo: brasão + município + "Comprovante de manifestação"
- Centro: **protocolo** em Plex Mono 28px, com botão de copiar ao lado
- **Chave de acesso** em Plex Mono, com aviso fixo: "Esta chave aparece só agora. Guarde-a para acompanhar sua manifestação." Botões: Copiar, Baixar PDF, Imprimir
- Selo circular "PROTOCOLADO" em `--carimbo`, levemente rotacionado (−4°), com data e hora dentro. É o único elemento rotacionado da UI
- Em impressão: tudo em preto e branco, selo vira contorno, sem fundo
- Confirmação de que o usuário guardou (checkbox "Anotei ou salvei meu protocolo e minha chave") antes de habilitar "Concluir"

### Régua de prazo (componente-assinatura)
Mostra onde a manifestação está no prazo legal, na lista e no detalhe.
- Barra horizontal fina (6px) segmentada: prazo inicial e, se houver, prorrogação (30+30 ou 20+10)
- Preenchimento proporcional aos dias corridos, cor conforme o estado
- Ao lado, texto em Plex Mono: `faltam 12 dias` / `vence hoje` / `venceu há 3 dias`
- Estados, sempre com **texto + ícone + cor**:
  - `no prazo` — `--no-prazo`, ícone relógio
  - `vencendo` (≤ 5 dias) — `--atencao`, ícone alerta
  - `vencido` — `--carimbo`, ícone alerta preenchido
  - `prorrogado` — `--atencao`, segunda faixa da régua aparece ativa
  - `respondida no prazo` — `--no-prazo`, ícone check, régua congelada no ponto da resposta
- Em tabela, versão compacta: só o texto e o ícone, sem barra

### Linha de tramitação
- Lista vertical com trilho fino à esquerda (`--linha`) e um ponto por movimento
- Cada item: data/hora (Plex Mono, metadado), ação em texto claro ("Encaminhada para Secretaria de Obras"), responsável
- Ponto do movimento atual em `--destaque`, os anteriores em `--tinta-suave`, o estado "vencido" em `--carimbo`
- Resposta oficial aparece com fundo `--papel-baixo` e borda esquerda de 1px. Sem barra grossa colorida
- Mostrada igualmente ao cidadão e ao ouvidor; só campos internos (notas de triagem) ficam ocultos ao cidadão

### Botão primário
- Fundo `--destaque`, texto `--papel-alto`, Atkinson 700 16–18px
- Altura mínima 48px (56px no fluxo do cidadão), raio 6px
- Sem sombra. Pressed: `--destaque-forte`. Foco: anel de 3px `--destaque` a 40% com offset 2px

### Botão secundário e terciário
- Secundário: borda 1.5px `--tinta`, texto `--tinta`, fundo transparente
- Terciário: texto `--destaque`, sublinhado, sem caixa
- Ação destrutiva: secundário com texto e borda `--carimbo`, sempre com confirmação

### Campo de formulário
- Rótulo acima, sempre visível (nada de placeholder como rótulo), Atkinson 700 14px
- Ajuda em metadado abaixo do rótulo quando o termo é técnico ("Pedido LAI: solicitação de informação pública")
- Fundo `--papel-alto`, borda 1.5px `--linha` mais escura (30%), raio 6px, altura 48px
- Foco: borda `--destaque` 2px. Erro: borda `--carimbo`, mensagem em texto abaixo com ícone, nunca só cor
- Obrigatório: escrito "(obrigatório)" no rótulo, sem asterisco vermelho solto. Opcionais recebem "(opcional)"
- Mensagem de erro diz o que fazer: "Informe um e-mail no formato nome@exemplo.com", não "Campo inválido"

### Formulário de nova manifestação (fluxo do cidadão)
Um passo por tela, indicador de passo em texto ("Passo 2 de 4: Descreva o que aconteceu"), sem barra de progresso animada.
1. **Tipo** — cinco opções em lista vertical (não grade de ícones). Cada uma com nome e uma linha explicando quando usar. LAI e denúncia com aviso sobre prazos e sigilo
2. **Descrição** — textarea grande, contador discreto, dica do que ajuda a resolver mais rápido (local, data, secretaria)
3. **Identificação** — nome e contato, ou opção de manter identidade preservada quando permitido, com explicação curta do que isso significa
4. **Revisão e envio** — resumo legível de tudo, botão "Enviar manifestação"
Rascunho salvo no navegador a cada campo. Nada de "Voltar" que apague dados.

### Card de manifestação
- Fundo `--papel-alto`, borda 1px `--linha`, raio 6px, sem sombra
- Linha superior: protocolo (Plex Mono) + tipo em texto
- Título da manifestação em Newsreader 600
- Rodapé: secretaria atual, régua de prazo compacta
- Um card por manifestação; sem cards aninhados. Sem faixa lateral colorida

### Tabela do ouvidor
- Densa: linha de 44px, cabeçalho em `--papel-baixo`, texto de 15px
- Colunas padrão: Protocolo · Tipo · Assunto · Secretaria · Situação · Prazo. A coluna de prazo ordena por dias restantes por padrão, mais urgente primeiro
- Linhas com prazo vencido ganham um ícone e o texto do prazo em `--carimbo`; a linha em si não pinta de vermelho
- Filtros como faixa horizontal acima da tabela (tipo, secretaria, situação, prazo), com chips removíveis
- Atalhos de teclado (`j/k` navegar, `Enter` abrir, `/` buscar), listados num painel de ajuda
- Ação em lote só para encaminhar a mesma secretaria

### Badge de situação
Texto sempre presente; fundo cor a 12%, texto cor cheia, raio 4px, Atkinson 700 13px.
- `recebida` — `--tinta-suave`
- `em triagem` — `--destaque`
- `encaminhada` — `--destaque`
- `em análise` — `--destaque`
- `respondida` — `--no-prazo`
- `concluída` — `--tinta-suave` com ícone check
- `prorrogada` — `--atencao`
- `sigilo` — `--sigilo`, ícone cadeado

### Anexos
- Área de envio com botão explícito "Escolher arquivo" (arrastar é bônus, nunca única opção)
- Lista com nome, tamanho (Plex Mono) e remover
- Limites de tipo e tamanho escritos antes do envio, não depois do erro

### Alertas e avisos
- Faixa com fundo da cor semântica a 10%, borda 1px na cor a 40%, ícone e texto
- Nada de toast que some sozinho para informação que o usuário precisa ler (ex.: chave de acesso). Toast só para confirmações triviais

### Divisores
- `--linha`, 1px. Espaçamento faz o trabalho de separar antes da linha

## Telas e navegação

### Público (sem login)
1. **Início** — nome do município, dois botões grandes: "Registrar manifestação" e "Acompanhar por protocolo". Abaixo, texto curto explicando prazos e direitos. Sem hero, sem imagem de banco de fotos
2. **Registrar** — fluxo em 4 passos descrito acima
3. **Comprovante** — tela final com o componente de comprovante
4. **Acompanhar** — campos de protocolo e chave, resultado com dados da manifestação, régua de prazo e linha de tramitação
5. **Transparência** — indicadores públicos do município (quantidade por tipo, prazo médio, percentual respondido no prazo) em tabelas simples e um gráfico de barras sem enfeite. Números com o período explícito

### Autenticado (painel)
Layout: barra lateral estreita à esquerda com texto e ícone (ícone sozinho só se colapsada), cabeçalho do município no topo, conteúdo central.

- **Ouvidor**: Fila de triagem · Todas as manifestações · Prazos · Secretarias · Relatórios
- **Servidor**: Minhas manifestações (da secretaria) · Histórico
- **Admin município**: Usuários · Secretarias · Configurações do município (cor de destaque, brasão)
- **Admin plataforma**: Municípios · Usuários administradores · Saúde do serviço

Tela de **detalhe da manifestação** (mesma estrutura para todos os perfis, com ações conforme o perfil):
- Topo: protocolo, tipo, situação, régua de prazo
- Coluna principal: texto da manifestação, anexos, linha de tramitação, campo de resposta
- Coluna lateral (desktop): secretaria, responsável, datas legais, ações (encaminhar, responder, prorrogar, encerrar)
- Ações que alteram prazo (prorrogar) exigem justificativa escrita, registrada na tramitação

## Layout

- Fluxo do cidadão: coluna única, máximo 640px, padding lateral 20px no celular
- Painel: grid de 12 colunas, conteúdo máximo 1280px, barra lateral 232px
- Escala de espaçamento: 4, 8, 12, 16, 24, 32, 48, 64
- Espaço entre seções: 32–48px; entre campos: 20px
- Alvo de toque mínimo: 48×48px (56px no fluxo do cidadão)
- Raio: 6px em campos e cards, 4px em badges. Nada acima disso, e nada de pílula
- Sem sombras, com exceção de menu suspenso e modal (uma sombra única, neutra, 0 8px 24px `--tinta` a 12%)
- Breakpoints: 480 / 768 / 1100

## Movimento

- Transições de 120–160ms, `ease-out`. Só em cor, opacidade e transform
- O selo "PROTOCOLADO" entra com um único carimbar: escala 1.15 → 1 e opacidade, 180ms, uma vez
- Nenhum parallax, nenhum contador subindo, nenhum skeleton pulsante em loop (skeleton estático de cor de `--papel-baixo`)
- `prefers-reduced-motion`: tudo instantâneo, selo já aparece no lugar

## Impressão

Comprovante e detalhe da manifestação têm folha de estilo `@media print` própria: fundo branco, tinta preta, sem botões, protocolo e chave em destaque, URL de acompanhamento impressa em texto por extenso, QR code opcional para o link de acompanhamento (sem a chave embutida).

## Voz e microcopy

- Direta, no imperativo gentil: "Descreva o que aconteceu", "Guarde este protocolo"
- Explica termos legais na primeira aparição
- Mensagens de erro dizem o que houve e o que fazer
- Prazos sempre com unidade e tipo: "20 dias corridos", "faltam 3 dias úteis"
- Nada de "Ops", "Uau", "Tudo certo!", "Oba". Confirmação é: "Manifestação registrada. Protocolo 2026-9999901-000004."
- Estados vazios explicam o próximo passo: "Nenhuma manifestação aguardando triagem. Novas manifestações aparecem aqui assim que forem registradas."

## O que evitar (a lista de "cara de IA")

- Gradiente em fundo, botão ou texto
- Card com faixa colorida na lateral esquerda como marcador de status
- Grade de três ícones com título e frase em cima do fundo
- Hero centralizado com título enorme, subtítulo e dois botões
- Glassmorphism, blur de fundo, brilho, sombras coloridas
- Roxo/azul-néon como cor de marca (o único roxo é o de sigilo, apagado, e é semântico)
- Inter, Poppins ou Montserrat como fonte-padrão
- Emojis como ícones ou decoração
- Pílulas para tudo (badge, botão, campo)
- Ilustrações de banco de imagens com gente de cabeça grande
- Números de KPI gigantes com seta verde e "+12%" sem contexto
- Texto de marketing ("Transformando a gestão pública com inovação")
- Skeleton, spinner e microanimação em tudo

## Implementação (React + Vite)

- Tokens em `src/styles/tokens.css` como custom properties; tema do município injeta apenas `--destaque` e `--destaque-forte` no `<html>`
- CSS Modules ou CSS puro com camadas (`@layer base, componentes, utilitarios`); evitar biblioteca de componentes que imponha visual (MUI, Chakra) e evitar Tailwind com paleta padrão. Para comportamento acessível (dialog, menu, combobox), usar Radix UI ou React Aria sem estilo
- Componentes-assinatura em `src/componentes/`: `Comprovante`, `ReguaPrazo`, `LinhaTramitacao`, `BadgeSituacao`, `CampoFormulario`
- Fontes self-hosted em `public/fontes/`, apenas os pesos usados
- Cálculo de prazo mostrado no front é sempre derivado do dado que vem da API (data limite); a regra legal fica no backend
- Testes de contraste automatizados sobre os pares de tokens; teste de acessibilidade (axe) nas telas do fluxo do cidadão
- O protótipo navegável em `prototipo/` usa os mesmos tokens, copiados para um `<style>` único
