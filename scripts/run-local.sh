#!/usr/bin/env bash
set -euo pipefail

docker compose up -d postgres redis
./mvnw spring-boot:run
