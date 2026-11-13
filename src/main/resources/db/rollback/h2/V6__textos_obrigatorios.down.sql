-- Reversao de V6; somente metadados, sem alterar registros.
ALTER TABLE auditoria DROP CONSTRAINT ck_auditoria_detalhes_preenchido;
ALTER TABLE auditoria DROP CONSTRAINT ck_auditoria_tipo_preenchido;
ALTER TABLE movimentos DROP CONSTRAINT ck_movimentos_observacao_preenchido;
ALTER TABLE ordens DROP CONSTRAINT ck_ordens_responsavel_preenchido;
ALTER TABLE ordens DROP CONSTRAINT ck_ordens_relato_preenchido;
ALTER TABLE ordens DROP CONSTRAINT ck_ordens_problema_preenchido;
ALTER TABLE ordens DROP CONSTRAINT ck_ordens_local_preenchido;
ALTER TABLE ordens DROP CONSTRAINT ck_ordens_solicitante_preenchido;
ALTER TABLE materiais DROP CONSTRAINT ck_materiais_unidade_preenchido;
ALTER TABLE materiais DROP CONSTRAINT ck_materiais_nome_preenchido;
ALTER TABLE materiais DROP CONSTRAINT ck_materiais_codigo_preenchido;
ALTER TABLE usuarios DROP CONSTRAINT ck_usuarios_nome_completo_preenchido;
