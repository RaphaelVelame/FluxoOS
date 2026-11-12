param([switch]$Demo, [switch]$H2)
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
. (Join-Path $PSScriptRoot 'ambiente.ps1')
$java = Get-Command java -ErrorAction SilentlyContinue
if (-not $java) { throw 'Instale o JDK 25 ou superior e reabra o terminal.' }
$javaVersion = (& $java.Source -version 2>&1 | Out-String)
if ($LASTEXITCODE -ne 0 -or $javaVersion -notmatch 'version "(\d+)(?:\.(\d+))?') {
    throw 'Nao foi possivel identificar a versao do Java. Instale o JDK 25 ou superior.'
}
$javaMajor = [int]$Matches[1]
if ($javaMajor -eq 1) { $javaMajor = [int]$Matches[2] }
if ($javaMajor -lt 25) { throw "JDK 25 ou superior necessario; encontrado Java $javaMajor." }
$nomes = @('SPRING_PROFILES_ACTIVE', 'DATABASE_URL', 'DATABASE_USER', 'DATABASE_PASSWORD',
    'COOKIE_SECURE', 'PORT', 'SERVER_ADDRESS', 'BOOTSTRAP_PASSWORD', 'DEMO_DATA',
    'MAX_SESSOES', 'LOGIN_MAX_ERROS', 'LOGIN_MAX_ERROS_IP', 'LOGIN_JANELA_MINUTOS',
    'LOGIN_BLOQUEIO_MINUTOS', 'LOGIN_MAX_IDENTIFICADORES', 'SEGURANCA_LIMPEZA_MS')
$anteriores = @{}
foreach ($nome in $nomes) { $anteriores[$nome] = [Environment]::GetEnvironmentVariable($nome, 'Process') }
try {
    $config = Read-FluxoOsEnv
    foreach ($nome in $nomes) {
        if ($config.ContainsKey($nome) -and $null -eq $anteriores[$nome]) {
            [Environment]::SetEnvironmentVariable($nome, $config[$nome], 'Process')
        }
    }
    if ($H2) {
        $env:SPRING_PROFILES_ACTIVE = 'default'
        $env:DATABASE_URL = 'jdbc:h2:file:./data/fluxoos;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH'
        $env:DATABASE_USER = 'sa'
        $env:DATABASE_PASSWORD = ''
        $env:COOKIE_SECURE = 'false'
    }
    $postgres = ($env:SPRING_PROFILES_ACTIVE -split ',').Trim() -contains 'postgres'
    if ($env:DATABASE_URL -like 'jdbc:postgresql:*' -and -not $postgres) {
        throw 'Ative SPRING_PROFILES_ACTIVE=postgres para usar uma conexao PostgreSQL.'
    }
    $precisaAdmin = -not (Test-Path -LiteralPath 'data/fluxoos.mv.db')
    if ($postgres) {
        $conexao = Get-FluxoOsPostgres -Config $config
        $temTabela = Invoke-FluxoOsSql $conexao "SELECT to_regclass('public.usuarios') IS NOT NULL;"
        $precisaAdmin = $temTabela -ne 't'
        if (-not $precisaAdmin) {
            $precisaAdmin = (Invoke-FluxoOsSql $conexao 'SELECT count(*) FROM public.usuarios;') -eq '0'
        }
        Write-Host "PostgreSQL: $($conexao.HostName):$($conexao.Port)/$($conexao.Database) (usuario $($conexao.UserName))."
        # O perfil exige HTTPS por padrao. A execucao local usa HTTP, salvo configuracao explicita.
        if ($null -eq $env:COOKIE_SECURE) { $env:COOKIE_SECURE = 'false' }
    }
    if (-not $env:BOOTSTRAP_PASSWORD -and $precisaAdmin) {
        $segredo = Read-Host 'Defina a senha inicial do admin (minimo de 15 caracteres)' -AsSecureString
        $ponteiro = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($segredo)
        try { $env:BOOTSTRAP_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ponteiro) }
        finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ponteiro) }
        if ($env:BOOTSTRAP_PASSWORD.Length -lt 15) { throw 'A senha precisa ter pelo menos 15 caracteres.' }
    }
    if ($Demo) { $env:DEMO_DATA = 'true' }
    $porta = if ($env:PORT) { $env:PORT } else { '8080' }
    Write-Host "Acesse http://localhost:$porta quando o servidor iniciar. Use Ctrl+C para encerrar."
    & .\mvnw.cmd spring-boot:run
    $resultado = $LASTEXITCODE
} finally {
    foreach ($nome in $nomes) { [Environment]::SetEnvironmentVariable($nome, $anteriores[$nome], 'Process') }
}
exit $resultado
