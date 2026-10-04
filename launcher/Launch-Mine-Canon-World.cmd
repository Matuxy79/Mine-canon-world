@echo off
setlocal
cd /d "%~dp0"
set "JAVA="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA=%JAVA_HOME%\bin\java.exe"
if not defined JAVA for /f "delims=" %%J in ('where java.exe 2^>nul') do if not defined JAVA set "JAVA=%%J"
if not defined JAVA (
    echo Mine Canon World requires a Java JDK 17 or newer.
    echo Install a JDK, or set JAVA_HOME to your Java installation.
    pause
    exit /b 1
)
"%JAVA%" "%~dp0MineCanonLauncher.java"
if errorlevel 1 (
    echo.
    echo The launcher could not start. A Java JDK 17 or newer is required.
    pause
    exit /b 1
)
