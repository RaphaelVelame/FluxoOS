-- Reversao de V10; somente metadados, sem alterar registros.
ALTER TABLE idempotencia DROP CONSTRAINT ck_idempotencia_conteudo;
ALTER TABLE idempotencia DROP CONSTRAINT ck_idempotencia_chave;
ALTER TABLE ordens DROP CONSTRAINT ck_ordens_conteudo_abertura;
ALTER TABLE ordens DROP CONSTRAINT ck_ordens_chave_abertura;
ALTER TABLE ordens DROP CONSTRAINT ck_ordens_abertura_conjunto;
