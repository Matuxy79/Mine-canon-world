@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0launcher\Build-MineCanon.ps1" %*
if errorlevel 1 (
    echo.
    echo MineCanon build failed. See the error above.
    pause
    exit /b 1
)
echo.
echo Done. Double-click MineCanon.lnk in this folder to start.
pause
