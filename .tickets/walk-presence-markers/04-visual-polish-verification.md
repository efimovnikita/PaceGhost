# 04: Визуальная полировка и проверка

**What to build:** Маркеры выглядят как неоновые точки в стиле Two-Pass Glow (полупрозрачная аура CyanGhost + яркое ядро, читаемое число), вписываясь в OLED-тему. Фича подтверждена скриншотами и полным набором проверок.

**Blocked by:** 03

**Status:** ready-for-agent

- [ ] Маркеры отрисованы с аурой и ядром CyanGhost; число читаемо на всех размерах.
- [ ] Скриншоты эмулятора на zoom 3, 9, 10 и после drill-down собраны в user-facing артефакт.
- [ ] `pwsh scripts/check_localization.ps1` и `pwsh scripts/check_android_compat.ps1` проходят.
- [ ] `.\gradlew.bat testDebugUnitTest` и `.\gradlew.bat assembleDebug` зелёные.
