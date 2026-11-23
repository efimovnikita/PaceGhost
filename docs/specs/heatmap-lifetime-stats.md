# Spec: Heatmap Lifetime Stats (Stats Peek Sheet)

## Problem Statement
На экране **Personal Heatmap** пользователь видит визуальную карту всех своих исторических маршрутов, но не получает никакой количественной сводки по накопленной активности (общего километража, количества прогулок, суммарного времени и шагов). При этом карта должна оставаться максимально чистой и свободной от постоянного визуального шума — статистика нужна по запросу, без перехода на отдельный экран.

## Solution
На экран `Personal Heatmap` добавляется **Stats Peek Sheet** — компактная немодальная нижняя шторка в стилистике OLED Dark Theme, которая по умолчанию свёрнута в тонкий полупрозрачный язычок по центру нижнего края экрана (drag handle + подпись `Stats`). Пользователь раскрывает панель свайпом вверх за язычок или тапом по нему. Внутри отображается **Lifetime Stats** — сводка за всё время по всем **Qualifying Walks** (`durationSeconds >= 60` и `distanceMeters >= 50`), включающая главный показатель `Total Distance` и компактную сетку из `Total Walks`, `Total Time`, `Total Steps`, `Avg Speed` (взвешенная средняя) и `Longest Walk`. Карта выше панели остаётся видимой и без затемнения, кнопки зума/центрирования плавно скрываются при раскрытии, а кнопка «Назад» сначала сворачивает шторку.

## User Stories
1. Как пользователь, я хочу, чтобы при открытии `Personal Heatmap` статистика была скрыта по умолчанию, чтобы карта маршрутов оставалась главной и не была перегружена.
2. Как пользователь, я хочу видеть компактный язычок (`Stats`) внизу по центру экрана, чтобы сразу понимать, откуда вытягивается панель статистики, и не конфликтовать с жестами карты или системной навигацией Android.
3. Как пользователь, я хочу раскрывать панель свайпом вверх за язычок или простым тапом по нему, чтобы удобно открывать сводку одной рукой.
4. Как пользователь, я хочу закрывать панель свайпом вниз, повторным тапом по ручке или системной кнопкой «Назад», чтобы быстро возвращаться к карте.
5. Как пользователь, я хочу, чтобы нажатие системной кнопки «Назад» при открытой панели сначала сворачивало панель, а только следующее нажатие закрывало экран `Personal Heatmap`.
6. Как пользователь, я хочу, чтобы в раскрытом состоянии шторка занимала компактную нижнюю часть экрана (~35–40%) без затемнения карты сверху, чтобы одновременно видеть свою тепловую карту и цифры.
7. Как пользователь, я хочу, чтобы плавающие кнопки карты (`+`, `-`, `Recenter`) не перекрывались с язычком в свёрнутом состоянии и плавно исчезали (`fade out`), пока шторка раскрыта.
8. Как пользователь, я хочу видеть общий пройденный километраж (`Total Distance`) главным крупным показателем в формате `142.8 km`, чтобы мгновенно считывать свой суммарный прогресс.
9. Как пользователь, я хочу видеть в компактной сетке `Total Walks`, `Total Time` (`42h 15m` / `35m` / `0m`), `Total Steps` (`184,320`), `Avg Speed` (`5.2 km/h`) и `Longest Walk` (`12.4 km`), чтобы оценивать общую картину своей активности без прокрутки панели.
10. Как пользователь, я хочу, чтобы случайные короткие старты и микро-прогулки (`< 60 сек` или `< 50 м`) автоматически исключались из подсчёта **Lifetime Stats**, чтобы статистика оставалась честной и не искажала среднюю скорость и счётчик прогулок.
11. Как пользователь, я хочу, чтобы `Avg Speed` рассчитывалась как взвешенная средняя (`totalDistance / totalDuration`), а не среднее арифметическое скоростей отдельных прогулок, чтобы короткие прогулки не перекашивали итог.
12. Как новый пользователь (или если в базе нет ни одной `Qualifying Walk`), я хочу по-прежнему видеть язычок `Stats`, а при его раскрытии — нули/`—` и понятную подсказку `"Record your first walk to see lifetime stats"`, без ошибок и падений.
13. Как пользователь, я хочу, чтобы все подписи и форматы чисел в панели были строго на английском языке и соответствовали OLED Dark Theme (`#121212` / `#1E1E1E`, акценты `CyanGhost` / `NeonGreenRunner`).

## Implementation Decisions

### 1. Модель данных и SQL-агрегация (без миграции Room)
- **Domain model** `domain/model/LifetimeStats.kt`:
  - `walkCount: Int = 0`
  - `totalDistanceMeters: Double = 0.0`
  - `totalDurationSeconds: Long = 0L`
  - `totalSteps: Long = 0L`
  - `longestWalkMeters: Double = 0.0`
  - Производные свойства:
    - `hasQualifyingWalks: Boolean get() = walkCount > 0`
    - `weightedAverageSpeedKmh: Double?` — `(totalDistanceMeters / 1000.0) / (totalDurationSeconds / 3600.0)` при `walkCount > 0 && totalDurationSeconds > 0L && totalDistanceMeters > 0.0`, иначе `null`.
- **Порог `Qualifying Walk`**:
  - Единые константы (`MIN_DURATION_SECONDS = 60L`, `MIN_DISTANCE_METERS = 50.0`), согласованные с `WalkEntity.calculateMedianSpeedKmh`.
- **Запрос в `WalkDao.getLifetimeStats(minDurationSeconds, minDistanceMeters)`**:
  - Один SQL-агрегат без выгрузки всех `WalkEntity` в память:
    ```sql
    SELECT COUNT(*) AS walkCount,
           COALESCE(SUM(distanceMeters), 0.0) AS totalDistanceMeters,
           COALESCE(SUM(durationSeconds), 0) AS totalDurationSeconds,
           COALESCE(SUM(stepCount), 0) AS totalSteps,
           COALESCE(MAX(distanceMeters), 0.0) AS longestWalkMeters
    FROM walks
    WHERE durationSeconds >= :minDurationSeconds
      AND distanceMeters >= :minDistanceMeters
    ```
  - Прокидывается через `WalkRepository.getLifetimeStats()` (с default-реализацией `LifetimeStats()` в интерфейсе, чтобы не ломать существующие фейки в тестах) и `WalkRepositoryImpl`.

### 2. Форматирование (`domain/model/LifetimeStatsFormatter.kt` или методы форматирования)
Все строки формируются с `Locale.US`:
- **Total Distance**: `String.format(Locale.US, "%.1f km", totalDistanceMeters / 1000.0)` → `"142.8 km"`, при `0` → `"0.0 km"`.
- **Total Walks**: `"47"` (`walkCount.toString()`).
- **Total Time**:
  - `hours = totalDurationSeconds / 3600`, `minutes = (totalDurationSeconds % 3600) / 60`;
  - если `hours > 0`: `"${hours}h ${minutes}m"` (например, `"42h 15m"`);
  - если `hours == 0`: `"${minutes}m"` (например, `"35m"`, при `0` → `"0m"`).
- **Total Steps**: разделитель тысяч через `NumberFormat.getIntegerInstance(Locale.US)` → `"184,320"`, при `0` → `"0"`.
- **Avg Speed**: при наличии `weightedAverageSpeedKmh` → `String.format(Locale.US, "%.1f km/h", speed)` (например, `"5.2 km/h"`), иначе `"—"`.
- **Longest Walk**: при `walkCount > 0 && longestWalkMeters > 0.0` → `String.format(Locale.US, "%.1f km", longestWalkMeters / 1000.0)`, иначе `"—"`.

### 3. ViewModel (`HeatmapViewModel.kt`)
- `HeatmapUiState` дополняется полем `lifetimeStats: LifetimeStats = LifetimeStats()`.
- В `init` запускается загрузка `loadLifetimeStats()` параллельно с `loadRecentBounds()` и `loadWalkAnchors()` (однократная загрузка при входе на экран, как и у `walkAnchors`; при ошибке — безопасный fallback к `LifetimeStats()`).
- Сигнатура `@JvmOverloads constructor(...)` у `HeatmapViewModel` сохраняется без нарушений Android `ViewModelProvider`.

### 4. UI-компонент (`ui/heatmap/components/StatsPeekSheet.kt` и `HeatmapScreen.kt`)
- **Состояние**: `isStatsExpanded: Boolean` (`rememberSaveable { mutableStateOf(false) }`) на уровне `HeatmapScreen`.
- **Навигация «Назад»**: `BackHandler` в `HeatmapScreen`: если `isStatsExpanded`, сбрасывает `isStatsExpanded = false`; иначе вызывает `onNavigateBack()`.
- **Жесты и анимация**:
  - Свёрнутое состояние: компактная полупрозрачная капсула/язычок (`DarkSurfaceTranslucent`, скруглённые верхние углы, тонкий drag handle `CyanGhost`/нейтральный и подпись `"Stats"` + иконка стрелки вверх) по центру внизу (`Alignment.BottomCenter`, с учётом `navigationBarsPadding()`). Не пересекается с колонкой FAB справа (`Alignment.BottomEnd`).
  - Взаимодействие: вертикальный свайп (`detectVerticalDragGestures` на области ручки/панели, чтобы не перехватывать жесты остальной карты OsmDroid) + `clickable` на ручке/язычке. Порог свайпа для переключения состояния — например, 40–60dp или достаточная вертикальная скорость.
  - Раскрытое состояние: плавная анимация (`AnimatedContent` / `animateDpAsState` / spring-анимация высоты и смещения) в компактную карточку на всю ширину с небольшими горизонтальными отступами или закреплённую к низу, без scrim-затемнения над картой.
- **Кнопки карты (`+`, `-`, `Recenter`)**:
  - Обёрнуты в `AnimatedVisibility(visible = !isStatsExpanded, enter = fadeIn(), exit = fadeOut())`, чтобы плавно исчезать при открытой шторке.
- **Компоновка контента внутри раскрытой панели (строго English UI)**:
  - Drag handle сверху (с `contentDescription = "Collapse Stats"` / `"Expand Stats"`).
  - Hero-блок: заголовок `"TOTAL DISTANCE"` (`TextSecondary`) и крупное значение (например `142.8 km`, акцент `NeonGreenRunner` или `CyanGhost`).
  - Компактная сетка (2 колонки × 3 ячейки или 3+2 в зависимости от баланса, без скролла): `Total Walks`, `Total Time`, `Total Steps`, `Avg Speed`, `Longest Walk`.
  - При `!lifetimeStats.hasQualifyingWalks`: нули / `"—"` и подпись `"Record your first walk to see lifetime stats"` (`TextSecondary`).
- **Документация**: термины `Qualifying Walk`, `Lifetime Stats`, `Stats Peek Sheet` уже зафиксированы в [`GLOSSARY.md`](file:///C:/Users/--/source/repos/PaceGhost/GLOSSARY.md#L63-L73).

## Testing Decisions
- **Определение хорошего теста**: тест проверяет внешне наблюдаемое поведение (агрегированные значения в `uiState.lifetimeStats`, исключение микро-прогулок, корректность взвешенной средней скорости и форматирования), а не детали внутренней реализации SQL-генерации или Compose-анимаций.
- **Основной интеграционный шов**: `WalkDao` (in-memory Room под Robolectric, по образцу `WalkDaoAnchorTest`) → `WalkRepositoryImpl` → `HeatmapViewModel`:
  - пустая БД → `walkCount = 0`, `hasQualifyingWalks = false`, `weightedAverageSpeedKmh = null`;
  - набор из валидных `Qualifying Walks` → точные `walkCount`, `totalDistanceMeters`, `totalDurationSeconds`, `totalSteps`, `longestWalkMeters`, `weightedAverageSpeedKmh`;
  - смешанный набор с микро-прогулками (`duration = 59s`, `distance = 49.9m`, граничные `duration = 60s && distance = 50.0m`) → микро-прогулки исключены, граничная прогулка включена;
  - проверка взвешенной средней (1 короткая быстрая прогулка + 1 длинная медленная прогулка → итог близок к длинной, а не к простому среднему);
  - сохранение `@JvmOverloads`-конструктора `HeatmapViewModel` (проверяется в `HeatmapViewModelTest` и `checkAndroidCompatibility`).
- **Модульные тесты форматирования (pure JVM)**:
  - `Total Distance`: `0.0` → `"0.0 km"`, `142840.0` → `"142.8 km"`;
  - `Total Time`: `0L` → `"0m"`, `2100L` → `"35m"`, `152100L` → `"42h 15m"`;
  - `Total Steps`: `0L` → `"0"`, `184320L` → `"184,320"`;
  - `Avg Speed`: `null` → `"—"`, `5.24` → `"5.2 km/h"`;
  - `Longest Walk`: при `0` прогулок → `"—"`, при `12400.0` → `"12.4 km"`.
- **Визуальная и интерактивная проверка на Android-эмуляторе**:
  - скриншот свёрнутого состояния (Peek Handle по центру, FAB справа без наложения);
  - скриншот раскрытого состояния с демо-прогулками (проверка сетки, контраста OLED Dark Theme, скрытия FAB);
  - проверка жеста свайпа вверх/вниз, тапа по язычку и кнопки Back;
  - проверка Empty State (0 qualifying walks).
- **Автоматические гейты репозитория**:
  - `.\gradlew.bat testDebugUnitTest`
  - `pwsh scripts/check_localization.ps1`
  - `pwsh scripts/check_android_compat.ps1`

## Out of Scope
- Фильтрация статистики по временным периодам (`This Week` / `This Month` / `This Year`).
- Динамический пересчёт статистики по видимой области карты (viewport-scoped stats).
- Расширенные метрики: стрики (`Current Streak`), активные дни (`Active Days`), лучший темп/скорость, набор высоты, калории.
- Миграция схемы БД Room или кеширование агрегатов в отдельной таблице.
- Реактивное обновление статистики в реальном времени при изменении БД, пока экран `Personal Heatmap` уже открыт.
- Отображение панели статистики на других экранах (`HomeScreen`, `ActiveWalkScreen`, `TrackPreviewScreen`).
- Поясняющие сноски в UI о пороге фильтрации `Qualifying Walk`.

## Further Notes
- При росте истории до десятков тысяч прогулок SQL-агрегат по таблице `walks` всё равно останется мгновенным (тысячи строк, не `track_points`), поэтому индексы или денормализация не требуются.
- В будущем в `StatsPeekSheet` можно без слома архитектуры добавить чипы переключения периодов (`All Time` / `Month` / `Year`), передавая `minStartTime` в `WalkDao.getLifetimeStats(...)`.
- Все новые строки интерфейса должны быть строго на английском языке — соблюдение проверяется задачей `checkLocalization` в `app/build.gradle.kts`.
