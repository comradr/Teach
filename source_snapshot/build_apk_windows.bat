@echo off
setlocal
cd /d "%~dp0"

echo ========================================
echo MKSH Planner - debug APK build
echo ========================================

if not exist .env (
  copy /Y .env.example .env >nul
  echo Created .env from .env.example.
  echo Put your real GEMINI_API_KEY into .env if needed.
)

call gradlew.bat --no-daemon assembleDebug
if errorlevel 1 (
  echo.
  echo BUILD FAILED
  exit /b 1
)

copy /Y "app\build\outputs\apk\debug\app-debug.apk" "MKSH-Planner-debug.apk" >nul

echo.
echo BUILD SUCCESSFUL
echo APK: %CD%\MKSH-Planner-debug.apk
endlocal
