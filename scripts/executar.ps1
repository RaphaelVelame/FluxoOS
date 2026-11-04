param([switch]$Demo)
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
if (-not (Get-Command java -ErrorAction SilentlyContinue)) { throw 'Instale o JDK 17 ou superior e reabra o terminal.' }
if (-not $env:BOOTSTRAP_PASSWORD -and -not (Test-Path 'data/fluxoos.mv.db')) {
    $segredo = Read-Host 'Defina a senha inicial do admin (minimo de 15 caracteres)' -AsSecureString
    $ponteiro = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($segredo)
    try { $env:BOOTSTRAP_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ponteiro) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ponteiro) }
    if ($env:BOOTSTRAP_PASSWORD.Length -lt 15) { throw 'A senha precisa ter pelo menos 15 caracteres.' }
}
if ($Demo) { $env:DEMO_DATA = 'true' }
Write-Host 'Acesse http://localhost:8080 quando o servidor iniciar. Use Ctrl+C para encerrar.'
try { & .\mvnw.cmd spring-boot:run; $resultado = $LASTEXITCODE }
finally { Remove-Item Env:BOOTSTRAP_PASSWORD -ErrorAction SilentlyContinue }
exit $resultado
