-- V7: integridade_usuarios
-- Preflight de todo o assunto antes do primeiro DDL; abortar, nunca sanear dados.

-- ck_usuarios_login_formato: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_usuarios_login_formato: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM usuarios WHERE (nome_usuario ~ '^[a-z0-9._-]{3,80}$') IS NOT TRUE) preflight(violacoes);

-- ck_usuarios_hash_formato: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_usuarios_hash_formato: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM usuarios WHERE (CASE WHEN hash_senha ~ '^pbkdf2_sha256[$][0-9]{6,7}[$][A-Za-z0-9_-]{22}[$][A-Za-z0-9_-]{43}$' THEN split_part(hash_senha,'$',2)::integer BETWEEN 600000 AND 2000000 ELSE false END) IS NOT TRUE) preflight(violacoes);

-- ck_usuarios_versao_sessao_nao_negativa: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_usuarios_versao_sessao_nao_negativa: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM usuarios WHERE (versao_sessao >= 0) IS NOT TRUE) preflight(violacoes);

ALTER TABLE usuarios ADD CONSTRAINT ck_usuarios_login_formato
    CHECK ((nome_usuario ~ '^[a-z0-9._-]{3,80}$') IS TRUE);

ALTER TABLE usuarios ADD CONSTRAINT ck_usuarios_hash_formato
    CHECK ((CASE WHEN hash_senha ~ '^pbkdf2_sha256[$][0-9]{6,7}[$][A-Za-z0-9_-]{22}[$][A-Za-z0-9_-]{43}$' THEN split_part(hash_senha,'$',2)::integer BETWEEN 600000 AND 2000000 ELSE false END) IS TRUE);

ALTER TABLE usuarios ADD CONSTRAINT ck_usuarios_versao_sessao_nao_negativa
    CHECK ((versao_sessao >= 0) IS TRUE);
