# Cidade Transparente e a LGPD

Como o sistema trata dados pessoais e qual artigo de lei fundamenta cada decisão. Este documento cumpre dois papéis:

- **registro das operações de tratamento**, exigido pela LGPD (art. 37);
- **relatório de impacto simplificado** (art. 38), já pronto caso a ANPD o solicite.

A regra do projeto é: **a lei vem antes da conveniência**. Se uma funcionalidade conflitar com a LGPD, a LAI ou a Lei 13.460, muda-se a funcionalidade.

Legenda: ✅ implementado · ⏳ planejado (etapa indicada)

---

## 1. Papéis

| Papel (LGPD) | Quem | Consequência no sistema |
|---|---|---|
| **Controlador** (art. 5º, VI; art. 23) | Cada prefeitura | Decide sobre o tratamento dos dados dos seus cidadãos. Os dados de uma prefeitura ficam isolados dos de outra (`municipio_id` vem do token, com FKs compostas no banco). ✅ |
| **Operador** (art. 5º, VII; art. 39) | A empresa que fornece a plataforma | Trata os dados só conforme as instruções da prefeitura. O perfil `ADMIN_PLATAFORMA` não acessa nenhuma rota com dados de município: o token dele não tem `municipioId`, e toda consulta municipal falha de forma fechada (nega o acesso). ✅ |
| **Encarregado** (arts. 23, III, e 41) | Pessoa indicada por cada prefeitura | Informado ao cadastrar o município, só pode ser alterado pelo ADMIN da própria prefeitura (`PUT /municipios/{id}/encarregado`). É publicado no aviso de privacidade. ✅ |

## 2. Bases legais: por que não é consentimento

O sistema **não depende de consentimento**. A prefeitura é obrigada por lei a manter a ouvidoria (Lei 13.460/2017; Lei 13.608/2018, art. 4º-A) e a responder pedidos de informação (Lei 12.527/2011). Por isso as bases legais são:

- **art. 7º, II:** cumprimento de obrigação legal, no registro e na resposta de manifestações;
- **art. 7º, III:** execução de política pública, no cadastro do cidadão;
- **art. 11, II, "a" e "b":** para dados sensíveis que o cidadão escreva no texto da manifestação, como os de saúde ou de religião.

Uma caixa de "li e aceito" seria **juridicamente errada**, porque sugere que o cidadão pode recusar um tratamento que a lei impõe. No lugar dela existe o **aviso de privacidade** (arts. 9º e 23, I), público em `GET /municipios/{id}/privacidade`. ✅

**Crianças e adolescentes** (art. 14): o Enunciado CD/ANPD nº 1/2023 admite as bases legais dos arts. 7º e 11 para esse público, desde que prevaleça o melhor interesse. Um adolescente pode, portanto, se manifestar pela mesma base legal. Não há coleta de dado específico de menores.

## 3. Inventário de dados pessoais

| Dado | Onde fica | Finalidade | Base legal | Quem acessa | Retenção |
|---|---|---|---|---|---|
| Nome, e-mail | `usuario`, `cidadao` | Identificar o manifestante e permitir o login | art. 7º, III | O próprio titular e o OUVIDOR (⏳ manifestação) | Enquanto a conta existir; depois, junto às manifestações (art. 16, I) |
| Senha | `usuario.senha_hash` | Autenticação | art. 7º, III | Ninguém: só o hash BCrypt é guardado ✅ | Enquanto a conta existir |
| CPF (opcional) | `cidadao.cpf` | Distinguir homônimos | art. 7º, III | O próprio titular e o OUVIDOR | Como o nome |
| Telefone (opcional) | `cidadao.telefone` | Contato sobre a manifestação | art. 7º, III | O próprio titular e o OUVIDOR | Como o nome |
| Conteúdo da manifestação e anexos | `manifestacao`, `anexo` | Atender a manifestação | art. 7º, II; art. 11, II, "a" e "b" | OUVIDOR e o SERVIDOR da secretaria destinatária | Tabela de temporalidade do município (Lei 8.159/1991, art. 9º) |
| Identidade de quem se manifesta | `manifestacao.cidadao_id` | Resposta e eventual apuração | Lei 13.460, art. 10, § 7º | **Só o OUVIDOR** e o próprio cidadão, nunca o SERVIDOR (⏳ manifestação) | Acesso restrito por até 100 anos (LAI, art. 31, § 1º, I) |
| Dados da equipe | `usuario` | Autoria dos atos administrativos | art. 23 | O ADMIN do mesmo município | Conta desativada é conservada para rastrear quem praticou cada ato |
| Registro de acesso (IP, data, hora) | `auditoria` (⏳) | Atender ordem judicial (Marco Civil, art. 15) | art. 7º, II | Ninguém pelo sistema; só por ordem judicial | **6 meses**, depois o IP é apagado |
| Trilha de acesso à identidade | `auditoria` (⏳) | Prestação de contas (art. 37; Decreto 10.153, art. 6º, § 3º) | art. 7º, II | Controle interno da prefeitura | Junto com a manifestação |

**O que o sistema não coleta:** data de nascimento, endereço, gênero, localização, dados de navegação. Nada disso é necessário para a ouvidoria (art. 6º, III). O token JWT carrega só id, perfil e município: nenhum dado pessoal legível. ✅

## 4. Como cada princípio do art. 6º aparece no sistema

| Princípio | Como o sistema cumpre |
|---|---|
| I. Finalidade / II. Adequação | Os dados só servem à ouvidoria e à LAI. Não há uso para marketing nem venda (declarado no aviso) ✅ |
| III. Necessidade | CPF e telefone são opcionais. O ADMIN não enxerga contas de cidadãos (`/usuarios` lista só a equipe, e conta de cidadão responde 404) ✅. O SERVIDOR não verá a identidade do manifestante ⏳ |
| IV. Livre acesso | `GET /minha-conta` para todos os perfis, inclusive a equipe ✅ |
| V. Qualidade | `PUT /minha-conta` corrige os dados e mantém `cidadao` sincronizado com `usuario` ✅ |
| VI. Transparência | Aviso de privacidade público, versionado, com base legal e retenção de cada tratamento ✅ |
| VII. Segurança / VIII. Prevenção | Ver seção 7 |
| IX. Não discriminação | O sistema não toma decisão automatizada sobre o cidadão |
| X. Responsabilização | Este documento, a versão do aviso, a trilha de auditoria ⏳ e as regras reforçadas no banco (CHECK e FKs) |

## 5. Direitos do titular (art. 18)

| Direito | Como exercer | Situação |
|---|---|---|
| Confirmação e acesso (I, II) | `GET /api/v1/minha-conta` | ✅ |
| Correção (III) | `PUT /api/v1/minha-conta` | ✅ |
| Encerramento da conta (IV, VI) | `DELETE /api/v1/minha-conta`: bloqueia o login e **não apaga** as manifestações. A resposta explica o motivo, como exige o art. 18, § 4º, II | ✅ |
| Informação sobre compartilhamento (VII) | Seção "compartilhamento" do aviso | ✅ |
| Anonimização, bloqueio ou eliminação de dados excessivos; oposição (IV, § 2º) | Pedido ao encarregado, pelo e-mail publicado no aviso | ✅ (canal) |
| Petição à ANPD (§ 1º) | Link no aviso | ✅ |
| Portabilidade (V) | Não se aplica: não há "outro fornecedor" de ouvidoria municipal. O `GET /minha-conta` entrega os dados em JSON | — |

**Prazos:** perante o Poder Público, seguem a LAI (LGPD art. 23, § 3º): 20 dias, prorrogáveis por mais 10.

**Por que o encerramento não apaga nada:** manifestações são documentos públicos (Lei 8.159/1991, art. 7º). Eliminá-las exige autorização da instituição arquivística (art. 9º), e a LGPD autoriza conservar dados para cumprir obrigação legal (art. 16, I). O direito de eliminação do art. 18, VI, vale para dados tratados **com consentimento**, que não é o caso aqui.

## 6. Identidade, denúncia anônima e ordem judicial

O cidadão escolhe entre duas formas de se manifestar:

1. **Identificado:** logado. A identidade é restrita ao OUVIDOR (Lei 13.460, art. 10, § 7º; Lei 13.608, art. 4º-B). Antes de ir à secretaria, a manifestação é pseudonimizada, ou seja, segue sem a identificação (Decreto 10.153, art. 6º, § 4º, usado como referência). ⏳
2. **Anônimo:** sem login, só para tipos com `permite_anonimo` (a denúncia). Fica `cidadao_id = NULL`, e o acompanhamento é feito pelo protocolo mais uma chave de acesso. ⏳

**Com ordem judicial**, a prefeitura entrega o que tiver (LAI, art. 31, § 3º, III; Marco Civil, arts. 10, § 1º, e 22):

- na manifestação identificada, a identidade;
- na anônima, só o registro de acesso (IP, data e hora) dos últimos 6 meses (Marco Civil, art. 15).

**Sem ordem judicial**, o registro de acesso não é entregue a ninguém (Marco Civil, art. 15, § 3º). Dados cadastrais podem ser requisitados por autoridade com competência legal (Marco Civil, art. 10, § 3º).

A denúncia anônima é válida para iniciar uma apuração preliminar, mas sozinha não basta para instaurar procedimento (STF, HC 84.827 e Inq 1.957).

## 7. Segurança (arts. 46 a 49)

| Medida | Situação |
|---|---|
| Senha só como hash BCrypt | ✅ |
| Token JWT assinado (HS256), segredo de no mínimo 32 bytes, fora do repositório | ✅ |
| Token com validade de 2h, que limita o estrago de um vazamento | ✅ |
| Isolamento entre prefeituras no código (token) e no banco (FKs compostas) | ✅ |
| Registro de outro município responde 404, sem confirmar que o id existe | ✅ |
| Mensagem de login idêntica para e-mail inexistente e senha errada | ✅ |
| Erros padronizados (ProblemDetail) sem dado pessoal nem stack trace | ✅ |
| Rota pública nunca expõe dado pessoal | ✅ |
| Consulta por protocolo exige chave de acesso aleatória, guardada só como hash. Sem ela, o protocolo sequencial permitiria percorrer e ler manifestações alheias | ⏳ manifestação |
| Trilha de quem acessou a identidade do manifestante | ⏳ manifestação |
| Anexos com tipo e tamanho restritos (5 MB no banco) | ✅ tamanho · ⏳ tipos |
| HTTPS obrigatório | Fora do código: configuração de implantação |

**Limitação conhecida:** um token emitido antes do encerramento da conta continua válido até expirar (no máximo 2h). Revogar tokens na hora exigiria consultar o banco a cada requisição. O token curto é o equilíbrio escolhido.

## 8. Retenção e eliminação

| Dado | Regra |
|---|---|
| Manifestações, trâmites, respostas e anexos | Tabela de temporalidade de documentos de cada prefeitura (Lei 8.159/1991). O sistema **nunca apaga sozinho** |
| Identificação do manifestante | Acesso restrito por até 100 anos (LAI, art. 31) |
| Contas encerradas ou desativadas | Conservadas, com login bloqueado (art. 16, I) |
| IP do registro de acesso | Apagado depois de 6 meses por uma rotina diária ⏳ |

## 9. Incidentes de segurança

A prefeitura (controladora) comunica à ANPD e aos titulares afetados, em **até 3 dias úteis** a partir de quando soube que o incidente atingiu dados pessoais, todo incidente que possa gerar risco ou dano relevante (art. 48; Resolução CD/ANPD nº 15/2024).

A empresa da plataforma (operadora) avisa a prefeitura imediatamente. A comunicação precisa conter os itens do art. 48, § 1º: a natureza dos dados, os titulares afetados, as medidas de proteção, os riscos, os motivos de eventual demora e as medidas de mitigação. A trilha de auditoria ⏳ serve para determinar o que foi acessado.

## 10. Riscos e mitigação (relatório de impacto simplificado)

| Risco | Impacto | Mitigação |
|---|---|---|
| Retaliação contra quem denuncia, se a identidade vazar para a secretaria denunciada | Alto | Identidade visível só para o OUVIDOR ⏳; denúncia anônima ⏳; trilha de acesso ⏳ |
| Enumeração de protocolos para ler manifestações alheias | Alto | Chave de acesso obrigatória na consulta pública ⏳ |
| Vazamento de dados de uma prefeitura para outra | Alto | Isolamento no token e no banco ✅ |
| Operador (plataforma) acessando dados de cidadãos | Médio | `ADMIN_PLATAFORMA` sem acesso ✅; contrato entre operador e controlador (fora do sistema) |
| Dados sensíveis no texto livre da manifestação | Médio | Acesso restrito por perfil e por secretaria ⏳; base legal no art. 11 |
| Vazamento de token | Médio | Validade de 2h ✅; HTTPS (implantação) |
| Senhas expostas em vazamento do banco | Médio | BCrypt ✅ |

## 11. Fora do escopo do código

Estas obrigações são processos da prefeitura. O sistema dá suporte a elas, mas não as substitui:

- indicar formalmente o encarregado e publicar o ato de indicação;
- manter o contrato entre operador e controlador, com instruções de tratamento, sigilo e aviso de incidentes;
- manter a tabela de temporalidade de documentos e o processo de eliminação autorizado pelo arquivo público;
- manter o procedimento interno de resposta a incidentes;
- capacitar ouvidores e servidores (art. 41, § 2º, III).

## Referências

- [Lei 13.709/2018 (LGPD)](https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm)
- [Lei 12.527/2011 (LAI)](https://www.planalto.gov.br/ccivil_03/_ato2011-2014/2011/lei/l12527.htm)
- [Lei 13.460/2017 (Código de Defesa do Usuário do Serviço Público)](https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2017/lei/l13460.htm)
- [Lei 13.608/2018 (Informante e ouvidorias)](https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13608.htm)
- [Lei 12.965/2014 (Marco Civil da Internet)](https://www.planalto.gov.br/ccivil_03/_ato2011-2014/2014/lei/l12965.htm)
- [Lei 8.159/1991 (Arquivos públicos)](https://www.planalto.gov.br/ccivil_03/leis/l8159.htm)
- [Decreto 10.153/2019 (Proteção do denunciante)](https://www.planalto.gov.br/ccivil_03/_ato2019-2022/2019/decreto/d10153.htm)
- [Decreto 9.492/2018](https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/decreto/d9492.htm)
- [Resolução CD/ANPD nº 15/2024 (Comunicação de incidentes)](https://www.gov.br/anpd/pt-br/assuntos/noticias/anpd-aprova-o-regulamento-de-comunicacao-de-incidente-de-seguranca)
- [Enunciado CD/ANPD nº 1/2023 (Crianças e adolescentes)](https://www.gov.br/anpd/pt-br/assuntos/noticias/anpd-divulga-enunciado-sobre-o-tratamento-de-dados-pessoais-de-criancas-e-adolescentes)
- [STF: investigação não pode se basear só em denúncia anônima](https://portal.stf.jus.br/noticias/verNoticiaDetalhe.asp?idConteudo=414497)
