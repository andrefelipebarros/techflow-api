@echo off
rem Gera o instalador TechFlow-1.0.0.exe (atalho no menu iniciar e na area de trabalho).
rem Precisa do JDK 21 e do WiX Toolset 3 no PATH (https://wixtoolset.org).
setlocal
cd /d "%~dp0\.."

call build.bat
if errorlevel 1 exit /b 1

jpackage --type exe ^
  --name TechFlow ^
  --input dist ^
  --main-jar TechFlow.jar ^
  --main-class com.alipe.techflow.desktop.Main ^
  --icon packaging\techflow.ico ^
  --app-version 1.0.0 ^
  --vendor "Andre Felipe de Barros" ^
  --description "Interface desktop da TechFlow API" ^
  --java-options "-Dfile.encoding=UTF-8" ^
  --win-menu --win-shortcut --win-dir-chooser ^
  --dest dist-exe
