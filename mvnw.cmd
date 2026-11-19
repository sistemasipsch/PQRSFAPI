@REM ----------------------------------------------------------------------------
@REM Maven Wrapper Script for Windows
@REM ----------------------------------------------------------------------------
@echo off
setlocal

if "%JAVA_HOME%"=="" (
    if exist "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2.6.3\jbr" (
        set "JAVA_HOME=C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2.6.3\jbr"
    )
)

if exist "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2.6.3\plugins\maven\lib\maven3\bin\mvn.cmd" (
    call "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2.6.3\plugins\maven\lib\maven3\bin\mvn.cmd" %*
) else (
    mvn %*
)
