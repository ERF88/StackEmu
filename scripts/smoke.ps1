$ErrorActionPreference = 'Stop'
$page = $null
for ($attempt = 0; $attempt -lt 30; $attempt++) {
    try {
        $page = Invoke-WebRequest 'http://127.0.0.1:8080/s3?lang=en' -TimeoutSec 5
        if ($page.Content -match 'S3 accessible') { break }
    } catch {
        $page = $null
    }
    Start-Sleep -Seconds 1
}
if ($page.StatusCode -ne 200 -or $page.Content -notmatch 'S3 accessible') {
    throw 'GUI is not connected to the emulator'
}
$fragment = Invoke-WebRequest 'http://127.0.0.1:8080/buckets?lang=pt-BR' -Headers @{'HX-Request' = 'true' }
if ($fragment.Content -notmatch 'S3 acess\u00edvel' -or $fragment.Content -match '<!doctype') {
    throw 'Localized partial response failed'
}
Invoke-RestMethod 'http://127.0.0.1:8080/q/health/live' | Out-Null
$connection = (Invoke-WebRequest 'http://127.0.0.1:8080/connection?lang=en&theme=light').Content
if ($connection -notmatch 'Effective endpoint' -or $connection -notmatch 'http://(?:ministack|localhost|127\.0\.0\.1):4566' -or $connection -notmatch 'data-theme="light"') {
    throw 'Effective connection configuration or light theme failed'
}
$gallery = (Invoke-WebRequest 'http://127.0.0.1:8080/components?lang=pt-BR').Content
if ($gallery -notmatch 'Galeria de componentes') { throw 'Component gallery failed' }
Write-Output 'PASS: GUI, localized fragment, process health, connection configuration and gallery'
