@echo off
rem Gera o TechFlow.exe com o Java embutido (nao precisa de Java instalado para abrir).
rem Precisa rodar no Windows, com o JDK 21 ou mais novo no PATH (jpackage vem nele).
setlocal
cd /d "%~dp0\.."

call build.bat
if errorlevel 1 exit /b 1

if exist dist-exe rmdir /s /q dist-exe

jpackage --type app-image ^
  --name TechFlow ^
  --input dist ^
  --main-jar TechFlow.jar ^
  --main-class com.alipe.techflow.desktop.Main ^
  --icon packaging\techflow.ico ^
  --app-version 1.0.0 ^
  --vendor "Andre Felipe de Barros" ^
  --description "Interface desktop da TechFlow API" ^
  --java-options "-Dfile.encoding=UTF-8" ^
  --dest dist-exe
if errorlevel 1 exit /b 1

echo.
echo Pronto: dist-exe\TechFlow\TechFlow.exe
echo Para um instalador, instale o WiX Toolset 3 e rode packaging\build-installer.bat
