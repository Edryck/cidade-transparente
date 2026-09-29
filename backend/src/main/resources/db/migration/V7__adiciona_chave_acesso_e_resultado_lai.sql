-- Chave de acesso da consulta pública por protocolo. O protocolo (ANO-IBGE-SEQUENCIAL) é previsível e não
-- pode servir de credencial: sem a chave, qualquer um percorreria os números e leria manifestações alheias
-- (LGPD arts. 6º, VII, e 46). Só o hash SHA-256 é guardado; a chave aparece uma única vez, na abertura.
ALTER TABLE manifestacao ADD COLUMN chave_acesso_hash VARCHAR(64);

-- Manifestações já existentes recebem uma chave aleatória que ninguém conhece: continuam acessíveis pelo
-- login do cidadão e da equipe, só não pela consulta pública.
UPDATE manifestacao SET chave_acesso_hash = encode(sha256(gen_random_uuid()::text::bytea), 'hex');

ALTER TABLE manifestacao ALTER COLUMN chave_acesso_hash SET NOT NULL;

-- Resultado da resposta a pedido LAI (Lei 12.527, art. 11, § 1º). Define se cabe recurso: o art. 15 só o
-- admite contra indeferimento de acesso (NEGADO ou PARCIALMENTE_CONCEDIDO). Nulo para tipos de ouvidoria.
ALTER TABLE resposta
    ADD COLUMN resultado_lai VARCHAR(25),
    ADD CONSTRAINT ck_resposta_resultado_lai CHECK (resultado_lai IN (
        'CONCEDIDO', 'PARCIALMENTE_CONCEDIDO', 'NEGADO', 'INEXISTENTE'
    ));
