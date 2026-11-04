@echo off
rem Compila o TechFlow Desktop e gera dist\TechFlow.jar (precisa do JDK 17 ou mais novo).
setlocal
cd /d "%~dp0"

if exist out rmdir /s /q out
if exist dist rmdir /s /q dist
mkdir out
mkdir dist

dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 --release 17 -d out @sources.txt
if errorlevel 1 (
  del sources.txt
  echo Erro ao compilar.
  exit /b 1
)
del sources.txt

jar --create --file dist\TechFlow.jar --main-class com.alipe.techflow.desktop.Main -C out .
if errorlevel 1 exit /b 1
echo Pronto: dist\TechFlow.jar
