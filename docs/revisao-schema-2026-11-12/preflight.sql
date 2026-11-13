-- PRE-VALIDACAO: somente desenvolvimento local; executar apenas em ambiente autorizado.
-- Auditoria executada em 2026-11-12T20:34:33.660398-03:00. Nao contem DDL nem saneamento.
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;
SET LOCAL statement_timeout = '30s';

-- ck_usuarios_nome_completo_preenchido [propor]
-- nome_completo: texto preenchido e já aparado
-- Resultado observado: 0
SELECT count(*) FROM public.usuarios WHERE ((length(regexp_replace(nome_completo, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (nome_completo = regexp_replace(nome_completo, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS NOT TRUE;

-- diag_usuarios_nome_completo_trim [avaliar]
-- nome_completo: espaços/controles nas extremidades
-- Resultado observado: 0
SELECT count(*) FROM public.usuarios WHERE (nome_completo IS NULL OR nome_completo = regexp_replace(nome_completo, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g')) IS NOT TRUE;

-- ck_materiais_codigo_preenchido [propor]
-- codigo: texto preenchido e já aparado
-- Resultado observado: 0
SELECT count(*) FROM public.materiais WHERE ((length(regexp_replace(codigo, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (codigo = regexp_replace(codigo, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS NOT TRUE;

-- diag_materiais_codigo_trim [avaliar]
-- codigo: espaços/controles nas extremidades
-- Resultado observado: 0
SELECT count(*) FROM public.materiais WHERE (codigo IS NULL OR codigo = regexp_replace(codigo, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g')) IS NOT TRUE;

-- ck_materiais_nome_preenchido [propor]
-- nome: texto preenchido e já aparado
-- Resultado observado: 0
SELECT count(*) FROM public.materiais WHERE ((length(regexp_replace(nome, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (nome = regexp_replace(nome, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS NOT TRUE;

-- diag_materiais_nome_trim [avaliar]
-- nome: espaços/controles nas extremidades
-- Resultado observado: 0
SELECT count(*) FROM public.materiais WHERE (nome IS NULL OR nome = regexp_replace(nome, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g')) IS NOT TRUE;

-- ck_materiais_unidade_preenchido [propor]
-- unidade: texto preenchido e já aparado
-- Resultado observado: 0
SELECT count(*) FROM public.materiais WHERE ((length(regexp_replace(unidade, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (unidade = regexp_replace(unidade, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS NOT TRUE;

-- diag_materiais_unidade_trim [avaliar]
-- unidade: espaços/controles nas extremidades
-- Resultado observado: 0
SELECT count(*) FROM public.materiais WHERE (unidade IS NULL OR unidade = regexp_replace(unidade, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g')) IS NOT TRUE;

-- ck_ordens_solicitante_preenchido [propor]
-- solicitante: texto preenchido e já aparado
-- Resultado observado: 0
SELECT count(*) FROM public.ordens WHERE ((length(regexp_replace(solicitante, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (solicitante = regexp_replace(solicitante, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS NOT TRUE;

-- diag_ordens_solicitante_trim [avaliar]
-- solicitante: espaços/controles nas extremidades
-- Resultado observado: 0
SELECT count(*) FROM public.ordens WHERE (solicitante IS NULL OR solicitante = regexp_replace(solicitante, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g')) IS NOT TRUE;

-- ck_ordens_local_preenchido [propor]
-- local: texto preenchido e já aparado
-- Resultado observado: 0
SELECT count(*) FROM public.ordens WHERE ((length(regexp_replace(local, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (local = regexp_replace(local, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS NOT TRUE;

-- diag_ordens_local_trim [avaliar]
-- local: espaços/controles nas extremidades
-- Resultado observado: 0
SELECT count(*) FROM public.ordens WHERE (local IS NULL OR local = regexp_replace(local, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g')) IS NOT TRUE;

-- ck_ordens_problema_preenchido [propor]
-- problema: texto preenchido e já aparado
-- Resultado observado: 0
SELECT count(*) FROM public.ordens WHERE ((length(regexp_replace(problema, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (problema = regexp_replace(problema, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS NOT TRUE;

-- diag_ordens_problema_trim [avaliar]
-- problema: espaços/controles nas extremidades
-- Resultado observado: 0
SELECT count(*) FROM public.ordens WHERE (problema IS NULL OR problema = regexp_replace(problema, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g')) IS NOT TRUE;

-- ck_ordens_relato_preenchido [propor]
-- relato: texto preenchido e já aparado quando presente
-- Resultado observado: 0
SELECT count(*) FROM public.ordens WHERE (relato IS NULL OR ((length(regexp_replace(relato, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (relato = regexp_replace(relato, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g')))) IS NOT TRUE;

-- diag_ordens_relato_trim [avaliar]
-- relato: espaços/controles nas extremidades
-- Resultado observado: 0
SELECT count(*) FROM public.ordens WHERE (relato IS NULL OR relato = regexp_replace(relato, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g')) IS NOT TRUE;

-- ck_ordens_responsavel_preenchido [propor]
-- responsavel: texto preenchido e já aparado quando presente
-- Resultado observado: 0
SELECT count(*) FROM public.ordens WHERE (responsavel IS NULL OR ((length(regexp_replace(responsavel, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (responsavel = regexp_replace(responsavel, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g')))) IS NOT TRUE;

-- diag_ordens_responsavel_trim [avaliar]
-- responsavel: espaços/controles nas extremidades
-- Resultado observado: 0
SELECT count(*) FROM public.ordens WHERE (responsavel IS NULL OR responsavel = regexp_replace(responsavel, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g')) IS NOT TRUE;

-- ck_movimentos_observacao_preenchido [propor]
-- observacao: texto preenchido e já aparado
-- Resultado observado: 0
SELECT count(*) FROM public.movimentos WHERE ((length(regexp_replace(observacao, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (observacao = regexp_replace(observacao, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS NOT TRUE;

-- diag_movimentos_observacao_trim [avaliar]
-- observacao: espaços/controles nas extremidades
-- Resultado observado: 0
SELECT count(*) FROM public.movimentos WHERE (observacao IS NULL OR observacao = regexp_replace(observacao, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g')) IS NOT TRUE;

-- ck_auditoria_tipo_preenchido [propor]
-- tipo: texto preenchido e já aparado
-- Resultado observado: 0
SELECT count(*) FROM public.auditoria WHERE ((length(regexp_replace(tipo, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (tipo = regexp_replace(tipo, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS NOT TRUE;

-- diag_auditoria_tipo_trim [avaliar]
-- tipo: espaços/controles nas extremidades
-- Resultado observado: 0
SELECT count(*) FROM public.auditoria WHERE (tipo IS NULL OR tipo = regexp_replace(tipo, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g')) IS NOT TRUE;

-- ck_auditoria_detalhes_preenchido [propor]
-- detalhes: texto preenchido e já aparado
-- Resultado observado: 0
SELECT count(*) FROM public.auditoria WHERE ((length(regexp_replace(detalhes, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (detalhes = regexp_replace(detalhes, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS NOT TRUE;

-- diag_auditoria_detalhes_trim [avaliar]
-- detalhes: espaços/controles nas extremidades
-- Resultado observado: 0
SELECT count(*) FROM public.auditoria WHERE (detalhes IS NULL OR detalhes = regexp_replace(detalhes, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g')) IS NOT TRUE;

-- ck_usuarios_login_formato [propor]
-- login canônico: 3–80 caracteres ASCII minúsculos
-- Resultado observado: 0
SELECT count(*) FROM public.usuarios WHERE (nome_usuario ~ '^[a-z0-9._-]{3,80}$') IS NOT TRUE;

-- ck_usuarios_hash_formato [propor]
-- envelope PBKDF2 compatível com o verificador atual
-- Resultado observado: 0
SELECT count(*) FROM public.usuarios WHERE (CASE WHEN hash_senha ~ '^pbkdf2_sha256[$][0-9]{6,7}[$][A-Za-z0-9_-]{22}[$][A-Za-z0-9_-]{43}$' THEN split_part(hash_senha,'$',2)::integer BETWEEN 600000 AND 2000000 ELSE false END) IS NOT TRUE;

-- ck_usuarios_versao_sessao_nao_negativa [propor]
-- versão de sessão não negativa
-- Resultado observado: 0
SELECT count(*) FROM public.usuarios WHERE (versao_sessao >= 0) IS NOT TRUE;

-- ck_movimentos_tipo [propor]
-- tipo pertence ao enum
-- Resultado observado: 0
SELECT count(*) FROM public.movimentos WHERE (tipo IN ('ENTRADA','SAIDA','DEVOLUCAO','AJUSTE')) IS NOT TRUE;

-- ck_movimentos_sinal [propor]
-- entrada/devolução positivas; saída negativa; ajuste não zero
-- Resultado observado: 0
SELECT count(*) FROM public.movimentos WHERE ((tipo IN ('ENTRADA','DEVOLUCAO') AND variacao > 0) OR (tipo='SAIDA' AND variacao < 0) OR (tipo='AJUSTE' AND variacao <> 0)) IS NOT TRUE;

-- ck_movimentos_ordem_por_tipo [propor]
-- saída exige OS; entrada, devolução e ajuste manuais não possuem OS
-- Resultado observado: 0
SELECT count(*) FROM public.movimentos WHERE ((tipo='SAIDA' AND ordem_id IS NOT NULL) OR (tipo IN ('ENTRADA','DEVOLUCAO','AJUSTE') AND ordem_id IS NULL)) IS NOT TRUE;

-- ck_ordens_tecnico_apos_inicio [propor]
-- OS iniciada/concluída possui técnico
-- Resultado observado: 0
SELECT count(*) FROM public.ordens WHERE (situacao='ABERTA' OR equipe_id IS NOT NULL) IS NOT TRUE;

-- ck_ordens_conclusao_completa [propor]
-- conclusão exige relato, responsável, assinatura e instante
-- Resultado observado: 0
SELECT count(*) FROM public.ordens WHERE (situacao <> 'CONCLUIDA' OR (relato IS NOT NULL AND responsavel IS NOT NULL AND assinatura IS NOT NULL AND assinado_em IS NOT NULL)) IS NOT TRUE;

-- ck_ordens_conclusao_exclusiva [propor]
-- campos de assinatura só são preenchidos ao concluir
-- Resultado observado: 0
SELECT count(*) FROM public.ordens WHERE (situacao='CONCLUIDA' OR (responsavel IS NULL AND assinatura IS NULL AND assinado_em IS NULL)) IS NOT TRUE;

-- ck_ordens_relato_apos_inicio [propor]
-- relato só existe após iniciar atendimento
-- Resultado observado: 0
SELECT count(*) FROM public.ordens WHERE (relato IS NULL OR situacao IN ('EM_ANDAMENTO','CONCLUIDA')) IS NOT TRUE;

-- ck_ordens_assinatura_envelope [propor]
-- assinatura: prefixo PNG/base64 e limite de 200.000 caracteres; validação parcial
-- Resultado observado: 0
SELECT count(*) FROM public.ordens WHERE (assinatura IS NULL OR (char_length(assinatura) <= 200000 AND assinatura ~ '^data:image/png;base64,[A-Za-z0-9+/]+={0,2}$')) IS NOT TRUE;

-- ck_ordens_abertura_conjunto [propor]
-- metadados de abertura todos ausentes (legado) ou todos presentes
-- Resultado observado: 0
SELECT count(*) FROM public.ordens WHERE (num_nonnulls(autor_abertura_id,chave_abertura,conteudo_abertura) IN (0,3)) IS NOT TRUE;

-- ck_ordens_chave_abertura [propor]
-- chave de abertura de 16–80 caracteres alfanuméricos/hífen
-- Resultado observado: 0
SELECT count(*) FROM public.ordens WHERE (chave_abertura IS NULL OR chave_abertura ~ '^[a-zA-Z0-9-]{16,80}$') IS NOT TRUE;

-- ck_ordens_conteudo_abertura [propor]
-- digest SHA-256 hexadecimal minúsculo com 64 caracteres
-- Resultado observado: 0
SELECT count(*) FROM public.ordens WHERE (conteudo_abertura IS NULL OR conteudo_abertura ~ '^[0-9a-f]{64}$') IS NOT TRUE;

-- ck_idempotencia_chave [propor]
-- chave de movimento de 16–80 caracteres alfanuméricos/hífen
-- Resultado observado: 0
SELECT count(*) FROM public.idempotencia WHERE (chave ~ '^[a-zA-Z0-9-]{16,80}$') IS NOT TRUE;

-- ck_idempotencia_conteudo [propor]
-- digest SHA-256 hexadecimal minúsculo com 64 caracteres
-- Resultado observado: 0
SELECT count(*) FROM public.idempotencia WHERE (conteudo ~ '^[0-9a-f]{64}$') IS NOT TRUE;

-- uq_movimentos_id_usuario [propor]
-- UNIQUE (id,usuario_id): chave de suporte para FK composta; grupos duplicados
-- Resultado observado: 0
SELECT count(*) FROM (SELECT id,usuario_id FROM public.movimentos GROUP BY id,usuario_id HAVING count(*)>1) d;

-- fk_idempotencia_movimento_autor [propor]
-- FK composta garante que o dono da chave é o autor do movimento
-- Resultado observado: 0
SELECT count(*) FROM public.idempotencia i WHERE NOT EXISTS(SELECT 1 FROM public.movimentos m WHERE m.id=i.movimento_id AND m.usuario_id=i.usuario_id);

-- uq_movimentos_consumo_os_material [propor]
-- um consumo por material/OS; grupos duplicados
-- Resultado observado: 0
SELECT count(*) FROM (SELECT ordem_id,material_id FROM public.movimentos WHERE ordem_id IS NOT NULL GROUP BY ordem_id,material_id HAVING count(*)>1) d;

-- existente_materiais_quantidade [existente]
-- quantidade
-- Resultado observado: 0
SELECT count(*) FROM public.materiais WHERE (quantidade>=0) IS NOT TRUE;

-- existente_materiais_quantidade_minima [existente]
-- quantidade_minima
-- Resultado observado: 0
SELECT count(*) FROM public.materiais WHERE (quantidade_minima>=0) IS NOT TRUE;

-- existente_itens_os_quantidade [existente]
-- quantidade
-- Resultado observado: 0
SELECT count(*) FROM public.itens_os WHERE (quantidade>0) IS NOT TRUE;

-- existente_movimentos_variacao [existente]
-- variacao
-- Resultado observado: 0
SELECT count(*) FROM public.movimentos WHERE (variacao<>0) IS NOT TRUE;

-- existente_movimentos_saldo_apos [existente]
-- saldo_apos
-- Resultado observado: 0
SELECT count(*) FROM public.movimentos WHERE (saldo_apos>=0) IS NOT TRUE;

-- existente_usuarios_perfil [existente]
-- perfil enum
-- Resultado observado: 0
SELECT count(*) FROM public.usuarios WHERE (perfil IN ('CHEFE','SUPORTE_TI','TECNICO','SOLICITANTE')) IS NOT TRUE;

-- existente_ordens_prioridade [existente]
-- prioridade enum
-- Resultado observado: 0
SELECT count(*) FROM public.ordens WHERE (prioridade IN ('BAIXA','MEDIA','ALTA')) IS NOT TRUE;

-- existente_ordens_situacao [existente]
-- situação enum
-- Resultado observado: 0
SELECT count(*) FROM public.ordens WHERE (situacao IN ('ABERTA','EM_ANDAMENTO','CONCLUIDA')) IS NOT TRUE;

-- existente_unicidade_usuarios [existente]
-- unicidade (nome_usuario): grupos duplicados
-- Resultado observado: 0
SELECT count(*) FROM (SELECT nome_usuario FROM public.usuarios WHERE true GROUP BY nome_usuario HAVING count(*)>1) d;

-- existente_unicidade_materiais [existente]
-- unicidade (codigo): grupos duplicados
-- Resultado observado: 0
SELECT count(*) FROM (SELECT codigo FROM public.materiais WHERE true GROUP BY codigo HAVING count(*)>1) d;

-- existente_unicidade_itens_os [existente]
-- unicidade (ordem_id,material_id): grupos duplicados
-- Resultado observado: 0
SELECT count(*) FROM (SELECT ordem_id,material_id FROM public.itens_os WHERE true GROUP BY ordem_id,material_id HAVING count(*)>1) d;

-- existente_unicidade_idempotencia [existente]
-- unicidade (usuario_id,chave): grupos duplicados
-- Resultado observado: 0
SELECT count(*) FROM (SELECT usuario_id,chave FROM public.idempotencia WHERE true GROUP BY usuario_id,chave HAVING count(*)>1) d;

-- existente_unicidade_ordens [existente]
-- unicidade (autor_abertura_id,chave_abertura): grupos duplicados
-- Resultado observado: 0
SELECT count(*) FROM (SELECT autor_abertura_id,chave_abertura FROM public.ordens WHERE autor_abertura_id IS NOT NULL AND chave_abertura IS NOT NULL GROUP BY autor_abertura_id,chave_abertura HAVING count(*)>1) d;

-- existente_auditoria_alvo_id_fkey [existente]
-- FOREIGN KEY (alvo_id) REFERENCES usuarios(id)
-- Resultado observado: 0
SELECT count(*) FROM public.auditoria f WHERE f.alvo_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM public.usuarios r WHERE r.id=f.alvo_id);

-- existente_auditoria_ordem_id_fkey [existente]
-- FOREIGN KEY (ordem_id) REFERENCES ordens(id)
-- Resultado observado: 0
SELECT count(*) FROM public.auditoria f WHERE f.ordem_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM public.ordens r WHERE r.id=f.ordem_id);

-- existente_auditoria_usuario_id_fkey [existente]
-- FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
-- Resultado observado: 0
SELECT count(*) FROM public.auditoria f WHERE f.usuario_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM public.usuarios r WHERE r.id=f.usuario_id);

-- existente_idempotencia_movimento_id_fkey [existente]
-- FOREIGN KEY (movimento_id) REFERENCES movimentos(id)
-- Resultado observado: 0
SELECT count(*) FROM public.idempotencia f WHERE f.movimento_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM public.movimentos r WHERE r.id=f.movimento_id);

-- existente_idempotencia_usuario_id_fkey [existente]
-- FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
-- Resultado observado: 0
SELECT count(*) FROM public.idempotencia f WHERE f.usuario_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM public.usuarios r WHERE r.id=f.usuario_id);

-- existente_itens_os_material_id_fkey [existente]
-- FOREIGN KEY (material_id) REFERENCES materiais(id)
-- Resultado observado: 0
SELECT count(*) FROM public.itens_os f WHERE f.material_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM public.materiais r WHERE r.id=f.material_id);

-- existente_itens_os_ordem_id_fkey [existente]
-- FOREIGN KEY (ordem_id) REFERENCES ordens(id)
-- Resultado observado: 0
SELECT count(*) FROM public.itens_os f WHERE f.ordem_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM public.ordens r WHERE r.id=f.ordem_id);

-- existente_movimentos_material_id_fkey [existente]
-- FOREIGN KEY (material_id) REFERENCES materiais(id)
-- Resultado observado: 0
SELECT count(*) FROM public.movimentos f WHERE f.material_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM public.materiais r WHERE r.id=f.material_id);

-- existente_movimentos_ordem_id_fkey [existente]
-- FOREIGN KEY (ordem_id) REFERENCES ordens(id)
-- Resultado observado: 0
SELECT count(*) FROM public.movimentos f WHERE f.ordem_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM public.ordens r WHERE r.id=f.ordem_id);

-- existente_movimentos_usuario_id_fkey [existente]
-- FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
-- Resultado observado: 0
SELECT count(*) FROM public.movimentos f WHERE f.usuario_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM public.usuarios r WHERE r.id=f.usuario_id);

-- existente_ordens_autor_abertura_id_fkey [existente]
-- FOREIGN KEY (autor_abertura_id) REFERENCES usuarios(id)
-- Resultado observado: 0
SELECT count(*) FROM public.ordens f WHERE f.autor_abertura_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM public.usuarios r WHERE r.id=f.autor_abertura_id);

-- existente_ordens_equipe_id_fkey [existente]
-- FOREIGN KEY (equipe_id) REFERENCES usuarios(id)
-- Resultado observado: 0
SELECT count(*) FROM public.ordens f WHERE f.equipe_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM public.usuarios r WHERE r.id=f.equipe_id);

-- existente_not_null_ordens [existente]
-- NOT NULL em id, solicitante, local, problema, prioridade, situacao, criado_em
-- Resultado observado: 0
SELECT count(*) FROM public.ordens WHERE id IS NULL OR solicitante IS NULL OR local IS NULL OR problema IS NULL OR prioridade IS NULL OR situacao IS NULL OR criado_em IS NULL;

-- existente_not_null_itens_os [existente]
-- NOT NULL em ordem_id, material_id, quantidade
-- Resultado observado: 0
SELECT count(*) FROM public.itens_os WHERE ordem_id IS NULL OR material_id IS NULL OR quantidade IS NULL;

-- existente_not_null_usuarios [existente]
-- NOT NULL em id, nome_usuario, nome_completo, perfil, ativo, hash_senha, versao_sessao
-- Resultado observado: 0
SELECT count(*) FROM public.usuarios WHERE id IS NULL OR nome_usuario IS NULL OR nome_completo IS NULL OR perfil IS NULL OR ativo IS NULL OR hash_senha IS NULL OR versao_sessao IS NULL;

-- existente_not_null_auditoria [existente]
-- NOT NULL em id, tipo, detalhes, criado_em
-- Resultado observado: 0
SELECT count(*) FROM public.auditoria WHERE id IS NULL OR tipo IS NULL OR detalhes IS NULL OR criado_em IS NULL;

-- existente_not_null_materiais [existente]
-- NOT NULL em id, codigo, nome, unidade, quantidade, quantidade_minima, ativo
-- Resultado observado: 0
SELECT count(*) FROM public.materiais WHERE id IS NULL OR codigo IS NULL OR nome IS NULL OR unidade IS NULL OR quantidade IS NULL OR quantidade_minima IS NULL OR ativo IS NULL;

-- existente_not_null_movimentos [existente]
-- NOT NULL em id, material_id, usuario_id, tipo, variacao, saldo_apos, observacao, criado_em
-- Resultado observado: 0
SELECT count(*) FROM public.movimentos WHERE id IS NULL OR material_id IS NULL OR usuario_id IS NULL OR tipo IS NULL OR variacao IS NULL OR saldo_apos IS NULL OR observacao IS NULL OR criado_em IS NULL;

-- existente_not_null_idempotencia [existente]
-- NOT NULL em usuario_id, chave, conteudo, movimento_id
-- Resultado observado: 0
SELECT count(*) FROM public.idempotencia WHERE usuario_id IS NULL OR chave IS NULL OR conteudo IS NULL OR movimento_id IS NULL;

-- diag_saldo_material_ledger [fase2]
-- materiais cujo saldo difere da soma de movimentos
-- Resultado observado: 0
SELECT count(*) FROM public.materiais m LEFT JOIN (SELECT material_id,sum(variacao) saldo FROM public.movimentos GROUP BY material_id) v ON v.material_id=m.id WHERE m.quantidade<>coalesce(v.saldo,0);

-- diag_saldo_apos_ledger [fase2]
-- movimentos cujo saldo_apos difere do acumulado por id (ordem técnica, não data do evento)
-- Resultado observado: 0
SELECT count(*) FROM (SELECT saldo_apos,sum(variacao) OVER(PARTITION BY material_id ORDER BY id ROWS UNBOUNDED PRECEDING) esperado FROM public.movimentos) x WHERE saldo_apos<>esperado;

-- diag_consumo_item [fase2]
-- saídas sem item de OS de mesma quantidade
-- Resultado observado: 0
SELECT count(*) FROM public.movimentos m WHERE m.tipo='SAIDA' AND NOT EXISTS(SELECT 1 FROM public.itens_os i WHERE i.ordem_id=m.ordem_id AND i.material_id=m.material_id AND i.quantidade::bigint=-(m.variacao::bigint));

-- diag_item_consumo [fase2]
-- itens de OS sem saída correspondente
-- Resultado observado: 0
SELECT count(*) FROM public.itens_os i WHERE NOT EXISTS(SELECT 1 FROM public.movimentos m WHERE m.tipo='SAIDA' AND m.ordem_id=i.ordem_id AND m.material_id=i.material_id AND i.quantidade::bigint=-(m.variacao::bigint));

-- diag_tecnico_perfil [fase2]
-- OS atribuídas a usuário de perfil diferente de TECNICO
-- Resultado observado: 0
SELECT count(*) FROM public.ordens o JOIN public.usuarios u ON u.id=o.equipe_id WHERE u.perfil<>'TECNICO';

-- diag_tecnico_inativo_atual [avaliar]
-- OS com técnico hoje inativo (estado atual não comprova erro histórico)
-- Resultado observado: 0
SELECT count(*) FROM public.ordens o JOIN public.usuarios u ON u.id=o.equipe_id WHERE NOT u.ativo;

-- diag_ordens_autor_abertura_id_not_null [adiar]
-- autor_abertura_id obrigatório: avaliar legado
-- Resultado observado: 0
SELECT count(*) FROM public.ordens WHERE (autor_abertura_id IS NOT NULL) IS NOT TRUE;

-- diag_ordens_chave_abertura_not_null [adiar]
-- chave_abertura obrigatório: avaliar legado
-- Resultado observado: 0
SELECT count(*) FROM public.ordens WHERE (chave_abertura IS NOT NULL) IS NOT TRUE;

-- diag_ordens_conteudo_abertura_not_null [adiar]
-- conteudo_abertura obrigatório: avaliar legado
-- Resultado observado: 0
SELECT count(*) FROM public.ordens WHERE (conteudo_abertura IS NOT NULL) IS NOT TRUE;

-- diag_conclusao_cronologia [avaliar]
-- assinado_em >= criado_em quando presente
-- Resultado observado: 0
SELECT count(*) FROM public.ordens WHERE (assinado_em IS NULL OR assinado_em >= criado_em) IS NOT TRUE;

COMMIT;
