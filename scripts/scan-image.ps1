#Requires -Version 7.0
param([string]$Image = 'stackemu:local', [switch]$Enforce, [switch]$RequireClean)
$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path -Parent $PSScriptRoot
$releaseDirectory = Join-Path $projectDirectory 'work/release'
$cacheDirectory = Join-Path $projectDirectory 'work/trivy-cache'
New-Item -ItemType Directory -Path $releaseDirectory, $cacheDirectory -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $cacheDirectory 'tmp') -Force | Out-Null
$scannerIdentity = '10001:10001'
if (-not $IsWindows) {
    $scannerUserId = & id -u
    $scannerGroupId = & id -g
    if ($LASTEXITCODE -ne 0) { throw 'Scanner host identity could not be determined' }
    $scannerIdentity = "${scannerUserId}:${scannerGroupId}"
}
$archive = Join-Path $releaseDirectory 'image.tar'
docker image save --output $archive $Image
if ($LASTEXITCODE -ne 0) { throw 'Image export failed' }
# No Docker socket, credentials, home directory or source tree is mounted into the scanner.
docker run --rm --user $scannerIdentity --read-only --cap-drop ALL --security-opt no-new-privileges --tmpfs '/tmp:rw,nosuid,size=64m' `
    -e TMPDIR=/cache/tmp `
    -v "${releaseDirectory}:/scan" -v "${cacheDirectory}:/cache" aquasec/trivy:0.76.0@sha256:2416d1f74045b57274df1a340eee6b510cacd73c66a6ccb8da092b117eb7d222 image `
    --cache-dir /cache --input /scan/image.tar --scanners vuln --list-all-pkgs --format json --output /scan/vulnerabilities.json --timeout 15m --no-progress --exit-code 0
if ($LASTEXITCODE -ne 0) { throw 'Scan failed: no vulnerability result can be claimed' }
$report = Get-Content (Join-Path $releaseDirectory 'vulnerabilities.json') -Raw | ConvertFrom-Json
$osPackages = @($report.Results | Where-Object Class -EQ 'os-pkgs' | ForEach-Object { $_.Packages } | Where-Object { $null -ne $_ })
$javaPackages = @($report.Results | Where-Object Type -EQ 'jar' | ForEach-Object { $_.Packages } | Where-Object { $null -ne $_ })
if ($RequireClean -and ($osPackages.Count -eq 0 -or $javaPackages.Count -eq 0)) {
    throw 'Clean gate requires detected OS and Java package inventories, not an empty/unrecognized scan'
}
$findings = @($report.Results | ForEach-Object { $_.Vulnerabilities } | Where-Object { $null -ne $_ })
$blocking = @($findings | Where-Object Severity -In @('HIGH', 'CRITICAL'))
$gateFindingCount = $(if ($RequireClean) { $findings.Count } else { $blocking.Count })
$summary = [ordered]@{
    image = $Image
    imageId = (docker image inspect --format '{{.Id}}' $Image)
    scannedAtUtc = [DateTime]::UtcNow.ToString('o')
    scanner = 'Trivy 0.76.0'
    osPackages = $osPackages.Count
    javaPackages = $javaPackages.Count
    findings = $findings.Count
    highCritical = $blocking.Count
    gatePolicy = $(if ($RequireClean) { 'all findings' } else { 'HIGH/CRITICAL' })
    vulnerabilityGate = $(if ($gateFindingCount -eq 0) { 'pass' } else { 'blocked' })
    publicRelease = 'blocked: legal name, human accessibility and clean-user review pending'
}
if ($LASTEXITCODE -ne 0) { throw 'Image identity could not be recorded' }
$summary | ConvertTo-Json | Set-Content (Join-Path $releaseDirectory 'scan-summary.json') -Encoding utf8NoBOM
Write-Output "Scan complete: $($findings.Count) findings; $($blocking.Count) HIGH/CRITICAL. No findings are suppressed."
if ($RequireClean -and $findings.Count -gt 0) { throw 'Verification blocked by reported vulnerabilities of any severity' }
if ($Enforce -and $blocking.Count -gt 0) { throw 'Release blocked by HIGH/CRITICAL vulnerabilities' }
