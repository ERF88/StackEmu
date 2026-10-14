$ErrorActionPreference = 'Stop'
$page = $null
for ($attempt = 0; $attempt -lt 30; $attempt++) {
    try {
        $page = Invoke-WebRequest 'http://127.0.0.1:8080/' -TimeoutSec 5
        if ($page.Content -match 'Connected') { break }
    } catch { }
    Start-Sleep -Seconds 1
}
if ($page.StatusCode -ne 200 -or $page.Content -notmatch 'Connected') {
    throw 'GUI is not connected to the emulator'
}
$fragment = Invoke-WebRequest 'http://127.0.0.1:8080/buckets?lang=pt-BR' -Headers @{'HX-Request'='true'}
if ($fragment.Content -notmatch 'Conectado' -or $fragment.Content -match '<!doctype') {
    throw 'Localized partial response failed'
}
Invoke-RestMethod 'http://127.0.0.1:8080/q/health/live' | Out-Null
Write-Output 'PASS: GUI, localized fragment and process health'
