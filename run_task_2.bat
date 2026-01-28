@echo off
echo === Build FileSharingService ===
call gradlew.bat :FileSharingService:clean :FileSharingService:installDist

if %ERRORLEVEL% neq 0 (
    echo Build failed
    pause
    exit /b %ERRORLEVEL%
)

echo === Run app ===
call FileSharingService\build\install\FileSharingService\bin\FileSharingService.bat

pause
