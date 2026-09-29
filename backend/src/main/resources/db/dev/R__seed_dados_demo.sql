-- Dados de demonstração (só perfil dev). Repeatable: o Flyway reexecuta quando o arquivo muda,
-- então tudo aqui precisa ser idempotente (ON CONFLICT / NOT EXISTS) e buscar ids por chave natural.
-- Municípios fictícios com código IBGE iniciado em 99 (não existe município real com esse prefixo).

INSERT INTO municipio (nome, uf, codigo_ibge) VALUES
    ('Vila Serena',   'PR', '9999901'),
    ('Campo Aurora',  'PR', '9999902')
ON CONFLICT (codigo_ibge) DO NOTHING;

-- Encarregado de dados de cada prefeitura (LGPD art. 41). Só preenche se ainda estiver vazio,
-- para não sobrescrever o que o ADMIN tiver alterado pela API.
UPDATE municipio m
   SET encarregado_nome = e.nome, encarregado_email = e.email
  FROM (VALUES
        ('9999901', 'Encarregada de Dados de Vila Serena',  'lgpd@demo.gov.br'),
        ('9999902', 'Encarregado de Dados de Campo Aurora', 'lgpd@outra.gov.br')
       ) AS e (ibge, nome, email)
 WHERE m.codigo_ibge = e.ibge
   AND m.encarregado_nome IS NULL;

INSERT INTO secretaria (municipio_id, nome, sigla, email)
SELECT m.id, s.nome, s.sigla, s.email
  FROM (VALUES
        ('9999901', 'Secretaria de Obras e Urbanismo', 'SEMOB', 'obras@demo.gov.br'),
        ('9999901', 'Secretaria de Saúde',             'SEMSA', 'saude@demo.gov.br'),
        ('9999901', 'Secretaria de Educação',          'SEMED', 'educacao@demo.gov.br'),
        ('9999902', 'Secretaria de Administração',     'SEMAD', 'adm@outra.gov.br')
       ) AS s (ibge, nome, sigla, email)
  JOIN municipio m ON m.codigo_ibge = s.ibge
ON CONFLICT (municipio_id, sigla) DO NOTHING;

-- Senhas: plataforma123, admin123, ouvidor123, servidor123, cidadao123 (BCrypt, custo 10)
INSERT INTO usuario (nome, email, senha_hash, perfil_id, municipio_id, secretaria_id)
SELECT u.nome, u.email, u.senha_hash, p.id, m.id, s.id
  FROM (VALUES
        ('Administrador da Plataforma', 'plataforma@demo.gov.br', '$2a$10$GLeKzzxOhXpFgACGDDQ4M.KDZG/tWHhruJaUkqNaqrxs5keXD5xsu', 'ADMIN_PLATAFORMA', NULL,      NULL),
        ('Admin Vila Serena',           'admin@demo.gov.br',      '$2a$10$TbC2Iv8/7luJGlvz0H3Hause3Rn6PUQDnR/2LwXgZS0dcvKM7.1yS', 'ADMIN',            '9999901', NULL),
        ('Ouvidora Vila Serena',        'ouvidor@demo.gov.br',    '$2a$10$GfPd2bh6GeAJ0iRqVqDgseGSPmjNqXwDBwQpEgrptWK6XZgLwYimu', 'OUVIDOR',          '9999901', NULL),
        ('Servidor de Obras',           'servidor@demo.gov.br',   '$2a$10$.HBtConCsmVVjzuJAuUe4eiRgVuJiJiL0sVrXJrGxlPqbEaYiBglu', 'SERVIDOR',         '9999901', 'SEMOB'),
        ('Maria Cidadã',                'cidadao@demo.gov.br',    '$2a$10$iNuWJQpMOSTTkcFuvobFluS9FgAE9FwQMy4Zzm17di7oOlT5HBPw2', 'CIDADAO',          '9999901', NULL),
        ('Admin Campo Aurora',          'admin@outra.gov.br',     '$2a$10$TbC2Iv8/7luJGlvz0H3Hause3Rn6PUQDnR/2LwXgZS0dcvKM7.1yS', 'ADMIN',            '9999902', NULL),
        ('Ouvidor Campo Aurora',        'ouvidor@outra.gov.br',   '$2a$10$GfPd2bh6GeAJ0iRqVqDgseGSPmjNqXwDBwQpEgrptWK6XZgLwYimu', 'OUVIDOR',          '9999902', NULL)
       ) AS u (nome, email, senha_hash, perfil, ibge, sigla)
  JOIN perfil p ON p.nome = u.perfil
  LEFT JOIN municipio m ON m.codigo_ibge = u.ibge
  LEFT JOIN secretaria s ON s.municipio_id = m.id AND s.sigla = u.sigla
ON CONFLICT (email) DO NOTHING;

INSERT INTO cidadao (municipio_id, usuario_id, nome, cpf, email, telefone)
SELECT u.municipio_id, u.id, u.nome, '12345678909', u.email, '41999990000'
  FROM usuario u
 WHERE u.email = 'cidadao@demo.gov.br'
ON CONFLICT (usuario_id) DO NOTHING;

-- Uma manifestação em cada estado interessante para o HATEOAS, mais uma no outro município
-- (para demonstrar o isolamento). Datas relativas a now() para os prazos fazerem sentido na demo.
-- Chave de acesso de demonstração: 'DEMO-' + sequencial (ex.: DEMO-000004). Em produção a chave é aleatória.
INSERT INTO manifestacao (municipio_id, protocolo, tipo_manifestacao_id, cidadao_id, secretaria_id,
                          assunto, descricao, status, data_abertura, data_limite, prorrogada, data_encerramento,
                          chave_acesso_hash)
SELECT m.id, d.protocolo, t.id, c.id, s.id, d.assunto, d.descricao, d.status,
       now() - make_interval(days => d.dias_atras),
       (now() - make_interval(days => d.dias_atras))::date + d.dias_prazo,
       d.prorrogada,
       CASE WHEN d.status = 'ENCERRADA' THEN now() - interval '1 day' END,
       encode(sha256(convert_to('DEMO-' || right(d.protocolo, 6), 'UTF8')), 'hex')
  FROM (VALUES
        ('9999901', '2026-9999901-000001', 'RECLAMACAO',        TRUE,  NULL,    'Buraco na Rua das Palmeiras',     'Buraco grande em frente ao número 120, risco para motociclistas.', 'RECEBIDA',    2,  30, FALSE),
        ('9999901', '2026-9999901-000002', 'DENUNCIA',          FALSE, NULL,    'Descarte irregular de entulho',   'Caçambas despejando entulho no terreno baldio da Av. Central.',     'EM_ANALISE',  5,  30, FALSE),
        ('9999901', '2026-9999901-000003', 'PEDIDO_INFORMACAO', TRUE,  'SEMOB', 'Contratos de pavimentação 2026',  'Solicito cópia dos contratos de pavimentação firmados em 2026.',    'ENCAMINHADA', 18, 30, TRUE),
        ('9999901', '2026-9999901-000004', 'PEDIDO_INFORMACAO', TRUE,  'SEMOB', 'Cronograma de obras do bairro',   'Qual o cronograma das obras previstas para o bairro Jardim Sul?',   'RESPONDIDA',  12, 20, FALSE),
        ('9999901', '2026-9999901-000005', 'ELOGIO',            TRUE,  'SEMOB', 'Recapeamento da Av. Brasil',      'Obra entregue antes do prazo e com a sinalização bem feita.',       'ENCERRADA',   25, 30, FALSE),
        ('9999902', '2026-9999902-000001', 'DENUNCIA',          FALSE, NULL,    'Poço artesiano sem licença',      'Poço sendo perfurado sem placa de licença na Estrada Rural 4.',     'RECEBIDA',    1,  30, FALSE)
       ) AS d (ibge, protocolo, tipo, identificada, sigla, assunto, descricao, status, dias_atras, dias_prazo, prorrogada)
  JOIN municipio m ON m.codigo_ibge = d.ibge
  JOIN tipo_manifestacao t ON t.codigo = d.tipo
  LEFT JOIN cidadao c ON d.identificada AND c.municipio_id = m.id
  LEFT JOIN secretaria s ON s.municipio_id = m.id AND s.sigla = d.sigla
ON CONFLICT (protocolo) DO NOTHING;

-- Bancos criados antes da V7 receberam chave aleatória nas manifestações de demo: volta para a conhecida
UPDATE manifestacao
   SET chave_acesso_hash = encode(sha256(convert_to('DEMO-' || right(protocolo, 6), 'UTF8')), 'hex')
 WHERE protocolo LIKE '2026-999990_-%'
   AND chave_acesso_hash <> encode(sha256(convert_to('DEMO-' || right(protocolo, 6), 'UTF8')), 'hex');

INSERT INTO protocolo_sequencia (municipio_id, ano, ultimo)
SELECT m.id, 2026, x.ultimo
  FROM (VALUES ('9999901', 5), ('9999902', 1)) AS x (ibge, ultimo)
  JOIN municipio m ON m.codigo_ibge = x.ibge
ON CONFLICT (municipio_id, ano) DO UPDATE SET ultimo = GREATEST(protocolo_sequencia.ultimo, EXCLUDED.ultimo);

-- Trâmites: só insere para manifestações de demo que ainda não têm histórico
INSERT INTO tramite (manifestacao_id, status_anterior, status_novo, usuario_id, secretaria_destino_id, descricao, registrado_em)
SELECT ma.id, tr.anterior, tr.novo, u.id, s.id, tr.descricao, ma.data_abertura + make_interval(days => tr.dia)
  FROM (VALUES
        ('2026-9999901-000001', NULL,          'RECEBIDA',    NULL,                   NULL,    'Manifestação registrada',                          0),
        ('2026-9999901-000002', NULL,          'RECEBIDA',    NULL,                   NULL,    'Manifestação registrada',                          0),
        ('2026-9999901-000002', 'RECEBIDA',    'EM_ANALISE',  'ouvidor@demo.gov.br',  NULL,    'Em análise pela ouvidoria',                        1),
        ('2026-9999901-000003', NULL,          'RECEBIDA',    NULL,                   NULL,    'Manifestação registrada',                          0),
        ('2026-9999901-000003', 'RECEBIDA',    'EM_ANALISE',  'ouvidor@demo.gov.br',  NULL,    'Em análise pela ouvidoria',                        1),
        ('2026-9999901-000003', 'EM_ANALISE',  'ENCAMINHADA', 'ouvidor@demo.gov.br',  'SEMOB', 'Encaminhada à Secretaria de Obras',                2),
        ('2026-9999901-000003', 'ENCAMINHADA', 'ENCAMINHADA', 'ouvidor@demo.gov.br',  NULL,    'Prazo prorrogado por 10 dias: volume de contratos exige levantamento no arquivo', 15),
        ('2026-9999901-000004', NULL,          'RECEBIDA',    NULL,                   NULL,    'Manifestação registrada',                          0),
        ('2026-9999901-000004', 'RECEBIDA',    'EM_ANALISE',  'ouvidor@demo.gov.br',  NULL,    'Em análise pela ouvidoria',                        1),
        ('2026-9999901-000004', 'EM_ANALISE',  'ENCAMINHADA', 'ouvidor@demo.gov.br',  'SEMOB', 'Encaminhada à Secretaria de Obras',                1),
        ('2026-9999901-000004', 'ENCAMINHADA', 'RESPONDIDA',  'servidor@demo.gov.br', NULL,    'Resposta enviada pela Secretaria de Obras',        8),
        ('2026-9999901-000005', NULL,          'RECEBIDA',    NULL,                   NULL,    'Manifestação registrada',                          0),
        ('2026-9999901-000005', 'RECEBIDA',    'EM_ANALISE',  'ouvidor@demo.gov.br',  NULL,    'Em análise pela ouvidoria',                        1),
        ('2026-9999901-000005', 'EM_ANALISE',  'ENCAMINHADA', 'ouvidor@demo.gov.br',  'SEMOB', 'Encaminhada à Secretaria de Obras',                2),
        ('2026-9999901-000005', 'ENCAMINHADA', 'RESPONDIDA',  'servidor@demo.gov.br', NULL,    'Elogio repassado à equipe de pavimentação',        10),
        ('2026-9999901-000005', 'RESPONDIDA',  'ENCERRADA',   'ouvidor@demo.gov.br',  NULL,    'Manifestação encerrada',                           24),
        ('2026-9999902-000001', NULL,          'RECEBIDA',    NULL,                   NULL,    'Manifestação registrada',                          0)
       ) AS tr (protocolo, anterior, novo, email, sigla, descricao, dia)
  JOIN manifestacao ma ON ma.protocolo = tr.protocolo
  LEFT JOIN usuario u ON u.email = tr.email
  LEFT JOIN secretaria s ON s.municipio_id = ma.municipio_id AND s.sigla = tr.sigla
 WHERE NOT EXISTS (SELECT 1 FROM tramite x WHERE x.manifestacao_id = ma.id);

-- O pedido LAI 000004 foi atendido em parte (documento preparatório, LAI art. 7º, § 3º): cabe recurso,
-- então a demo mostra o link "recurso" para o cidadão
INSERT INTO resposta (manifestacao_id, usuario_id, secretaria_id, texto, resultado_lai, respondida_em)
SELECT ma.id, u.id, s.id, r.texto, r.resultado, ma.data_abertura + make_interval(days => r.dia)
  FROM (VALUES
        ('2026-9999901-000004', 'SEMOB', 'As obras do Jardim Sul começam em novembro, com conclusão prevista para março. O cronograma detalhado por rua ainda está em elaboração e será disponibilizado quando aprovado (Lei 12.527, art. 7º, § 3º).', 'PARCIALMENTE_CONCEDIDO', 8),
        ('2026-9999901-000005', 'SEMOB', 'Agradecemos o elogio, que foi repassado à equipe de pavimentação.', NULL, 10)
       ) AS r (protocolo, sigla, texto, resultado, dia)
  JOIN manifestacao ma ON ma.protocolo = r.protocolo
  JOIN secretaria s ON s.municipio_id = ma.municipio_id AND s.sigla = r.sigla
  JOIN usuario u ON u.email = 'servidor@demo.gov.br'
 WHERE NOT EXISTS (SELECT 1 FROM resposta x WHERE x.manifestacao_id = ma.id);

-- Bancos que já tinham a resposta de demo antes da V7: registra o resultado LAI
UPDATE resposta r
   SET resultado_lai = 'PARCIALMENTE_CONCEDIDO'
  FROM manifestacao ma
 WHERE r.manifestacao_id = ma.id
   AND ma.protocolo = '2026-9999901-000004'
   AND r.resultado_lai IS NULL;
