-- V10: formato_idempotencia
-- Preflight de todo o assunto antes do primeiro DDL; abortar, nunca sanear dados.

-- ck_ordens_abertura_conjunto: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_ordens_abertura_conjunto: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM ordens WHERE ((CASE WHEN autor_abertura_id IS NULL THEN 0 ELSE 1 END + CASE WHEN chave_abertura IS NULL THEN 0 ELSE 1 END + CASE WHEN conteudo_abertura IS NULL THEN 0 ELSE 1 END) IN (0,3)) IS NOT TRUE) preflight(violacoes);

-- ck_ordens_chave_abertura: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_ordens_chave_abertura: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM ordens WHERE (chave_abertura IS NULL OR REGEXP_LIKE(chave_abertura, '^[a-zA-Z0-9-]{16,80}\z')) IS NOT TRUE) preflight(violacoes);

-- ck_ordens_conteudo_abertura: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_ordens_conteudo_abertura: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM ordens WHERE (conteudo_abertura IS NULL OR REGEXP_LIKE(conteudo_abertura, '^[0-9a-f]{64}\z')) IS NOT TRUE) preflight(violacoes);

-- ck_idempotencia_chave: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_idempotencia_chave: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM idempotencia WHERE (REGEXP_LIKE(chave, '^[a-zA-Z0-9-]{16,80}\z')) IS NOT TRUE) preflight(violacoes);

-- ck_idempotencia_conteudo: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_idempotencia_conteudo: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM idempotencia WHERE (REGEXP_LIKE(conteudo, '^[0-9a-f]{64}\z')) IS NOT TRUE) preflight(violacoes);

ALTER TABLE ordens ADD CONSTRAINT ck_ordens_abertura_conjunto
    CHECK (((CASE WHEN autor_abertura_id IS NULL THEN 0 ELSE 1 END + CASE WHEN chave_abertura IS NULL THEN 0 ELSE 1 END + CASE WHEN conteudo_abertura IS NULL THEN 0 ELSE 1 END) IN (0,3)) IS TRUE);

ALTER TABLE ordens ADD CONSTRAINT ck_ordens_chave_abertura
    CHECK ((chave_abertura IS NULL OR REGEXP_LIKE(chave_abertura, '^[a-zA-Z0-9-]{16,80}\z')) IS TRUE);

ALTER TABLE ordens ADD CONSTRAINT ck_ordens_conteudo_abertura
    CHECK ((conteudo_abertura IS NULL OR REGEXP_LIKE(conteudo_abertura, '^[0-9a-f]{64}\z')) IS TRUE);

ALTER TABLE idempotencia ADD CONSTRAINT ck_idempotencia_chave
    CHECK ((REGEXP_LIKE(chave, '^[a-zA-Z0-9-]{16,80}\z')) IS TRUE);

ALTER TABLE idempotencia ADD CONSTRAINT ck_idempotencia_conteudo
    CHECK ((REGEXP_LIKE(conteudo, '^[0-9a-f]{64}\z')) IS TRUE);
