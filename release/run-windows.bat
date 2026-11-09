@echo off
if exist "%~dp0app\bin\lwjgl3.bat" (
  call "%~dp0app\bin\lwjgl3.bat" %*
) else (
  java -jar "%~dp0roulette-runner-1.0.0.jar" %*
)
