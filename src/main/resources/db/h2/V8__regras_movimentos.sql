-- V8: regras_movimentos
-- Preflight de todo o assunto antes do primeiro DDL; abortar, nunca sanear dados.

-- ck_movimentos_tipo: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_movimentos_tipo: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM movimentos WHERE (tipo IN ('ENTRADA','SAIDA','DEVOLUCAO','AJUSTE')) IS NOT TRUE) preflight(violacoes);

-- ck_movimentos_sinal: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_movimentos_sinal: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM movimentos WHERE ((tipo IN ('ENTRADA','DEVOLUCAO') AND variacao > 0) OR (tipo='SAIDA' AND variacao < 0) OR (tipo='AJUSTE' AND variacao <> 0)) IS NOT TRUE) preflight(violacoes);

-- ck_movimentos_ordem_por_tipo: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight ck_movimentos_ordem_por_tipo: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM movimentos WHERE ((tipo='SAIDA' AND ordem_id IS NOT NULL) OR (tipo IN ('ENTRADA','DEVOLUCAO','AJUSTE') AND ordem_id IS NULL)) IS NOT TRUE) preflight(violacoes);

ALTER TABLE movimentos ADD CONSTRAINT ck_movimentos_tipo
    CHECK ((tipo IN ('ENTRADA','SAIDA','DEVOLUCAO','AJUSTE')) IS TRUE);

ALTER TABLE movimentos ADD CONSTRAINT ck_movimentos_sinal
    CHECK (((tipo IN ('ENTRADA','DEVOLUCAO') AND variacao > 0) OR (tipo='SAIDA' AND variacao < 0) OR (tipo='AJUSTE' AND variacao <> 0)) IS TRUE);

ALTER TABLE movimentos ADD CONSTRAINT ck_movimentos_ordem_por_tipo
    CHECK (((tipo='SAIDA' AND ordem_id IS NOT NULL) OR (tipo IN ('ENTRADA','DEVOLUCAO','AJUSTE') AND ordem_id IS NULL)) IS TRUE);
