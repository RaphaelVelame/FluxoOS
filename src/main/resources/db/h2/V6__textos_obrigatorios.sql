-- V6: textos_obrigatorios
-- Preflight de todo o assunto antes do primeiro DDL; abortar, nunca sanear dados.

-- ck_usuarios_nome_completo_preenchido: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_usuarios_nome_completo_preenchido: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM usuarios WHERE ((length(regexp_replace(nome_completo, '[\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]', '')) > 0) AND (nome_completo = regexp_replace(nome_completo, '^[\u0001-\u0020]+|[\u0001-\u0020]+$', ''))) IS NOT TRUE) preflight(violacoes);

-- ck_materiais_codigo_preenchido: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_materiais_codigo_preenchido: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM materiais WHERE ((length(regexp_replace(codigo, '[\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]', '')) > 0) AND (codigo = regexp_replace(codigo, '^[\u0001-\u0020]+|[\u0001-\u0020]+$', ''))) IS NOT TRUE) preflight(violacoes);

-- ck_materiais_nome_preenchido: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_materiais_nome_preenchido: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM materiais WHERE ((length(regexp_replace(nome, '[\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]', '')) > 0) AND (nome = regexp_replace(nome, '^[\u0001-\u0020]+|[\u0001-\u0020]+$', ''))) IS NOT TRUE) preflight(violacoes);

-- ck_materiais_unidade_preenchido: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_materiais_unidade_preenchido: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM materiais WHERE ((length(regexp_replace(unidade, '[\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]', '')) > 0) AND (unidade = regexp_replace(unidade, '^[\u0001-\u0020]+|[\u0001-\u0020]+$', ''))) IS NOT TRUE) preflight(violacoes);

-- ck_ordens_solicitante_preenchido: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_ordens_solicitante_preenchido: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM ordens WHERE ((length(regexp_replace(solicitante, '[\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]', '')) > 0) AND (solicitante = regexp_replace(solicitante, '^[\u0001-\u0020]+|[\u0001-\u0020]+$', ''))) IS NOT TRUE) preflight(violacoes);

-- ck_ordens_local_preenchido: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_ordens_local_preenchido: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM ordens WHERE ((length(regexp_replace(local, '[\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]', '')) > 0) AND (local = regexp_replace(local, '^[\u0001-\u0020]+|[\u0001-\u0020]+$', ''))) IS NOT TRUE) preflight(violacoes);

-- ck_ordens_problema_preenchido: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_ordens_problema_preenchido: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM ordens WHERE ((length(regexp_replace(problema, '[\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]', '')) > 0) AND (problema = regexp_replace(problema, '^[\u0001-\u0020]+|[\u0001-\u0020]+$', ''))) IS NOT TRUE) preflight(violacoes);

-- ck_ordens_relato_preenchido: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_ordens_relato_preenchido: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM ordens WHERE (relato IS NULL OR ((length(regexp_replace(relato, '[\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]', '')) > 0) AND (relato = regexp_replace(relato, '^[\u0001-\u0020]+|[\u0001-\u0020]+$', '')))) IS NOT TRUE) preflight(violacoes);

-- ck_ordens_responsavel_preenchido: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_ordens_responsavel_preenchido: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM ordens WHERE (responsavel IS NULL OR ((length(regexp_replace(responsavel, '[\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]', '')) > 0) AND (responsavel = regexp_replace(responsavel, '^[\u0001-\u0020]+|[\u0001-\u0020]+$', '')))) IS NOT TRUE) preflight(violacoes);

-- ck_movimentos_observacao_preenchido: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_movimentos_observacao_preenchido: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM movimentos WHERE ((length(regexp_replace(observacao, '[\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]', '')) > 0) AND (observacao = regexp_replace(observacao, '^[\u0001-\u0020]+|[\u0001-\u0020]+$', ''))) IS NOT TRUE) preflight(violacoes);

-- ck_auditoria_tipo_preenchido: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_auditoria_tipo_preenchido: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM auditoria WHERE ((length(regexp_replace(tipo, '[\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]', '')) > 0) AND (tipo = regexp_replace(tipo, '^[\u0001-\u0020]+|[\u0001-\u0020]+$', ''))) IS NOT TRUE) preflight(violacoes);

-- ck_auditoria_detalhes_preenchido: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_auditoria_detalhes_preenchido: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM auditoria WHERE ((length(regexp_replace(detalhes, '[\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]', '')) > 0) AND (detalhes = regexp_replace(detalhes, '^[\u0001-\u0020]+|[\u0001-\u0020]+$', ''))) IS NOT TRUE) preflight(violacoes);

ALTER TABLE usuarios ADD CONSTRAINT ck_usuarios_nome_completo_preenchido
    CHECK (((length(regexp_replace(nome_completo, '[\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]', '')) > 0) AND (nome_completo = regexp_replace(nome_completo, '^[\u0001-\u0020]+|[\u0001-\u0020]+$', ''))) IS TRUE);

ALTER TABLE materiais ADD CONSTRAINT ck_materiais_codigo_preenchido
    CHECK (((length(regexp_replace(codigo, '[\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]', '')) > 0) AND (codigo = regexp_replace(codigo, '^[\u0001-\u0020]+|[\u0001-\u0020]+$', ''))) IS TRUE);

ALTER TABLE materiais ADD CONSTRAINT ck_materiais_nome_preenchido
    CHECK (((length(regexp_replace(nome, '[\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]', '')) > 0) AND (nome = regexp_replace(nome, '^[\u0001-\u0020]+|[\u0001-\u0020]+$', ''))) IS TRUE);

ALTER TABLE materiais ADD CONSTRAINT ck_materiais_unidade_preenchido
    CHECK (((length(regexp_replace(unidade, '[\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]', '')) > 0) AND (unidade = regexp_replace(unidade, '^[\u0001-\u0020]+|[\u0001-\u0020]+$', ''))) IS TRUE);

ALTER TABLE ordens ADD CONSTRAINT ck_ordens_solicitante_preenchido
    CHECK (((length(regexp_replace(solicitante, '[\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]', '')) > 0) AND (solicitante = regexp_replace(solicitante, '^[\u0001-\u0020]+|[\u0001-\u0020]+$', ''))) IS TRUE);

ALTER TABLE ordens ADD CONSTRAINT ck_ordens_local_preenchido
    CHECK (((length(regexp_replace(local, '[\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]', '')) > 0) AND (local = regexp_replace(local, '^[\u0001-\u0020]+|[\u0001-\u0020]+$', ''))) IS TRUE);

ALTER TABLE ordens ADD CONSTRAINT ck_ordens_problema_preenchido
    CHECK (((length(regexp_replace(problema, '[\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]', '')) > 0) AND (problema = regexp_replace(problema, '^[\u0001-\u0020]+|[\u0001-\u0020]+$', ''))) IS TRUE);

ALTER TABLE ordens ADD CONSTRAINT ck_ordens_relato_preenchido
    CHECK ((relato IS NULL OR ((length(regexp_replace(relato, '[\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]', '')) > 0) AND (relato = regexp_replace(relato, '^[\u0001-\u0020]+|[\u0001-\u0020]+$', '')))) IS TRUE);

ALTER TABLE ordens ADD CONSTRAINT ck_ordens_responsavel_preenchido
    CHECK ((responsavel IS NULL OR ((length(regexp_replace(responsavel, '[\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]', '')) > 0) AND (responsavel = regexp_replace(responsavel, '^[\u0001-\u0020]+|[\u0001-\u0020]+$', '')))) IS TRUE);

ALTER TABLE movimentos ADD CONSTRAINT ck_movimentos_observacao_preenchido
    CHECK (((length(regexp_replace(observacao, '[\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]', '')) > 0) AND (observacao = regexp_replace(observacao, '^[\u0001-\u0020]+|[\u0001-\u0020]+$', ''))) IS TRUE);

ALTER TABLE auditoria ADD CONSTRAINT ck_auditoria_tipo_preenchido
    CHECK (((length(regexp_replace(tipo, '[\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]', '')) > 0) AND (tipo = regexp_replace(tipo, '^[\u0001-\u0020]+|[\u0001-\u0020]+$', ''))) IS TRUE);

ALTER TABLE auditoria ADD CONSTRAINT ck_auditoria_detalhes_preenchido
    CHECK (((length(regexp_replace(detalhes, '[\u0009-\u000D\u001C-\u0020\u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]', '')) > 0) AND (detalhes = regexp_replace(detalhes, '^[\u0001-\u0020]+|[\u0001-\u0020]+$', ''))) IS TRUE);
