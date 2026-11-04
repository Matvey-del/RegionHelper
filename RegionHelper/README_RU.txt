REGION HELPER — FABRIC 1.21.4
=============================

СБОРКА
------
Требуется полный JDK 21 и доступ в интернет при первой сборке.

Windows:
  build.bat
или:
  gradlew.bat clean build

Linux/macOS:
  ./gradlew clean build

Готовый мод:
  build/libs/region-helper-1.0.0.jar

ЗАВИСИМОСТИ ПРОЕКТА
-------------------
Minecraft:       1.21.4
Mappings:        Yarn 1.21.4+build.8
Fabric Loader:   0.19.3
Fabric API:      0.119.3+1.21.4
Fabric Loom:     1.9.2
Gradle:          8.12.1
Java:            21 (bytecode release 21)

WRAPPER
-------
В gradle/wrapper/gradle-wrapper.jar помещён переданный пользователем JAR.
Это кастомный bootstrap (не стандартный официальный Gradle Wrapper JAR): он
требует distributionSha256Sum и проверяет SHA-256 загружаемого Gradle ZIP.
Контрольная сумма Gradle 8.12.1 указана в gradle-wrapper.properties.

ВАЖНО О ПРОВЕРКЕ
----------------
Структура проекта, конфигурация, wrapper properties и контрольная сумма
проверены локально. Полную Gradle/Fabric-компиляцию в среде подготовки
выполнить не удалось: сетевой DNS недоступен, поэтому Gradle 8.12.1 и
зависимости Fabric/Minecraft не могут быть загружены. Не считайте этот архив
подтверждённо собранным, пока `build.bat` или `gradlew.bat build` не завершится
со строкой BUILD SUCCESSFUL на компьютере с доступом к репозиториям.

Если сборка упадёт, пришлите полный build.log — по нему можно исправить
конкретную несовместимость API/зависимостей.
