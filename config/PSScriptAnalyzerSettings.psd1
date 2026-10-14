@{
    Severity = @('Error', 'Warning')
    IncludeDefaultRules = $true
    Rules = @{
        PSUseConsistentIndentation = @{
            Enable = $true
            Kind = 'space'
            IndentationSize = 4
        }
        PSUseConsistentWhitespace = @{
            Enable = $true
            CheckOpenBrace = $true
            CheckInnerBrace = $true
            CheckOperator = $true
            CheckSeparator = $true
        }
        PSUseCorrectCasing = @{ Enable = $true }
    }
}
