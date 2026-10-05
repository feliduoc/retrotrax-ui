@echo off
rem Genera dist\RetroTrax\RetroTrax.exe (carpeta portable, no necesita Java instalado)
rem Requisitos: JDK 21 completo (con jpackage). Maven en el PATH es opcional:
rem si no esta, usa el jar que generes desde IntelliJ (panel Maven, Lifecycle, package).
rem Ejecutar desde la carpeta del proyecto.

where mvn >nul 2>nul
if %errorlevel%==0 (
  call mvn -q clean package || goto :error
) else (
  echo No encontre Maven en el PATH. Uso el jar que ya generaste desde IntelliJ.
)

if not exist target\ea-retrotrax-ui-1.1.jar (
  echo Falta target\ea-retrotrax-ui-1.1.jar. Genera el jar primero desde IntelliJ: panel Maven, Lifecycle, clean y package.
  goto :error
)

rmdir /s /q dist 2>nul
rmdir /s /q build-input 2>nul
mkdir build-input
copy /y target\ea-retrotrax-ui-1.1.jar build-input\ >nul

rem Si hay un icon.ico junto a este archivo, se usa como icono del .exe
set ICON=
if exist icon.ico set ICON=--icon icon.ico

jpackage --type app-image ^
  --name RetroTrax ^
  --input build-input ^
  --main-jar ea-retrotrax-ui-1.1.jar ^
  --main-class cl.feliminish.Main ^
  --dest dist ^
  %ICON% ^
  --java-options "-Dfile.encoding=UTF-8" || goto :error

rmdir /s /q build-input
echo.
echo Listo: dist\RetroTrax\RetroTrax.exe
exit /b 0

:error
echo.
echo Algo fallo. Revisa el mensaje de arriba.
exit /b 1
