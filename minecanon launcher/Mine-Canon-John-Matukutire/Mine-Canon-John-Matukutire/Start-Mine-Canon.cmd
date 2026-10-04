@echo off
setlocal
cd /d "%~dp0"
set "MINECANON_JAVA=java"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "MINECANON_JAVA=%JAVA_HOME%\bin\java.exe"
"%MINECANON_JAVA%" -jar "%~dp0MineCanonLauncher.jar"
if errorlevel 1 (
  echo.
  echo Mine Canon could not start. Install Java 17 or newer and extract the entire ZIP.
  echo Keep the Forge installer beside MineCanonLauncher.jar.
  pause
)
endlocal
