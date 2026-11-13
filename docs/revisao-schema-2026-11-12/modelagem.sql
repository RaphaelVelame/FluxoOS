BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;
SET LOCAL statement_timeout = '20s';
SELECT jsonb_build_object(
 'coletado_em',current_timestamp,'somente_leitura',current_setting('transaction_read_only'),
 'datas_estruturadas_apenas_na_observacao',(SELECT count(*) FROM movimentos
    WHERE observacao ~ '^Entrada da planilha em [0-9]{4}-[0-9]{2}-[0-9]{2}: '),
 'codigos_material_so_digitos',(SELECT count(*) FROM materiais WHERE codigo ~ '^[0-9]+$'),
 'nomes_repetidos',(SELECT coalesce(jsonb_agg(to_jsonb(x) ORDER BY x.nome),'[]'::jsonb) FROM (
    SELECT lower(btrim(nome)) nome,count(*) registros,array_agg(codigo ORDER BY codigo) codigos,
        array_agg(DISTINCT referencia_fabricante) referencias
    FROM materiais GROUP BY lower(btrim(nome)) HAVING count(*)>1) x),
 'grupos_mesma_descricao_referencia_unidade',(SELECT count(*) FROM (
    SELECT lower(btrim(nome)),lower(btrim(coalesce(referencia_fabricante,''))),unidade FROM materiais
    GROUP BY lower(btrim(nome)),lower(btrim(coalesce(referencia_fabricante,''))),unidade HAVING count(*)>1) x),
 'unidades',(SELECT jsonb_agg(to_jsonb(x)) FROM (SELECT unidade,count(*) materiais,sum(quantidade) saldo FROM materiais GROUP BY unidade ORDER BY unidade) x),
 'nomes_fabricantes_distintos',(SELECT count(DISTINCT referencia_fabricante) FROM materiais),
 'auditoria_importada_sem_material_atual',(SELECT count(*) FROM (
    SELECT CASE WHEN tipo='MATERIAL_IMPORTADO' THEN detalhes::jsonb ELSE '{}'::jsonb END d FROM auditoria) a
    WHERE a.d ? 'materialId' AND NOT EXISTS(SELECT 1 FROM materiais m WHERE m.id=(a.d->>'materialId')::bigint)),
 'auditoria_com_snapshot_removido',(SELECT count(*) FROM auditoria WHERE tipo='MATERIAL_REMOVIDO'),
 'ordens_com_assinatura',(SELECT count(*) FROM ordens WHERE assinatura IS NOT NULL),
 'tipos_movimento',(SELECT jsonb_agg(to_jsonb(x)) FROM (SELECT tipo,count(*) registros FROM movimentos GROUP BY tipo) x),
 'tipos_auditoria',(SELECT jsonb_agg(to_jsonb(x)) FROM (SELECT tipo,count(*) registros FROM auditoria GROUP BY tipo ORDER BY tipo) x)
)::text;
COMMIT;
