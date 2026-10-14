param([switch]$Fix)

$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path -Parent $PSScriptRoot
Import-Module "$projectDirectory/work/modules/PSScriptAnalyzer/1.24.0/PSScriptAnalyzer.psd1" -ErrorAction Stop
$settings = "$projectDirectory/config/PSScriptAnalyzerSettings.psd1"
if ($Fix) {
    Get-ChildItem "$projectDirectory/scripts/*.ps1" | ForEach-Object {
        $formatted = Invoke-Formatter -ScriptDefinition (Get-Content -Raw -LiteralPath $_.FullName) -Settings $settings
        [System.IO.File]::WriteAllText($_.FullName, $formatted.Replace("`r`n", "`n"), [System.Text.UTF8Encoding]::new($false))
    }
}
$findings = @(Invoke-ScriptAnalyzer -Path "$projectDirectory/scripts" -Recurse -Settings $settings)
if ($findings.Count -gt 0) {
    $findings | Format-Table -AutoSize
    throw 'PowerShell style validation failed'
}
Write-Output 'PASS: PowerShell analysis'
