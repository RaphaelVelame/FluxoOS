-- Apenas para funcionalidade ainda sem dados. Recusar a perda de propriedade registrada.
-- Contar antes de adicionar a regra; nunca corrigir registros.
SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight rollback_V13_propriedade_preservada: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT (SELECT count(*) FROM estoques)+(SELECT count(*) FROM materiais WHERE estoque_id IS NOT NULL)+(SELECT count(*) FROM movimentos WHERE estoque_id IS NOT NULL)+(SELECT count(*) FROM itens_os WHERE estoque_id IS NOT NULL)) preflight(violacoes);
ALTER TABLE itens_os DROP CONSTRAINT fk_itens_os_material_estoque;
ALTER TABLE movimentos DROP CONSTRAINT fk_movimentos_material_estoque;
ALTER TABLE itens_os DROP CONSTRAINT fk_itens_os_estoque;
ALTER TABLE movimentos DROP CONSTRAINT fk_movimentos_estoque;
ALTER TABLE materiais DROP CONSTRAINT fk_materiais_estoque;
ALTER TABLE materiais DROP CONSTRAINT uq_materiais_id_estoque;
DROP INDEX idx_materiais_estoque;
DROP INDEX idx_movimentos_estoque;
ALTER TABLE itens_os DROP COLUMN estoque_id;
ALTER TABLE movimentos DROP COLUMN estoque_id;
ALTER TABLE materiais DROP COLUMN estoque_id;
DROP TABLE estoques;
