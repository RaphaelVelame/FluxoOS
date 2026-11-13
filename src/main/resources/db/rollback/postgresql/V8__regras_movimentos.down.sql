-- Reversao de V8; somente metadados, sem alterar registros.
ALTER TABLE movimentos DROP CONSTRAINT ck_movimentos_ordem_por_tipo;
ALTER TABLE movimentos DROP CONSTRAINT ck_movimentos_sinal;
ALTER TABLE movimentos DROP CONSTRAINT ck_movimentos_tipo;
