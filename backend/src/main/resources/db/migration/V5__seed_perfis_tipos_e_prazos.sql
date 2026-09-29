-- Dados de referência necessários em qualquer ambiente

INSERT INTO perfil (nome) VALUES
    ('ADMIN_PLATAFORMA'),
    ('ADMIN'),
    ('OUVIDOR'),
    ('SERVIDOR'),
    ('CIDADAO');

-- Ouvidoria (Lei 13.460/2017) não tem fase recursal; LAI (Lei 12.527/2011) tem, e exige identificação
INSERT INTO tipo_manifestacao (codigo, nome, categoria, permite_anonimo, permite_recurso) VALUES
    ('RECLAMACAO',         'Reclamação',             'OUVIDORIA', FALSE, FALSE),
    ('DENUNCIA',           'Denúncia',               'OUVIDORIA', TRUE,  FALSE),
    ('SUGESTAO',           'Sugestão',               'OUVIDORIA', FALSE, FALSE),
    ('ELOGIO',             'Elogio',                 'OUVIDORIA', FALSE, FALSE),
    ('PEDIDO_INFORMACAO',  'Pedido de informação',   'LAI',       FALSE, TRUE);

-- Regras federais (municipio_id NULL). Ouvidoria: 30 + 30 (art. 16 da Lei 13.460).
-- LAI: 20 + 10 (art. 11), recurso em 10 dias e julgamento em 5 (art. 15).
INSERT INTO prazo_legal (tipo_manifestacao_id, municipio_id, dias_resposta, permite_prorrogacao,
                         dias_prorrogacao, dias_interposicao_recurso, dias_julgamento_recurso)
SELECT t.id, NULL, 30, TRUE, 30, NULL, NULL
  FROM tipo_manifestacao t
 WHERE t.categoria = 'OUVIDORIA';

INSERT INTO prazo_legal (tipo_manifestacao_id, municipio_id, dias_resposta, permite_prorrogacao,
                         dias_prorrogacao, dias_interposicao_recurso, dias_julgamento_recurso)
SELECT t.id, NULL, 20, TRUE, 10, 10, 5
  FROM tipo_manifestacao t
 WHERE t.categoria = 'LAI';
