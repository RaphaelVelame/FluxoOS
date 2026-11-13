-- Reversao de V11; somente metadados, sem alterar registros.
ALTER TABLE idempotencia DROP CONSTRAINT fk_idempotencia_movimento_autor;
ALTER TABLE movimentos DROP CONSTRAINT uq_movimentos_id_usuario;
