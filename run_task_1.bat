@echo off
echo === Build project ===
call gradlew.bat clean build

if %ERRORLEVEL% neq 0 (
    echo Build failed
    pause
    exit /b %ERRORLEVEL%
)

echo === Run jar ===
java -jar WaterSortingProblem\build\libs\Test_assignment_in_Doczilla-1.0-SNAPSHOT.jar

pause
