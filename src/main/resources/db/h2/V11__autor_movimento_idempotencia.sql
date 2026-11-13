-- V11: autor_movimento_idempotencia
-- Preflight de todo o assunto antes do primeiro DDL; abortar, nunca sanear dados.

-- uq_movimentos_id_usuario: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight uq_movimentos_id_usuario: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM (SELECT id,usuario_id FROM movimentos GROUP BY id,usuario_id HAVING count(*)>1) d) preflight(violacoes);

-- fk_idempotencia_movimento_autor: contar violacoes antes de adicionar a constraint.
SELECT CAST(CASE WHEN violacoes = 0 THEN '0'
    ELSE 'Preflight fk_idempotencia_movimento_autor: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM idempotencia i WHERE NOT EXISTS(SELECT 1 FROM movimentos m WHERE m.id=i.movimento_id AND m.usuario_id=i.usuario_id)) preflight(violacoes);

ALTER TABLE movimentos ADD CONSTRAINT uq_movimentos_id_usuario
    UNIQUE (id, usuario_id);

ALTER TABLE idempotencia ADD CONSTRAINT fk_idempotencia_movimento_autor
    FOREIGN KEY (movimento_id, usuario_id) REFERENCES movimentos(id, usuario_id);
