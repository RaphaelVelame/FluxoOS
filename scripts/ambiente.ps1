function Read-FluxoOsEnv {
    param([string]$Path = (Join-Path (Split-Path $PSScriptRoot -Parent) '.env'))
    $valores = @{}
    if (-not (Test-Path -LiteralPath $Path)) { return $valores }
    foreach ($linha in Get-Content -LiteralPath $Path -Encoding UTF8) {
        $texto = $linha.Trim()
        if (-not $texto -or $texto.StartsWith('#')) { continue }
        if ($texto -notmatch '^([A-Z][A-Z0-9_]*)=(.*)$') {
            throw "Linha invalida em $Path. Use NOME=valor."
        }
        $nome = $Matches[1]
        $valor = $Matches[2].Trim()
        if ($valor.Length -ge 2 -and (($valor.StartsWith('"') -and $valor.EndsWith('"')) -or
                ($valor.StartsWith("'") -and $valor.EndsWith("'")))) {
            $valor = $valor.Substring(1, $valor.Length - 2)
        }
        $valores[$nome] = $valor
    }
    return $valores
}

function Get-FluxoOsPsql {
    $comando = Get-Command psql -ErrorAction SilentlyContinue
    if ($comando) { return $comando.Source }
    $instalacoes = Join-Path $env:ProgramFiles 'PostgreSQL'
    if (Test-Path -LiteralPath $instalacoes) {
        $diretorios = Get-ChildItem -LiteralPath $instalacoes -Directory |
            Where-Object { $_.Name -match '^\d+(\.\d+){0,2}$' } |
            Sort-Object { [version]($_.Name + '.0') } -Descending
        foreach ($diretorio in $diretorios) {
            $candidato = Join-Path $diretorio.FullName 'bin/psql.exe'
            if (Test-Path -LiteralPath $candidato) { return $candidato }
        }
    }
    throw 'psql nao encontrado. Adicione a pasta bin do PostgreSQL ao PATH.'
}

function Get-FluxoOsPostgres {
    param([hashtable]$Config)
    $valores = @{}
    foreach ($nome in @('DATABASE_URL', 'DATABASE_USER', 'DATABASE_PASSWORD')) {
        $valorAmbiente = [Environment]::GetEnvironmentVariable($nome, 'Process')
        $valores[$nome] = if ($null -ne $valorAmbiente) { $valorAmbiente } else { $Config[$nome] }
    }
    if (-not $valores.DATABASE_URL -or -not $valores.DATABASE_URL.StartsWith('jdbc:postgresql://')) {
        throw 'Defina DATABASE_URL com uma URL jdbc:postgresql:// no .env ou no ambiente.'
    }
    $uri = [Uri]$valores.DATABASE_URL.Substring(5)
    $banco = [Uri]::UnescapeDataString($uri.AbsolutePath.TrimStart('/'))
    if (-not $uri.Host -or -not $banco -or -not $valores.DATABASE_USER -or -not $valores.DATABASE_PASSWORD) {
        throw 'Preencha DATABASE_URL, DATABASE_USER e DATABASE_PASSWORD no .env ou no ambiente.'
    }
    return @{
        Psql = Get-FluxoOsPsql
        HostName = $uri.DnsSafeHost
        Port = $(if ($uri.Port -gt 0) { $uri.Port } else { 5432 })
        Database = $banco
        UserName = $valores.DATABASE_USER
        Password = $valores.DATABASE_PASSWORD
        Url = $valores.DATABASE_URL
    }
}

function Invoke-FluxoOsSql {
    param([hashtable]$Connection, [string]$Sql)
    $senhaAnterior = $env:PGPASSWORD
    $timeoutAnterior = $env:PGCONNECT_TIMEOUT
    $opcoesAnteriores = $env:PGOPTIONS
    $encodingAnterior = $env:PGCLIENTENCODING
    try {
        $env:PGPASSWORD = $Connection.Password
        $env:PGCONNECT_TIMEOUT = '5'
        $env:PGOPTIONS = '-c search_path=public'
        $env:PGCLIENTENCODING = 'UTF8'
        $argumentos = @('-X', '-w', '-h', $Connection.HostName, '-p', $Connection.Port,
            '-U', $Connection.UserName, '-d', $Connection.Database, '-v', 'ON_ERROR_STOP=1', '-t', '-A', '-f', '-')
        $resultado = $Sql | & $Connection.Psql @argumentos
        if ($LASTEXITCODE -ne 0) { throw 'Falha no PostgreSQL. Verifique o servico e as credenciais de conexao.' }
        return $resultado
    } finally {
        [Environment]::SetEnvironmentVariable('PGPASSWORD', $senhaAnterior, 'Process')
        [Environment]::SetEnvironmentVariable('PGCONNECT_TIMEOUT', $timeoutAnterior, 'Process')
        [Environment]::SetEnvironmentVariable('PGOPTIONS', $opcoesAnteriores, 'Process')
        [Environment]::SetEnvironmentVariable('PGCLIENTENCODING', $encodingAnterior, 'Process')
    }
}
