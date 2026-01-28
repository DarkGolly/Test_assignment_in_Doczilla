# Test_assignment_in_Doczilla

Многомодульный проект Gradle, состоящий из трех независимых Java-приложений.

## Projects

### WaterSortingProblem
Консольное приложение, которое решает головоломку сортировки воды для заданного начального состояния, выводит начальное и конечное состояния пробирки и последовательность ходов.
### FileSharingService
Сервис обмена файлами по протоколу HTTP с аутентификацией на основе токенов, конечными точками для загрузки/скачивания и небольшим пользовательским интерфейсом на HTML/JS. Файлы хранятся в `uploads/`, а метаданные — во встроенной базе данных H2 в `data/`.
Порт по умолчанию: 8080 (можно переопределить с помощью `PORT`).

### WeatherService
HTTP-сервис, получающий данные о погоде из Open-Meteo, кэширующий результаты на 15 минут и отображающий HTML или JSON.
Используйте `/weather?city=CityName` и `format=json`.
Порт по умолчанию: 8080 (можно переопределить с помощью `PORT`).

Примечание: оба сервиса по умолчанию используют порт 8080. Если вы запускаете их одновременно, установите `PORT` для одного из них.

## How to run

> Для удобства все команды выполняются из корневого каталога репозитория.

### WaterSortingProblem

Windows:
```bat
run_task_1.bat
```

Linux/macOS:
```bash
./gradlew :WaterSortingProblem:clean :WaterSortingProblem:build
java -jar WaterSortingProblem/build/libs/WaterSortingProblem-1.0-SNAPSHOT.jar
```

### FileSharingService

Windows:
```bat
run_task_2.bat
```

Linux/macOS:
```bash
./gradlew :FileSharingService:clean :FileSharingService:installDist
./FileSharingService/build/install/FileSharingService/bin/FileSharingService
```

Открыть в браузере:
- http://localhost:8080/

### WeatherService

Windows:
```bat
run_task_3.bat
```

Linux/macOS:
```bash
./gradlew :WeatherService:clean :WeatherService:installDist
./WeatherService/build/install/WeatherService/bin/WeatherService
```

Примеры запросов:
- http://localhost:8080/weather?city=London
- http://localhost:8080/weather?city=London&format=json
