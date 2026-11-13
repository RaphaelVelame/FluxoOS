-- Reversao de V9; somente metadados, sem alterar registros.
ALTER TABLE ordens DROP CONSTRAINT ck_ordens_assinatura_envelope;
ALTER TABLE ordens DROP CONSTRAINT ck_ordens_relato_apos_inicio;
ALTER TABLE ordens DROP CONSTRAINT ck_ordens_conclusao_exclusiva;
ALTER TABLE ordens DROP CONSTRAINT ck_ordens_conclusao_completa;
ALTER TABLE ordens DROP CONSTRAINT ck_ordens_tecnico_apos_inicio;
