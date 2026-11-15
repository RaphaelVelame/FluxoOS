-- Executar depois de reverter V18. Recusa descartar informacao nova; nunca limpar para passar.
-- H2: parar todos os escritores; DDL tem autocommit. Fazer backup antes.
-- Contagem anterior a constraint. Abortar e tratar explicitamente, nunca corrigir automaticamente.
SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight rollback_V17_atendimentos_preservados: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM ordens WHERE cliente_id IS NOT NULL OR tipo_atendimento IS DISTINCT FROM 'LEGADO') preflight(violacoes);
-- Contagem anterior a constraint. Abortar e tratar explicitamente, nunca corrigir automaticamente.
SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight rollback_V17_clientes_preservados: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM clientes c WHERE (SELECT count(*) FROM estoques e WHERE e.tipo='CLIENTE' AND e.cliente_id=c.id AND e.empresa_id=c.empresa_id AND e.codigo=c.codigo AND e.nome=c.nome)<>1 OR EXISTS(SELECT 1 FROM estoques e WHERE e.cliente_id=c.id AND (e.tipo<>'CLIENTE' OR e.empresa_id<>c.empresa_id OR e.codigo<>c.codigo OR e.nome<>c.nome))) preflight(violacoes);
-- Contagem anterior a constraint. Abortar e tratar explicitamente, nunca corrigir automaticamente.
SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight rollback_V17_vinculos_preservados: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM estoques e WHERE (e.tipo='EMPRESA' AND e.cliente_id IS NOT NULL) OR (e.tipo='CLIENTE' AND NOT EXISTS(SELECT 1 FROM clientes c WHERE c.id=e.cliente_id AND c.empresa_id=e.empresa_id AND c.codigo=e.codigo AND c.nome=e.nome))) preflight(violacoes);

ALTER TABLE ordens DROP COLUMN tipo_atendimento;
ALTER TABLE ordens DROP COLUMN cliente_id;
ALTER TABLE estoques DROP COLUMN cliente_id;
DROP TABLE clientes;
