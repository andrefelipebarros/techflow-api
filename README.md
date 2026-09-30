# TechFlow API

API REST para gerenciar projetos e tarefas. Cada usuário enxerga apenas os próprios projetos e as tarefas deles.

## Stack

- Java 25 e Spring Boot 4.1
- Spring Security com JWT (resource server, assinatura HS256)
- Spring Data JPA e PostgreSQL, com migrations via Flyway
- Redis para cache da consulta de projeto
- Kafka (opcional) para publicar eventos de tarefa
- Swagger / OpenAPI
- JUnit 5 e Mockito
- Docker Compose e GitHub Actions

## Estrutura

```text
src/main/java/com/alipe/techflow
├── application      casos de uso (auth, project, task)
├── domain           entidades, enums e repositórios
├── infrastructure   segurança, mensageria e configuração
└── interfaces/rest  controllers e tratamento de erros
```

## Rodando localmente

Requisitos: JDK 25, Docker e Git.

```bash
docker compose up -d postgres redis
./mvnw spring-boot:run
```

Para habilitar o Kafka:

```bash
docker compose --profile messaging up -d postgres redis kafka
APP_KAFKA_ENABLED=true ./mvnw spring-boot:run
```

No Windows, use `./mvnw.cmd spring-boot:run`.

- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

## Endpoints

| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/api/v1/auth/register` | Cadastro |
| POST | `/api/v1/auth/login` | Login, retorna o token |
| GET | `/api/v1/users/me` | Usuário logado |
| POST, GET | `/api/v1/projects` | Criar e listar (paginado) |
| GET, PUT, DELETE | `/api/v1/projects/{id}` | Consultar, atualizar e remover |
| POST, GET | `/api/v1/projects/{projectId}/tasks` | Criar e listar tarefas do projeto |
| GET, PUT, DELETE | `/api/v1/tasks/{taskId}` | Consultar, atualizar e remover tarefa |
| PUT | `/api/v1/tasks/{taskId}/status` | Alterar status |
| GET | `/api/v1/admin/users`, `/api/v1/admin/summary` | Somente ADMIN |

A especificação completa está em `docs/openapi.yaml` e há uma collection em `docs/postman.json`.

## Exemplo

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"name": "Alice Silva", "email": "alice@example.com", "password": "Senha@123"}'
```

Use o `accessToken` retornado no header `Authorization: Bearer <token>`.

Para promover um usuário a ADMIN, veja `scripts/create-admin.sql`.

## Testes

```bash
./mvnw test
```

Hoje são testes unitários dos serviços (Mockito) e do controller (`@WebMvcTest`).

## CI/CD

O workflow `.github/workflows/ci-cd.yml` roda em pull requests e pushes:

1. testes e package com Maven
2. upload do JAR como artifact
3. build e publicação da imagem Docker no GitHub Container Registry (push na `main` ou tag `v*`)

O job de deploy por SSH só roda manualmente e se a variável `DEPLOY_ENABLED` estiver como `true`.

## Próximos passos

- Testes de integração com Testcontainers (PostgreSQL e Redis)
- Consumidor dos eventos de tarefa publicados no Kafka
- Filtros de listagem por status e prioridade
