$ErrorActionPreference = 'Stop'
$page = Invoke-WebRequest 'http://127.0.0.1:8080/'
if ($page.StatusCode -ne 200 -or $page.Content -notmatch 'Connected') {
    throw 'GUI is not connected to the emulator'
}
$fragment = Invoke-WebRequest 'http://127.0.0.1:8080/buckets?lang=pt-BR' -Headers @{'HX-Request'='true'}
if ($fragment.Content -notmatch 'Conectado' -or $fragment.Content -match '<!doctype') {
    throw 'Localized partial response failed'
}
Invoke-RestMethod 'http://127.0.0.1:8080/q/health/live' | Out-Null
Write-Output 'PASS: GUI, localized fragment and process health'
