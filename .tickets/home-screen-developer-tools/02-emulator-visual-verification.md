# 02: Визуальная E2E-верификация на Android-эмуляторе

**What to build:** Собрать `debug` APK, установить на Android-эмулятор и визуально проверить полный пользовательский и разработческий сценарий на главном экране: чистый `TopAppBar` без кнопок `Demo` и `Export Logs`, открытие скрытого диалога `Developer Tools` долгим нажатием на логотип `PaceGhost`, переключение `Demo Mode`, отображение баннера `"Demo Mode Active"` с кнопкой `"Exit Demo"` и подписью `"New Walk (Demo)"`, а также возврат в обычный режим по `"Exit Demo"`. Зафиксировать скриншоты в пользовательском артефакте.

**Blocked by:** 01: Очистка TopAppBar главного экрана и скрытый диалог Developer Tools

**Status:** done

- [x] `debug` APK успешно собирается (`.\gradlew.bat assembleDebug`) и устанавливается на Android-эмулятор без крашей в `logcat` / `paceghost.log`.
- [x] На стартовом `HomeScreen` в `TopAppBar` визуально видны только 3 иконки (`Personal Heatmap`, `Import GPX`, `Pace Alert Settings`), логотип `PaceGhost` не переносится, следов `Demo` и `Export Logs` нет.
- [x] Долгое нажатие на логотип `PaceGhost` открывает диалог `"Developer Tools"`, где в OLED Dark Theme отображаются версия приложения, переключатель `"Demo Mode"` и кнопка `"Export Debug Logs"`.
- [x] Включение `"Demo Mode"` и закрытие диалога отображают на главном экране компактный баннер `"Demo Mode Active"` с кнопкой `"Exit Demo"` и подпись `"New Walk (Demo)"` на нижней кнопке.
- [x] Нажатие `"Exit Demo"` на баннере мгновенно скрывает баннер и возвращает подпись `"New Walk"`.
- [x] Скриншоты всех ключевых состояний (чистый `HomeScreen`, открытый `Developer Tools`, активный `Demo Mode` с баннером `Exit Demo`) сохранены и встроены в пользовательский артефакт (`UserFacing: true`).
- [x] Эмулятор корректно остановлен после завершения проверки.
