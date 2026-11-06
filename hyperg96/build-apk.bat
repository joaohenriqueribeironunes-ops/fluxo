@echo off
echo ==========================================
echo   HyperG96 - Build APK Local
echo ==========================================
echo.

echo Verificando Java...
java -version 2>&1 | findstr /R /C:"17" >nul
if errorlevel 1 (
    echo ERRO: Java 17+ nao encontrado!
    echo Instale com: winget install EclipseAdoptium.Temurin.21.JDK
    pause
    exit /b 1
)
echo Java OK!
echo.

echo [1/1] Buildando APK (debug)...
cd /d "%~dp0"
call gradlew.bat assembleDebug --no-daemon
if errorlevel 1 (
    echo ERRO no build do APK
    pause
    exit /b 1
)

echo.
echo ==========================================
echo   SUCESSO! APK gerado em:
echo   app\build\outputs\apk\debug\app-debug.apk
echo ==========================================
echo.
echo Para instalar no celular:
echo   adb install app\build\outputs\apk\debug\app-debug.apk
echo   (ou copie o APK para o aparelho e abra pelo gerenciador)
echo.
pause
