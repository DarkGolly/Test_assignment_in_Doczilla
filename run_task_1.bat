@echo off
echo === Build WaterSortingProblem ===
call gradlew.bat :WaterSortingProblem:clean :WaterSortingProblem:build

if %ERRORLEVEL% neq 0 (
    echo Build failed
    pause
    exit /b %ERRORLEVEL%
)

echo === Run jar ===
java -jar WaterSortingProblem\build\libs\WaterSortingProblem-1.0-SNAPSHOT.jar

pause
