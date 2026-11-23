# 01: Скрытие встроенных кнопок зума osmdroid на всех картах через общую конфигурацию

**What to build:** Отключить встроенные белые кнопки зума `osmdroid` (`CustomZoomButtonsController.Visibility.NEVER`) на всех трёх экранах с картой (**Personal Heatmap**, **Track Preview**, **Active Walk**) через единую функцию базовой конфигурации `MapView`. Жест pinch-to-zoom остаётся активным везде, кастомные OLED-кнопки `+` / `-` и `Recenter` на экране **Personal Heatmap** работают без изменений. Spec: `docs/specs/hide-osmdroid-zoom-controls.md`.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] Создан общий helper базовой конфигурации `MapView` (`MAPNIK`, `multiTouchControls`, `tilesScaledToDpi`, отключение повторения карты, `zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)`).
- [ ] Все три компонента карты (**Personal Heatmap**, **Track Preview**, **Active Walk**) используют общий helper без нарушения своих специфичных настроек (фильтры, overlay-слои, начальный zoom, lifecycle `onDetach()`).
- [ ] Добавлен Robolectric JVM-тест на общий helper: встроенные кнопки зума остаются скрытыми даже после `zoomController.activate()`, `multiTouchControls` включён, а программные `controller.zoomIn()` / `zoomOut()` меняют масштаб.
- [ ] Проходят `.\gradlew.bat testDebugUnitTest`, `pwsh scripts/check_localization.ps1` и `pwsh scripts/check_android_compat.ps1`.
