-- V12: consumo_unico_por_os
-- Preflight de todo o assunto antes do primeiro DDL; abortar, nunca sanear dados.

-- uq_movimentos_consumo_os_material: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight uq_movimentos_consumo_os_material: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM (SELECT ordem_id,material_id FROM movimentos WHERE ordem_id IS NOT NULL GROUP BY ordem_id,material_id HAVING count(*)>1) d) preflight(violacoes);

ALTER TABLE movimentos ADD CONSTRAINT uq_movimentos_consumo_os_material
    UNIQUE (ordem_id, material_id);
