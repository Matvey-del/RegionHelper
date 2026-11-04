СБОРКА REGIONHELPER ДЛЯ MINECRAFT 1.21.4

ВАЖНО ДЛЯ WINDOWS 7
-------------------
На Windows 7 в этой конфигурации Gradle 8.12.1 завершается до компиляции:
"Could not initialize native services" / "gradle-fileevents.dll".
Это сбой нативного компонента Gradle, а не ошибка Java-кода мода.
Поэтому в архив добавлена сборка в GitHub Actions на Windows Server.

КАК СОБРАТЬ В ОБЛАКЕ
--------------------
1. Создай аккаунт на https://github.com/ (если его ещё нет).
2. Создай новый пустой репозиторий (New repository).
3. Распакуй этот архив на компьютере.
4. Загрузи ВСЁ содержимое папки RegionHelper в репозиторий GitHub.
   Важно: вместе с исходниками загрузи скрытую папку .github/workflows.
5. Открой репозиторий на GitHub -> вкладка Actions.
6. Выбери workflow "Build RegionHelper".
7. Нажми "Run workflow" -> "Run workflow".
8. Дождись зелёной галочки.
9. Открой завершённый запуск workflow. Внизу страницы в Artifacts
   скачай RegionHelper-Fabric-1.21.4.
10. Распакуй скачанный artifact. Внутри будет JAR из build/libs.

ЧТО ДЕЛАЕТ WORKFLOW
-------------------
- Использует Windows Server от GitHub Actions.
- Устанавливает JDK 21.
- Запускает Gradle 8.12.1 без файлового наблюдения.
- Выполняет clean build.
- Загружает собранные JAR-файлы как artifact.

ПРОВЕРКА ПРОЕКТА
----------------
Проверены структура архива, Gradle wrapper, build.gradle, gradle.properties,
settings.gradle, fabric.mod.json и наличие Java-исходников.
Полную компиляцию в этой среде выполнить не удалось: нет доступа к внешним
репозиториям Gradle/Fabric/Minecraft. Успешная сборка не заявляется до
прохождения GitHub Actions.

ПРИМЕЧАНИЕ
----------
Готовый модовый JAR можно перенести на Windows 7, если сама игра и её
загрузчик Fabric запускаются с совместимой Java. Для Minecraft 1.21.4
требуется Java 21; Windows 7 официально не поддерживается многими
современными сборками Java 21.
