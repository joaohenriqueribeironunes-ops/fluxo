@echo off
echo ==========================================
echo   Fluxo - Setup GitHub para Build APK
echo ==========================================
echo.

echo [1/5] Inicializando repositorio Git...
git init

echo [2/5] Adicionando arquivos...
git add .

echo [3/5] Commit inicial...
git commit -m "feat: Fluxo app completo com build APK via GitHub Actions"

echo [4/5] Renomeando branch para main...
git branch -M main

echo.
echo ==========================================
echo   PROXIMOS PASSOS (execute manualmente):
echo ==========================================
echo.
echo 1. Crie um repositorio no GitHub:
echo    https://github.com/new
echo    Nome: fluxo
echo    Nao marque "Add README" / .gitignore / license
echo.
echo 2. Conecte e faca push:
echo    git remote add origin https://github.com/SEU-USUARIO/fluxo.git
echo    git push -u origin main
echo.
echo 3. O GitHub Actions vai buildar automaticamente!
echo    Va em: https://github.com/SEU-USUARIO/fluxo/actions
echo    Clique no workflow "Build Android APK"
echo    Baixe o artefato "fluxo-debug-apk"
echo.
echo 4. Para criar release com APK anexado:
echo    git tag v1.0.0
echo    git push origin v1.0.0
echo.
echo ==========================================
pause