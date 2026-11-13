-- V6: textos_obrigatorios
-- Preflight de todo o assunto antes do primeiro DDL; abortar, nunca sanear dados.

-- ck_usuarios_nome_completo_preenchido: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_usuarios_nome_completo_preenchido: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM usuarios WHERE ((length(regexp_replace(nome_completo, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (nome_completo = regexp_replace(nome_completo, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS NOT TRUE) preflight(violacoes);

-- ck_materiais_codigo_preenchido: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_materiais_codigo_preenchido: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM materiais WHERE ((length(regexp_replace(codigo, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (codigo = regexp_replace(codigo, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS NOT TRUE) preflight(violacoes);

-- ck_materiais_nome_preenchido: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_materiais_nome_preenchido: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM materiais WHERE ((length(regexp_replace(nome, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (nome = regexp_replace(nome, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS NOT TRUE) preflight(violacoes);

-- ck_materiais_unidade_preenchido: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_materiais_unidade_preenchido: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM materiais WHERE ((length(regexp_replace(unidade, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (unidade = regexp_replace(unidade, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS NOT TRUE) preflight(violacoes);

-- ck_ordens_solicitante_preenchido: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_ordens_solicitante_preenchido: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM ordens WHERE ((length(regexp_replace(solicitante, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (solicitante = regexp_replace(solicitante, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS NOT TRUE) preflight(violacoes);

-- ck_ordens_local_preenchido: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_ordens_local_preenchido: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM ordens WHERE ((length(regexp_replace(local, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (local = regexp_replace(local, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS NOT TRUE) preflight(violacoes);

-- ck_ordens_problema_preenchido: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_ordens_problema_preenchido: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM ordens WHERE ((length(regexp_replace(problema, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (problema = regexp_replace(problema, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS NOT TRUE) preflight(violacoes);

-- ck_ordens_relato_preenchido: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_ordens_relato_preenchido: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM ordens WHERE (relato IS NULL OR ((length(regexp_replace(relato, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (relato = regexp_replace(relato, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g')))) IS NOT TRUE) preflight(violacoes);

-- ck_ordens_responsavel_preenchido: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_ordens_responsavel_preenchido: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM ordens WHERE (responsavel IS NULL OR ((length(regexp_replace(responsavel, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (responsavel = regexp_replace(responsavel, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g')))) IS NOT TRUE) preflight(violacoes);

-- ck_movimentos_observacao_preenchido: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_movimentos_observacao_preenchido: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM movimentos WHERE ((length(regexp_replace(observacao, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (observacao = regexp_replace(observacao, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS NOT TRUE) preflight(violacoes);

-- ck_auditoria_tipo_preenchido: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_auditoria_tipo_preenchido: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM auditoria WHERE ((length(regexp_replace(tipo, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (tipo = regexp_replace(tipo, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS NOT TRUE) preflight(violacoes);

-- ck_auditoria_detalhes_preenchido: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_auditoria_detalhes_preenchido: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM auditoria WHERE ((length(regexp_replace(detalhes, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (detalhes = regexp_replace(detalhes, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS NOT TRUE) preflight(violacoes);

ALTER TABLE usuarios ADD CONSTRAINT ck_usuarios_nome_completo_preenchido
    CHECK (((length(regexp_replace(nome_completo, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (nome_completo = regexp_replace(nome_completo, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS TRUE);

ALTER TABLE materiais ADD CONSTRAINT ck_materiais_codigo_preenchido
    CHECK (((length(regexp_replace(codigo, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (codigo = regexp_replace(codigo, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS TRUE);

ALTER TABLE materiais ADD CONSTRAINT ck_materiais_nome_preenchido
    CHECK (((length(regexp_replace(nome, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (nome = regexp_replace(nome, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS TRUE);

ALTER TABLE materiais ADD CONSTRAINT ck_materiais_unidade_preenchido
    CHECK (((length(regexp_replace(unidade, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (unidade = regexp_replace(unidade, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS TRUE);

ALTER TABLE ordens ADD CONSTRAINT ck_ordens_solicitante_preenchido
    CHECK (((length(regexp_replace(solicitante, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (solicitante = regexp_replace(solicitante, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS TRUE);

ALTER TABLE ordens ADD CONSTRAINT ck_ordens_local_preenchido
    CHECK (((length(regexp_replace(local, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (local = regexp_replace(local, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS TRUE);

ALTER TABLE ordens ADD CONSTRAINT ck_ordens_problema_preenchido
    CHECK (((length(regexp_replace(problema, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (problema = regexp_replace(problema, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS TRUE);

ALTER TABLE ordens ADD CONSTRAINT ck_ordens_relato_preenchido
    CHECK ((relato IS NULL OR ((length(regexp_replace(relato, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (relato = regexp_replace(relato, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g')))) IS TRUE);

ALTER TABLE ordens ADD CONSTRAINT ck_ordens_responsavel_preenchido
    CHECK ((responsavel IS NULL OR ((length(regexp_replace(responsavel, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (responsavel = regexp_replace(responsavel, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g')))) IS TRUE);

ALTER TABLE movimentos ADD CONSTRAINT ck_movimentos_observacao_preenchido
    CHECK (((length(regexp_replace(observacao, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (observacao = regexp_replace(observacao, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS TRUE);

ALTER TABLE auditoria ADD CONSTRAINT ck_auditoria_tipo_preenchido
    CHECK (((length(regexp_replace(tipo, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (tipo = regexp_replace(tipo, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS TRUE);

ALTER TABLE auditoria ADD CONSTRAINT ck_auditoria_detalhes_preenchido
    CHECK (((length(regexp_replace(detalhes, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (detalhes = regexp_replace(detalhes, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS TRUE);
