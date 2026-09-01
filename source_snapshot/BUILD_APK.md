# Сборка APK

Проект подготовлен для сборки debug APK.

## Самый простой вариант — GitHub Actions

1. Создайте пустой репозиторий на GitHub.
2. Загрузите **содержимое этой папки** в корень репозитория.
3. Если нужен рабочий Gemini внутри APK, откройте в GitHub:
   **Settings → Secrets and variables → Actions → New repository secret**.
4. Создайте секрет с именем `GEMINI_API_KEY` и вставьте свой ключ.
5. Откройте вкладку **Actions → Build APK → Run workflow**.
6. После завершения сборки откройте запуск workflow и скачайте artifact **MKSH-Planner-debug-apk**.
7. Внутри будет `app-debug.apk`.

Workflow сам использует JDK 17, ставит Android SDK Platform 36.1 и запускает `assembleDebug`.

## Через Android Studio

1. Откройте эту папку как проект.
2. Дождитесь Gradle Sync.
3. Создайте рядом с `.env.example` файл `.env`:

   `GEMINI_API_KEY=ВАШ_КЛЮЧ`

4. Выберите **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
5. APK появится в:

   `app/build/outputs/apk/debug/app-debug.apk`

## Важно

- В проект уже положен исправный `gradle/wrapper/gradle-wrapper.jar`.
- Для AGP 9.1.1 нужен JDK 17 и Gradle 9.3.1.
- Проект компилируется против Android SDK 36.1 (`compileSdk 36`, `minorApiLevel 1`).
- Debug APK не требует вашего release-keystore.
