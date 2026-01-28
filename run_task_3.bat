@echo off
echo === Build WeatherService ===
call gradlew.bat :WeatherService:clean :WeatherService:installDist

if %ERRORLEVEL% neq 0 (
    echo Build failed
    pause
    exit /b %ERRORLEVEL%
)

echo === Run app ===
call WeatherService\build\install\WeatherService\bin\WeatherService.bat

pause
