param([switch]$IncluirImportacao)
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
. (Join-Path $PSScriptRoot 'ambiente.ps1')
# Usa apenas os binarios instalados. Nao le o .env nem se conecta ao banco operacional.
$bin = Split-Path (Get-FluxoOsPsql) -Parent
$initdb = Join-Path $bin 'initdb.exe'
$pgctl = Join-Path $bin 'pg_ctl.exe'
if (-not (Test-Path -LiteralPath $initdb) -or -not (Test-Path -LiteralPath $pgctl)) {
    throw 'Instale os binarios completos do PostgreSQL (initdb e pg_ctl).'
}
$raizTeste = Join-Path (Get-Location) ('target/postgres-test-' + [Guid]::NewGuid().ToString('N'))
[IO.Directory]::CreateDirectory($raizTeste) | Out-Null
$dados = Join-Path $raizTeste 'pgdata'
$listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, 0)
$listener.Start()
try { $porta = $listener.LocalEndpoint.Port } finally { $listener.Stop() }
$nomes = @('TEST_DATABASE_URL','TEST_DATABASE_USER','TEST_DATABASE_PASSWORD','TEST_DATABASE_DISPOSABLE',
    'DATABASE_URL','DATABASE_USER','DATABASE_PASSWORD','SPRING_PROFILES_ACTIVE','PGOPTIONS','PGHOST','PGPORT')
$anteriores = @{}
foreach ($nome in $nomes) { $anteriores[$nome] = [Environment]::GetEnvironmentVariable($nome, 'Process') }
$resultado = 1
try {
    & $initdb -D $dados -U fluxoos_teste -A trust --encoding=UTF8 --locale=C > (Join-Path $raizTeste 'initdb.log')
    if ($LASTEXITCODE -ne 0) { throw "Falha ao criar cluster descartavel. Consulte $raizTeste/initdb.log" }
    & $pgctl -D $dados -l (Join-Path $raizTeste 'postgres.log') -o "-h 127.0.0.1 -p $porta" -w -t 20 start
    if ($LASTEXITCODE -ne 0) { throw "Falha ao iniciar cluster descartavel. Consulte $raizTeste/postgres.log" }
    $env:TEST_DATABASE_URL = "jdbc:postgresql://127.0.0.1:$porta/postgres"
    $env:TEST_DATABASE_USER = 'fluxoos_teste'
    $env:TEST_DATABASE_PASSWORD = 'somente-cluster-descartavel'
    $env:TEST_DATABASE_DISPOSABLE = 'true'
    $env:DATABASE_URL = $env:TEST_DATABASE_URL
    $env:DATABASE_USER = $env:TEST_DATABASE_USER
    $env:DATABASE_PASSWORD = $env:TEST_DATABASE_PASSWORD
    $env:SPRING_PROFILES_ACTIVE = 'default'
    [Environment]::SetEnvironmentVariable('PGOPTIONS',$null,'Process')
    Write-Host "Validando no cluster descartavel 127.0.0.1:$porta. O banco configurado no .env nao sera utilizado."
    & .\mvnw.cmd '-Dfluxoos.build.directory=target/postgres-verificacao' '-Dtest=FluxoOperacionalTest,RevogacaoSessaoTest,MigracaoSessaoTest,LimitesLoginTest,IntegridadeSchemaTest,PropriedadeEstoqueTest,MultiTenantRbacTest,MigracaoMultiTenantTest' test
    $resultado = $LASTEXITCODE
    if ($resultado -eq 0 -and $IncluirImportacao) {
        & (Join-Path (Split-Path $PSScriptRoot -Parent) 'tests/importacao-estoque.ps1') -Postgres
    }
} finally {
    try {
        if (Test-Path -LiteralPath (Join-Path $dados 'postmaster.pid')) {
            & $pgctl -D $dados -m fast -w -t 20 stop
            if ($LASTEXITCODE -ne 0) { throw "Nao foi possivel encerrar o cluster de testes em $dados." }
        }
    } finally {
        foreach ($nome in $nomes) { [Environment]::SetEnvironmentVariable($nome, $anteriores[$nome], 'Process') }
    }
    Write-Host "Cluster descartavel encerrado. Logs e dados de teste em $raizTeste."
}
exit $resultado
