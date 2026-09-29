# CLAUDE.md

Guia para o Claude Code trabalhar neste repositório. Projeto acadêmico (exame de suficiência de TDS — UTFPR), com prazo real: entrega 01/11/2026.

## O que é o projeto

**Cidade Transparente**: API REST + frontend de ouvidoria, LAI (Lei de Acesso à Informação) e transparência para prefeituras de pequeno porte. Cidadão registra manifestação, ouvidor encaminha pra secretaria certa, servidor responde, sistema controla prazo legal em cada etapa. Multi-tenant por município.

A documentação completa do domínio, entidades, regras de negócio e endpoints está em `docs/documentacao-previa.md` — leia esse arquivo antes de mexer em modelagem, fluxo de manifestação ou regras de prazo.

## Requisitos do exame (não negociáveis)

- API REST em Spring Boot, mínimo 10 entidades de domínio
- ORM com Spring Data JPA
- Versionamento de banco com Flyway
- Autenticação por token JWT
- Testes de todos os endpoints (coleção Postman/Insomnia)
- Modelo de maturidade de Richardson (nível 3 / HATEOAS nos recursos principais)
- Frontend consumindo a API
- README com instruções de execução

## Stack

- Java 21, Spring Boot 3 (Web, Data JPA, Security, HATEOAS, Validation)
- PostgreSQL + Flyway
- JWT (jjwt) com Spring Security
- springdoc-openapi (Swagger UI)
- Frontend: Flutter Web
- Docker Compose só para subir o banco local

## Convenções de código

- **Poucos arquivos, poucas funções, sem sobre-modularização.** Não criar uma classe/interface para cada mínima abstração (nada de `IUsuarioService` + `UsuarioServiceImpl` só por padrão — uma classe concreta resolve, a menos que exista razão real para interface). Preferir um service por agregado, não um por caso de uso.
- Pacote por camada técnica (`domain`, `repository`, `service`, `controller`, `dto`), não por feature — ver estrutura de pastas abaixo.
- DTOs de request/response separados da entidade JPA; a entidade nunca é serializada direto na resposta.
- Regras de prazo (dias, prorrogação) ficam em dado (tabela `PrazoLegal`), nunca hardcoded no código.
- Nomes e comentários em português (é um domínio de administração pública brasileira); nomes de classes/métodos em português quando isso for mais natural para o domínio (ex.: `Manifestacao`, `encaminhar()`), sem forçar tradução artificial.

## Banco e migrations

- `spring.jpa.hibernate.ddl-auto=validate` sempre. O Hibernate nunca cria/altera schema — só o Flyway.
- Toda mudança de schema é uma migration nova em `src/main/resources/db/migration`, nunca edição de uma migration já aplicada.
- Nomenclatura: `V{numero}__descricao_curta.sql`.

## Segurança

- Claims do JWT: `sub` (usuário), `roles`, `municipioId`.
- Todo repository/query de dado município-específico filtra por `municipioId` vindo do token — nunca por parâmetro vindo do cliente. Isolamento entre municípios é regra de segurança, não só de negócio.
- Autorização por perfil com `@PreAuthorize`, perfis: `ADMIN_PLATAFORMA`, `ADMIN`, `OUVIDOR`, `SERVIDOR`, `CIDADAO`.
- `ADMIN_PLATAFORMA` não tem município: só acessa `/municipios` e cria o primeiro ADMIN de cada município. Nenhuma rota com dados de município o inclui no `@PreAuthorize`, e ler `municipioId` de um token sem esse claim lança exceção (o filtro falha fechado, nunca vira "sem filtro").

## LGPD e conformidade legal

- Lei e LGPD vêm antes de conveniência: o sistema deve ser fiel ao que uma prefeitura real precisaria. Toda decisão sobre dado pessoal cita o artigo que a fundamenta. Inventário de dados, bases legais e regras em `docs/lgpd.md` — leia antes de mexer em dado pessoal, rota pública ou retenção.
- Prefeitura é a controladora; a plataforma é operadora (LGPD art. 39): `ADMIN_PLATAFORMA` nunca acessa dado de cidadão.
- Identidade de quem se manifesta é informação restrita (Lei 13.460 art. 10 § 7º): visível só para o OUVIDOR e para o próprio cidadão, nunca para SERVIDOR nem em rota pública.
- Rota pública nunca expõe dado pessoal. Identificador sequencial (protocolo) não serve como credencial.
- Registros da administração não se apagam (Lei 8.159 art. 9º; LGPD art. 16, I): DELETE desativa/encerra, e a resposta explica a base legal da conservação.

## Testes

- Toda rota nova precisa de request correspondente na coleção Postman (`postman/CidadeTransparente.postman_collection.json`), incluindo casos de erro relevantes (401/403/404/409).
- Não é objetivo do exame ter suite JUnit completa — só entra se sobrar tempo do cronograma.

## Ao gerar código

- Perguntar antes de adicionar dependência nova não listada na stack acima.
- Ao criar uma entidade, sempre perguntar (ou verificar `docs/documentacao-previa.md`) se ela precisa de migration Flyway correspondente antes de considerar a tarefa concluída.
- Endpoints de ação usam substantivo, não verbo (`POST /manifestacoes/{id}/encaminhamento`, não `/encaminhar`).
- Regras de transição da manifestação ficam só em `FluxoManifestacao.impedimento()`: a mesma função valida a ação (409) e gera os links. Nunca duplicar uma regra de estado no controller ou no service.
- Ao implementar HATEOAS, os `_links` retornados variam de acordo com o estado da entidade (ex.: manifestação `ENCERRADA` não retorna link de ação nenhum) — isso é requisito central do exame, não detalhe cosmético.