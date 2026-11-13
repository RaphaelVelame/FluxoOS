BEGIN;
SET LOCAL lock_timeout = '10s';
LOCK TABLE empresas, usuarios, usuario_roles, ordens, materiais, estoques, movimentos, itens_os IN ACCESS EXCLUSIVE MODE;
-- Executar somente apos V16 e V15 down, em manutencao exclusiva.
-- Recusar qualquer perda de tenant ou funcao nova; a volta nao apaga nem corrige linhas de negocio.
WITH esperado AS (SELECT id AS usuario_id,'ROLE_ADMIN_EMPRESA' AS role FROM usuarios WHERE perfil='CHEFE'
UNION ALL SELECT id,'ROLE_SUPORTE_TECNICO' FROM usuarios WHERE perfil='SUPORTE_TI'
UNION ALL SELECT id,'ROLE_ESTOQUISTA' FROM usuarios WHERE perfil='SUPORTE_TI'
UNION ALL SELECT id,'ROLE_SUPORTE_EXTERNO' FROM usuarios WHERE perfil='TECNICO'), diferencas AS (
 SELECT usuario_id,role FROM usuario_roles EXCEPT SELECT usuario_id,role FROM esperado
), ausentes AS (
 SELECT usuario_id,role FROM esperado EXCEPT SELECT usuario_id,role FROM usuario_roles
)
SELECT CAST(CASE WHEN n=0 THEN '0' ELSE 'Preflight rollback_V14_roles_preservadas: ' || CAST(n AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT (SELECT count(*) FROM diferencas)+(SELECT count(*) FROM ausentes) AS n) p;
SELECT CAST(CASE WHEN n=0 THEN '0' ELSE 'Preflight rollback_V14_tenants_preservados: ' || CAST(n AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT (SELECT count(*) FROM empresas WHERE id<>1 OR nome<>'Empresa legada')+
(SELECT count(*) FROM usuarios WHERE empresa_id IS DISTINCT FROM 1)+(SELECT count(*) FROM ordens WHERE empresa_id IS DISTINCT FROM 1)+(SELECT count(*) FROM materiais WHERE empresa_id IS DISTINCT FROM 1)+(SELECT count(*) FROM estoques WHERE empresa_id IS DISTINCT FROM 1)+(SELECT count(*) FROM movimentos WHERE empresa_id IS DISTINCT FROM 1)+(SELECT count(*) FROM itens_os WHERE empresa_id IS DISTINCT FROM 1) AS n) p;
DROP TABLE usuario_roles;
ALTER TABLE itens_os DROP COLUMN empresa_id;
ALTER TABLE movimentos DROP COLUMN empresa_id;
ALTER TABLE estoques DROP COLUMN empresa_id;
ALTER TABLE materiais DROP COLUMN empresa_id;
ALTER TABLE ordens DROP COLUMN empresa_id;
ALTER TABLE usuarios DROP COLUMN empresa_id;
DROP TABLE empresas;
COMMIT;
