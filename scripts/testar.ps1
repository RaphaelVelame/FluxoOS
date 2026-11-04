$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
& .\mvnw.cmd test
exit $LASTEXITCODE
