-- Somente leitura em V16: projeta a conversao V17 sem gravar nada.
-- IDs de estoque servem apenas como chaves virtuais nesta contagem; V17 usa a sequencia.
WITH clientes AS (SELECT id,empresa_id,codigo,nome FROM public.estoques WHERE tipo='CLIENTE'),
estoques AS (SELECT e.*,CASE WHEN tipo='CLIENTE' THEN id ELSE NULL END AS cliente_id FROM public.estoques e),
ordens AS (SELECT o.*,CAST(NULL AS BIGINT) AS cliente_id,CAST('LEGADO' AS VARCHAR) AS tipo_atendimento FROM public.ordens o)
SELECT 'clientes_not_null' AS regra, (SELECT count(*) FROM clientes WHERE id IS NULL OR empresa_id IS NULL OR codigo IS NULL OR nome IS NULL) AS linhas_incompativeis;

WITH clientes AS (SELECT id,empresa_id,codigo,nome FROM public.estoques WHERE tipo='CLIENTE'),
estoques AS (SELECT e.*,CASE WHEN tipo='CLIENTE' THEN id ELSE NULL END AS cliente_id FROM public.estoques e),
ordens AS (SELECT o.*,CAST(NULL AS BIGINT) AS cliente_id,CAST('LEGADO' AS VARCHAR) AS tipo_atendimento FROM public.ordens o)
SELECT 'clientes_pkey' AS regra, (SELECT coalesce(sum(n),0) FROM (SELECT count(*) n FROM clientes GROUP BY id HAVING count(*)>1) d) AS linhas_incompativeis;

WITH clientes AS (SELECT id,empresa_id,codigo,nome FROM public.estoques WHERE tipo='CLIENTE'),
estoques AS (SELECT e.*,CASE WHEN tipo='CLIENTE' THEN id ELSE NULL END AS cliente_id FROM public.estoques e),
ordens AS (SELECT o.*,CAST(NULL AS BIGINT) AS cliente_id,CAST('LEGADO' AS VARCHAR) AS tipo_atendimento FROM public.ordens o)
SELECT 'uq_clientes_id_empresa' AS regra, (SELECT coalesce(sum(n),0) FROM (SELECT count(*) n FROM clientes GROUP BY id,empresa_id HAVING count(*)>1) d) AS linhas_incompativeis;

WITH clientes AS (SELECT id,empresa_id,codigo,nome FROM public.estoques WHERE tipo='CLIENTE'),
estoques AS (SELECT e.*,CASE WHEN tipo='CLIENTE' THEN id ELSE NULL END AS cliente_id FROM public.estoques e),
ordens AS (SELECT o.*,CAST(NULL AS BIGINT) AS cliente_id,CAST('LEGADO' AS VARCHAR) AS tipo_atendimento FROM public.ordens o)
SELECT 'uq_clientes_codigo_empresa' AS regra, (SELECT coalesce(sum(n),0) FROM (SELECT count(*) n FROM clientes GROUP BY empresa_id,codigo HAVING count(*)>1) d) AS linhas_incompativeis;

WITH clientes AS (SELECT id,empresa_id,codigo,nome FROM public.estoques WHERE tipo='CLIENTE'),
estoques AS (SELECT e.*,CASE WHEN tipo='CLIENTE' THEN id ELSE NULL END AS cliente_id FROM public.estoques e),
ordens AS (SELECT o.*,CAST(NULL AS BIGINT) AS cliente_id,CAST('LEGADO' AS VARCHAR) AS tipo_atendimento FROM public.ordens o)
SELECT 'fk_clientes_empresa' AS regra, (SELECT count(*) FROM clientes c WHERE NOT EXISTS(SELECT 1 FROM empresas p WHERE p.id=c.empresa_id)) AS linhas_incompativeis;

WITH clientes AS (SELECT id,empresa_id,codigo,nome FROM public.estoques WHERE tipo='CLIENTE'),
estoques AS (SELECT e.*,CASE WHEN tipo='CLIENTE' THEN id ELSE NULL END AS cliente_id FROM public.estoques e),
ordens AS (SELECT o.*,CAST(NULL AS BIGINT) AS cliente_id,CAST('LEGADO' AS VARCHAR) AS tipo_atendimento FROM public.ordens o)
SELECT 'ck_clientes_codigo' AS regra, (SELECT count(*) FROM clientes WHERE NOT (((length(regexp_replace(codigo, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0 AND codigo = regexp_replace(codigo, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g')) IS TRUE))) AS linhas_incompativeis;

WITH clientes AS (SELECT id,empresa_id,codigo,nome FROM public.estoques WHERE tipo='CLIENTE'),
estoques AS (SELECT e.*,CASE WHEN tipo='CLIENTE' THEN id ELSE NULL END AS cliente_id FROM public.estoques e),
ordens AS (SELECT o.*,CAST(NULL AS BIGINT) AS cliente_id,CAST('LEGADO' AS VARCHAR) AS tipo_atendimento FROM public.ordens o)
SELECT 'ck_clientes_nome' AS regra, (SELECT count(*) FROM clientes WHERE NOT (((length(regexp_replace(nome, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0 AND nome = regexp_replace(nome, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g')) IS TRUE))) AS linhas_incompativeis;

WITH clientes AS (SELECT id,empresa_id,codigo,nome FROM public.estoques WHERE tipo='CLIENTE'),
estoques AS (SELECT e.*,CASE WHEN tipo='CLIENTE' THEN id ELSE NULL END AS cliente_id FROM public.estoques e),
ordens AS (SELECT o.*,CAST(NULL AS BIGINT) AS cliente_id,CAST('LEGADO' AS VARCHAR) AS tipo_atendimento FROM public.ordens o)
SELECT 'fk_estoques_cliente_empresa' AS regra, (SELECT count(*) FROM estoques e WHERE cliente_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM clientes c WHERE c.id=e.cliente_id AND c.empresa_id=e.empresa_id)) AS linhas_incompativeis;

WITH clientes AS (SELECT id,empresa_id,codigo,nome FROM public.estoques WHERE tipo='CLIENTE'),
estoques AS (SELECT e.*,CASE WHEN tipo='CLIENTE' THEN id ELSE NULL END AS cliente_id FROM public.estoques e),
ordens AS (SELECT o.*,CAST(NULL AS BIGINT) AS cliente_id,CAST('LEGADO' AS VARCHAR) AS tipo_atendimento FROM public.ordens o)
SELECT 'ck_estoques_cliente' AS regra, (SELECT count(*) FROM estoques WHERE ((tipo='CLIENTE' AND cliente_id IS NOT NULL) OR (tipo='EMPRESA' AND cliente_id IS NULL)) IS NOT TRUE) AS linhas_incompativeis;

WITH clientes AS (SELECT id,empresa_id,codigo,nome FROM public.estoques WHERE tipo='CLIENTE'),
estoques AS (SELECT e.*,CASE WHEN tipo='CLIENTE' THEN id ELSE NULL END AS cliente_id FROM public.estoques e),
ordens AS (SELECT o.*,CAST(NULL AS BIGINT) AS cliente_id,CAST('LEGADO' AS VARCHAR) AS tipo_atendimento FROM public.ordens o)
SELECT 'fk_ordens_cliente_empresa' AS regra, (SELECT count(*) FROM ordens o WHERE cliente_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM clientes c WHERE c.id=o.cliente_id AND c.empresa_id=o.empresa_id)) AS linhas_incompativeis;

WITH clientes AS (SELECT id,empresa_id,codigo,nome FROM public.estoques WHERE tipo='CLIENTE'),
estoques AS (SELECT e.*,CASE WHEN tipo='CLIENTE' THEN id ELSE NULL END AS cliente_id FROM public.estoques e),
ordens AS (SELECT o.*,CAST(NULL AS BIGINT) AS cliente_id,CAST('LEGADO' AS VARCHAR) AS tipo_atendimento FROM public.ordens o)
SELECT 'ordens_tipo_atendimento_not_null' AS regra, (SELECT count(*) FROM ordens WHERE tipo_atendimento IS NULL) AS linhas_incompativeis;

WITH clientes AS (SELECT id,empresa_id,codigo,nome FROM public.estoques WHERE tipo='CLIENTE'),
estoques AS (SELECT e.*,CASE WHEN tipo='CLIENTE' THEN id ELSE NULL END AS cliente_id FROM public.estoques e),
ordens AS (SELECT o.*,CAST(NULL AS BIGINT) AS cliente_id,CAST('LEGADO' AS VARCHAR) AS tipo_atendimento FROM public.ordens o)
SELECT 'ck_ordens_cliente' AS regra, (SELECT count(*) FROM ordens WHERE ((tipo_atendimento='CLIENTE' AND cliente_id IS NOT NULL) OR (tipo_atendimento IN ('LEGADO','INTERNO') AND cliente_id IS NULL)) IS NOT TRUE) AS linhas_incompativeis;
