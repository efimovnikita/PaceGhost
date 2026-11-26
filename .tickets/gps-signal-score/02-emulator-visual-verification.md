# 02: Визуальная проверка на Android-эмуляторе

**What to build:** Провести сквозную визуальную проверку отображения **GPS Signal Score** в `WalkSummaryDialog` на реальном Android-эмуляторе: собрать Debug APK, запустить прогулку в Demo-режиме, открыть диалог сохранения и убедиться, что строка `GPS Signal` (`Good · 100%`) корректно выровнена в карточке статистики, окрашена в `NeonGreenRunner`, не вызывает переполнения или обрезания текста и соответствует OLED Dark Theme. Сделать и приложить скриншот в пользовательский артефакт. Спецификация: `docs/specs/gps-signal-score.md`.

**Blocked by:** 01: Tracer Bullet — Расчёт GPS Signal Score и отображение в WalkSummaryDialog

**Status:** done

- [x] Debug APK собирается без ошибок (`.\gradlew.bat assembleDebug`) и устанавливается на Android-эмулятор.
- [x] На эмуляторе запущена прогулка в Demo-режиме (минимум несколько активных секунд) и открыт `WalkSummaryDialog`.
- [x] На скриншоте подтверждена строка `GPS Signal` со значением `Good · 100%` зелёного цвета (`NeonGreenRunner`) после `Avg Pace`, без наложений и переполнения; скриншот встроен в пользовательский артефакт.
- [x] `.\gradlew.bat testDebugUnitTest`, `pwsh scripts/check_localization.ps1` и `pwsh scripts/check_android_compat.ps1` зелёные.
