ALTER TABLE auditoria ADD COLUMN alvo_id BIGINT REFERENCES usuarios(id);
