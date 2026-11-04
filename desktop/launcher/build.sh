#!/usr/bin/env bash
# Recompila o TechFlow.exe (lançador em Go). Precisa de Go 1.21+ e de dist/TechFlow.jar pronto.
set -euo pipefail
cd "$(dirname "$0")"
cp ../dist/TechFlow.jar TechFlow.jar
GOOS=windows GOARCH=amd64 CGO_ENABLED=0 go build -trimpath -ldflags "-s -w -H=windowsgui" -o ../TechFlow.exe .
rm -f TechFlow.jar
echo "Gerado: ../TechFlow.exe"
