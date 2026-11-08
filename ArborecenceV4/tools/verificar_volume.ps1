param([string]$Behavior, [string]$Academic, [string]$Base)
$argsList = @()
if ($Behavior) { $argsList += @('--behavior', $Behavior) }
if ($Academic) { $argsList += @('--academic', $Academic) }
if ($Base) { $argsList += @('--base', $Base) }
python "$PSScriptRoot/verificar_volume.py" @argsList
exit $LASTEXITCODE
