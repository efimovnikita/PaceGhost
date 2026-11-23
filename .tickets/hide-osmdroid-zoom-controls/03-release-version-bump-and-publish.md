# 03: Поднятие версии, коммит, тег и публикация релиза через GitHub Actions

**What to build:** Зафиксировать релиз патч-версии `1.13.1` (`versionCode = 21`) в `app/build.gradle.kts`, выполнить финальные проверки, закоммитить все изменения, создать аннотированный тег `v1.13.1` и запушить ветку `main` и тег в удалённый репозиторий, чтобы стриггерить автоматический релизный пайплайн GitHub Actions. Spec: `docs/specs/hide-osmdroid-zoom-controls.md`.

**Blocked by:** 02: Визуальная верификация Personal Heatmap на Android-эмуляторе

**Status:** done

- [x] В `app/build.gradle.kts` обновлены `versionCode = 21` и `versionName = "1.13.1"`.
- [x] Успешно пройдены `pwsh scripts/check_localization.ps1`, `pwsh scripts/check_android_compat.ps1` и `.\gradlew.bat testDebugUnitTest`.
- [x] Все изменения закоммичены в `main` с понятным release-сообщением (`chore(release): bump version to 1.13.1`).
- [x] Создан аннотированный тег `v1.13.1` (`git tag -a v1.13.1 -m "Release v1.13.1"`).
- [x] Ветка `main` и тег `v1.13.1` отправлены в `origin` (`git push origin main ; git push origin v1.13.1`), запущен GitHub Actions workflow публикации релиза.
