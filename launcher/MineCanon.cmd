@echo off
setlocal
if exist "%~dp0dist\MineCanon\MineCanon.exe" (
    start "" "%~dp0dist\MineCanon\MineCanon.exe"
    exit /b 0
)
call "%~dp0Launch-Mine-Canon-World.cmd"
