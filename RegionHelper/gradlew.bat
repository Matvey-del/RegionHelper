@echo off
setlocal EnableExtensions
cd /d "%~dp0"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" goto java_home
where java >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Java 21 is not installed or not on PATH.
  exit /b 2
)
set "JAVA_CMD=java"
goto run
:java_home
set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
:run
"%JAVA_CMD%" -version
"%JAVA_CMD%" -classpath "gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%
