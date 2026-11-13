param([switch]$Postgres)
$ErrorActionPreference = 'Stop'
$raiz = Split-Path $PSScriptRoot -Parent
. (Join-Path $raiz 'scripts/ambiente.ps1')
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$pasta = Join-Path $raiz ('target/importacao-test-' + [guid]::NewGuid().ToString('N'))
[IO.Directory]::CreateDirectory($pasta) | Out-Null
$importador = Join-Path $raiz 'scripts/importar-estoque.ps1'

function Assert-Importacao($Condicao, [string]$Mensagem) {
    if (-not $Condicao) { throw $Mensagem }
}

function New-Fixture([string]$Nome, [string]$Inicial = '3', [string]$Total = '5', [switch]$Conflito) {
    $colunas = @('I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
        'AA','AB','AC','AD','AE','AF','AG','AH','AI','AJ','AK','AL','AM')
    $datas = for ($i = 0; $i -lt $colunas.Count; $i++) {
        '<c r="' + $colunas[$i] + '7"><v>' + (46332 + $i) + '</v></c>'
    }
    $primeiroCodigo = if ($Conflito) { '9' } else { '1' }
    $xml = @"
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>
<row r="7"><c r="A7" t="inlineStr"><is><t>C&#xF3;digo</t></is></c>
<c r="B7" t="inlineStr"><is><t>Descri&#xE7;&#xE3;o</t></is></c>
<c r="F7" t="inlineStr"><is><t>Refer&#xEA;ncia Fab.</t></is></c>
<c r="G7" t="inlineStr"><is><t>Unid.</t></is></c><c r="H7" t="inlineStr"><is><t>S. Ant.</t></is></c>
$($datas -join '')<c r="AN7" t="inlineStr"><is><t>Total</t></is></c></row>
<row r="8"><c r="A8"><v>$primeiroCodigo</v></c><c r="B8" t="inlineStr"><is><t>Pe&#xE7;a d'agua</t></is></c>
<c r="F8" t="inlineStr"><is><t>Fab'&amp;A</t></is></c><c r="G8" t="inlineStr"><is><t>Unid.</t></is></c>
<c r="H8"><v>$Inicial</v></c><c r="I8"><v>2</v></c><c r="AN8"><f>SUM(H8:AM8)</f><v>$Total</v></c></row>
<row r="9"><c r="A9"><v>1</v></c><c r="B9" t="inlineStr"><is><t>Cabo</t></is></c>
<c r="G9" t="inlineStr"><is><t>Met,</t></is></c><c r="AN9"><v>0</v></c></row>
<row r="10"><c r="B10" t="inlineStr"><is><t>Sem codigo</t></is></c>
<c r="G10" t="inlineStr"><is><t>Unid.</t></is></c><c r="H10"><v>1</v></c></row>
<row r="11"><c r="B11" s="1"/></row>
</sheetData></worksheet>
"@
    $arquivo = Join-Path $pasta "$Nome.xlsx"
    $zip = [IO.Compression.ZipFile]::Open($arquivo, [IO.Compression.ZipArchiveMode]::Create)
    try {
        $partes = @{
            'xl/workbook.xml' = '<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Estoque" sheetId="1" r:id="rId1"/></sheets></workbook>';
            'xl/_rels/workbook.xml.rels' = '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Target="worksheets/sheet1.xml"/></Relationships>';
            'xl/worksheets/sheet1.xml' = $xml
        }
        foreach ($nomeParte in $partes.Keys) {
            $writer = [IO.StreamWriter]::new($zip.CreateEntry($nomeParte).Open(), [Text.UTF8Encoding]::new($false))
            try { $writer.Write($partes[$nomeParte]) } finally { $writer.Dispose() }
        }
    } finally { $zip.Dispose() }
    return $arquivo
}

$valida = New-Fixture 'valida'
$arquivoSql = Join-Path $pasta 'importacao.sql'
$resumo = (& $importador -Planilha $valida -Validar -SqlSaida $arquivoSql | Out-String) | ConvertFrom-Json
Assert-Importacao ($resumo.materiais -eq 3 -and $resumo.comSaldo -eq 2 -and $resumo.semSaldo -eq 1) 'Contagem incorreta.'
Assert-Importacao ($resumo.avisos.Count -eq 2) 'Codigo repetido/ausente nao identificado.'
$sql = [IO.File]::ReadAllText($arquivoSql)
Assert-Importacao ($sql.Contains('1-L9') -and $sql.Contains('SEM-COD-L10')) 'Materiais distintos nao preservados.'
Assert-Importacao ($sql.Contains('Fab\u0027\u0026A') -and $sql.Contains('"saldo":5')) 'Referencia SQL ou saldo incorreto.'
foreach ($caso in @(@('negativa','-1','1'), @('fracionaria','1.5','3.5'), @('totalErrado','3','6'))) {
    $invalida = New-Fixture $caso[0] $caso[1] $caso[2]
    $rejeitada = $false
    try { & $importador -Planilha $invalida -Validar | Out-Null } catch { $rejeitada = $true }
    Assert-Importacao $rejeitada "Entrada invalida aceita: $($caso[0])"
}
Write-Output 'OK: extracao, saldos sem cache, acentos, referencias, codigos, negativos, fracoes e totais divergentes.'

if (-not $Postgres) { return }
if ($env:TEST_DATABASE_DISPOSABLE -ne 'true' -or $env:DATABASE_URL -ne $env:TEST_DATABASE_URL) {
    throw 'Use scripts/testar-postgres.ps1 -IncluirImportacao para testar apenas em cluster descartavel.'
}
$db = Get-FluxoOsPostgres (Read-FluxoOsEnv (Join-Path $raiz '.env'))
$schema = 'importacao_test_' + [guid]::NewGuid().ToString('N')
$encodingAnterior = $OutputEncoding
$OutputEncoding = [Text.UTF8Encoding]::new($false)
try {
    $estrutura = "CREATE SCHEMA $schema; SET search_path TO $schema;`n"
    $migracoes = @(Get-ChildItem (Join-Path $raiz 'src/main/resources/db/migration/*.sql')) +
        @(Get-ChildItem (Join-Path $raiz 'src/main/resources/db/postgresql/*.sql'))
    foreach ($migracao in $migracoes | Sort-Object { [int]([regex]::Match($_.Name, '^V(\d+)').Groups[1].Value) }) {
        $estrutura += [IO.File]::ReadAllText($migracao.FullName) + "`n"
    }
    $hashTeste = 'pbkdf2_sha256$600000$AAAAAAAAAAAAAAAAAAAAAA$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'
    $estrutura += @"
INSERT INTO usuarios(id,nome_usuario,nome_completo,perfil,ativo,hash_senha) VALUES(1,'teste','Teste','CHEFE',true,'$hashTeste');
INSERT INTO materiais(id,codigo,nome,unidade,quantidade,quantidade_minima,ativo) VALUES(2,'PRESERVAR','Existente','un',10,0,true);
"@
    Invoke-FluxoOsSql $db $estrutura | Out-Null
    $sqlTeste = $sql.Replace('BEGIN;', "BEGIN; SET LOCAL search_path TO $schema;")
    Invoke-FluxoOsSql $db $sqlTeste | Out-Null
    $estado = Invoke-FluxoOsSql $db "SET search_path TO $schema; SELECT count(*)||'|'||sum(quantidade) FROM materiais; SELECT count(*) FROM movimentos; SELECT referencia_fabricante FROM materiais WHERE codigo='1';"
    Assert-Importacao ($estado -contains '4|16' -and $estado -contains '3' -and $estado -contains "Fab'&A") 'Carga incompleta ou dados existentes alterados.'
    Invoke-FluxoOsSql $db "UPDATE $schema.materiais SET quantidade=4 WHERE codigo='1';" | Out-Null
    Invoke-FluxoOsSql $db $sqlTeste | Out-Null
    $estado = Invoke-FluxoOsSql $db "SELECT count(*)||'|'||sum(quantidade) FROM $schema.materiais; SELECT count(*) FROM $schema.movimentos;"
    Assert-Importacao ($estado -contains '4|15' -and $estado -contains '3') 'Reimportacao duplicou dados ou restaurou saldo antigo.'
    $conflito = New-Fixture 'conflito' -Conflito
    & $importador -Planilha $conflito -Validar -SqlSaida $arquivoSql | Out-Null
    $sqlConflito = [IO.File]::ReadAllText($arquivoSql).Replace('BEGIN;', "BEGIN; SET LOCAL search_path TO $schema;")
    $rejeitada = $false
    try { Invoke-FluxoOsSql $db $sqlConflito | Out-Null } catch { $rejeitada = $true }
    Assert-Importacao $rejeitada 'Conflito com codigo existente foi aceito.'
    $estado = Invoke-FluxoOsSql $db "SELECT count(*)||'|'||sum(quantidade) FROM $schema.materiais; SELECT count(*) FROM $schema.movimentos; SELECT count(*) FROM $schema.auditoria;"
    Assert-Importacao ($estado -contains '4|15' -and $estado -contains '3' -and $estado -contains '4') 'Rollback parcial: lote com conflito deixou registros.'
    Write-Output 'OK: PostgreSQL, preservacao de dados, movimentos, idempotencia e rollback integral em conflito.'
} finally {
    Invoke-FluxoOsSql $db "DROP SCHEMA IF EXISTS $schema CASCADE;" | Out-Null
    $OutputEncoding = $encodingAnterior
}
