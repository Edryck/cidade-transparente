# Cidade Transparente

Plataforma de ouvidoria, Lei de Acesso à Informação (LAI) e transparência para prefeituras de pequeno porte. Projeto do exame de suficiência de Desenvolvimento de Sistemas (TDS) — UTFPR.

O cidadão registra uma manifestação (reclamação, denúncia, sugestão, elogio ou pedido LAI), o ouvidor encaminha para a secretaria responsável, o servidor responde, e o sistema controla o prazo legal em cada etapa (30+30 dias para ouvidoria, 20+10 dias para LAI). Multi-tenant: cada prefeitura tem seus próprios usuários, secretarias e manifestações, isolados entre si.

Documentação completa do domínio, entidades, regras de negócio e endpoints em [`docs/documentacao-previa.md`](docs/documentacao-previa.md).

## Stack

- **Backend:** Java 21, Spring Boot 3 (Web, Data JPA, Security, HATEOAS, Validation)
- **Banco:** PostgreSQL, versionado com Flyway
- **Autenticação:** JWT
- **Documentação da API:** springdoc-openapi (Swagger UI)
- **Frontend:** Flutter Web

## Pré-requisitos

- JDK 21
- Maven (ou o wrapper `./mvnw`, incluído no projeto)
- Docker e Docker Compose (para o banco local)
- Flutter SDK (para o frontend)
- Postman ou Insomnia (para rodar a coleção de testes)

## Como rodar

### 1. Banco de dados

```bash
docker compose up -d
```

Sobe um PostgreSQL local. As migrations do Flyway rodam sozinhas quando o backend inicia.

### 2. Backend

```bash
cd backend
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`. Documentação interativa (Swagger UI) em `http://localhost:8080/swagger-ui.html`.

### 3. Frontend

```bash
cd frontend
flutter pub get
flutter run -d chrome
```

### 4. Testes dos endpoints

Importe a coleção `postman/CidadeTransparente.postman_collection.json` no Postman ou Insomnia. Rode primeiro a requisição de login para obter o token — as demais já usam a variável de ambiente da coleção automaticamente.

## Configuração

Variáveis de ambiente (ou `backend/src/main/resources/application.yml`):

| Variável | Descrição | Padrão (dev) |
|---|---|---|
| `DB_URL` | URL de conexão do PostgreSQL | `jdbc:postgresql://localhost:5432/cidade_transparente` |
| `DB_USER` | Usuário do banco | `postgres` |
| `DB_PASSWORD` | Senha do banco | `postgres` |
| `JWT_SECRET` | Chave usada para assinar o token (HS256, **mínimo 32 caracteres**; a aplicação não sobe sem ela) | (definir localmente, nunca commitar) |
| `JWT_EXPIRATION` | Validade do token, em milissegundos | `86400000` (24h) |

## Usuários de demonstração

Criados pela migration repeatable `db/dev/R__seed_dados_demo.sql`, carregada só no perfil `dev` (o padrão ao rodar localmente; em outro ambiente, defina `SPRING_PROFILES_ACTIVE`):

| Perfil | Usuário | Senha |
|---|---|---|
| ADMIN_PLATAFORMA | plataforma@demo.gov.br | plataforma123 |
| ADMIN | admin@demo.gov.br | admin123 |
| OUVIDOR | ouvidor@demo.gov.br | ouvidor123 |
| SERVIDOR | servidor@demo.gov.br | servidor123 |
| CIDADAO | cidadao@demo.gov.br | cidadao123 |
| OUVIDOR (outro município) | ouvidor@outra.gov.br | ouvidor123 |

O seed cria dois municípios fictícios (Vila Serena e Campo Aurora) para demonstrar o isolamento: o ouvidor de um não enxerga as manifestações do outro.

## Estrutura do projeto

```
cidade-transparente/
├── docs/documentacao-previa.md   # domínio, entidades, regras, endpoints
├── postman/                      # coleção de testes
├── backend/                      # API Spring Boot
└── frontend/                     # app Flutter Web
```