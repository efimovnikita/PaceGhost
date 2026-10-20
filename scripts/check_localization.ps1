# scripts/check_localization.ps1
# Ensures 100% English UI and resources across production source files

$cyrillicPattern = "[\u0400-\u04FF]"
$targetDir = "app/src/main"

Write-Host "Checking for untranslated Cyrillic text in $targetDir..." -ForegroundColor Cyan

$matches = rg $cyrillicPattern $targetDir --line-number
if ($matches) {
    Write-Host "`n[ERROR] Found untranslated Cyrillic text in production source code:" -ForegroundColor Red
    $matches | ForEach-Object { Write-Host "  $_" -ForegroundColor Yellow }
    Write-Host "`nAll UI labels, TTS phrases, and strings must be English.`n" -ForegroundColor Red
    exit 1
} else {
    Write-Host "[SUCCESS] Localization check passed: 100% English source code in $targetDir.`n" -ForegroundColor Green
    exit 0
}
