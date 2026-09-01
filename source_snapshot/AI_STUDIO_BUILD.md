# MKSH Planner — AI Studio handoff

Это цельный Android/Gradle-проект, без GitHub overlay-патчей.

## Что уже внутри
- актуальные исправления Gemini/API;
- API key берётся из `.env` через Secrets Gradle Plugin;
- fallback на IPv4 при ответе Google `User location is not supported for the API use`;
- настройки Gemini убраны из UI;
- в верхней части остаются `Шаблоны` и `Очистить`;
- Room / Compose / фото / голос / генератор и существующие функции проекта сохранены;
- Firebase/iText-зависимости, которые не использовались, убраны.

## Сборка
Использовать JDK 17.

Debug APK:
```bash
./gradlew testDebugUnitTest assembleDebug
```

APK появится здесь:
`app/build/outputs/apk/debug/app-debug.apk`

## Важно
`.env` содержит личный Gemini API key владельца проекта. Не публикуйте этот ZIP или `.env` в открытом репозитории.
