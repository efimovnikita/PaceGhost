# Spec: Safe Walk Deletion

## Problem Statement
На главном экране (`HomeScreen`) на каждой карточке прошлой прогулки (`WalkCard`) в правом нижнем углу находится прямая кнопка удаления (иконка корзины). Одно случайное нажатие на неё — например, при прокрутке списка или попытке нажать соседнюю кнопку — мгновенно и безвозвратно удаляет прогулку и все её GPS-точки из базы данных Room и сбрасывает кэш тепловой карты, без какого-либо подтверждения. Пользователь рискует потерять записанные маршруты и статистику из-за одного неосторожного касания.

## Solution
Защитить удаление прогулки двумя уровнями осознанного взаимодействия на главном экране:
1. **Снизить доступность кнопки удаления на `WalkCard`**: убрать прямые кнопки `Delete` и `Export GPX` из нижнего ряда действий карточки, оставив там только два основных действия маршрута (`Walk with Ghost` и `View track on map`). В **правом верхнем углу шапки карточки** добавить кнопку overflow-меню (`⋮` / `More options`), а дату прогулки перенести второй строкой под название маршрута. Внутри выпадающего меню (`DropdownMenu`) разместить два пункта: `Export GPX` и (ниже, выделенный красным `ErrorRed`) `Delete`.
2. **Добавить модальное окно подтверждения (`DeleteWalkConfirmationDialog`)**: выбор `Delete` в меню не удаляет прогулку сразу, а открывает Material3 `AlertDialog` в OLED Dark Theme с названием прогулки, датой, дистанцией и предупреждением о необратимости действия. Прогулка и карточка удаляются из базы данных Room и с главного экрана **только после явного нажатия `Delete` в модальном окне**, с последующим сбросом кэша тепловой карты и снэкбаром `"Walk deleted"`.

## User Stories
1. Как пользователь главного экрана, я хочу, чтобы на карточке прошлой прогулки (`WalkCard`) не было прямой кнопки удаления в один клик, чтобы при прокрутке ленты невозможно было случайно удалить маршрут.
2. Как пользователь главного экрана, я хочу, чтобы второстепенные действия карточки (`Export GPX` и `Delete`) были скрыты в overflow-меню (`⋮`) в правом верхнем углу карточки, физически отделённом от основных кнопок запуска (`Walk with Ghost`) и просмотра карты (`View track on map`).
3. Как пользователь главного экрана, я хочу, чтобы дата прогулки отображалась аккуратной подписью под названием маршрута, чтобы длинные названия не обрезались из-за кнопки `⋮` в правом верхнем углу.
4. Как пользователь, я хочу открыть меню `⋮` на карточке и выбрать `Export GPX`, чтобы экспортировать маршрут в GPX так же, как раньше, без риска случайно запустить удаление.
5. Как пользователь, я хочу, чтобы пункт `Delete` в меню `⋮` находился ниже `Export GPX` и был визуально выделен красным цветом (`ErrorRed`), чтобы его нельзя было спутать с безопасными действиями.
6. Как пользователь, выбравший `Delete` в меню `⋮`, я хочу увидеть модальное окно подтверждения (`"Delete walk?"`), в котором явно указаны название прогулки, её дата и дистанция, а также предупреждение о том, что действие необратимо, чтобы быть на 100% уверенным, какую именно прогулку я удаляю.
7. Как пользователь, я хочу, чтобы до нажатия подтверждения в модальном окне прогулка оставалась в базе данных и в списке на главном экране.
8. Как пользователь, передумавший удалять прогулку, я хочу нажать `Cancel`, тапнуть вне модального окна или нажать системную кнопку «Назад», чтобы диалог закрылся, а прогулка осталась нетронутой.
9. Как пользователь, подтвердивший удаление кнопкой `Delete` в модальном окне, я хочу, чтобы прогулка и её трек были немедленно удалены из базы данных Room, карточка исчезла с главного экрана, кэш тепловой карты обновился, и появилось уведомление `"Walk deleted"`.
10. Как пользователь, я хочу, чтобы все тексты меню, диалога, кнопок и `contentDescription` были строго на английском языке и соответствовали OLED Dark Theme приложения PaceGhost.

## Implementation Decisions

### 1. Обновление карточки прогулки (`app/src/main/java/com/paceghost/app/ui/home/components/WalkCard.kt`)
- **Шапка карточки (Header)**:
  - Левая часть (`Column(modifier = Modifier.weight(1f))`):
    - Название прогулки (`walk.title`, `maxLines = 1`, `TextOverflow.Ellipsis`, `TextPrimary`, `FontWeight.Bold`, `17.sp`).
    - Ниже (`Spacer(height = 2.dp)`): отформатированная дата (`dd MMM yyyy, HH:mm`, `labelSmall`, `TextSecondary`).
  - Правая часть (`Box`):
    - Кнопка overflow-меню `IconButton` с иконкой `Icons.Filled.MoreVert`, `contentDescription = "More options"`, цвет `TextSecondary`.
    - Привязанный `DropdownMenu` в OLED-стиле (`containerColor = DarkSurfaceVariant`):
      1. `DropdownMenuItem`: текст `"Export GPX"` (`TextPrimary`), ведущая иконка `Icons.Filled.Share` (`TextPrimary`). При нажатии закрывает меню и вызывает `onExportGpx(walk.id)`.
      2. Визуальный разделитель `HorizontalDivider` (тонкий, ненавязчивый оттенок `DarkSurface` / `TextSecondary.copy(alpha = 0.15f)`).
      3. `DropdownMenuItem`: текст `"Delete"` (`ErrorRed`), ведущая иконка `Icons.Filled.DeleteOutline` (`ErrorRed`). При нажатии закрывает меню и вызывает `onDelete(walk.id)`.
- **Нижний ряд действий (Action Row)**:
  - Остаются только 2 основные кнопки маршрута слева:
    - `FilledIconButton` (`CyanGhost` / `DirectionsRun`, `"Walk with Ghost"`)
    - `OutlinedIconButton` (`CyanGhost` / `Map`, `"View track on map"`)
  - Прямые кнопки `Share` (`Export GPX`) и `DeleteOutline` (`Delete walk`) из нижнего ряда полностью удаляются.
- **Контракт `WalkCard`**:
  - Сигнатура колбэков (`onChallengeGhost`, `onExportGpx`, `onDelete`, `onPreviewTrack`) сохраняется совместимой; `onDelete` теперь инициирует запрос на удаление (открытие диалога на `HomeScreen`), а не прямое удаление из БД.

### 2. Модальное окно подтверждения (`app/src/main/java/com/paceghost/app/ui/home/components/DeleteWalkConfirmationDialog.kt`)
- Выделенный stateless Composable `DeleteWalkConfirmationDialog(walk: WalkEntity, onConfirm: () -> Unit, onDismiss: () -> Unit)`:
  - Построен на Material3 `AlertDialog` с `containerColor = DarkSurface`, `titleContentColor = TextPrimary`, `textContentColor = TextSecondary`.
  - **Title**: `"Delete walk?"` (`FontWeight.Bold`, `TextPrimary`).
  - **Text**: отображает название прогулки, дату (`dd MMM yyyy, HH:mm`) и дистанцию (`PaceCalculator.formatDistance(walk.distanceMeters)`), а также предупреждение о необратимости:
    `"Are you sure you want to delete \"${walk.title}\" ($formattedDate, $formattedDistance)? This action cannot be undone."`
  - **Confirm button**: `TextButton` с текстом `"Delete"`, `contentColor = ErrorRed`, `FontWeight.Bold` → вызывает `onConfirm()`.
  - **Dismiss button**: `TextButton` с текстом `"Cancel"`, `contentColor = TextSecondary` → вызывает `onDismiss()`.
  - `onDismissRequest = onDismiss` (тап вне окна и системная кнопка «Назад» безопасно отменяют действие).

### 3. Интеграция на главном экране (`app/src/main/java/com/paceghost/app/ui/home/HomeScreen.kt`)
- Локальное UI-состояние: `var walkPendingDelete by remember { mutableStateOf<WalkEntity?>(null) }`.
- В `items(walks, key = { it.id })`:
  - В `WalkCard` передаётся `onDelete = { walkPendingDelete = walk }`.
- При `walkPendingDelete != null` рендерится `DeleteWalkConfirmationDialog`:
  - `onConfirm`: вызывает `viewModel.deleteWalk(pendingWalk.id)` и сбрасывает `walkPendingDelete = null`.
  - `onDismiss`: сбрасывает `walkPendingDelete = null` без вызова `viewModel.deleteWalk`.

### 4. Без изменений
- [`HomeViewModel.kt`](file:///C:/Users/--/source/repos/PaceGhost/app/src/main/java/com/paceghost/app/ui/home/HomeViewModel.kt): существующий `deleteWalk(id)` уже корректно удаляет прогулку через репозиторий и отправляет `"Walk deleted"`.
- [`WalkRepository.kt`](file:///C:/Users/--/source/repos/PaceGhost/app/src/main/java/com/paceghost/app/data/repository/WalkRepository.kt) и Room DB (`WalkDao`, `WalkEntity`, `TrackPointEntity`): без изменений (каскадное удаление трек-пойнтов и `clearHeatmapCache()` уже работают).
- [`TrackPreviewScreen.kt`](file:///C:/Users/--/source/repos/PaceGhost/app/src/main/java/com/paceghost/app/ui/preview/TrackPreviewScreen.kt): без изменений.
- `GLOSSARY.md` и `docs/adr/`: обновлений не требуется.

## Testing Decisions
- **Определение хорошего теста**: тест проверяет внешне наблюдаемое пользовательское поведение (видимость кнопок и пунктов меню, открытие и содержимое диалога, отмену без побочных эффектов, вызов удаления строго после подтверждения) через Compose Semantics и реальные пользовательские действия, не завязываясь на внутренние переменные состояния.
- **Согласованный шов тестирования (Robolectric + Compose UI Test, по образцу [`StatsPeekSheetTest.kt`](file:///C:/Users/--/source/repos/PaceGhost/app/src/test/java/com/paceghost/app/ui/heatmap/components/StatsPeekSheetTest.kt))**:
  - Файл `app/src/test/java/com/paceghost/app/ui/home/components/WalkCardDeleteFlowTest.kt` с `RobolectricTestRunner`, `@Config(sdk = [34])`, `createAndroidComposeRule<ComponentActivity>()`:
    1. На карточке `WalkCard` отсутствуют прямые кнопки `"Delete walk"` и `"Export GPX"`; отображаются `"Walk with Ghost"`, `"View track on map"` и `"More options"`.
    2. Тап по `"More options"` раскрывает меню с пунктами `"Export GPX"` и `"Delete"`.
    3. Тап по `"Export GPX"` закрывает меню и вызывает `onExportGpx(walk.id)`, не вызывая `onDelete`.
    4. Тап по `"Delete"` в меню закрывает меню и открывает диалог `"Delete walk?"`, отображающий название прогулки, дату, дистанцию и предупреждение `"This action cannot be undone."`; колбэк фактического удаления при этом **ещё не вызван**.
    5. Нажатие `"Cancel"` в диалоге закрывает диалог и не вызывает удаление.
    6. Dismiss диалога (`onDismissRequest` / системная кнопка «Назад») закрывает диалог и не вызывает удаление.
    7. Нажатие `"Delete"` в диалоге закрывает диалог и вызывает удаление ровно 1 раз с `walk.id`.
- **Визуальная и E2E-проверка (Android-эмулятор)**:
  - Снять скриншоты `HomeScreen`:
    1. Обновлённая `WalkCard` (шапка с датой под названием и `⋮` в правом верхнем углу, 2 кнопки в нижнем ряду).
    2. Раскрытое overflow-меню `⋮` с `Export GPX` и красным `Delete`.
    3. Открытое модальное окно `"Delete walk?"` с контекстом прогулки.
    4. Состояние после нажатия `"Delete"` в диалоге (карточка исчезла из списка/БД + виден снэкбар `"Walk deleted"`).
- **Автоматические гейты репозитория**:
  - `.\gradlew.bat testDebugUnitTest`
  - `pwsh scripts/check_localization.ps1`
  - `pwsh scripts/check_android_compat.ps1`

## Out of Scope
- Добавление кнопки удаления на экран предпросмотра маршрута (`TrackPreviewScreen`).
- Механизм отмены удаления (`Undo` snackbar) и мягкое удаление (soft-delete) в базе данных.
- Дополнительные барьеры подтверждения (таймер задержки на кнопке `Delete`, удержание кнопки, чекбокс или ввод названия).
- Массовое удаление нескольких прогулок (multi-select режим).
- Добавление новых пунктов в меню `⋮` помимо `Export GPX` и `Delete` (например, `Rename` или `Favorite`).
- Изменение схемы базы данных Room или логики инвалидации кэша тепловой карты.

## Further Notes
- Перенос второстепенных действий в overflow-меню `⋮` в правом верхнем углу `WalkCard` создаёт чистую точку расширения: будущие действия карточки (например, переименование маршрута или добавление в избранное) можно будет добавлять в это же меню без перегрузки макета карточки.
- Все новые пользовательские строки и `contentDescription` должны быть строго на английском языке — это автоматически проверяется задачей `checkLocalization` и скриптом `scripts/check_localization.ps1`.
