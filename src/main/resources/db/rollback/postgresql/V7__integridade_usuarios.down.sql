-- Reversao de V7; somente metadados, sem alterar registros.
ALTER TABLE usuarios DROP CONSTRAINT ck_usuarios_versao_sessao_nao_negativa;
ALTER TABLE usuarios DROP CONSTRAINT ck_usuarios_hash_formato;
ALTER TABLE usuarios DROP CONSTRAINT ck_usuarios_login_formato;
