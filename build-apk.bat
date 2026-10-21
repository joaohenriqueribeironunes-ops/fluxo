@echo off
echo ==========================================
echo   Fluxo - Build APK Local
echo ==========================================
echo.

echo Verificando Java...
java -version 2>&1 | findstr /R "17 18 19 20 21 22 23" >nul
if errorlevel 1 (
    echo ERRO: Java 17+ nao encontrado!
    echo.
    echo Instale com:
    echo   winget install EclipseAdoptium.Temurin.21.JDK
    echo.
    echo Ou baixe em: https://adoptium.net/temurin/releases/
    pause
    exit /b 1
)

echo Java OK!
echo.

echo [1/3] Buildando web app...
npm run build
if errorlevel 1 (
    echo ERRO no build web
    pause
    exit /b 1
)

echo [2/3] Sincronizando Capacitor...
npx cap sync android
if errorlevel 1 (
    echo ERRO no sync
    pause
    exit /b 1
)

echo [3/3] Buildando APK...
cd android
./gradlew assembleDebug --no-daemon
if errorlevel 1 (
    echo ERRO no build APK
    cd ..
    pause
    exit /b 1
)
cd ..

echo.
echo ==========================================
echo   SUCESSO! APK gerado em:
echo   android\app\build\outputs\apk\debug\app-debug.apk
echo ==========================================
echo.
echo Para instalar no celular (modo desenvolvedor ativo):
echo   adb install android\app\build\outputs\apk\debug\app-debug.apk
echo.
pause