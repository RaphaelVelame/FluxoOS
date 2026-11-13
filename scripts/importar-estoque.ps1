param(
    [Parameter(Mandatory = $true)][string]$Planilha,
    [switch]$Validar,
    [string]$SqlSaida
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'ambiente.ps1')
Add-Type -AssemblyName System.IO.Compression.FileSystem
Add-Type -AssemblyName System.IO.Compression

function Read-EstoqueXml($Zip, [string]$Nome) {
    $entrada = $Zip.GetEntry($Nome)
    if (-not $entrada -or $entrada.Length -gt 10MB) { throw "Parte XLSX ausente ou muito grande: $Nome" }
    $stream = $entrada.Open()
    $settings = [Xml.XmlReaderSettings]::new()
    $settings.DtdProcessing = [Xml.DtdProcessing]::Prohibit
    $settings.XmlResolver = $null
    $reader = [Xml.XmlReader]::Create($stream, $settings)
    try {
        $documento = [Xml.XmlDocument]::new()
        $documento.XmlResolver = $null
        $documento.Load($reader)
        return ,$documento
    } finally { $reader.Dispose(); $stream.Dispose() }
}

function Get-EstoqueTexto([string]$Endereco) {
    $celula = $celulas[$Endereco]
    if (-not $celula) { return '' }
    if ($celula.SelectSingleNode('*[local-name()="f"]')) { throw "Formula inesperada em $Endereco." }
    if ($celula.t -eq 's') { return $textos[[int]$celula.v].Trim() }
    if ($celula.t -eq 'inlineStr') { return $celula.is.InnerText.Trim() }
    return ([string]$celula.v).Trim()
}

function Get-EstoqueNumero([string]$Endereco) {
    $texto = Get-EstoqueTexto $Endereco
    if (-not $texto) { return 0 }
    $numero = 0.0
    if (-not [double]::TryParse($texto, [Globalization.NumberStyles]::Float,
            [Globalization.CultureInfo]::InvariantCulture, [ref]$numero) -or
            [double]::IsNaN($numero) -or [double]::IsInfinity($numero) -or
            $numero -lt 0 -or $numero -gt [int]::MaxValue -or $numero -ne [math]::Truncate($numero)) {
        throw "Quantidade/codigo invalido em $Endereco`: $texto. Esperado inteiro nao negativo."
    }
    return [int]$numero
}

function Quote-EstoqueSql([string]$Texto) { return "'" + $Texto.Replace("'", "''") + "'" }

$arquivo = (Resolve-Path -LiteralPath $Planilha).Path
# O hash e a leitura usam o mesmo arquivo bloqueado contra alteracoes durante a extracao.
$streamArquivo = [IO.File]::Open($arquivo, [IO.FileMode]::Open, [IO.FileAccess]::Read, [IO.FileShare]::Read)
$zip = $null
try {
    $sha = [Security.Cryptography.SHA256]::Create()
    try { $hash = ([BitConverter]::ToString($sha.ComputeHash($streamArquivo))).Replace('-', '').ToLowerInvariant() }
    finally { $sha.Dispose() }
    $streamArquivo.Position = 0
    $zip = [IO.Compression.ZipArchive]::new($streamArquivo, [IO.Compression.ZipArchiveMode]::Read, $true)
    $workbook = Read-EstoqueXml $zip 'xl/workbook.xml'
    $relacoes = Read-EstoqueXml $zip 'xl/_rels/workbook.xml.rels'
    $aba = @($workbook.workbook.sheets.sheet | Where-Object name -eq 'Estoque')
    if ($aba.Count -ne 1) { throw 'A planilha precisa conter uma unica aba chamada Estoque.' }
    $rid = $aba[0].GetAttribute('id', 'http://schemas.openxmlformats.org/officeDocument/2006/relationships')
    $relacao = @($relacoes.Relationships.Relationship | Where-Object Id -eq $rid)
    if ($relacao.Count -ne 1 -or $relacao[0].TargetMode -eq 'External') { throw 'Referencia de aba invalida.' }
    $destino = [string]$relacao[0].Target
    $parte = if ($destino.StartsWith('/')) { $destino.TrimStart('/') } else { 'xl/' + $destino }
    $xml = Read-EstoqueXml $zip $parte
    $textos = @()
    if ($zip.GetEntry('xl/sharedStrings.xml')) {
        $shared = Read-EstoqueXml $zip 'xl/sharedStrings.xml'
        $textos = @($shared.sst.si | ForEach-Object { $_.InnerText })
    }
    $celulas = @{}
    foreach ($linha in $xml.worksheet.sheetData.row) {
        foreach ($celula in $linha.c) { $celulas[[string]$celula.r] = $celula }
    }
    foreach ($cabecalho in @(@('A7', "C$([char]0x00f3)digo"),
            @('B7', "Descri$([char]0x00e7)$([char]0x00e3)o"),
            @('F7', "Refer$([char]0x00ea)ncia Fab."),
            @('G7', 'Unid.'), @('H7', 'S. Ant.'), @('AN7', 'Total'))) {
        if ((Get-EstoqueTexto $cabecalho[0]) -ne $cabecalho[1]) { throw "Layout inesperado em $($cabecalho[0])." }
    }
    $colunas = @('I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
        'AA','AB','AC','AD','AE','AF','AG','AH','AI','AJ','AK','AL','AM')
    $datas = @{}
    $anterior = $null
    foreach ($coluna in $colunas) {
        $dia = [datetime]::FromOADate((Get-EstoqueNumero "${coluna}7"))
        if ($anterior -and $dia -ne $anterior.AddDays(1)) { throw 'As datas das entradas devem ser consecutivas.' }
        $datas[$coluna] = $dia.ToString('yyyy-MM-dd')
        $anterior = $dia
    }
    $codigos = [Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
    $avisos = [Collections.Generic.List[string]]::new()
    $materiais = @(foreach ($linha in $xml.worksheet.sheetData.row) {
        $n = [int]$linha.r
        if ($n -le 7) { continue }
        $nome = Get-EstoqueTexto "B$n"
        if (-not $nome) {
            # Linhas apenas formatadas sao ignoradas; nao descarte registros incompletos.
            foreach ($coluna in @('A','F','G','H') + $colunas) {
                if (Get-EstoqueTexto "$coluna$n") { throw "Linha $n possui dados sem descricao." }
            }
            continue
        }
        $original = Get-EstoqueTexto "A$n"
        $codigo = if ($original) { [string](Get-EstoqueNumero "A$n") } else { "SEM-COD-L$n" }
        if (-not $original) { $avisos.Add("Linha $n sem codigo: atribuido $codigo ($nome).") }
        if (-not $codigos.Add($codigo)) {
            $codigo = "$codigo-L$n"
            if (-not $codigos.Add($codigo)) { throw "Codigo gerado repetido: $codigo" }
            $avisos.Add("Linha $n com codigo repetido: atribuido $codigo ($nome).")
        }
        $unidadeOriginal = Get-EstoqueTexto "G$n"
        $unidade = switch -Regex ($unidadeOriginal) {
            '^(Unid\.?|un)$' { 'un'; break }
            '^(Met[.,]?|m)$' { 'm'; break }
            default { throw "Unidade desconhecida em G${n}: $unidadeOriginal" }
        }
        $referencia = Get-EstoqueTexto "F$n"
        if ($nome.Length -gt 160 -or $referencia.Length -gt 160 -or $codigo.Length -gt 60) {
            throw "Texto maior que o campo do banco na linha $n."
        }
        $inicial = Get-EstoqueNumero "H$n"
        $entradas = @(foreach ($coluna in $colunas) {
            $quantidade = Get-EstoqueNumero "$coluna$n"
            if ($quantidade -gt 0) {
                [pscustomobject]@{ celula = "$coluna$n"; data = $datas[$coluna]; quantidade = $quantidade }
            }
        })
        $saldo = [long]$inicial
        foreach ($entrada in $entradas) { $saldo += $entrada.quantidade }
        if ($saldo -gt [int]::MaxValue) { throw "Saldo excede o limite na linha $n." }
        $total = $celulas["AN$n"]
        if ($total -and [string]$total.v) {
            $cache = 0.0
            if (-not [double]::TryParse([string]$total.v, [Globalization.NumberStyles]::Float,
                    [Globalization.CultureInfo]::InvariantCulture, [ref]$cache) -or $cache -ne $saldo) {
                throw "Total AN$n diverge da soma de H${n}:AM$n."
            }
        }
        [pscustomobject]@{ linha = $n; codigo = $codigo; codigoOriginal = $original;
            nome = $nome; referencia = $referencia; unidade = $unidade; unidadeOriginal = $unidadeOriginal;
            inicial = $inicial; entradas = $entradas; saldo = [int]$saldo }
    })
    if ($materiais.Count -eq 0) { throw 'Nenhum material encontrado.' }
} finally {
    if ($zip) { $zip.Dispose() }
    $streamArquivo.Dispose()
}

$resumo = [ordered]@{
    arquivo = [IO.Path]::GetFileName($arquivo); sha256 = $hash; materiais = $materiais.Count;
    comSaldo = @($materiais | Where-Object saldo -gt 0).Count;
    semSaldo = @($materiais | Where-Object saldo -eq 0).Count;
    referencias = @($materiais | Where-Object referencia).Count;
    saldosPorUnidade = @($materiais | Group-Object unidade | ForEach-Object {
        @{ unidade = $_.Name; saldo = ($_.Group | Measure-Object saldo -Sum).Sum }
    }); avisos = @($avisos)
}
$resumo | ConvertTo-Json -Depth 5 | Write-Output

$json = Quote-EstoqueSql (ConvertTo-Json -InputObject @($materiais) -Depth 6 -Compress)
$nomeArquivo = Quote-EstoqueSql ([IO.Path]::GetFileName($arquivo))
# Um lote e uma transacao: erros ou conflitos revertem materiais, movimentos e auditoria juntos.
$sql = @"
BEGIN;
SET LOCAL standard_conforming_strings = on;
SET LOCAL lock_timeout = '10s';
LOCK TABLE materiais, movimentos, auditoria IN SHARE ROW EXCLUSIVE MODE;
DO `$importacao`$
DECLARE
    item jsonb;
    entrada jsonb;
    material_id bigint;
    ator bigint;
    saldo integer;
    origem text;
BEGIN
    IF EXISTS (SELECT 1 FROM auditoria WHERE tipo = 'IMPORTACAO_ESTOQUE' AND detalhes = '$hash') THEN
        RAISE NOTICE 'Arquivo ja importado; nenhum saldo alterado.';
        RETURN;
    END IF;
    SELECT id INTO ator FROM usuarios WHERE ativo AND perfil = 'CHEFE' ORDER BY id LIMIT 1;
    IF ator IS NULL THEN RAISE EXCEPTION 'Inicialize a conta administrativa antes da importacao.'; END IF;
    FOR item IN SELECT value FROM jsonb_array_elements($($json)::jsonb) LOOP
        IF EXISTS (SELECT 1 FROM materiais WHERE codigo = item->>'codigo') THEN
            RAISE EXCEPTION 'Codigo % ja cadastrado. Importacao cancelada sem alterar saldos.', item->>'codigo';
        END IF;
        material_id := nextval('sequencia_ids');
        origem := $nomeArquivo || ' | Estoque!linha ' || (item->>'linha');
        INSERT INTO materiais (id,codigo,nome,unidade,referencia_fabricante,quantidade,quantidade_minima,ativo)
        VALUES (material_id,item->>'codigo',item->>'nome',item->>'unidade',NULLIF(item->>'referencia',''),
            (item->>'saldo')::integer,0,true);
        saldo := (item->>'inicial')::integer;
        IF saldo > 0 THEN
            INSERT INTO movimentos (id,material_id,usuario_id,tipo,variacao,saldo_apos,observacao)
            VALUES (nextval('sequencia_ids'),material_id,ator,'ENTRADA',saldo,saldo,
                'Saldo anterior importado: ' || origem || ' | H' || (item->>'linha'));
        END IF;
        FOR entrada IN SELECT value FROM jsonb_array_elements(item->'entradas') LOOP
            saldo := saldo + (entrada->>'quantidade')::integer;
            INSERT INTO movimentos (id,material_id,usuario_id,tipo,variacao,saldo_apos,observacao)
            VALUES (nextval('sequencia_ids'),material_id,ator,'ENTRADA',(entrada->>'quantidade')::integer,saldo,
                'Entrada da planilha em ' || (entrada->>'data') || ': ' || origem || ' | ' || (entrada->>'celula'));
        END LOOP;
        IF saldo <> (item->>'saldo')::integer THEN RAISE EXCEPTION 'Saldo inconsistente.'; END IF;
        INSERT INTO auditoria (id,usuario_id,tipo,detalhes)
        VALUES (nextval('sequencia_ids'),ator,'MATERIAL_IMPORTADO',
            jsonb_build_object('sha256','$hash','materialId',material_id,'linha',item->'linha',
                'codigoOriginal',item->>'codigoOriginal','codigo',item->>'codigo',
                'unidadeOriginal',item->>'unidadeOriginal','origem',origem,'minimoInformado',false)::text);
    END LOOP;
    INSERT INTO auditoria (id,usuario_id,tipo,detalhes)
    VALUES (nextval('sequencia_ids'),ator,'IMPORTACAO_ESTOQUE','$hash');
END
`$importacao`$;
SELECT count(*) AS materiais_importados FROM auditoria
WHERE (CASE WHEN tipo = 'MATERIAL_IMPORTADO' THEN detalhes::jsonb END)->>'sha256' = '$hash';
COMMIT;
"@
if ($SqlSaida) {
    [IO.File]::WriteAllText($ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($SqlSaida),
        $sql, [Text.UTF8Encoding]::new($false))
}
if ($Validar) { return }
$config = Read-FluxoOsEnv
$conexao = Get-FluxoOsPostgres $config
# O psql recebe o SQL pelo stdin em UTF-8, sem expor credenciais na linha de comando.
$encodingAnterior = $OutputEncoding
try {
    $OutputEncoding = [Text.UTF8Encoding]::new($false)
    Invoke-FluxoOsSql $conexao $sql
} finally { $OutputEncoding = $encodingAnterior }
