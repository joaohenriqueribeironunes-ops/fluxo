@echo off
setlocal
python "%~dp0verificar_volume.py" %*
exit /b %ERRORLEVEL%
