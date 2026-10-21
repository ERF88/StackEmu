#Requires -Version 7.0
param([string]$BaseUrl = 'http://127.0.0.1:8080')

$ErrorActionPreference = 'Stop'
function Invoke-TestCompose {
    param([Parameter(Mandatory)][string[]]$ComposeArguments)
    & docker compose @ComposeArguments
    if ($LASTEXITCODE -ne 0) { throw 'Streaming fixture command failed' }
}

$fixtureId = [guid]::NewGuid().ToString('N')
$bucket = "stackemu-stream-$fixtureId"
$projectDirectory = Split-Path -Parent $PSScriptRoot
$fixtureDirectory = Join-Path $projectDirectory "work/streaming-$fixtureId"
New-Item -ItemType Directory -Path $fixtureDirectory -Force | Out-Null
$source = Join-Path $fixtureDirectory 'source.bin'
$destination = Join-Path $fixtureDirectory 'download.bin'
$containerFile = "/tmp/stackemu-stream-$fixtureId.bin"
$bucketCreated = $false
$containerFileCreated = $false
$credentials = @('exec', '-T', '-e', 'AWS_ACCESS_KEY_ID=test', '-e', 'AWS_SECRET_ACCESS_KEY=test',
    'ministack', 'aws', '--endpoint-url', 'http://localhost:4566', '--region', 'us-east-1', 's3api')
try {
    $chunk = [byte[]]::new(65536)
    [System.Random]::new(42).NextBytes($chunk)
    $file = [System.IO.File]::Create($source)
    try {
        for ($index = 0; $index -lt 2048; $index++) { $file.Write($chunk, 0, $chunk.Length) }
    } finally { $file.Dispose() }
    Invoke-TestCompose -ComposeArguments ($credentials + @('create-bucket', '--bucket', $bucket)) | Out-Null
    $bucketCreated = $true
    Invoke-TestCompose -ComposeArguments @('cp', $source, "ministack:$containerFile")
    $containerFileCreated = $true
    Invoke-TestCompose -ComposeArguments ($credentials + @('put-object', '--bucket', $bucket,
            '--key', 'large.bin', '--body', $containerFile, '--content-type', 'application/octet-stream')) | Out-Null

    $ready = $false
    for ($attempt = 0; $attempt -lt 30; $attempt++) {
        try {
            $page = Invoke-WebRequest "$BaseUrl/connection?lang=en" -TimeoutSec 5
            if ($page.StatusCode -eq 200 -and $page.Content -match 'S3 accessible') { $ready = $true; break }
        } catch { $ready = $false }
        Start-Sleep -Seconds 1
    }
    if (-not $ready) { throw 'GUI did not become ready for the streaming test' }
    $heapOptions = Invoke-TestCompose -ComposeArguments @('exec', '-T', 'gui', 'printenv', 'JAVA_TOOL_OPTIONS')
    if ($heapOptions -notmatch '-Xmx64m') { throw 'Apply compose.streaming-test.yaml before this test' }
    $timer = [System.Diagnostics.Stopwatch]::StartNew()
    Invoke-WebRequest "$BaseUrl/s3/download?bucket=$bucket&key=large.bin" -OutFile $destination -TimeoutSec 120
    $timer.Stop()
    $length = (Get-Item -LiteralPath $destination).Length
    $sourceHash = (Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash
    $downloadHash = (Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash
    if ($length -ne 134217728 -or $sourceHash -ne $downloadHash) { throw 'Streaming size/hash mismatch' }
    Invoke-RestMethod "$BaseUrl/q/health/live" | Out-Null
    Write-Output "PASS: 128 MiB streamed with -Xmx64m; SHA256=$downloadHash; seconds=$($timer.Elapsed.TotalSeconds)"
} finally {
    if ($bucketCreated) {
        Invoke-TestCompose -ComposeArguments ($credentials + @('delete-object', '--bucket', $bucket, '--key', 'large.bin')) | Out-Null
        Invoke-TestCompose -ComposeArguments ($credentials + @('delete-bucket', '--bucket', $bucket)) | Out-Null
    }
    if ($containerFileCreated) {
        Invoke-TestCompose -ComposeArguments @('exec', '-T', 'ministack', 'rm', '--', $containerFile)
    }
    foreach ($fixtureFile in @($source, $destination)) {
        if (Test-Path -LiteralPath $fixtureFile) { Remove-Item -LiteralPath $fixtureFile -Force }
    }
    Remove-Item -LiteralPath $fixtureDirectory
}
