-- V18: todas as contagens precedem qualquer constraint.
-- Contagem anterior a constraint. Abortar e tratar explicitamente, nunca corrigir automaticamente.
SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight clientes_not_null: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM clientes WHERE id IS NULL OR empresa_id IS NULL OR codigo IS NULL OR nome IS NULL) preflight(violacoes);

-- Contagem anterior a constraint. Abortar e tratar explicitamente, nunca corrigir automaticamente.
SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight clientes_pkey: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT coalesce(sum(n),0) FROM (SELECT count(*) n FROM clientes GROUP BY id HAVING count(*)>1) d) preflight(violacoes);

-- Contagem anterior a constraint. Abortar e tratar explicitamente, nunca corrigir automaticamente.
SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight uq_clientes_id_empresa: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT coalesce(sum(n),0) FROM (SELECT count(*) n FROM clientes GROUP BY id,empresa_id HAVING count(*)>1) d) preflight(violacoes);

-- Contagem anterior a constraint. Abortar e tratar explicitamente, nunca corrigir automaticamente.
SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight uq_clientes_codigo_empresa: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT coalesce(sum(n),0) FROM (SELECT count(*) n FROM clientes GROUP BY empresa_id,codigo HAVING count(*)>1) d) preflight(violacoes);

-- Contagem anterior a constraint. Abortar e tratar explicitamente, nunca corrigir automaticamente.
SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight fk_clientes_empresa: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM clientes c WHERE NOT EXISTS(SELECT 1 FROM empresas p WHERE p.id=c.empresa_id)) preflight(violacoes);

-- Contagem anterior a constraint. Abortar e tratar explicitamente, nunca corrigir automaticamente.
SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight ck_clientes_codigo: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM clientes WHERE NOT (((length(regexp_replace(codigo, '[\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]', '')) > 0 AND codigo = regexp_replace(codigo, '^[\u0001-\u0020]+|[\u0001-\u0020]+$', '')) IS TRUE))) preflight(violacoes);

-- Contagem anterior a constraint. Abortar e tratar explicitamente, nunca corrigir automaticamente.
SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight ck_clientes_nome: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM clientes WHERE NOT (((length(regexp_replace(nome, '[\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]', '')) > 0 AND nome = regexp_replace(nome, '^[\u0001-\u0020]+|[\u0001-\u0020]+$', '')) IS TRUE))) preflight(violacoes);

-- Contagem anterior a constraint. Abortar e tratar explicitamente, nunca corrigir automaticamente.
SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight fk_estoques_cliente_empresa: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM estoques e WHERE cliente_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM clientes c WHERE c.id=e.cliente_id AND c.empresa_id=e.empresa_id)) preflight(violacoes);

-- Contagem anterior a constraint. Abortar e tratar explicitamente, nunca corrigir automaticamente.
SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight ck_estoques_cliente: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM estoques WHERE ((tipo='CLIENTE' AND cliente_id IS NOT NULL) OR (tipo='EMPRESA' AND cliente_id IS NULL)) IS NOT TRUE) preflight(violacoes);

-- Contagem anterior a constraint. Abortar e tratar explicitamente, nunca corrigir automaticamente.
SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight fk_ordens_cliente_empresa: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM ordens o WHERE cliente_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM clientes c WHERE c.id=o.cliente_id AND c.empresa_id=o.empresa_id)) preflight(violacoes);

-- Contagem anterior a constraint. Abortar e tratar explicitamente, nunca corrigir automaticamente.
SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight ordens_tipo_atendimento_not_null: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM ordens WHERE tipo_atendimento IS NULL) preflight(violacoes);

-- Contagem anterior a constraint. Abortar e tratar explicitamente, nunca corrigir automaticamente.
SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight ck_ordens_cliente: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM ordens WHERE ((tipo_atendimento='CLIENTE' AND cliente_id IS NOT NULL) OR (tipo_atendimento IN ('LEGADO','INTERNO') AND cliente_id IS NULL)) IS NOT TRUE) preflight(violacoes);

ALTER TABLE clientes ALTER COLUMN id SET NOT NULL;
ALTER TABLE clientes ALTER COLUMN empresa_id SET NOT NULL;
ALTER TABLE clientes ALTER COLUMN codigo SET NOT NULL;
ALTER TABLE clientes ALTER COLUMN nome SET NOT NULL;
ALTER TABLE clientes ADD CONSTRAINT clientes_pkey PRIMARY KEY(id);
ALTER TABLE clientes ADD CONSTRAINT uq_clientes_id_empresa UNIQUE(id,empresa_id);
ALTER TABLE clientes ADD CONSTRAINT uq_clientes_codigo_empresa UNIQUE(empresa_id,codigo);
ALTER TABLE clientes ADD CONSTRAINT fk_clientes_empresa FOREIGN KEY(empresa_id) REFERENCES empresas(id);
ALTER TABLE clientes ADD CONSTRAINT ck_clientes_codigo CHECK((length(regexp_replace(codigo, '[\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]', '')) > 0 AND codigo = regexp_replace(codigo, '^[\u0001-\u0020]+|[\u0001-\u0020]+$', '')) IS TRUE);
ALTER TABLE clientes ADD CONSTRAINT ck_clientes_nome CHECK((length(regexp_replace(nome, '[\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]', '')) > 0 AND nome = regexp_replace(nome, '^[\u0001-\u0020]+|[\u0001-\u0020]+$', '')) IS TRUE);
ALTER TABLE estoques ADD CONSTRAINT fk_estoques_cliente_empresa FOREIGN KEY(cliente_id,empresa_id) REFERENCES clientes(id,empresa_id);
ALTER TABLE estoques ADD CONSTRAINT ck_estoques_cliente CHECK(((tipo='CLIENTE' AND cliente_id IS NOT NULL) OR (tipo='EMPRESA' AND cliente_id IS NULL)) IS TRUE);
ALTER TABLE ordens ADD CONSTRAINT fk_ordens_cliente_empresa FOREIGN KEY(cliente_id,empresa_id) REFERENCES clientes(id,empresa_id);
ALTER TABLE ordens ALTER COLUMN tipo_atendimento SET NOT NULL;
ALTER TABLE ordens ADD CONSTRAINT ck_ordens_cliente CHECK(((tipo_atendimento='CLIENTE' AND cliente_id IS NOT NULL) OR (tipo_atendimento IN ('LEGADO','INTERNO') AND cliente_id IS NULL)) IS TRUE);
CREATE INDEX idx_estoques_cliente_empresa ON estoques(cliente_id,empresa_id);
CREATE INDEX idx_ordens_cliente_empresa ON ordens(cliente_id,empresa_id);
