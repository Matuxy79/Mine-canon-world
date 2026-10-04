@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Install-MineCanon.ps1"
if errorlevel 1 (
    echo.
    echo MineCanon installation failed. See the error above.
    pause
    exit /b 1
)
echo.
echo MineCanon is ready. Open the MineCanon shortcut on your desktop.
pause
