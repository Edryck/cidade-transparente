# Product

## Register

product

## Brand

**Cidade Transparente**
Plataforma de ouvidoria, Lei de Acesso à Informação (LAI) e transparência para prefeituras de pequeno porte. Cada prefeitura usa a plataforma como se fosse o canal dela (nome, brasão, cor de destaque), mas por baixo é um sistema só, multi-tenant, mantido por quem opera a plataforma.

## Problema

Em município pequeno, "ouvidoria" costuma ser um e-mail, um caderno no balcão ou um formulário que ninguém acompanha. O cidadão reclama e não sabe se alguém leu. O servidor da secretaria recebe o pedido por WhatsApp, sem protocolo. O prazo legal (30+30 dias para ouvidoria, 20+10 para LAI) corre sem que ninguém esteja olhando o relógio, e a prefeitura descumpre a lei sem perceber. Quando vem um pedido LAI com prazo vencido, o problema já virou processo.

Sem protocolo, não há como provar que a manifestação existiu. Sem controle de prazo, a prefeitura fica exposta. Sem isolamento entre municípios, nenhum ouvidor pode confiar que os dados do cidadão estão protegidos.

## Solução

Uma aplicação web em que o cidadão registra a manifestação e recebe um **comprovante com protocolo e chave de acesso**. O ouvidor triagem e encaminha para a secretaria responsável. O servidor responde. O sistema controla o prazo legal em cada etapa e avisa antes de vencer. O cidadão acompanha tudo pelo protocolo, sem precisar de conta.

Backend Spring Boot com API REST/HATEOAS e JWT; frontend React + Vite; PostgreSQL com Flyway. Cada prefeitura enxerga só os próprios usuários, secretarias e manifestações.

## Usuários

Cinco perfis, cada um com contexto e escopo próprios.

**Cidadão** — quem registra e acompanha. Pode ser um aposentado abrindo a manifestação no celular do filho, ou alguém que só tem o telefone e dados móveis limitados. Escolaridade e familiaridade digital variam muito. Em geral só usa o sistema uma vez, num momento de irritação ou necessidade, e precisa sair dali com certeza de que foi registrado. Pode acompanhar por protocolo + chave de acesso, sem login.

**Servidor (secretaria)** — recebe manifestações encaminhadas para a sua secretaria e responde. Não é um usuário de sistema por vocação: tem outras dez tarefas no dia. Precisa ver rápido o que é dele, o que vence primeiro e o que falta responder. Só enxerga o que foi encaminhado à sua secretaria.

**Ouvidor** — a pessoa que faz a plataforma funcionar. Faz a triagem de tudo o que chega, encaminha, cobra as secretarias, decide prorrogação, encerra. Usa o sistema o dia inteiro, em desktop, e vive em listas e filtros. Tem que enxergar todos os prazos do município de relance.

**Admin (município)** — cadastra usuários e secretarias da própria prefeitura e ajusta configurações locais. Não vê o conteúdo das manifestações por virtude do cargo, apenas administra a estrutura.

**Admin da plataforma** — quem opera o sistema como um todo: cria municípios, o primeiro admin de cada um, monitora saúde do serviço. Único perfil que atravessa tenants, e por isso o mais restrito em escopo de dados.

## Propósito do produto

Fazer a lei funcionar em município onde não há equipe para isso. A ouvidoria e a LAI existem por lei; a plataforma torna o cumprimento o caminho mais fácil.

Sucesso:
- O cidadão registra uma manifestação em menos de 3 minutos, no celular, e sai com o protocolo em mãos
- Nenhum prazo legal vence sem que o ouvidor tenha sido avisado antes
- O ouvidor sabe, ao abrir o sistema, o que precisa fazer hoje, sem procurar
- Um servidor responde sem precisar de treinamento além de uma explicação de dois minutos
- Nenhuma manifestação de um município aparece para outro, nem por acidente

## Jornada de ponta a ponta

1. **Abertura**: cidadão escolhe o tipo (reclamação, denúncia, sugestão, elogio ou pedido LAI), descreve, anexa arquivos se quiser, identifica-se ou opta pelo sigilo quando a lei permitir
2. **Comprovante**: o sistema devolve protocolo (`2026-9999901-000004`) e chave de acesso, mostrada uma única vez, com opção de imprimir, baixar PDF ou copiar
3. **Triagem**: o ouvidor vê a manifestação, classifica e encaminha para a secretaria competente
4. **Resposta**: o servidor da secretaria responde (com anexos, se preciso) ou pede prorrogação com justificativa
5. **Prazo**: o relógio legal corre em cada etapa; aproximou-se do vencimento, o ouvidor e a secretaria são avisados
6. **Acompanhamento**: o cidadão consulta pelo protocolo e vê a tramitação completa, com data e responsável de cada movimento
7. **Encerramento**: manifestação respondida e concluída; o histórico permanece consultável

## Stack

| Camada | Tecnologias |
|--------|-------------|
| Backend | Java 21, Spring Boot 3 (Web, Data JPA, Security, HATEOAS, Validation) |
| Banco | PostgreSQL + Flyway |
| Autenticação | JWT (HS256) |
| Docs da API | springdoc-openapi (Swagger UI) |
| Frontend | React + Vite |

## Personalidade da marca

Séria, clara, acessível, confiável, direta. Fala como um documento oficial bem escrito, não como um app de consumo nem como um balcão de repartição.

A interface deve passar a sensação de **um protocolo de papel que ganhou memória**: o carimbo, o número, a data, o responsável e o prazo estão sempre à vista. A confiança vem da precisão, e não de gradiente, mascote ou frase de efeito.

Tom de texto: português simples, frases curtas, verbos no lugar de substantivos abstratos. "Sua manifestação foi registrada" em vez de "Registro efetuado com sucesso". Nunca "Ops!", nunca exclamação decorativa, nunca linguagem de startup. Nada de jargão jurídico sem explicação ao lado.

## Anti-referências

- Portal de prefeitura padrão: banner de carrossel, foto do prefeito, dezessete menus, tudo em caixa alta
- Painel SaaS genérico: gradiente, cards de KPI com seta verde subindo, ilustração de gente segurando gráfico
- App de consumo ou gamificado: pontos, confete, mensagem simpática demais para um pedido de acesso à informação
- Chatbot flutuante como porta de entrada: manifestação é registro formal, não conversa
- Cópia descarada do gov.br: usamos a seriedade e a acessibilidade dele como referência, mas não o mesmo visual

## Princípios de design

1. **O protocolo é o produto** — número, chave, data e prazo aparecem com destaque em toda tela em que a manifestação aparece. É a coisa mais importante que o cidadão leva embora.
2. **Prazo nunca é surpresa** — o estado do prazo (no prazo, vencendo, vencido, prorrogado) tem forma própria e aparece na lista, no detalhe e no e-mail. Nunca só por cor.
3. **Quem chega uma vez precisa de zero atrito; quem chega todo dia precisa de densidade** — o fluxo do cidadão é largo, calmo, um passo por vez. O painel do ouvidor é compacto, com tabela e teclado.
4. **Sem cadastro para o que a lei não exige** — consultar por protocolo não pede conta. Cada campo obrigatório precisa de uma justificativa.
5. **Isolamento é visível** — o município aparece sempre no topo (nome e brasão), para que nenhum usuário tenha dúvida de em qual prefeitura está atuando.
6. **Honestidade sobre o que acontece** — se a manifestação foi encaminhada mas ninguém abriu, o cidadão vê isso. Se o prazo foi prorrogado, vê a justificativa. Transparência começa pela própria ferramenta.

## Acessibilidade e inclusão

- WCAG AA como mínimo, AAA para texto do fluxo do cidadão sempre que possível
- Público inclui idosos e pessoas com baixa visão: texto base de 17–18px, contraste alto, alvos de toque ≥ 48px
- Funciona bem em celular de entrada, com rede lenta: JS enxuto, sem fonte pesada, formulário que não perde o que foi digitado
- Navegação completa por teclado e leitor de tela; estados de prazo têm texto, ícone e cor
- Português (Brasil) como idioma único; datas em dd/mm/aaaa, prazos em dias corridos ou úteis sempre indicados
- Movimento reduzido respeitado (`prefers-reduced-motion`)
- LGPD: coleta mínima, denúncia com opção de identidade preservada quando a lei permitir, retenção e anexos conforme `docs/lgpd.md`
- Impressão é cidadã de primeira classe: o comprovante precisa sair bem numa impressora comum, em preto e branco
