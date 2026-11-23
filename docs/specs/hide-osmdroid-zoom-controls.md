# Spec: Hide Built-in osmdroid Zoom Controls

## Problem Statement
На экране **Personal Heatmap** при взаимодействии с картой (пан, зум, тап) внизу по центру всплывают стандартные белые кнопки `-` и `+` библиотеки `osmdroid`. Они визуально дублируют уже существующие кастомные OLED-кнопки зума (`+` / `-`) в правом нижнем углу и выбиваются из тёмной темы приложения. Та же конфигурация `MapView` по умолчанию активна на экранах **Track Preview** и **Active Walk**, где встроенные кнопки osmdroid скрываются за нижними HUD-панелями и создают риск случайных нажатий или визуальных артефактов на нестандартных соотношениях сторон.

## Solution
Отключить встроенные кнопки зума `osmdroid` (`CustomZoomButtonsController.Visibility.NEVER`) на всех трёх картах приложения через единый helper базовой конфигурации `MapView`. Жестовое масштабирование (`pinch-to-zoom`) остаётся включённым везде, а на экране **Personal Heatmap** единственными экранными кнопками зума остаются существующие кастомные OLED FAB-кнопки `+` / `-` и `Recenter` в правом нижнем углу.

## User Stories
1. Как пользователь экрана **Personal Heatmap**, я хочу при прокрутке и масштабировании карты не видеть белых системных кнопок `-` / `+` внизу по центру, чтобы интерфейс оставался чистым и соответствовал OLED Dark Theme.
2. Как пользователь экрана **Personal Heatmap**, я хочу продолжать приближать и отдалять карту через кастомные тёмно-циановые кнопки `+` / `-` справа внизу без изменения их поведения.
3. Как пользователь экрана **Personal Heatmap**, я хочу продолжать масштабировать карту жестом pinch-to-zoom.
4. Как пользователь экрана **Track Preview**, я хочу, чтобы встроенные белые кнопки osmdroid не появлялись поверх или под нижней HUD-карточкой, при сохранении pinch-to-zoom и кнопки `Recenter`.
5. Как пользователь экрана **Active Walk**, я хочу, чтобы встроенные белые кнопки osmdroid не появлялись при взаимодействии с картой, при сохранении pinch-to-zoom.
6. Как разработчик, я хочу иметь единую точку базовой конфигурации `MapView`, чтобы любые будущие экраны с картой автоматически наследовали корректные настройки без дублирования.

## Implementation Decisions
- **Единый helper конфигурации `MapView`**: вынести общую базовую настройку `osmdroid` `MapView` (например, `MapView.applyPaceGhostDefaults()` в общем UI-модуле `ui/common/MapViewDefaults.kt`):
  - `setTileSource(TileSourceFactory.MAPNIK)`
  - `setMultiTouchControls(true)`
  - `setTilesScaledToDpi(true)`
  - `isHorizontalMapRepetitionEnabled = false`
  - `isVerticalMapRepetitionEnabled = false`
  - `zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)`
- **Точки подключения (все 3 `MapView` в приложении)**:
  - `app/src/main/java/com/paceghost/app/ui/heatmap/components/HeatmapMapView.kt`
  - `app/src/main/java/com/paceghost/app/ui/preview/components/TrackPreviewMapView.kt`
  - `app/src/main/java/com/paceghost/app/ui/active/components/OsmMapView.kt`
- **Инварианты**:
  - Специфичные настройки каждого экрана (цвет фона, цветовой фильтр инверсии, overlay-слои, начальный zoom, lifecycle `onDetach()`) остаются без изменений в соответствующих компонентах.
  - Кастомные FAB-кнопки `+` / `-` и `Recenter` в `HeatmapScreen.kt` и `HeatmapMapController` остаются без изменений.
- **Документация**: новых терминов в `GLOSSARY.md` и новых ADR в `docs/adr/` не требуется.

## Testing Decisions
- Хороший тест проверяет наблюдаемое поведение сконфигурированного `MapView` (недоступность/скрытие встроенных кнопок зума, работоспособность мультитач-зума и программного зума), а не внутренние детали вызовов.
- **Основной шов (Robolectric JVM-тест, по образцу `WalkPresenceMarkersOverlayTest.kt`)**:
  - Тест на `MapView.applyPaceGhostDefaults()`:
    - встроенный `zoomController` переведён в `Visibility.NEVER` (вызов `activate()` не делает встроенные кнопки видимыми и не перехватывает тач-события);
    - мультитач-управление включено, повторение карты по вертикали и горизонтали отключено, масштабирование тайлов по DPI включено;
    - программное масштабирование через `mapView.controller.zoomIn()` / `zoomOut()` продолжает изменять уровень зума.
- **Визуальная проверка (Android-эмулятор)**:
  - Открыть экран **Personal Heatmap**, выполнить панорамирование (свайп) и/или зум карты и снять скриншот **немедленно** после взаимодействия (в пределах ~0.3–0.5 сек, пока стандартный `SHOW_AND_FADEOUT` не успел бы затухнуть), подтверждая отсутствие белых кнопок `-` / `+` внизу по центру при сохранённых кастомных FAB справа внизу.
- **Регрессионный прогон**:
  - `.\gradlew.bat testDebugUnitTest`
  - `pwsh scripts/check_localization.ps1`
  - `pwsh scripts/check_android_compat.ps1`

## Out of Scope
- Изменение внешнего вида, расположения или состояния (disabled/enabled на границах зума) кастомных FAB-кнопок `+` / `-` в `HeatmapScreen.kt`.
- Добавление кастомных кнопок `+` / `-` на экраны `TrackPreviewScreen` и `ActiveWalkScreen`.
- Изменение логики `HeatmapMapController`, lifecycle-обёрток `MapView` или цветовых фильтров базовой карты.

## Further Notes
- Изменение не добавляет новых UI-строк и не требует миграций базы данных или инвалидации кэша тайлов.
- При добавлении новых экранов с `osmdroid` `MapView` в будущем следует переиспользовать общий helper базовой конфигурации, чтобы исключить повторное появление стандартных белых контролов.
