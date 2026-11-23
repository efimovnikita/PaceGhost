# 01: Tracer Bullet — Lifetime Stats из БД до раскрываемой панели на Heatmap

**What to build:** Пользователь на экране `Personal Heatmap` видит по центру нижнего края компактный полупрозрачный язычок `Stats`, который по тапу раскрывает немодальную нижнюю панель **Stats Peek Sheet** с **Lifetime Stats** за всё время (главный показатель `Total Distance` и компактная сетка: `Total Walks`, `Total Time`, `Total Steps`, `Avg Speed`, `Longest Walk`). Статистика агрегируется одним SQL-запросом по всем **Qualifying Walks** (`durationSeconds >= 60` и `distanceMeters >= 50`) без миграции БД; при отсутствии таких прогулок язычок остаётся видимым, а панель показывает нули/`—` и подсказку `"Record your first walk to see lifetime stats"`. Спецификация: `docs/specs/heatmap-lifetime-stats.md`.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] SQL-агрегат возвращает `walkCount`, `totalDistanceMeters`, `totalDurationSeconds`, `totalSteps` и `longestWalkMeters` только по `Qualifying Walks` (`durationSeconds >= 60` и `distanceMeters >= 50`); микро-прогулки (`59s` или `49.9m`) исключены, граничная прогулка (`60s` и `50.0m`) включена.
- [ ] `Avg Speed` вычисляется как взвешенная средняя (`totalDistance / totalDuration`), возвращая `null` / `"—"` при отсутствии данных или нулевой длительности.
- [ ] Интеграционный тест `WalkDao` (in-memory Room / Robolectric) → `WalkRepositoryImpl` → `HeatmapViewModel` проверяет пустую БД, валидные прогулки, фильтрацию микро-прогулок и взвешенную среднюю скорость; `@JvmOverloads`-конструктор `HeatmapViewModel` сохранён.
- [ ] Pure JVM тесты подтверждают форматирование всех 6 метрик в `Locale.US` (`142.8 km` / `0.0 km`, `42h 15m` / `35m` / `0m`, `184,320` / `0`, `5.2 km/h` / `—`, `12.4 km` / `—`).
- [ ] На экране `Personal Heatmap` панель по умолчанию свёрнута в нижний центрированный язычок `Stats` (не пересекается с FAB), по тапу раскрывается без затемнения карты и показывает все 6 метрик (или Empty State) в OLED Dark Theme строго на английском языке.
- [ ] `.\gradlew.bat testDebugUnitTest`, `pwsh scripts/check_localization.ps1` и `pwsh scripts/check_android_compat.ps1` зелёные.
