#!/usr/bin/env bash
# Compila o TechFlow Desktop e gera dist/TechFlow.jar (precisa do JDK 17 ou mais novo).
set -euo pipefail
cd "$(dirname "$0")"

rm -rf out dist
mkdir -p out dist
javac -encoding UTF-8 --release 17 -d out $(find src -name '*.java')
jar --create --file dist/TechFlow.jar --main-class com.alipe.techflow.desktop.Main -C out .
echo "Pronto: dist/TechFlow.jar"

if [ "${1:-}" = "test" ]; then
  mkdir -p out-tests
  javac -encoding UTF-8 -cp out -d out-tests tests/SmokeTest.java
  java -cp out:out-tests SmokeTest
fi
