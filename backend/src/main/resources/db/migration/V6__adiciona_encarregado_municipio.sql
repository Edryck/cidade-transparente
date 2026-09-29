-- Encarregado pelo tratamento de dados pessoais (LGPD arts. 23, III, e 41): indicado pela prefeitura,
-- que é a controladora, e divulgado publicamente no aviso de privacidade (art. 41, § 1º).
-- Nulo para municípios já cadastrados: a migration não tem como saber quem é o encarregado real, e inventar
-- um valor seria pior que deixar a lacuna visível. A API exige o encarregado em todo município novo.
ALTER TABLE municipio
    ADD COLUMN encarregado_nome  VARCHAR(150),
    ADD COLUMN encarregado_email VARCHAR(150),
    ADD CONSTRAINT ck_municipio_encarregado CHECK ((encarregado_nome IS NULL) = (encarregado_email IS NULL));
