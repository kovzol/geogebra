@echo off
setlocal
set "DIRNAME=%~dp0"
if "%DIRNAME%" == "" set "DIRNAME=.\"
set "APP_HOME=%DIRNAME%.."
set "JAVA_HOME=%APP_HOME%\jre"

set "CP=%APP_HOME%\lib\*"
set "MAIN=org.geogebra.desktop.GeoGebra3D"
set "VM_ARGS=-Dnashorn.args=--no-deprecation-warning"

"%JAVA_HOME%\bin\java.exe" -cp "%CP%" %VM_ARGS% %MAIN% %*

:: If exit code was non-zero (crash/error), pause so the user can read the console
if %ERRORLEVEL% neq 0 (
    echo.
    echo Application exited with error code %ERRORLEVEL%.
    pause
)

endlocal