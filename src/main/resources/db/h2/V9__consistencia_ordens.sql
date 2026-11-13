-- V9: consistencia_ordens
-- Preflight de todo o assunto antes do primeiro DDL; abortar, nunca sanear dados.

-- ck_ordens_tecnico_apos_inicio: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_ordens_tecnico_apos_inicio: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM ordens WHERE (situacao='ABERTA' OR equipe_id IS NOT NULL) IS NOT TRUE) preflight(violacoes);

-- ck_ordens_conclusao_completa: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_ordens_conclusao_completa: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM ordens WHERE (situacao <> 'CONCLUIDA' OR (relato IS NOT NULL AND responsavel IS NOT NULL AND assinatura IS NOT NULL AND assinado_em IS NOT NULL)) IS NOT TRUE) preflight(violacoes);

-- ck_ordens_conclusao_exclusiva: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_ordens_conclusao_exclusiva: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM ordens WHERE (situacao='CONCLUIDA' OR (responsavel IS NULL AND assinatura IS NULL AND assinado_em IS NULL)) IS NOT TRUE) preflight(violacoes);

-- ck_ordens_relato_apos_inicio: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_ordens_relato_apos_inicio: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM ordens WHERE (relato IS NULL OR situacao IN ('EM_ANDAMENTO','CONCLUIDA')) IS NOT TRUE) preflight(violacoes);

-- ck_ordens_assinatura_envelope: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_ordens_assinatura_envelope: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM ordens WHERE (assinatura IS NULL OR (char_length(assinatura) <= 200000 AND REGEXP_LIKE(assinatura, '^data:image/png;base64,[A-Za-z0-9+/]+={0,2}\z'))) IS NOT TRUE) preflight(violacoes);

ALTER TABLE ordens ADD CONSTRAINT ck_ordens_tecnico_apos_inicio
    CHECK ((situacao='ABERTA' OR equipe_id IS NOT NULL) IS TRUE);

ALTER TABLE ordens ADD CONSTRAINT ck_ordens_conclusao_completa
    CHECK ((situacao <> 'CONCLUIDA' OR (relato IS NOT NULL AND responsavel IS NOT NULL AND assinatura IS NOT NULL AND assinado_em IS NOT NULL)) IS TRUE);

ALTER TABLE ordens ADD CONSTRAINT ck_ordens_conclusao_exclusiva
    CHECK ((situacao='CONCLUIDA' OR (responsavel IS NULL AND assinatura IS NULL AND assinado_em IS NULL)) IS TRUE);

ALTER TABLE ordens ADD CONSTRAINT ck_ordens_relato_apos_inicio
    CHECK ((relato IS NULL OR situacao IN ('EM_ANDAMENTO','CONCLUIDA')) IS TRUE);

ALTER TABLE ordens ADD CONSTRAINT ck_ordens_assinatura_envelope
    CHECK ((assinatura IS NULL OR (char_length(assinatura) <= 200000 AND REGEXP_LIKE(assinatura, '^data:image/png;base64,[A-Za-z0-9+/]+={0,2}\z'))) IS TRUE);
