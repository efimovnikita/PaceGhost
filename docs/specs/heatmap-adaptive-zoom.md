# Spec: Adaptive Heatmap Zoom Scaling and Cutoff (Адаптивное масштабирование и порог видимости персональной тепловой карты)

## Problem Statement
В текущей реализации модуля Personal Heatmap толщина неонового следа маршрутов зафиксирована в экранных пикселях (`strokeWidth = 56 px` для внешней ауры и `12 px` / `4 px` для ядра) независимо от текущего масштаба карты (zoom). В результате при отдалении карты (zoom out) физический размер следа на местности экспоненциально растет:
- На `zoom = 16` след покрывает улицу (~70 м).
- На `zoom = 10` след раздувается в пятно диаметром ~4.8 км.
- На `zoom = 6` даже короткая прогулка закрывает пятном треть Московской области (~70+ км).

Кроме того, отсутствие нижнего порога отрисовки теплового слоя приводит к генерации тяжелых Bounding Box запросов к SQLite на гигантские географические области при отдалении до масштаба стран, что снижает производительность и засоряет кэш тайлов искаженными изображениями.

## Solution
Внедрить адаптивную геометрию теплового следа (**Adaptive Heat Trace**) и порог видимости слоя (**Heatmap Visibility Cutoff**):
1. **Свобода базовой карты**: Пользователь может свободно отдалять и приближать базовую темную карту без искусственных ограничений `MapView`.
2. **Порог отсечения теплового оверлея (`minHeatmapZoom = 10`)**: На масштабах ниже уровня мегаполиса (`zoom < 10`) модуль `HeatmapTileModuleProvider` мгновенно возвращает прозрачные тайлы без обращений к SQLite базе данных.
3. **Адаптивное масштабирование толщины кисти**: Для уровней масштабирования `zoom` от 10 до 20 толщина всех слоев рассеивания кисти (`densityTiers`) динамически масштабируется по плавной экспоненциальной формуле $k = 2^{(\text{zoom} - 16) \times 0.6}$ (с фиксацией $k = 1.0$ для $\text{zoom} \ge 16$) с гарантированным нижним порогом читаемости (ядро $\ge 2\text{ px}$, аура $\ge 4.5\text{ px}$). Маршруты на масштабе города остаются четкими неоновыми линиями и не превращаются в размытые круги.
4. **Ленивый разовый сброс кэша (`Lazy Cache Flush`)**: При открытии экрана `HeatmapScreen` проверяется версия кэша в настройках (`heatmap_cache_version`). Если сохраненная версия меньше актуальной (v2), каталог устаревших тайлов очищается в фоновом режиме, исключая показ артефактов старого рендеринга.

## User Stories
1. **As a user zooming out to city scale**, I want my walked routes to shrink into crisp, thin neon lines rather than expanding into giant blobs, so that I can see the exact layout of streets and corridors across the metropolitan area.
2. **As a user zooming far out into regional/country scale**, I want the heatmap overlay to gracefully disappear below zoom 10 without lag or freezing, so that I can navigate the global map smoothly without visual clutter.
3. **As a user at neighborhood/street scale (zoom 16–20)**, I want the walking routes to retain their rich Cyber Neon aura and saturated glow, so that local walk exploration looks vibrant and detailed.
4. **As a user with existing cached heatmap tiles**, I want the app to automatically purge outdated blurred tiles upon opening the heatmap screen after the update, so that I don't see obsolete bloated artifacts.
5. **As a low-power mobile user**, I want the map engine to avoid executing heavy spatial database queries when zoomed out past city limits, so that panning the map does not drain my battery or stutter.
6. **As a user panning around the city**, I want the transition between zoom levels to look natural and continuous without sudden jarring jumps in line thickness.

## Implementation Decisions

### 1. Архитектура и модули
* **[`HeatmapTileRasterizer.kt`](file:///C:/Users/--/source/repos/PaceGhost/app/src/main/java/com/paceghost/app/domain/heatmap/HeatmapTileRasterizer.kt)**:
  * Введение метода расчета слоев `fun getDensityTiersForZoom(zoom: Int): List<HeatmapDensityTier>`.
  * Применение масштабного коэффициента:
    $$k = \begin{cases} 1.0, & \text{if } \text{zoom} \ge 16 \\ 2^{(\text{zoom} - 16) \times 0.6}, & \text{if } 10 \le \text{zoom} < 16 \end{cases}$$
  * Ограничение минимальной толщины (Floor clamping):
    * Аура (`56f` на zoom 16) масштабируется как $\max(56 \times k, 4.5\text{f})$.
    * Промежуточные слои масштабируются пропорционально.
    * Ядро (`4f` на zoom 16) масштабируется как $\max(4 \times k, 2.0\text{f})$.
  * Передача параметра `zoom` в метод отрисовки `drawDensityHeatmap`.
* **[`HeatmapTileProvider.kt`](file:///C:/Users/--/source/repos/PaceGhost/app/src/main/java/com/paceghost/app/domain/heatmap/HeatmapTileProvider.kt)**:
  * Установка `getMinimumZoomLevel() = MIN_HEATMAP_ZOOM` (константа `10`).
  * В методе `getOrRenderTile(zoom, tileX, tileY)`: если `zoom < MIN_HEATMAP_ZOOM`, немедленно возвращать `null` без обращения к DAO.
* **[`HeatmapScreen.kt`](file:///C:/Users/--/source/repos/PaceGhost/app/src/main/java/com/paceghost/app/ui/heatmap/HeatmapScreen.kt) / [`HeatmapViewModel.kt`](file:///C:/Users/--/source/repos/PaceGhost/app/src/main/java/com/paceghost/app/ui/heatmap/HeatmapViewModel.kt)**:
  * Добавление механизма ленивой очистки устаревшего кэша через проверку версии (`HEATMAP_CACHE_SCHEMA_VERSION = 2`) в `SharedPreferences`.
  * При несоответствии версий: фоновое удаление файлов в `cacheDir/heatmap_tiles` и обновление версии до 2.
* **Архитектурный контекст**: Соответствует [ADR 0003: Adaptive Heatmap Trace Scaling and Zoom Cutoff](file:///C:/Users/--/source/repos/PaceGhost/docs/adr/0003-adaptive-heatmap-zoom-scaling-and-cutoff.md).

## Testing Decisions

### 1. JVM Domain & Component Tests
* **`HeatmapTileRasterizerTest`**:
  * Тест расчета `getDensityTiersForZoom`:
    - На `zoom = 16`: толщина ауры равна `56f`, ядра `4f`.
    - На `zoom = 10`: толщина ауры составляет ~`5f` ($\ge 4.5\text{f}$), ядра ровно `2f` (сработал floor).
    - На `zoom = 18`: масштаб ограничен $1.0$, аура не раздувается больше `56f`.
  * Тест рендеринга тайла на `zoom = 10`: генерация не падает, создает валидный `Bitmap`.
* **`HeatmapTileProviderTest`**:
  * Проверка `getMinimumZoomLevel() == 10`.
  * Проверка вызова `getOrRenderTile(zoom = 9, tileX = 0, tileY = 0)`: возвращает `null`, вызовы mock `walkDao.getPointsInBoundingBox` строго отсутствуют (0 вызовов).
* **`HeatmapCacheInvalidationTest`**:
  * Проверка логики ленивой миграции: если текущая версия в преференсах `0` или `1`, каталог очищается и записывается версия `2`. При повторном вызове очистка не триггерится.

### 2. UI & End-to-End Tests в Android Emulator
* Запуск эмулятора и установка актуального приложения с тестовыми треками.
* Открытие экрана `Personal Heatmap`.
* Выполнение серий масштабирования с захватом скриншотов через ADB:
  1. `zoom = 16` — детальный неоновый коридор.
  2. `zoom = 12` — районный масштаб с умеренно тонкими линиями.
  3. `zoom = 10` — масштаб города, трек представляет собой четкую нить без радиального размытия на город.
  4. `zoom = 8` — масштаб области, тепловой слой скрыт, карта чистая.
* Встраивание скриншотов в артефакт тестирования.

## Out of Scope
* Ограничение минимального зума базовой карты (`mapView.minZoomLevel`) — базовая карта остается свободной для навигации.
* Динамическая агрегация треков в кластеры точек на низких зумах.
* Пользовательская настройка коэффициента сжатия в интерфейсе настроек.

## Further Notes
* Отключение запросов к SQLite при `zoom < 10` существенно снижает нагрузку на дисковую подсистему устройства при быстром зум-ауте.
* Формула $k = 2^{(\text{zoom} - 16) \times 0.6}$ дает показатель $k \approx 0.08$ на зуме 10, что идеально соответствует визуальной плотности линий городской уличной сети на экранах современных смартфонов (xhdpi / xxhdpi).
