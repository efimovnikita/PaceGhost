# 03: Релиз — bump версии, тег и публикация

**What to build:** Убедиться в чистоте всех автоматических проверок и релизной сборки (`assembleRelease`), повысить версию приложения до `1.15.2` (`versionCode = 25`), зафиксировать релизный коммит, создать аннотированный тег `v1.15.2` и отправить ветку `main` и тег в `origin` для запуска автоматического CI/CD-пайплайна публикации.

**Blocked by:** 02: Визуальная E2E-верификация на Android-эмуляторе

**Status:** ready-for-agent

- [ ] В `app/build.gradle.kts` обновлены `versionCode` (`24` → `25`) и `versionName` (`"1.15.1"` → `"1.15.2"`).
- [ ] Успешно проходят локальные проверки: `pwsh scripts/check_localization.ps1`, `pwsh scripts/check_android_compat.ps1`, `.\gradlew.bat testDebugUnitTest` и `.\gradlew.bat assembleRelease`.
- [ ] Все изменения закоммичены (`chore(release): bump version to 1.15.2`), рабочее дерево `git status` чистое.
- [ ] Создан аннотированный тег `v1.15.2` (`git tag -a v1.15.2 -m "Release v1.15.2"`).
- [ ] Ветка `main` и тег `v1.15.2` отправлены в `origin` (`git push origin main ; git push origin v1.15.2`).
