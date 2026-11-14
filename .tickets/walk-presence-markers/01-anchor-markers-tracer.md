# 01: Прогулки видны на мировом масштабе одиночными маркерами

**What to build:** При отдалении карты heatmap ниже zoom 10 пользователь видит светящуюся точку CyanGhost в центре маршрута (Walk Anchor) каждой прогулки. На zoom ≥ 10 точки исчезают, работает только heatmap. Якоря вычисляются одним агрегатным запросом по `track_points` (GROUP BY walkId, центр bbox, HAVING COUNT(*) >= 2) при входе на экран, без миграции БД. Spec: `docs/specs/walk-presence-markers.md`.

**Blocked by:** None (can start immediately)

**Status:** done

- [x] Запрос якорей возвращает центр bbox и границы каждой прогулки; прогулки с 0/1 точкой исключены (Room in-memory тест по образцу WalkDaoSpatialTest).
- [x] Якоря доступны через репозиторий и появляются в состоянии экрана heatmap при входе (тест ViewModel с фейковым репозиторием); конструктор ViewModel остаётся `@JvmOverloads`.
- [x] Слой маркеров рисует точки только при zoom < 10; heatmap tile overlay не изменён.
- [x] Без прогулок экран открывается без ошибок.
- [x] `.\gradlew.bat testDebugUnitTest` зелёный.
