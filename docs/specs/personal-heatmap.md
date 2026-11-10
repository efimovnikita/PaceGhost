# Spec: Personal Heatmap (Персональная тепловая карта активности)

## Problem Statement
Пользователи трекера активности PaceGhost накапливают историю многочисленных пеших и беговых прогулок, однако в приложении отсутствует способ увидеть целостную картину своей активности на географической карте. Пользователь не может визуально оценить географию своих перемещений, выявить наиболее часто посещаемые улицы и районы, а также исследовать новые для себя маршруты. Попытка отобразить все накопленные точки маршрутов простым рисованием векторных линий в реальном времени приводит к резкой просадке FPS, лагам интерфейса и потенциальному падению приложения из-за нехватки оперативной памяти (OOM) на мобильных устройствах.

## Solution
Реализовать выделенный полноэкранный модуль **Personal Heatmap** («Персональная тепловая карта»), доступный из верхней панели главного экрана (`HomeScreen`). 
Модуль отображает кумулятивную карту всех пройденных маршрутов в фирменной эстетике OLED Dark Theme:
1. Базовые тайлы OpenStreetMap инвертируются и затемняются в рантайме через `ColorMatrixColorFilter`, создавая темный тактический фон без необходимости использования внешних платных сервисов или API-ключей.
2. Маршруты отрисовываются полупрозрачными неоновыми линиями в палитре **Cyber Neon Ramp** (от глубокого бирюзового до раскаленного неоново-зеленого и белого цвета) с двухпроходным аддитивным свечением (широкая аура + тонкое контрастное ядро).
3. Для обеспечения стабильных 60 FPS при навигации и масштабировании используется архитектура **Lazy On-Demand Heatmap Tile Cache**: тайлы накладываются поверх карты через штатный механизм `TilesOverlay` библиотеки Osmdroid, генерируются на лету при попадании в область видимости и кэшируются на диске в каталоге кэша приложения.

## User Stories
1. **As a runner/walker**, I want to tap an icon in the top app bar of the home screen, so that I can open the Personal Heatmap screen and explore my cumulative walking history.
2. **As a user with many recorded walks**, I want the heatmap to pan and zoom smoothly at 60 FPS without UI freezing or stutter, so that map exploration feels instantaneous and fluid.
3. **As a user walking in low-light/night conditions**, I want the heatmap screen to follow the OLED Dark Theme (pure black background and darkened map details), so that the screen does not blind me or waste battery on OLED displays.
4. **As an explorer**, I want frequently walked streets and intersections to glow significantly brighter with neon green and white light than routes I visited only once, so that I can easily distinguish my regular corridors from rare paths.
5. **As a user**, I want to easily center the map back onto my recent walking territory or current location with a single tap, so that I don't get lost when panning far away.
6. **As a user**, I want to tap a back button, so that I can immediately return to the main dashboard (`HomeScreen`).
7. **As a user**, I want new walks (or imported GPX files) to automatically reflect in the heatmap, so that my personal heat record is always up to date.
8. **As a user deleting a walk**, I want the deleted route to be removed from the heatmap upon subsequent views, so that deleted data does not persist visually.
9. **As a user with low device storage**, I want the heatmap tile cache to be stored in the system cache directory (`cacheDir`), so that the operating system can automatically reclaim disk space if the device runs low on storage without corrupting my primary walk data.

## Implementation Decisions

### 1. Архитектура и модули
* **Навигация**: Расширение интерфейса `Screen` в [`MainActivity.kt`](file:///C:/Users/--/source/repos/PaceGhost/app/src/main/java/com/paceghost/app/MainActivity.kt) новым состоянием `object Heatmap : Screen`.
* **UI Экран**: Создание Compose-экрана `HeatmapScreen.kt` в пакете `com.paceghost.app.ui.heatmap` со стандартным `Scaffold`, кастомным `TopAppBar` (кнопка Back, заголовок "Personal Heatmap") и плавающей кнопкой центрирования.
* **TopAppBar Integration**: Добавление компактной кнопки `IconButton` с иконкой `Icons.Filled.Layers` (`size = 38.dp`, `contentDescription = "Personal Heatmap"`) в `HomeScreen.kt` с сохранением плотности элементов и без переноса заголовка "PaceGhost".

### 2. База данных и пространственная выборка (Room)
* **Индексация**: Добавление составного индекса `@Index(value = ["latitude", "longitude"])` в класс сущности [`TrackPointEntity`](file:///C:/Users/--/source/repos/PaceGhost/app/src/main/java/com/paceghost/app/data/local/entity/TrackPointEntity.kt).
* **DAO Запрос**: Добавление в [`WalkDao`](file:///C:/Users/--/source/repos/PaceGhost/app/src/main/java/com/paceghost/app/data/local/WalkDao.kt) метода:
  ```kotlin
  @Query("SELECT * FROM track_points WHERE latitude BETWEEN :minLat AND :maxLat AND longitude BETWEEN :minLon AND :maxLon ORDER BY walkId, id ASC")
  suspend fun getPointsInBoundingBox(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): List<TrackPointEntity>
  ```
* Запрос возвращает упорядоченные точки для быстрого восстановления линейных сегментов по `walkId`. Для предотвращения разрывов линий на границах тайлов при выборке закладывается небольшой буфер запаса (padding ~5–10% размера тайла).

### 3. Генерация тайлов (Heatmap Tile Provider)
* **Класс `HeatmapTileProvider`**: Наследуется или интегрируется с механизмом провайдеров Osmdroid / локальной файловой отдачей.
* **Математика Web Mercator**: Преобразование координат `(z, x, y)` в географический Bounding Box `[minLat, minLon, maxLat, maxLon]` и обратный перевод GPS-точек в пиксельные координаты `(px, py)` на холсте 256x256.
* **Двухпроходный рендеринг свечения (Two-Pass Glow)**:
  * Создается `Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)`.
  * **Pass 1 (Aura)**: Отрисовка линий кистью `strokeWidth = 8.dp`, цвет `CyanGhost.copy(alpha = 0.20f)`, `strokeCap = Cap.ROUND`, `strokeJoin = Join.ROUND`.
  * **Pass 2 (Core)**: Отрисовка тех же сегментов кистью `strokeWidth = 2.dp`, цвет `NeonGreenRunner.copy(alpha = 0.60f)`.
  * Интенсивные перекрытия естественно суммируют альфа-канал и яркость, формируя цветовую шкалу **Cyber Neon Ramp**.
  * Готовый `Bitmap` сжимается в PNG и сохраняется на диск по пути `context.cacheDir/heatmap_tiles/{z}/{x}/{y}.png`.

### 4. Темная подложка базовой карты (Base Map Dark Filter)
* На стандартный слой тайлов Osmdroid `Mapnik` накладывается матричный фильтр:
  ```kotlin
  val matrix = ColorMatrix(floatArrayOf(
      -1f,  0f,  0f, 0f, 255f, // Invert Red
       0f, -1f,  0f, 0f, 255f, // Invert Green
       0f,  0f, -1f, 0f, 255f, // Invert Blue
       0f,  0f,  0f, 1f,   0f  // Alpha
  )).apply {
      // Снижение яркости и контраст для глубокого темного тона
  }
  mapView.overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(matrix))
  ```
* Фон карты (`mapView.setBackgroundColor(0xFF000000.toInt())`) устанавливается в чистый черный цвет `#000000`.

### 5. Инвалидация кэша
* При вызове методов добавления новой прогулки (`insertWalk`), импорта GPX (`GpxImporter`) или удаления прогулки (`deleteWalk` / `clearAllWalks`) в [`WalkRepository`](file:///C:/Users/--/source/repos/PaceGhost/app/src/main/java/com/paceghost/app/data/repository/WalkRepository.kt) инициируется очистка каталога: `File(context.cacheDir, "heatmap_tiles").deleteRecursively()`.

## Testing Decisions

### 1. Обязательная фаза тестирования в Android-эмуляторе с валидацией скриншотов
* **Инвариант процесса**: Ни один коммит по функциональности Heatmap не может быть отправлен в Git-репозиторий без прохождения сквозного прогона в работающем Android-эмуляторе.
* **Протокол тестирования в эмуляторе**:
  1. Сборка и установка debug APK на эмулятор: `.\gradlew.bat assembleDebug`, затем установка через ADB.
  2. Запуск приложения: `adb shell am start -n com.paceghost.app/.MainActivity`.
  3. Обеспечение тестовых данных: наличие как минимум 2–3 прогулок (через Demo-режим или генератор демо-треков), пересекающихся на одних и тех же улицах для проверки градиента яркости.
  4. Навигация на экран `Heatmap`: программный клик или открытие экрана, ожидание 2–3 секунды для полной растеризации тайлов.
  5. Снятие скриншотов через ADB: `adb shell screencap -p /sdcard/heatmap_screen.png`, выгрузка через `adb pull`.
  6. Встраивание скриншотов в артефакт сессии с `UserFacing: true` и предъявление пользователю на финальное утверждение.

### 2. Модульное и компонентное тестирование
* **Unit-тесты DAO**: Проверка корректности выборки `getPointsInBoundingBox` с граничными координатами. Точки вне прямоугольника отсекаются, точки внутри возвращаются отсортированными по `walkId, id`.
* **Unit-тесты проекции Web Mercator**: Проверка формул перевода `(lat, lon) <-> (x, y)` на известных эталонных координатах.
* **Линтеры и совместимость**: Обязательный запуск `pwsh scripts/check_localization.ps1` (проверка отсутствия неанглийских строк в коде) и `pwsh scripts/check_android_compat.ps1` перед завершением работ.

## Out of Scope
* Временные фильтры (фильтрация по годам/месяцам) — согласован чистый минималистичный режим "All Time".
* Индивидуальное отключение/скрытие конкретных прогулок на тепловой карте.
* Экспорт тепловой карты в растровый графический файл (PNG/JPEG) для соцсетей.
* Пользовательская настройка цветов палитры (настройка палитры зафиксирована в Cyber Neon Ramp).

## Further Notes
* **Операционная память**: Использование формата `ARGB_8888` для тайлов 256x256 требует всего 256 КБ на один тайл в памяти во время отрисовки, что полностью исключает риск OOM.
* **Производительность**: Тайловый механизм Osmdroid использует пул потоков для параллельной подгрузки тайлов, поэтому прорисовка карты не блокирует основной UI-поток (Main Thread).
