$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
. (Join-Path $PSScriptRoot 'ambiente.ps1')
$conexao = Get-FluxoOsPostgres -Config (Read-FluxoOsEnv)
if (([Uri]$conexao.Url.Substring(5)).Query) {
    throw 'Use uma DATABASE_URL sem parametros para o teste isolado de PostgreSQL.'
}
$schema = 'fluxoos_test_' + [Guid]::NewGuid().ToString('N')
$nomes = @('TEST_DATABASE_URL', 'TEST_DATABASE_USER', 'TEST_DATABASE_PASSWORD')
$anteriores = @{}
foreach ($nome in $nomes) { $anteriores[$nome] = [Environment]::GetEnvironmentVariable($nome, 'Process') }
$criado = $false
$resultado = 1
try {
    Invoke-FluxoOsSql $conexao "CREATE SCHEMA $schema;" | Out-Null
    $criado = $true
    $env:TEST_DATABASE_URL = $conexao.Url + '?currentSchema=' + $schema
    $env:TEST_DATABASE_USER = $conexao.UserName
    $env:TEST_DATABASE_PASSWORD = $conexao.Password
    Write-Host "Validando migracoes e fluxo operacional no esquema temporario $schema."
    & .\mvnw.cmd '-Dtest=FluxoOperacionalTest,RevogacaoSessaoTest,MigracaoSessaoTest,LimitesLoginTest' test
    $resultado = $LASTEXITCODE
} finally {
    foreach ($nome in $nomes) { [Environment]::SetEnvironmentVariable($nome, $anteriores[$nome], 'Process') }
    if ($criado) {
        Invoke-FluxoOsSql $conexao "SET client_min_messages = WARNING; DROP SCHEMA $schema CASCADE;" | Out-Null
        Write-Host 'Esquema temporario removido.'
    }
}
exit $resultado
