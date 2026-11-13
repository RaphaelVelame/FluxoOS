BEGIN;
SET LOCAL lock_timeout = '10s';
LOCK TABLE notificacoes IN ACCESS EXCLUSIVE MODE;
-- Notificacoes registradas exigem preservar dados antes de planejar uma volta.
SELECT CAST(CASE WHEN n=0 THEN '0' ELSE 'Preflight rollback_V16_notificacoes_preservadas: ' || CAST(n AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) AS n FROM notificacoes) p;
DROP TABLE notificacoes;
COMMIT;
