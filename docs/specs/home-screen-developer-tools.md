# Spec: Home Screen Cleanup & Hidden Developer Tools

## Problem Statement
На главном экране приложения (`HomeScreen`) в `TopAppBar` сейчас размещены два служебных инструмента разработчика рядом с основными пользовательскими действиями: чип переключения **Demo Mode** (`Icons.Filled.Science`) и кнопка **Export Logs** (`Icons.Filled.BugReport`). Это перегружает верхнюю панель (5 элементов справа от логотипа `PaceGhost`), отвлекает обычных пользователей непонятными техническими контролами и позволяет случайно активировать демо-режим в `release`-сборках, загрязняя базу данных синтетическими прогулками (`Victory Park Demo Track`).

## Solution
Убрать оба служебных элемента (`Demo` и `Export Logs`) с `TopAppBar` главного экрана и перенести их в скрытый диалог **Developer Tools**, открываемый долгим нажатием на логотип **PaceGhost**:
1. **Чистый `TopAppBar` на `HomeScreen`**: справа остаются только 3 пользовательские иконки — **Personal Heatmap**, **Import GPX** и **Pace Alert Settings**. В обычном состоянии на главном экране нет никаких упоминаний демо-режима и отладки.
2. **Скрытый диалог `DeveloperToolsDialog`**: открывается долгим нажатием (`onLongClick`) на логотип `PaceGhost` в `TopAppBar`.
   - В **`debug`-сборках** (`BuildConfig.DEBUG == true`): отображает переключатель **Demo Mode**, кнопку **Export Debug Logs** и строку версии приложения (`v<VERSION_NAME> (<VERSION_CODE>)`).
   - В **`release`-сборках** (`BuildConfig.DEBUG == false`): отображает **только** кнопку **Export Debug Logs** и строку версии приложения. Переключатель **Demo Mode** полностью скрыт, а `HomeViewModel.setDemoMode(true)` программно заблокирован проверкой `isDebugBuild`.
3. **Индикация активного `Demo Mode` (только `debug`)**: когда `Demo Mode` включён, на главном экране появляется компактный баннер с кнопкой быстрого выключения **Exit Demo** в один тап, а кнопки старта прогулки сохраняют подписи `"New Walk (Demo)"` и `"Start Demo"`.
4. **Сохранность пользовательских данных**: ранее сохранённые демо-треки в базе данных Room не удаляются автоматически — пользователь при желании удаляет их штатным механизмом удаления.

## User Stories
1. Как обычный пользователь, я хочу видеть в `TopAppBar` главного экрана только основные действия (`Personal Heatmap`, `Import GPX`, `Pace Alert Settings`), чтобы интерфейс оставался минималистичным, а логотип `PaceGhost` не переносился и не зажимался.
2. Как пользователь `release`-сборки, я хочу, чтобы на главном экране не было кнопки `Demo` и невозможно было случайно включить демо-режим и засорить историю прогулок тестовыми маршрутами.
3. Как пользователь `release`-сборки, столкнувшийся с ошибкой, я хочу открыть скрытый диалог `Developer Tools` долгим нажатием на логотип `PaceGhost` и нажать `Export Debug Logs`, чтобы поделиться файлом `paceghost.log` для диагностики.
4. Как пользователь `release`-сборки, открывший `Developer Tools`, я хочу видеть там только `Export Debug Logs` и версию приложения (`vX.Y.Z (code)`), без упоминания `Demo Mode`.
5. Как разработчик, тестирующий приложение на Android-эмуляторе в `debug`-сборке, я хочу открыть `Developer Tools` долгим нажатием на логотип `PaceGhost` и включить переключатель `Demo Mode`, чтобы протестировать ведение по призраку и симуляцию скорости без реального GPS.
6. Как разработчик в `debug`-сборке, я хочу, чтобы включение `Demo Mode` в `Developer Tools` по-прежнему заполняло демонстрационные треки в пустой БД (`AppDatabase.populateDemoWalk`) и сбрасывало кэш тепловой карты.
7. Как разработчик в `debug`-сборке с активным `Demo Mode`, я хочу видеть на главном экране компактный баннер с кнопкой `Exit Demo` и подписью `"New Walk (Demo)"` / `"Start Demo"` на кнопке старта, чтобы сразу понимать, что активна симуляция, и выходить из неё в одно касание без повторного открытия диалога.
8. Как разработчик, нажавший `Exit Demo` на баннере (или выключивший переключатель в диалоге), я хочу, чтобы баннер мгновенно исчез, а кнопки старта вернулись к `"New Walk"` / `"Start Walk"`.
9. Как пользователь с уже сохранёнными в БД старыми демо-треками, я хочу, чтобы обновление приложения не удаляло мои записи автоматически, оставляя решение за мной.
10. Как пользователь, я хочу, чтобы все тексты диалога `Developer Tools`, баннера `Demo Mode`, кнопок и `contentDescription` были строго на английском языке и соответствовали OLED Dark Theme приложения PaceGhost.

## Implementation Decisions

### 1. Включение `BuildConfig` (`app/build.gradle.kts`)
- В `android.buildFeatures` добавить `buildConfig = true`, чтобы генерировался класс `com.paceghost.app.BuildConfig` с полями `BuildConfig.DEBUG`, `BuildConfig.VERSION_NAME`, `BuildConfig.VERSION_CODE`.

### 2. Защита `Demo Mode` в `HomeViewModel` (`app/src/main/java/com/paceghost/app/ui/home/HomeViewModel.kt`)
- Добавить инъектируемый параметр с дефолтным значением и обязательной аннотацией `@JvmOverloads` (требование `ViewModelProvider` и `checkAndroidCompatibility`):
  - `class HomeViewModel @JvmOverloads constructor(application: Application, private val isDebugBuild: Boolean = BuildConfig.DEBUG) : AndroidViewModel(application)`
- В `setDemoMode(enabled: Boolean)`:
  - Если `!isDebugBuild && enabled`, метод является no-op (`_isDemoMode.value` остаётся `false`, `AppDatabase.populateDemoWalk` не вызывается).
  - При `isDebugBuild == true` поведение сохраняется прежним: обновление `_isDemoMode`, а при `enabled == true` — вызов `AppDatabase.populateDemoWalk(db)` и `repository.clearHeatmapCache()`.

### 3. Новый компонент `DeveloperToolsDialog` (`app/src/main/java/com/paceghost/app/ui/home/components/DeveloperToolsDialog.kt`)
- Stateless Composable:
  - Параметры: `isDebugBuild: Boolean`, `isDemoMode: Boolean`, `versionLabel: String`, `onToggleDemoMode: (Boolean) -> Unit`, `onExportLogs: () -> Unit`, `onDismiss: () -> Unit`.
- Визуальное оформление в OLED Dark Theme (`AlertDialog`, `containerColor = DarkSurface`, `RoundedCornerShape(24.dp)`):
  - **Title**: `"Developer Tools"` (`TextPrimary`, `FontWeight.Bold`) с подзаголовком версии приложения `versionLabel` (например, `"v1.15.1 (24)"`, `TextSecondary`).
  - **Секция `Demo Mode` (рендерится только при `isDebugBuild == true`)**:
    - Строка с иконкой `Icons.Filled.Science`, заголовком `"Demo Mode"`, кратким описанием `"Simulate GPS & ghost pacing for emulator testing"` и `Switch` (`CyanGhost` / `DarkSurfaceVariant`).
    - Разделитель `HorizontalDivider(color = DarkSurfaceVariant)` под секцией.
  - **Секция `Export Debug Logs` (рендерится всегда — и в `debug`, и в `release`)**:
    - Действие/кнопка с иконкой `Icons.Filled.BugReport`, текстом `"Export Debug Logs"` и описанием `"Share paceghost.log for troubleshooting"`. По нажатию вызывает `onExportLogs()` (и закрывает диалог).
  - **Dismiss button**: `TextButton` `"Close"` (`TextSecondary`). `onDismissRequest = onDismiss`.

### 4. Обновление `HomeScreen` (`app/src/main/java/com/paceghost/app/ui/home/HomeScreen.kt`)
- Параметр `isDebugBuild: Boolean = BuildConfig.DEBUG` (с дефолтным значением для удобного тестирования обоих режимов).
- **`TopAppBar`**:
  - Логотип `PaceGhost` (`Row`) оборачивается в `Modifier.combinedClickable(onClick = {}, onLongClick = { showDeveloperToolsDialog = true })` (с `ExperimentalFoundationApi`).
  - Из `actions` полностью удаляются чип `Demo` (`Icons.Filled.Science`) и кнопка `Export Logs` (`Icons.Filled.BugReport`).
  - В `actions` остаются ровно 3 `IconButton`: `Personal Heatmap` (`Layers`), `Import GPX` (`FileOpen`), `Pace Alert Settings` (`Settings`).
- **Логика экспорта логов**: выносится в локальную лямбду/функцию на `HomeScreen` и передаётся в `DeveloperToolsDialog.onExportLogs` (поведение `FileProvider`, `Intent.ACTION_SEND`, снэкбары пустого файла и ошибок сохраняются полностью).
- **Баннер активного `Demo Mode`**:
  - Когда `isDebugBuild && isDemoMode`, в верхней части контента `HomeScreen` (над списком прогулок или над `EmptyWalksView`, внутри `Column`) рендерится компактная плашка (`DarkSurfaceVariant` с акцентом `CyanGhost`):
    - Иконка `Icons.Filled.Science` (`CyanGhost`), текст `"Demo Mode Active"`, и справа компактная кнопка `TextButton` `"Exit Demo"` (`CyanGhost`, `FontWeight.Bold`), вызывающая `viewModel.setDemoMode(false)`.
  - Когда `isDemoMode == false` (или `!isDebugBuild`), баннер не рендерится вообще.
- **Кнопки старта**:
  - Эффективный флаг `val effectiveDemoMode = isDebugBuild && isDemoMode`.
  - Кнопка в `bottomBar` и в `EmptyWalksView` используют `effectiveDemoMode` для текста (`"New Walk (Demo)"` / `"New Walk"`, `"Start Demo"` / `"Start Walk"`) и передачи в `onStartWalk(..., effectiveDemoMode)`.

### 5. Без изменений
- `WalkTrackingService.kt`, `ActiveWalkScreen.kt`, `ActiveWalkViewModel.kt`, `DemoControlsOverlay.kt`, `DemoDataGenerator.kt`, `AppDatabase.kt`, `MainActivity.kt`, `WalkState.kt`, `GpsSignalScoreTracker.kt`, `VoiceSettingsDialog.kt` — без изменений.
- Существующие записи прогулок в Room DB — без изменений и без миграций.

## Testing Decisions
- **Определение хорошего теста**: тест проверяет внешне наблюдаемое поведение интерфейса и состояния через Compose Semantics и пользовательские жесты (`longClick`, `performClick`), не завязываясь на внутренние переменные.
- **Согласованный шов тестирования**:
  - Robolectric + Compose UI Test (`RobolectricTestRunner`, `@Config(sdk = [34])`, `createAndroidComposeRule<ComponentActivity>()`, по образцу [`WalkCardDeleteFlowTest.kt`](file:///C:/Users/--/source/repos/PaceGhost/app/src/test/java/com/paceghost/app/ui/home/components/WalkCardDeleteFlowTest.kt)) в новом файле `app/src/test/java/com/paceghost/app/ui/home/HomeDeveloperToolsTest.kt` (и юнит-тест `HomeViewModel` на `isDebugBuild = false`):
    1. В `TopAppBar` на `HomeScreen` отсутствуют элементы `"Demo Mode"`, `"Demo"`, `"DEMO"` и `"Export Logs"`; присутствуют `"Personal Heatmap"`, `"Import GPX"`, `"Pace Alert Settings"`.
    2. При выключенном `Demo Mode` баннер `"Demo Mode Active"` / `"Exit Demo"` не отображается, кнопка старта показывает `"New Walk"`.
    3. Долгое нажатие (`performTouchInput { longClick() }`) на логотип `PaceGhost` открывает диалог `"Developer Tools"` с версией приложения.
    4. При `isDebugBuild = true`: в диалоге отображаются переключатель `"Demo Mode"` и кнопка `"Export Debug Logs"`; включение `"Demo Mode"` вызывает колбэк включения, на `HomeScreen` появляется баннер `"Demo Mode Active"` с кнопкой `"Exit Demo"` и кнопка старта меняется на `"New Walk (Demo)"`; нажатие `"Exit Demo"` выключает режим и скрывает баннер.
    5. При `isDebugBuild = false` (эмуляция `release`): в диалоге отображаются `"Export Debug Logs"` и версия приложения, но `"Demo Mode"` отсутствует; `HomeViewModel(app, isDebugBuild = false).setDemoMode(true)` оставляет `isDemoMode.value == false`.
    6. Нажатие `"Export Debug Logs"` вызывает колбэк экспорта логов.
- **Визуальная проверка (Android-эмулятор)**:
  - Снять скриншоты:
    1. Чистый `HomeScreen` по умолчанию (в `TopAppBar` только 3 иконки).
    2. Открытый `DeveloperToolsDialog` после долгого нажатия на логотип `PaceGhost`.
    3. `HomeScreen` в `debug`-сборке с активным `Demo Mode` (виден баннер `"Demo Mode Active"` + `"Exit Demo"` и кнопка `"New Walk (Demo)"`).
- **Автоматические гейты репозитория**:
  - `.\gradlew.bat testDebugUnitTest`
  - `pwsh scripts/check_localization.ps1`
  - `pwsh scripts/check_android_compat.ps1`

## Out of Scope
- Удаление движка `Demo Mode` (`WalkTrackingService`, `DemoControlsOverlay`, `DemoDataGenerator`, `AppDatabase.populateDemoWalk`).
- Автоматическая очистка или миграция уже сохранённых демо-треков в Room DB.
- Переработка `VoiceSettingsDialog` в полноценный экран `Settings`.
- Добавление дополнительных диагностических функций (например, просмотр логов внутри приложения или очистка БД) в `DeveloperToolsDialog`.
- Изменение логики экспорта или формата файла `paceghost.log`.

## Further Notes
- При добавлении дефолтного параметра `isDebugBuild` в `HomeViewModel` обязательно использовать `@JvmOverloads constructor(...)`, чтобы не сломать `ViewModelProvider` reflection и гейт `checkAndroidCompatibility`.
- Все новые строки UI и `contentDescription` должны быть строго на английском языке — это проверяется автоматически задачей `checkLocalization`.
- Документация проекта уже обновлена: термин **Demo Mode** зафиксирован в [`GLOSSARY.md`](file:///C:/Users/--/source/repos/PaceGhost/GLOSSARY.md#L79-L81).
