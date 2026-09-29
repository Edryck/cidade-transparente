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
- Frontend: React + Vite (protótipo HTML estático em `prototipo/` antes)
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
- Se o mesmo erro persistir depois de 2 correções, parar e reportar a causa em vez de continuar tentando.
- Sem refactor, renomeação ou "melhoria" que não foi pedida. Se notar algo fora do escopo, anotar numa linha no resumo final.

## Economia de tokens

- Não reler `docs/documentacao-previa.md` e `docs/lgpd.md` inteiros a cada tarefa: buscar a seção relevante com Grep, ou Read com offset/limit.
- Não reler arquivo que acabou de editar (Edit já avisa se falhou). Ir direto aos arquivos conhecidos pela estrutura de pacotes, sem varrer o repositório.
- Filtrar saída de comandos (`mvn -q`, `| tail -30`, `grep`). Nunca despejar log inteiro.
- Durante o desenvolvimento, rodar só a pasta da coleção Postman afetada. A coleção inteira roda uma vez no fim, com saída resumida (só falhas e totais).
- Usar Edit em vez de reescrever o arquivo inteiro. Não criar arquivos que não foram pedidos (resumos, notas, docs extras).
- Não usar subagentes, a menos que o usuário peça. Fazer inline.
- Se algo já está definido neste arquivo ou na documentação, não perguntar de novo. Se precisar perguntar, juntar tudo numa mensagem só.

## Como se comunicar

- Durante o trabalho: proibido narrar ("agora vou atualizar o arquivo tal"), comentar resultado intermediário, repetir diffs ou tabelas de arquivo editado, e mostrar os comandos rodados.
- Resposta final: um único resumo, nesta ordem:
    1. resultado com o número que prova;
    2. como ficou, por assunto, com o porquê e o artigo da lei;
    3. erros meus, só se houve;
    4. commits sugeridos;
    5. cronograma e uma pergunta objetiva de próximo passo.
- Commits: nunca executar git add ou git commit, só sugerir. Um bloco por commit, com caminhos completos, tipo(escopo) em português e corpo explicando o porquê. Ordem: migration, depois o que depende dela, testes e docs por último.
- Formatação: sem elogio de abertura, sem emoji de status, sem termos de terminal (●, Bash(...), Updated ... (+20 -17)).