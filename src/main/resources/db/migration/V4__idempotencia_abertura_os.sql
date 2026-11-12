ALTER TABLE ordens ADD COLUMN autor_abertura_id BIGINT REFERENCES usuarios(id);
ALTER TABLE ordens ADD COLUMN chave_abertura VARCHAR(80);
ALTER TABLE ordens ADD COLUMN conteudo_abertura VARCHAR(64);
ALTER TABLE ordens ADD CONSTRAINT uq_ordens_abertura UNIQUE (autor_abertura_id, chave_abertura);
