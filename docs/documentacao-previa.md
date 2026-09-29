# Cidade Transparente API: documentação prévia (exame de TDS)

Plataforma de ouvidoria, LAI e transparência para prefeituras pequenas. Aqui o foco é a **API REST em Spring Boot** e o frontend que consome ela. O plano de negócio é um trabalho separado.

Entrega: **01/11/2026** (domingo). Meta real: fechar tudo até **30/10** e deixar o fim de semana de folga.

---

## 1. O que vai ser feito

Uma API multi-município: cada prefeitura (tenant) tem seus usuários, secretarias e manifestações. O cidadão registra uma manifestação (reclamação, sugestão, denúncia, elogio ou pedido de informação via LAI), o ouvidor encaminha para a secretaria certa, o servidor responde, e o sistema controla os prazos legais em cada etapa.

**Por que esse domínio serve pro exame:** tem fluxo com estados (máquina de estados), regra de negócio de verdade (cálculo e prorrogação de prazo), perfis com permissões diferentes (bom pra mostrar JWT) e relacionamentos ricos sem precisar forçar entidade.

## 2. Requisitos do exame e como cada um é atendido

| Requisito | Como |
|---|---|
| API REST com Spring Boot | Spring Boot 3, Java 21 (usar 17 se a aula usar) |
| ORM com Spring Data JPA | Entidades JPA + repositories, Hibernate por baixo |
| Versionamento de banco | Flyway, migrations em `db/migration` |
| Segurança com JWT | Spring Security, login gera token, filtro valida em toda requisição |
| Testes de todos os endpoints | Coleção Postman (ou Insomnia) com scripts de assert, um por endpoint |
| Richardson | Nível 2 em tudo e nível 3 (HATEOAS) nos recursos principais |
| Frontend | Aplicação web consumindo a API (ver seção 7) |
| 3 vídeos | Arquitetura/implementação, testes dos endpoints, código + frontend |
| README | Passo a passo de execução e configuração |

## 3. Stack

- **Backend:** Java 21, Spring Boot 3, Spring Web, Spring Data JPA, Spring Security, Spring HATEOAS, Bean Validation
- **Banco:** PostgreSQL (Docker Compose para subir local)
- **Migrations:** Flyway
- **JWT:** biblioteca jjwt (ou o resource server do Spring, o que for mais simples de explicar no vídeo)
- **Senha:** BCrypt
- **Docs da API (extra barato):** springdoc-openapi (Swagger UI)
- **Testes:** Postman/Insomnia (obrigatório). JUnit de integração só se sobrar tempo
- **Frontend:** recomendado Flutter Web (stack que já está no projeto pessoal e evita aprender framework novo em cima do prazo). React + Vite é a alternativa

## 4. Modelo de domínio (14 entidades)

Meta do exame é 10 ou mais. Ficam 12 no núcleo e 2 de reserva que podem cair se o tempo apertar.

| # | Entidade | Papel |
|---|---|---|
| 1 | `Municipio` | Tenant. Tudo pertence a um município |
| 2 | `Perfil` | Papel de acesso (ADMIN_PLATAFORMA, ADMIN, OUVIDOR, SERVIDOR, CIDADAO) |
| 3 | `Usuario` | Login, senha, município, perfil |
| 4 | `Secretaria` | Órgão/departamento que responde manifestações |
| 5 | `Cidadao` | Dados de quem manifesta (pode ser anônimo) |
| 6 | `TipoManifestacao` | Reclamação, denúncia, sugestão, elogio, pedido LAI. Define a regra de prazo |
| 7 | `Manifestacao` | Entidade central: protocolo, assunto, descrição, status, prazo |
| 8 | `Anexo` | Arquivos ligados à manifestação |
| 9 | `Tramite` | Histórico de cada mudança de status (quem, quando, o quê) |
| 10 | `Resposta` | Resposta oficial da secretaria |
| 11 | `Recurso` | Cidadão contesta a resposta (a LAI prevê fase recursal) |
| 12 | `PrazoLegal` | Regra de prazo por tipo: dias base, prorrogação permitida, dias de prorrogação |
| 13 | `Notificacao` (reserva) | Aviso de prazo próximo do vencimento |
| 14 | `Auditoria` (reserva) | Log de quem fez o quê |

**Relacionamentos principais:** Município 1:N Usuário, Secretaria e Manifestação. Manifestação N:1 Cidadão, TipoManifestação e Secretaria. Manifestação 1:N Anexo, Trâmite e Resposta. Resposta 1:1 Recurso (instância única). Usuário SERVIDOR N:1 Secretaria. TipoManifestação 1:N PrazoLegal: uma regra federal (`municipio_id` nulo) e, opcionalmente, uma por município que a sobrescreve (art. 45 da LAI permite regulamentação local).

## 5. Regras de negócio (o que dá profundidade)

**Ciclo de vida da manifestação:**

`RECEBIDA` → `EM_ANALISE` → `ENCAMINHADA` → `RESPONDIDA` → `ENCERRADA`

Desvios: `EM_RECURSO` (depois de respondida, só LAI), `ARQUIVADA` (a partir de RECEBIDA ou EM_ANALISE). Cada transição grava um `Tramite`. Transição inválida devolve 409.

| De | Ação (quem) | Para |
|---|---|---|
| RECEBIDA | análise (OUVIDOR) | EM_ANALISE |
| EM_ANALISE | encaminhamento (OUVIDOR) | ENCAMINHADA |
| ENCAMINHADA | reencaminhamento (OUVIDOR) | ENCAMINHADA |
| RECEBIDA, EM_ANALISE | arquivamento (OUVIDOR) | ARQUIVADA |
| ENCAMINHADA | resposta (SERVIDOR da secretaria) | RESPONDIDA |
| RESPONDIDA | recurso (CIDADAO, só tipo com `permite_recurso`, dentro do prazo) | EM_RECURSO |
| EM_RECURSO | julgamento deferido / indeferido (OUVIDOR) | ENCAMINHADA / ENCERRADA |
| RESPONDIDA | encerramento (OUVIDOR) | ENCERRADA |

**Prorrogação não é status**: é a flag `prorrogada` na manifestação (permitida em RECEBIDA, EM_ANALISE e ENCAMINHADA). Como status, ela apagaria a etapa em que a manifestação estava.

**Prazos (da pesquisa de mercado):**
- Ouvidoria (Lei 13.460/2017): 30 dias, prorrogável uma vez por mais 30
- Pedido LAI (Lei 12.527/2011): 20 dias, prorrogável uma vez por mais 10
- Os números ficam na tabela `PrazoLegal`, não no código, então mudar lei não exige deploy
- Regra municipal só pode ser mais protetiva ao cidadão (art. 45 da LAI manda observar as normas gerais): prazos de resposta, prorrogação e julgamento iguais ou menores que os federais; prazo para o cidadão recorrer igual ou maior; tipo LAI não pode perder a fase recursal
- Prorrogar exige justificativa e só pode acontecer uma vez
- `data_limite` é calculada na abertura e gravada: mudar o PrazoLegal depois não altera manifestações já abertas
- Recurso só existe para LAI (arts. 15 e 16 da Lei 12.527): 10 dias para interpor, 5 para julgar, julgado pelo OUVIDOR. A Lei 13.460 (ouvidoria) não prevê fase recursal

**Outras regras:**
- Protocolo gerado no formato `ANO-MUNICIPIO-SEQUENCIAL`
- Isolamento por município: o `municipioId` vem do token e filtra toda consulta. Ouvidor de uma cidade nunca vê dados de outra
- Manifestação anônima só para tipos com `permite_anonimo` (denúncia); pedido LAI exige identificação. O cidadão anônimo não recebe notificação
- Resposta só pode ser dada por servidor da secretaria para a qual a manifestação foi encaminhada

## 6. Segurança (JWT)

- `POST /api/v1/auth/login` devolve o access token (expira em poucas horas)
- Claims: `sub` (usuário), `roles`, `municipioId`
- Filtro do Spring Security valida o token em toda rota, exceto as públicas
- Autorização por perfil com `@PreAuthorize`

| Perfil | Pode |
|---|---|
| ADMIN_PLATAFORMA | Cadastrar e editar municípios e criar o primeiro ADMIN de cada um. Não tem município e não acessa dados de nenhum |
| ADMIN | Gerenciar o próprio município, secretarias, usuários e prazos |
| OUVIDOR | Ver todas as manifestações do município, encaminhar, prorrogar, arquivar, gerar relatório |
| SERVIDOR | Ver e responder só as manifestações da própria secretaria |
| CIDADAO | Registrar manifestação, acompanhar as próprias, abrir recurso |
| Público (sem token) | Consultar andamento por número de protocolo, listar municípios ativos, registrar-se como cidadão |

## 7. Endpoints (rascunho)

Base: `/api/v1`. Tudo em plural, verbos HTTP corretos, status codes corretos (201 com `Location`, 204, 400, 401, 403, 404, 409).

| Grupo | Endpoints |
|---|---|
| Auth | `POST /auth/login`, `POST /auth/registro-cidadao` |
| Municípios | `GET/POST /municipios`, `GET/PUT /municipios/{id}` (ADMIN_PLATAFORMA). O POST cria junto o primeiro ADMIN; o código IBGE não muda depois |
| Secretarias | CRUD `/secretarias` (leitura também para OUVIDOR). DELETE desativa, não apaga |
| Usuários | CRUD `/usuarios` (ADMIN, só perfis ADMIN/OUVIDOR/SERVIDOR; filtro `?perfil=`). DELETE desativa, não apaga |
| Tipos e prazos | `GET /tipos-manifestacao`, `GET /tipos-manifestacao/{id}`, `PUT/DELETE /tipos-manifestacao/{id}/prazo` (regra municipal; DELETE volta à federal) |
| Manifestações | `POST /manifestacoes`, `GET /manifestacoes` (filtros + paginação), `GET /manifestacoes/{id}` |
| Ações | `POST /manifestacoes/{id}/analise`, `/encaminhamento`, `/prorrogacao`, `/arquivamento`, `/encerramento` |
| Anexos | `POST/GET /manifestacoes/{id}/anexos` |
| Trâmites | `GET /manifestacoes/{id}/tramites` |
| Respostas | `POST /manifestacoes/{id}/respostas` |
| Recursos | `POST /respostas/{id}/recursos`, `PUT /recursos/{id}` (julgar) |
| Público | `GET /protocolos/{numero}`, `GET /municipios/ativos` (só id, nome e UF, para o cidadão escolher onde se registrar) |
| Relatórios | `GET /relatorios/gestao?ano=` (total por tipo, por secretaria, % no prazo) |

**Detalhe de projeto:** as ações são substantivos (`/prorrogacao`), não verbos (`/prorrogar`). Isso é o tipo de coisa que dá pra defender no vídeo de Richardson.

## 8. Richardson

- **Nível 0 → 1:** recursos com URI própria em vez de um endpoint único
- **Nível 2:** verbos HTTP e status codes com significado
- **Nível 3 (HATEOAS):** a resposta de uma manifestação traz `_links` que dependem do estado. Uma manifestação `RECEBIDA` tem link para `encaminhamento`; uma `RESPONDIDA` tem link para `recursos`; uma `ENCERRADA` não tem link de ação nenhum. O cliente descobre o que pode fazer olhando os links

## 9. Flyway

```
V1__cria_municipio_perfil_usuario.sql
V2__cria_secretaria_cidadao.sql
V3__cria_tipo_prazo_manifestacao.sql
V4__cria_anexo_tramite_resposta_recurso.sql
V5__seed_perfis_tipos_e_prazos.sql
db/dev/R__seed_dados_demo.sql  (repeatable e idempotente, só no perfil dev)
```

`spring.jpa.hibernate.ddl-auto=validate` para o Hibernate nunca mexer no schema, só o Flyway.

## 10. Estrutura do projeto

Pouca coisa e pouco arquivo, sem sobre-modularizar:

```
src/main/java/.../cidadetransparente/
  config/         (SecurityConfig, JwtFilter)
  domain/         (entidades + enums)
  repository/
  service/
  controller/
  dto/
  exception/      (handler global de erros)
src/main/resources/db/migration/
docker-compose.yml
README.md
postman/CidadeTransparente.postman_collection.json
frontend/
```

## 11. Frontend

Telas mínimas:
1. Login
2. Nova manifestação (cidadão)
3. Consulta por protocolo (pública)
4. Painel do ouvidor: lista com filtro por status, tipo e prazo, com destaque para o que está perto de vencer
5. Detalhe da manifestação: timeline de trâmites e botões de ação que aparecem conforme os `_links` da API
6. Relatório de gestão

Ponto forte pro vídeo: os botões vêm dos links HATEOAS, então o frontend não tem `if status == X` espalhado.

## 12. Entregáveis

- [ ] Código-fonte completo (backend + frontend)
- [ ] Scripts Flyway
- [ ] Coleção Postman/Insomnia cobrindo **todos** os endpoints (checar contra a lista da seção 7)
- [ ] README com pré-requisitos, como subir o banco, variáveis de ambiente, como rodar back e front, usuários de demo
- [ ] Vídeo 1: arquitetura e implementação
- [ ] Vídeo 2: testes de todos os endpoints
- [ ] Vídeo 3: código-fonte e frontend funcionando

## 13. Roteiro dos vídeos (o que precisa saber explicar)

**Vídeo 1, arquitetura:** camadas e por que separar assim; modelo de entidades; por que multi-tenant por coluna e não por schema; Flyway vs ddl-auto; como o prazo vira dado em vez de código.

**Vídeo 2, testes:** rodar a coleção inteira mostrando login, uso do token, caminho feliz e erros (401, 403, 404, 409) de cada grupo. Mostrar um caso de isolamento entre municípios.

**Vídeo 3, código e frontend:** mostrar o fluxo completo (cidadão abre, ouvidor encaminha, servidor responde, cidadão recorre), explicando o JWT no meio: onde é gerado, onde é validado, o que tem dentro.

A avaliação pesa demonstração de domínio, então cada decisão acima precisa ter um "por quê" na ponta da língua.

## 14. Cronograma

| Período | Foco |
|---|---|
| 28/09 a 05/10 | Fechar o plano de negócio (prioridade, prazo mais curto) |
| 06 a 12/10 | Setup, entidades, Flyway, autenticação JWT |
| 13 a 19/10 | CRUDs, fluxo da manifestação, regras de prazo |
| 20 a 26/10 | HATEOAS, relatórios, coleção Postman, frontend |
| 27 a 30/10 | Vídeos, README, revisão |
| 31/10 e 01/11 | Folga e entrega |

## 15. Riscos

- **Escopo inchar:** se apertar, cortam-se `Notificacao` e `Auditoria` (entidades 13 e 14) e o relatório. O núcleo de 12 já cumpre o exame
- **HATEOAS complicar o frontend:** começar com links só em `Manifestacao` e expandir se der
- **Vídeos deixados pro final:** gravar o vídeo de testes assim que a coleção Postman ficar pronta, sem esperar o frontend