# scripts/check_android_compat.ps1
# Enforces Android runtime compatibility rules across production code

$targetDir = "app/src/main"
$hasViolations = $false

Write-Host "Running Android runtime compatibility check in $targetDir..." -ForegroundColor Cyan

# 1. Check for banned regex flags (?U) which crash Android ICU
$bannedRegex = "\(\?[a-zA-Z]*U[a-zA-Z]*\)"
$matches = rg $bannedRegex $targetDir --line-number
if ($matches) {
    Write-Host "`n[ERROR] Found banned regex flag (?U) that crashes Android ICU at runtime:" -ForegroundColor Red
    $matches | ForEach-Object { Write-Host "  $_" -ForegroundColor Yellow }
    Write-Host "Android ICU does not support (?U). Use Unicode character classes like [\p{L}\p{N}] instead.`n" -ForegroundColor Red
    $hasViolations = $true
}

# 2. Check for AndroidViewModel classes with default arguments lacking @JvmOverloads
$vmFiles = Get-ChildItem -Path $targetDir -Filter "*ViewModel.kt" -Recurse
foreach ($file in $vmFiles) {
    $content = Get-Content $file.FullName -Raw
    if ($content -match "class\s+([A-Za-z0-9_]+)\s*\([^)]*=[^)]*\)\s*:\s*AndroidViewModel" -and $content -notmatch "@JvmOverloads") {
        Write-Host "`n[ERROR] ViewModel missing @JvmOverloads in $($file.Name):" -ForegroundColor Red
        Write-Host "  AndroidViewModel classes with default constructor parameters must be annotated with @JvmOverloads constructor(...) for ViewModelProvider reflection.`n" -ForegroundColor Yellow
        $hasViolations = $true
    }
}

if ($hasViolations) {
    exit 1
} else {
    Write-Host "[SUCCESS] Android compatibility check passed: No banned ICU flags or un-annotated ViewModels found.`n" -ForegroundColor Green
    exit 0
}
