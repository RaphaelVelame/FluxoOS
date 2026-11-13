-- V15: preflight completo antes de adicionar as protecoes entre tenants.
SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight fk_usuarios_empresa: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM usuarios t WHERE empresa_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM empresas e WHERE e.id=t.empresa_id)) preflight(violacoes);

SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight fk_ordens_empresa: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM ordens t WHERE empresa_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM empresas e WHERE e.id=t.empresa_id)) preflight(violacoes);

SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight fk_materiais_empresa: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM materiais t WHERE empresa_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM empresas e WHERE e.id=t.empresa_id)) preflight(violacoes);

SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight fk_estoques_empresa: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM estoques t WHERE empresa_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM empresas e WHERE e.id=t.empresa_id)) preflight(violacoes);

SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight fk_movimentos_empresa: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM movimentos t WHERE empresa_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM empresas e WHERE e.id=t.empresa_id)) preflight(violacoes);

SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight fk_itens_os_empresa: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM itens_os t WHERE empresa_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM empresas e WHERE e.id=t.empresa_id)) preflight(violacoes);

SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight fk_roles_usuario: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM usuario_roles r WHERE NOT EXISTS(SELECT 1 FROM usuarios u WHERE u.id=r.usuario_id)) preflight(violacoes);

SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight ck_roles_disponiveis: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM usuario_roles WHERE role NOT IN ('ROLE_ADMIN_MASTER','ROLE_ADMIN_EMPRESA','ROLE_SUPERVISOR','ROLE_SUPORTE_TECNICO','ROLE_SUPORTE_EXTERNO','ROLE_ESTOQUISTA')) preflight(violacoes);

SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight uq_usuarios_empresa: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM (SELECT id,empresa_id FROM usuarios GROUP BY id,empresa_id HAVING count(*)>1) d) preflight(violacoes);

SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight uq_ordens_empresa: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM (SELECT id,empresa_id FROM ordens GROUP BY id,empresa_id HAVING count(*)>1) d) preflight(violacoes);

SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight uq_materiais_empresa: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM (SELECT id,empresa_id FROM materiais GROUP BY id,empresa_id HAVING count(*)>1) d) preflight(violacoes);

SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight uq_estoques_empresa: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM (SELECT id,empresa_id FROM estoques GROUP BY id,empresa_id HAVING count(*)>1) d) preflight(violacoes);

SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight fk_ordens_equipe_id_tenant: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM ordens t WHERE equipe_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM usuarios p WHERE p.id=t.equipe_id AND p.empresa_id=t.empresa_id)) preflight(violacoes);

SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight fk_ordens_autor_abertura_id_tenant: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM ordens t WHERE autor_abertura_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM usuarios p WHERE p.id=t.autor_abertura_id AND p.empresa_id=t.empresa_id)) preflight(violacoes);

SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight fk_materiais_estoque_id_tenant: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM materiais t WHERE estoque_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM estoques p WHERE p.id=t.estoque_id AND p.empresa_id=t.empresa_id)) preflight(violacoes);

SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight fk_movimentos_material_id_tenant: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM movimentos t WHERE material_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM materiais p WHERE p.id=t.material_id AND p.empresa_id=t.empresa_id)) preflight(violacoes);

SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight fk_movimentos_usuario_id_tenant: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM movimentos t WHERE usuario_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM usuarios p WHERE p.id=t.usuario_id AND p.empresa_id=t.empresa_id)) preflight(violacoes);

SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight fk_movimentos_ordem_id_tenant: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM movimentos t WHERE ordem_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM ordens p WHERE p.id=t.ordem_id AND p.empresa_id=t.empresa_id)) preflight(violacoes);

SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight fk_movimentos_estoque_id_tenant: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM movimentos t WHERE estoque_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM estoques p WHERE p.id=t.estoque_id AND p.empresa_id=t.empresa_id)) preflight(violacoes);

SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight fk_itens_os_ordem_id_tenant: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM itens_os t WHERE ordem_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM ordens p WHERE p.id=t.ordem_id AND p.empresa_id=t.empresa_id)) preflight(violacoes);

SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight fk_itens_os_material_id_tenant: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM itens_os t WHERE material_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM materiais p WHERE p.id=t.material_id AND p.empresa_id=t.empresa_id)) preflight(violacoes);

SELECT CAST(CASE WHEN violacoes=0 THEN '0' ELSE 'Preflight fk_itens_os_estoque_id_tenant: ' || CAST(violacoes AS VARCHAR) || ' violation(s). No data changed.' END AS BIGINT)
FROM (SELECT count(*) FROM itens_os t WHERE estoque_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM estoques p WHERE p.id=t.estoque_id AND p.empresa_id=t.empresa_id)) preflight(violacoes);

ALTER TABLE usuarios ADD CONSTRAINT fk_usuarios_empresa FOREIGN KEY(empresa_id) REFERENCES empresas(id);
ALTER TABLE ordens ADD CONSTRAINT fk_ordens_empresa FOREIGN KEY(empresa_id) REFERENCES empresas(id);
ALTER TABLE materiais ADD CONSTRAINT fk_materiais_empresa FOREIGN KEY(empresa_id) REFERENCES empresas(id);
ALTER TABLE estoques ADD CONSTRAINT fk_estoques_empresa FOREIGN KEY(empresa_id) REFERENCES empresas(id);
ALTER TABLE movimentos ADD CONSTRAINT fk_movimentos_empresa FOREIGN KEY(empresa_id) REFERENCES empresas(id);
ALTER TABLE itens_os ADD CONSTRAINT fk_itens_os_empresa FOREIGN KEY(empresa_id) REFERENCES empresas(id);
ALTER TABLE usuario_roles ADD CONSTRAINT fk_roles_usuario FOREIGN KEY(usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE;
ALTER TABLE usuario_roles ADD CONSTRAINT ck_roles_disponiveis CHECK(role IN ('ROLE_ADMIN_MASTER','ROLE_ADMIN_EMPRESA','ROLE_SUPERVISOR','ROLE_SUPORTE_TECNICO','ROLE_SUPORTE_EXTERNO','ROLE_ESTOQUISTA'));
ALTER TABLE usuarios ADD CONSTRAINT uq_usuarios_empresa UNIQUE(id,empresa_id);
ALTER TABLE ordens ADD CONSTRAINT uq_ordens_empresa UNIQUE(id,empresa_id);
ALTER TABLE materiais ADD CONSTRAINT uq_materiais_empresa UNIQUE(id,empresa_id);
ALTER TABLE estoques ADD CONSTRAINT uq_estoques_empresa UNIQUE(id,empresa_id);
ALTER TABLE ordens ADD CONSTRAINT fk_ordens_equipe_id_tenant FOREIGN KEY(equipe_id,empresa_id) REFERENCES usuarios(id,empresa_id);
ALTER TABLE ordens ADD CONSTRAINT fk_ordens_autor_abertura_id_tenant FOREIGN KEY(autor_abertura_id,empresa_id) REFERENCES usuarios(id,empresa_id);
ALTER TABLE materiais ADD CONSTRAINT fk_materiais_estoque_id_tenant FOREIGN KEY(estoque_id,empresa_id) REFERENCES estoques(id,empresa_id);
ALTER TABLE movimentos ADD CONSTRAINT fk_movimentos_material_id_tenant FOREIGN KEY(material_id,empresa_id) REFERENCES materiais(id,empresa_id);
ALTER TABLE movimentos ADD CONSTRAINT fk_movimentos_usuario_id_tenant FOREIGN KEY(usuario_id,empresa_id) REFERENCES usuarios(id,empresa_id);
ALTER TABLE movimentos ADD CONSTRAINT fk_movimentos_ordem_id_tenant FOREIGN KEY(ordem_id,empresa_id) REFERENCES ordens(id,empresa_id);
ALTER TABLE movimentos ADD CONSTRAINT fk_movimentos_estoque_id_tenant FOREIGN KEY(estoque_id,empresa_id) REFERENCES estoques(id,empresa_id);
ALTER TABLE itens_os ADD CONSTRAINT fk_itens_os_ordem_id_tenant FOREIGN KEY(ordem_id,empresa_id) REFERENCES ordens(id,empresa_id);
ALTER TABLE itens_os ADD CONSTRAINT fk_itens_os_material_id_tenant FOREIGN KEY(material_id,empresa_id) REFERENCES materiais(id,empresa_id);
ALTER TABLE itens_os ADD CONSTRAINT fk_itens_os_estoque_id_tenant FOREIGN KEY(estoque_id,empresa_id) REFERENCES estoques(id,empresa_id);
CREATE INDEX idx_usuarios_tenant ON usuarios(empresa_id,id);
CREATE INDEX idx_ordens_tenant ON ordens(empresa_id,id);
CREATE INDEX idx_materiais_tenant ON materiais(empresa_id,id);
CREATE INDEX idx_estoques_tenant ON estoques(empresa_id,id);
CREATE INDEX idx_movimentos_tenant ON movimentos(empresa_id,id);
