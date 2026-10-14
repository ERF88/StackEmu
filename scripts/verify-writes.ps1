#Requires -Version 7.0
param([string]$BaseUrl = 'http://127.0.0.1:8080')

$ErrorActionPreference = 'Stop'
$fixtureId = [guid]::NewGuid().ToString('N')
$bucket = "stackemu-web-$fixtureId"
$unicodeLabel = 'a{0}{1}o' -f [char]0xE7, [char]0xE3
$key = "smoke/$unicodeLabel +%.txt"
$projectDirectory = Split-Path -Parent $PSScriptRoot
$fixtureDirectory = Join-Path $projectDirectory "work/write-smoke-$fixtureId"
$source = Join-Path $projectDirectory 'LICENSE'
$destination = Join-Path $fixtureDirectory 'download.bin'
$bucketCreated = $false
New-Item -ItemType Directory -Path $fixtureDirectory -Force | Out-Null
$credentials = @('exec', '-T', '-e', 'AWS_ACCESS_KEY_ID=test', '-e', 'AWS_SECRET_ACCESS_KEY=test',
    'ministack', 'aws', '--endpoint-url', 'http://localhost:4566', '--region', 'us-east-1', 's3api')
try {
    $form = Invoke-WebRequest "$BaseUrl/s3/write?action=bucket&lang=pt-BR" -SessionVariable mutationSession
    $token = [regex]::Match($form.Content, 'name="csrf-token" value="([^"]+)"').Groups[1].Value
    if (-not $token) { throw 'CSRF token absent from deployed form' }
    $body = @{ 'csrf-token' = $token; bucket = $bucket; confirm = 'yes'; lang = 'pt-BR' }
    $result = Invoke-WebRequest "$BaseUrl/s3/write/bucket" -WebSession $mutationSession -Method Post -Body $body
    if ($result.StatusCode -ne 200) { throw 'Bucket creation failed' }
    $bucketCreated = $true
    $body.key = 'smoke/'
    Invoke-WebRequest "$BaseUrl/s3/write/folder" -WebSession $mutationSession -Method Post -Body $body | Out-Null
    $upload = @{ 'csrf-token' = $token; bucket = $bucket; key = $key; confirm = 'yes'; file = Get-Item -LiteralPath $source }
    Invoke-WebRequest "$BaseUrl/s3/write/upload" -WebSession $mutationSession -Method Post -Form $upload | Out-Null
    $page = Invoke-WebRequest "$BaseUrl/s3/bucket?bucket=$bucket&prefix=smoke%2F"
    if ($page.Content -notmatch $unicodeLabel) { throw 'Unicode uploaded key absent from navigation' }
    $encodedKey = [uri]::EscapeDataString($key)
    Invoke-WebRequest "$BaseUrl/s3/download?bucket=$bucket&key=$encodedKey" -OutFile $destination
    if ((Get-FileHash -LiteralPath $source).Hash -ne (Get-FileHash -LiteralPath $destination).Hash) {
        throw 'Deployed upload/download SHA-256 mismatch'
    }
    $rejected = Invoke-WebRequest "$BaseUrl/s3/write/folder" -Method Post -Body @{bucket = $bucket; key = 'csrf/'; confirm = 'yes' } -SkipHttpErrorCheck
    if ($rejected.StatusCode -ne 400) { throw 'Deployed CSRF rejection failed' }
    Write-Output 'PASS: deployed bucket/marker/upload/navigation/download, SHA-256 and CSRF rejection'
} finally {
    if ($bucketCreated) {
        foreach ($fixtureKey in @($key, 'smoke/')) {
            & docker compose @credentials delete-object --bucket $bucket --key $fixtureKey | Out-Null
            if ($LASTEXITCODE -ne 0) { throw 'Fixture object cleanup failed' }
        }
        & docker compose @credentials delete-bucket --bucket $bucket | Out-Null
        if ($LASTEXITCODE -ne 0) { throw 'Fixture bucket cleanup failed' }
    }
    if (Test-Path -LiteralPath $destination) { Remove-Item -LiteralPath $destination -Force }
    Remove-Item -LiteralPath $fixtureDirectory
}
