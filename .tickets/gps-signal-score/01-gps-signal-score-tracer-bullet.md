# 01: Tracer Bullet — Расчёт GPS Signal Score и отображение в WalkSummaryDialog

**What to build:** Во время активной прогулки приложение непрерывно вычисляет сессионный **GPS Signal Score** (`0–100%`) как взвешенную долю активного времени (исключая паузы и автопаузы) в состояниях `GOOD` (`1.0`), `WEAK` (`0.5`) и `PDR_ACTIVE` (`0.0`). В реальном режиме активные секунды до первого валидного GPS-фикса и при отсутствии обновлений дольше `4.5с` честно засчитываются как `PDR_ACTIVE` (`0.0`); в Demo-режиме учёт ведётся по общим правилам (`Good · 100%`). На экране сохранения прогулки (`WalkSummaryDialog`) в карточке статистики после `Avg Pace` отображается строка `GPS Signal` в формате `Good · 92%` / `Fair · 68%` / `Poor · 34%` / `N/A` с цветовой индикацией OLED Dark Theme (`NeonGreenRunner`, `WarningAmber`, `ErrorRed`, `TextSecondary`). Метрика не сохраняется в Room DB и полностью сбрасывается при старте новой прогулки. Спецификация: `docs/specs/gps-signal-score.md`.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] Реализованы доменная модель `GpsSignalSummary` (`scorePercent`, `tier`, форматирование `Locale.US`) и pure-Kotlin движок `GpsSignalScoreTracker` с весами `GOOD=1.0`, `WEAK=0.5`, `PDR_ACTIVE=0.0` и порогами `Good >= 80%`, `Fair 50..79%`, `Poor < 50%`, `N/A` при `0` активных секунд.
- [ ] Pure JVM тесты `GpsSignalScoreTrackerTest` покрывают пустую сессию (`N/A`), чистые `GOOD`/`WEAK`/`PDR_ACTIVE`, смешанные сценарии и округление, точные границы (`80%`, `79%`, `50%`, `49%`), строгий учёт тиков до первого фикса и при таймауте `> 4500ms` (vs `<= 4500ms`), Demo-режим и `reset()` между прогулками.
- [ ] `WalkTrackingService` обновляет `WalkState.gpsSignalSummary` только на активных тиках (`isTracking && !isPaused && !isAutoPaused`), замораживает оценку на паузе/автопаузе и сбрасывает трекер при старте новой сессии.
- [ ] `WalkSummaryDialog` отображает 5-ю строку `GPS Signal` с корректным текстом (`Good · XX%`, `Fair · XX%`, `Poor · XX%`, `N/A`) и цветом (`NeonGreenRunner`, `WarningAmber`, `ErrorRed`, `TextSecondary`) строго на английском языке; изменения схемы Room DB отсутствуют.
- [ ] `.\gradlew.bat testDebugUnitTest`, `pwsh scripts/check_localization.ps1` и `pwsh scripts/check_android_compat.ps1` зелёные.
