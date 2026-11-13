-- V13: propriedade de estoque, sem atribuir proprietario ao legado.
CREATE TABLE estoques (id BIGINT, codigo VARCHAR(60), nome VARCHAR(160), tipo VARCHAR(20));
ALTER TABLE materiais ADD COLUMN estoque_id BIGINT;
ALTER TABLE movimentos ADD COLUMN estoque_id BIGINT;
ALTER TABLE itens_os ADD COLUMN estoque_id BIGINT;

-- Contar antes de adicionar a regra; nunca corrigir registros.
SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight estoques_pkey: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM (SELECT id FROM estoques GROUP BY id HAVING count(*)>1) d) preflight(violacoes);

-- Contar antes de adicionar a regra; nunca corrigir registros.
SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight estoques_not_null: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM estoques WHERE id IS NULL OR codigo IS NULL OR nome IS NULL OR tipo IS NULL) preflight(violacoes);

-- Contar antes de adicionar a regra; nunca corrigir registros.
SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight uq_estoques_codigo: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM (SELECT codigo FROM estoques GROUP BY codigo HAVING count(*)>1) d) preflight(violacoes);

-- Contar antes de adicionar a regra; nunca corrigir registros.
SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight ck_estoques_tipo: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM estoques WHERE (tipo IN ('EMPRESA','CLIENTE')) IS NOT TRUE) preflight(violacoes);

-- Contar antes de adicionar a regra; nunca corrigir registros.
SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight ck_estoques_codigo_preenchido: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM estoques WHERE (length(regexp_replace(codigo, '[\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]', '')) > 0 AND codigo = regexp_replace(codigo, '^[\u0001-\u0020]+|[\u0001-\u0020]+$', '')) IS NOT TRUE) preflight(violacoes);

-- Contar antes de adicionar a regra; nunca corrigir registros.
SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight ck_estoques_nome_preenchido: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM estoques WHERE (length(regexp_replace(nome, '[\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]', '')) > 0 AND nome = regexp_replace(nome, '^[\u0001-\u0020]+|[\u0001-\u0020]+$', '')) IS NOT TRUE) preflight(violacoes);

-- Contar antes de adicionar a regra; nunca corrigir registros.
SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight fk_materiais_estoque: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM materiais m WHERE estoque_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM estoques e WHERE e.id=m.estoque_id)) preflight(violacoes);

-- Contar antes de adicionar a regra; nunca corrigir registros.
SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight fk_movimentos_estoque: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM movimentos m WHERE estoque_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM estoques e WHERE e.id=m.estoque_id)) preflight(violacoes);

-- Contar antes de adicionar a regra; nunca corrigir registros.
SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight fk_itens_os_estoque: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM itens_os m WHERE estoque_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM estoques e WHERE e.id=m.estoque_id)) preflight(violacoes);

-- Contar antes de adicionar a regra; nunca corrigir registros.
SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight uq_materiais_id_estoque: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM (SELECT id,estoque_id FROM materiais GROUP BY id,estoque_id HAVING count(*)>1) d) preflight(violacoes);

-- Contar antes de adicionar a regra; nunca corrigir registros.
SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight fk_movimentos_material_estoque: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM movimentos m WHERE estoque_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM materiais p WHERE p.id=m.material_id AND p.estoque_id=m.estoque_id)) preflight(violacoes);

-- Contar antes de adicionar a regra; nunca corrigir registros.
SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight fk_itens_os_material_estoque: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM itens_os m WHERE estoque_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM materiais p WHERE p.id=m.material_id AND p.estoque_id=m.estoque_id)) preflight(violacoes);

ALTER TABLE estoques ALTER COLUMN id SET NOT NULL;
ALTER TABLE estoques ADD CONSTRAINT estoques_pkey PRIMARY KEY(id);
ALTER TABLE estoques ALTER COLUMN codigo SET NOT NULL;
ALTER TABLE estoques ALTER COLUMN nome SET NOT NULL;
ALTER TABLE estoques ALTER COLUMN tipo SET NOT NULL;
ALTER TABLE estoques ADD CONSTRAINT uq_estoques_codigo UNIQUE(codigo);
ALTER TABLE estoques ADD CONSTRAINT ck_estoques_tipo CHECK(tipo IN ('EMPRESA','CLIENTE'));
ALTER TABLE estoques ADD CONSTRAINT ck_estoques_codigo_preenchido CHECK((length(regexp_replace(codigo, '[\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]', '')) > 0 AND codigo = regexp_replace(codigo, '^[\u0001-\u0020]+|[\u0001-\u0020]+$', '')) IS TRUE);
ALTER TABLE estoques ADD CONSTRAINT ck_estoques_nome_preenchido CHECK((length(regexp_replace(nome, '[\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]', '')) > 0 AND nome = regexp_replace(nome, '^[\u0001-\u0020]+|[\u0001-\u0020]+$', '')) IS TRUE);
ALTER TABLE materiais ADD CONSTRAINT fk_materiais_estoque FOREIGN KEY(estoque_id) REFERENCES estoques(id);
ALTER TABLE movimentos ADD CONSTRAINT fk_movimentos_estoque FOREIGN KEY(estoque_id) REFERENCES estoques(id);
ALTER TABLE itens_os ADD CONSTRAINT fk_itens_os_estoque FOREIGN KEY(estoque_id) REFERENCES estoques(id);
ALTER TABLE materiais ADD CONSTRAINT uq_materiais_id_estoque UNIQUE(id,estoque_id);
ALTER TABLE movimentos ADD CONSTRAINT fk_movimentos_material_estoque FOREIGN KEY(material_id,estoque_id) REFERENCES materiais(id,estoque_id);
ALTER TABLE itens_os ADD CONSTRAINT fk_itens_os_material_estoque FOREIGN KEY(material_id,estoque_id) REFERENCES materiais(id,estoque_id);
CREATE INDEX idx_materiais_estoque ON materiais(estoque_id);
CREATE INDEX idx_movimentos_estoque ON movimentos(estoque_id);
