# 01: Очистка TopAppBar главного экрана и скрытый диалог Developer Tools

**What to build:** Убрать с верхней панели главного экрана (`HomeScreen`) чип переключения **Demo Mode** и кнопку **Export Logs**, оставив в `TopAppBar` только три пользовательские иконки (`Personal Heatmap`, `Import GPX`, `Pace Alert Settings`). Перенести оба инструмента разработчика в скрытый диалог **Developer Tools**, открываемый долгим нажатием на логотип **PaceGhost**: в `debug`-сборках диалог показывает переключатель **Demo Mode**, кнопку **Export Debug Logs** и версию приложения, а в `release`-сборках — только **Export Debug Logs** и версию, при этом включение `Demo Mode` заблокировано программно. Когда `Demo Mode` активен в `debug`-сборке, на главном экране отображается компактный баннер `"Demo Mode Active"` с кнопкой быстрого выхода `"Exit Demo"` в одно касание и подписи `"New Walk (Demo)"` / `"Start Demo"` на кнопках старта. Покрыть все сценарии (`debug` и `release`) автоматическими Robolectric + Compose UI тестами.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] В Gradle-конфигурации приложения включена генерация `BuildConfig` (`buildConfig = true`), доступны `BuildConfig.DEBUG`, `BuildConfig.VERSION_NAME`, `BuildConfig.VERSION_CODE`.
- [ ] `HomeViewModel` принимает инъектируемый параметр `isDebugBuild: Boolean = BuildConfig.DEBUG` с аннотацией `@JvmOverloads constructor(...)`; вызов `setDemoMode(true)` при `isDebugBuild == false` является no-op и не изменяет `isDemoMode` / не заполняет БД демо-треками.
- [ ] В `TopAppBar` на главном экране отсутствуют элементы `Demo` / `DEMO` и `Export Logs`; отображаются только `Personal Heatmap`, `Import GPX` и `Pace Alert Settings`.
- [ ] Долгое нажатие (`onLongClick`) на логотип `PaceGhost` открывает диалог `Developer Tools` в OLED Dark Theme с заголовком `"Developer Tools"` и версией приложения (`v<VERSION_NAME> (<VERSION_CODE>)`).
- [ ] В `Developer Tools` при `isDebugBuild == true` отображаются переключатель `"Demo Mode"` и действие `"Export Debug Logs"`; при `isDebugBuild == false` отображается только `"Export Debug Logs"` (секция `"Demo Mode"` полностью скрыта).
- [ ] Нажатие `"Export Debug Logs"` в диалоге запускает существующий механизм экспорта `paceghost.log` (включая обработку пустого файла и ошибок через snackbar) и закрывает диалог.
- [ ] При активном `Demo Mode` (и `isDebugBuild == true`) на главном экране отображается компактный баннер `"Demo Mode Active"` с кнопкой `"Exit Demo"` (выключает режим в один тап), а кнопки старта показывают `"New Walk (Demo)"` / `"Start Demo"`. При выключенном `Demo Mode` (или в `release`) баннер не отображается, а кнопки показывают `"New Walk"` / `"Start Walk"`.
- [ ] Существующие демо-треки в Room DB не удаляются автоматически; движок `Demo Mode` (`WalkTrackingService`, `DemoControlsOverlay`, `DemoDataGenerator`, `AppDatabase.populateDemoWalk`) работает без регрессий.
- [ ] Добавлены Robolectric + Compose UI тесты (`HomeDeveloperToolsTest`), проверяющие все перечисленные сценарии для `isDebugBuild = true` и `isDebugBuild = false`, включая `HomeViewModel.setDemoMode(true)` в release-режиме.
- [ ] Все тексты и `contentDescription` строго на английском языке; успешно проходят `.\gradlew.bat testDebugUnitTest`, `pwsh scripts/check_localization.ps1` и `pwsh scripts/check_android_compat.ps1`.
