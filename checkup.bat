@echo off
rem ============================================================
rem  oop-checkup launcher
rem  NOTE: keep this file ASCII-only.
rem  CMD parses .bat with the system ANSI code page (GBK on
rem  Chinese Windows), so UTF-8 Chinese here would break parsing.
rem ============================================================
setlocal

chcp 65001 >nul 2>&1

set "JAR=%~dp0target\oop-checkup.jar"

if not exist "%JAR%" (
    echo.
    echo [ERROR] jar not found: %JAR%
    echo Run "mvn package" in the project directory first.
    echo.
    exit /b 1
)

if "%~1"=="" (
    echo.
    echo   oop-checkup
    echo.
    echo   Usage:  checkup ^<project-path^> [options]
    echo.
    echo   Options:
    echo     --detail N        expand at most N findings per item ^(default 3^)
    echo     --include-tests   include test directories
    echo     --summary         one-line summary
    echo     --batch           treat each subdirectory as a project
    echo.
    echo   Examples:
    echo     checkup examples\before
    echo     checkup "<project-path>" --detail 20
    echo     checkup "<project-path>" --detail 20 ^> report.txt
    echo.
    exit /b 0
)

java -Dfile.encoding=UTF-8 -jar "%JAR%" %*

endlocal
