# Spec: Walk GPS Signal Score

## Problem Statement
Во время активной прогулки пользователь видит только мгновенный статус GPS-сигнала (`GPS: Good`, `GPS: Weak`, `PDR Mode`), но при завершении прогулки на экране сохранения (`WalkSummaryDialog`) у него нет итогового представления о том, насколько качественным был приём GPS на протяжении всего маршрута. В условиях городской застройки или глушения GPS пользователь не понимает, можно ли доверять записанному треку и стоит ли сохранять прогулку (например, для будущего использования как Ghost-маршрута).

## Solution
Вводится **GPS Signal Score** — сессионная метрика качества GPS-приёма за прогулку в диапазоне `0–100%`, рассчитываемая как взвешенная доля активного времени ходьбы (исключая паузы и автопаузы) в состояниях `GpsQuality` (`GOOD = 1.0`, `WEAK = 0.5`, `PDR_ACTIVE = 0.0`). Строгий посекундный учёт в `WalkTrackingService` гарантирует, что тики до первого валидного GPS-фикса и периоды «тихой» потери сигнала (`> 4.5с` без обновлений GPS) честно засчитываются как `PDR_ACTIVE` (`0.0`), а не завышаются значением по умолчанию. На экране сохранения `WalkSummaryDialog` в карточке статистики добавляется одна компактная строка `GPS Signal` со статусом, процентом и цветовой индикацией (`Good · 92%`, `Fair · 68%`, `Poor · 34%` или `N/A` при нулевой активной длительности). Метрика является сессионной и не сохраняется в базу данных Room.

## User Stories
1. Как пользователь, завершающий прогулку, я хочу видеть общую оценку качества GPS-сигнала прямо в `WalkSummaryDialog` вместе с `Distance`, `Time`, `Steps` и `Avg Pace`, чтобы сразу оценить надёжность записанного трека перед сохранением или удалением.
2. Как пользователь, я хочу видеть оценку в компактном формате «статус + процент» (например, `Good · 92%`, `Fair · 68%`, `Poor · 34%`), чтобы мгновенно считывать результат и при этом понимать точную долю качественного приёма.
3. Как пользователь, я хочу, чтобы цветовая индикация строки `GPS Signal` соответствовала OLED Dark Theme приложения (`NeonGreenRunner` для `Good`, `WarningAmber` для `Fair`, `ErrorRed` для `Poor`, `TextSecondary` для `N/A`), чтобы качество сигнала было понятно с первого взгляда.
4. Как пользователь, попавший в зону глушения GPS или туннель (режим PDR / отброшенные точки с точностью `> 35м`), я хочу, чтобы время без валидного GPS честно снижало итоговый **GPS Signal Score**, а не скрывалось за счёт усреднения только хороших точек.
5. Как пользователь, начавший прогулку в помещении или под глушилкой (до получения первого валидного GPS-фикса), я хочу, чтобы активные секунды без фикса засчитывались как отсутствие сигнала (`0.0`), а не как `Good` по умолчанию.
6. Как пользователь, у которого GPS «завис» без новых обновлений более чем на `4.5` секунды даже без срабатывания шагомера, я хочу, чтобы эти активные секунды также засчитывались как `PDR_ACTIVE` (`0.0`).
7. Как пользователь, поставивший прогулку на ручную паузу или остановившийся на светофоре (автопауза), я хочу, чтобы время на паузе не влияло на **GPS Signal Score**, чтобы остановки под навесом или в помещении не портили статистику ходьбы.
8. Как пользователь, нажавший `Finish` сразу после старта (`0` активных секунд), я хочу видеть аккуратное `N/A` вместо деления на ноль или фиктивных `100%`.
9. Как пользователь, проверяющий приложение в Demo-режиме (`isDemoMode = true`), я хочу, чтобы **GPS Signal Score** считался и отображался по общим правилам (`Good · 100%`), чтобы интерфейс `WalkSummaryDialog` работал единообразно.
10. Как пользователь, начинающий новую прогулку после сохранения или удаления предыдущей, я хочу, чтобы накопленные счётчики качества GPS полностью сбрасывались, чтобы прошлая сессия не влияла на новую.
11. Как пользователь, я хочу, чтобы все подписи и статусы (`GPS Signal`, `Good`, `Fair`, `Poor`, `N/A`) были строго на английском языке в соответствии со стандартами PaceGhost.

## Implementation Decisions

### 1. Доменная модель (`domain/model/GpsSignalSummary.kt`)
- Вводится неизменяемая модель результата `GpsSignalSummary`:
  - `scorePercent: Int?` — округлённый процент `0..100`, либо `null`, если `totalActiveSeconds == 0`.
  - `tier: GpsSignalTier` — перечисление:
    - `GOOD` (`scorePercent >= 80`)
    - `FAIR` (`scorePercent in 50..79`)
    - `POOR` (`scorePercent < 50`)
    - `UNAVAILABLE` (`scorePercent == null`)
  - `totalActiveSeconds: Int = 0`
- Форматирование отображаемой строки (строго `Locale.US`, строго English):
  - `GOOD` → `"Good · ${scorePercent}%"`
  - `FAIR` → `"Fair · ${scorePercent}%"`
  - `POOR` → `"Poor · ${scorePercent}%"`
  - `UNAVAILABLE` → `"N/A"`

### 2. Движок расчёта (`domain/engine/GpsSignalScoreTracker.kt`)
- Выделенный pure-Kotlin класс без Android-зависимостей (по аналогии с `AutoPauseDetector` и `FreeWalkCoachEngine`).
- Хранит счётчики активных секунд: `goodSeconds`, `weakSeconds`, `pdrSeconds`.
- Константы:
  - `STALE_GPS_THRESHOLD_MS = 4500L` (единое значение с `WalkTrackingService.handleStepCountUpdate`).
  - Веса: `WEIGHT_GOOD = 1.0`, `WEIGHT_WEAK = 0.5`, `WEIGHT_PDR = 0.0`.
  - Пороги категорий: `GOOD_THRESHOLD = 80`, `FAIR_THRESHOLD = 50`.
- Публичный контракт:
  - `recordActiveTick(currentQuality: GpsQuality, hasValidFix: Boolean, timeSinceLastFixMs: Long, isDemoMode: Boolean): GpsSignalSummary`
    - Определение эффективного состояния на тике:
      - если `isDemoMode == true` → используется `currentQuality` (в симуляторе всегда `GOOD`);
      - иначе, если `!hasValidFix || timeSinceLastFixMs > STALE_GPS_THRESHOLD_MS` → принудительно `GpsQuality.PDR_ACTIVE`;
      - иначе → `currentQuality`.
    - Инкрементирует соответствующий счётчик и возвращает актуальный `GpsSignalSummary`.
  - `currentSummary(): GpsSignalSummary`
    - При `total == 0` возвращает `GpsSignalSummary(scorePercent = null, tier = GpsSignalTier.UNAVAILABLE, totalActiveSeconds = 0)`.
    - Иначе `score = ((goodSeconds * 1.0 + weakSeconds * 0.5) / total * 100.0).roundToInt().coerceIn(0, 100)`.
  - `reset()` — обнуляет все счётчики.

### 3. Интеграция с `WalkState` и `WalkTrackingService`
- В [`WalkState.kt`](file:///C:/Users/--/source/repos/PaceGhost/app/src/main/java/com/paceghost/app/domain/model/WalkState.kt) добавляется поле:
  - `val gpsSignalSummary: GpsSignalSummary = GpsSignalSummary()`
- В [`WalkTrackingService.kt`](file:///C:/Users/--/source/repos/PaceGhost/app/src/main/java/com/paceghost/app/service/WalkTrackingService.kt):
  - Добавляется экземпляр `private val gpsSignalScoreTracker = GpsSignalScoreTracker()`.
  - В `resetTrackingData()` вызывается `gpsSignalScoreTracker.reset()`.
  - В `startTicker()` внутри ветки активной ходьбы (`if (state.isTracking && !state.isPaused && !state.isAutoPaused)`):
    - Вычисляются `hasValidFix = lastGpsUpdateTimeMs > 0L` и `timeSinceLastFixMs = if (hasValidFix) (System.currentTimeMillis() - lastGpsUpdateTimeMs).coerceAtLeast(0L) else Long.MAX_VALUE`.
    - Вызывается `val signalSummary = gpsSignalScoreTracker.recordActiveTick(state.gpsQuality, hasValidFix, timeSinceLastFixMs, state.isDemoMode)`.
    - `signalSummary` записывается в `_walkState.value = state.copy(..., gpsSignalSummary = signalSummary)`.
  - В ветке паузы / автопаузы тики в трекер не передаются (оценка замораживается).

### 4. UI-слой (`ActiveWalkScreen.kt` и `WalkSummaryDialog.kt`)
- В [`ActiveWalkScreen.kt`](file:///C:/Users/--/source/repos/PaceGhost/app/src/main/java/com/paceghost/app/ui/active/ActiveWalkScreen.kt) в вызов `WalkSummaryDialog(...)` передаётся `gpsSignalSummary = walkState.gpsSignalSummary`.
- В [`WalkSummaryDialog.kt`](file:///C:/Users/--/source/repos/PaceGhost/app/src/main/java/com/paceghost/app/ui/summary/WalkSummaryDialog.kt):
  - Добавляется параметр `gpsSignalSummary: GpsSignalSummary = GpsSignalSummary()` (со значением по умолчанию для обратной совместимости превью/тестов).
  - Внутри карточки статистики (`Card` → `Column`) после `Avg Pace` добавляется 5-я строка `SummaryRow`:
    - `label = "GPS Signal"`
    - `value = gpsSignalSummary.formatDisplay()`
    - `valueColor` по `gpsSignalSummary.tier`:
      - `GOOD` → `NeonGreenRunner`
      - `FAIR` → `WarningAmber`
      - `POOR` → `ErrorRed`
      - `UNAVAILABLE` → `TextSecondary`
- База данных (`WalkEntity`, `TrackPointEntity`, `AppDatabase`, `WalkDao`) не изменяется.
- Термин **GPS Signal Score** уже зафиксирован в [`GLOSSARY.md`](file:///C:/Users/--/source/repos/PaceGhost/GLOSSARY.md#L75-L77).

## Testing Decisions
- **Определение хорошего теста**: тест проверяет внешне наблюдаемое поведение (итоговый процент, категорию `GpsSignalTier`, отформатированную строку, реакцию на отсутствие/устаревание фикса и сброс сессии), не завязываясь на внутренние поля счётчиков.
- **Основной согласованный шов тестирования (Pure JVM)**: `GpsSignalScoreTracker` + `GpsSignalSummary` (`app/src/test/java/com/paceghost/app/domain/engine/GpsSignalScoreTrackerTest.kt`, по образцу `AutoPauseDetectorTest` и `FreeWalkCoachEngineTest`):
  - пустая сессия (`0` тиков) → `scorePercent = null`, `tier = UNAVAILABLE`, `"N/A"`;
  - 100% `GOOD` с валидным свежим фиксом → `100`, `GOOD`, `"Good · 100%"`;
  - 100% `WEAK` → `50`, `FAIR`, `"Fair · 50%"`;
  - 100% `PDR_ACTIVE` → `0`, `POOR`, `"Poor · 0%"`;
  - смешанные сценарии и округление (например, 8 `GOOD` + 2 `WEAK` → `90%` `GOOD`; 1 `GOOD` + 1 `WEAK` + 1 `PDR` → `50%` `FAIR`);
  - строгие границы категорий: `80%` (`GOOD`), `79%` (`FAIR`), `50%` (`FAIR`), `49%` (`POOR`);
  - реальный режим до первого фикса (`hasValidFix = false`, даже если `currentQuality = GOOD`) → засчитывается как `PDR_ACTIVE` (`0%`);
  - реальный режим при устаревшем фиксе (`timeSinceLastFixMs = 4501L` при `currentQuality = GOOD`) → засчитывается как `PDR_ACTIVE`, а при `4500L` — ещё как `GOOD`;
  - Demo-режим (`isDemoMode = true`, `hasValidFix = false`) → засчитывает `currentQuality` (`GOOD` → `100%`);
  - `reset()` полностью очищает состояние между прогулками.
- **Визуальная проверка на Android-эмуляторе**:
  - запуск прогулки в Demo-режиме → нажатие `Finish` → скриншот `WalkSummaryDialog` с проверкой строки `GPS Signal: Good · 100%`, цвета `NeonGreenRunner`, выравнивания и отсутствия переполнения.
- **Автоматические гейты репозитория**:
  - `.\gradlew.bat testDebugUnitTest`
  - `pwsh scripts/check_localization.ps1`
  - `pwsh scripts/check_android_compat.ps1`

## Out of Scope
- Сохранение **GPS Signal Score** в базу данных Room (`WalkEntity` / миграции схемы).
- Отображение качества GPS в списке истории прогулок (`WalkCard`), на экране превью (`TrackPreviewScreen`) или в экспорте GPX.
- Пересчёт качества GPS для ранее сохранённых или импортированных из GPX прогулок.
- Подробная разбивка по компонентам в UI (медианная точность в метрах, процент PDR отдельной строкой, количество отброшенных спайков).
- Предупреждения или автоматическая блокировка сохранения/выбора Ghost-маршрута при низком качестве GPS.
- Изменение логики переключения живого бейджа `GpsQuality` на `ActiveWalkScreen` или порогов фильтра Калмана / PDR (`15м`, `30м`, `35м`).

## Further Notes
- Если в будущем понадобится отображать **GPS Signal Score** в истории прогулок (`WalkCard`) или предупреждать при выборе слабого трека как Ghost-маршрута, `WalkEntity` можно будет расширить nullable-колонкой `gpsSignalScore: Int?` с Room-миграцией, сохранив `GpsSignalScoreTracker` и `GpsSignalSummary` без изменений.
- Все новые пользовательские строки должны быть строго на английском языке — это автоматически проверяется скриптом `scripts/check_localization.ps1`.
