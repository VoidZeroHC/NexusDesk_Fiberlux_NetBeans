@echo off
cd /d "%~dp0"
where java >nul 2>&1
if errorlevel 1 (echo Instale Java JDK 21 o superior.&pause&exit /b 1)
echo Iniciando NexusDesk Fiberlux... abre http://localhost:8090
java -jar NexusDesk_Fiberlux.jar
pause
