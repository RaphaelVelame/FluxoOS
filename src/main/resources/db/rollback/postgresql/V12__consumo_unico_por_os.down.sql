-- Reversao de V12; somente metadados, sem alterar registros.
ALTER TABLE movimentos DROP CONSTRAINT uq_movimentos_consumo_os_material;
