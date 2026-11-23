# 04: Релиз — bump версии, тег и публикация (v1.14.0)

**What to build:** Завершить поставку фичи **Heatmap Lifetime Stats**: поднять версию приложения (`versionCode = 22`, `versionName = "1.14.0"`), прогнать все проверки локализации, совместимости Android и юнит-тесты, зафиксировать релизный коммит, создать аннотированный тег `v1.14.0` и отправить изменения в `origin` для запуска автоматического CI/CD релизного пайплайна.

**Blocked by:** 03: Визуальная и интерактивная проверка на Android-эмуляторе

**Status:** done

- [x] Обновить `versionCode = 22` и `versionName = "1.14.0"` в `app/build.gradle.kts`.
- [x] Прогнать проверку локализации: `pwsh scripts/check_localization.ps1`.
- [x] Прогнать проверку совместимости Android: `pwsh scripts/check_android_compat.ps1`.
- [x] Прогнать все юнит-тесты: `.\gradlew.bat testDebugUnitTest`.
- [x] Закоммитить релизные изменения: `git commit -am "chore(release): bump version to 1.14.0"`.
- [x] Создать аннотированный тег и запушить в `origin`: `git tag -a v1.14.0 -m "Release v1.14.0" ; git push origin main ; git push origin v1.14.0`.
