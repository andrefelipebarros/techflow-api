# TechFlow API

API REST para gerenciar projetos e tarefas. Cada usuário enxerga apenas os próprios projetos e as tarefas deles.

![Java](https://img.shields.io/badge/Java-25-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-database-blue)
![Redis](https://img.shields.io/badge/Redis-cache-red)
![Docker](https://img.shields.io/badge/Docker-compose-2496ED)

## Por que criei este projeto

Estou me preparando para começar minha carreira como desenvolvedor Java backend júnior. Percebi que
seguir tutoriais isolados não me mostrava como as peças se encaixam num sistema de verdade, então
decidi construir um projeto completo e praticar tudo junto: autenticação com JWT, banco relacional com
migrations, cache com Redis, mensageria com Kafka, testes e pipeline de CI/CD.

Escolhi gerenciamento de projetos e tarefas porque já usei Trello, Jira e quadros Kanban no dia a dia
e queria entender como esse tipo de sistema funciona por dentro, do cadastro do usuário até a mudança
de status de uma tarefa.

A ideia nunca foi só "fazer funcionar". Cada decisão técnica abaixo nasceu de uma dúvida que precisei
resolver, e o projeto continua evoluindo conforme eu aprendo.

## O que eu queria aprender

- Isolar dados por usuário, garantindo que cada um veja apenas o que é seu
- Organizar o código em camadas (application, domain, infrastructure, interfaces)
- Usar cache onde faz sentido e entender o custo de invalidá-lo
- Publicar eventos com Kafka sem tornar a aplicação dependente dele
- Montar um pipeline de CI/CD de ponta a ponta, do teste até a imagem Docker

## Decisões e desafios

**Decisões**

- **Kafka opcional:** quis que qualquer pessoa conseguisse rodar o projeto só com PostgreSQL e Redis.
  Por isso a publicação de eventos fica desligada por padrão e é ativada com `APP_KAFKA_ENABLED=true`.
- **Cache só na consulta de projeto:** é a leitura mais repetida, então é onde o ganho é mais claro.
  Cachear tudo aumentaria a complexidade de invalidação sem benefício proporcional.
- **Flyway em vez de deixar o Hibernate criar as tabelas:** o schema fica versionado e reproduzível,
  como seria num ambiente real.
- **Arquitetura em camadas:** separar casos de uso, domínio, infraestrutura e controllers deixa o
  código mais fácil de testar e de evoluir.
- **Deploy manual e protegido por variável:** o job de deploy só roda quando eu disparo e com
  `DEPLOY_ENABLED=true`, para evitar publicação acidental.

**Desafios**

- **Isolamento por usuário:** garantir que ninguém acesse projetos ou tarefas de outra pessoa,
  mesmo conhecendo o ID, exigiu validar o dono do recurso em cada operação.
- **Spring Security como resource server com JWT (HS256):** entender o fluxo de validação do token
  e como o usuário autenticado chega até os casos de uso.
- **Coerência do cache:** definir quando invalidar a consulta de projeto ao atualizar ou remover.
- **Ambiente local reproduzível:** fazer PostgreSQL, Redis e Kafka subirem com Docker Compose,
  com o Kafka em um profile separado.

**O que faria diferente hoje:** começaria com Testcontainers desde o primeiro dia, para testar a
integração com PostgreSQL e Redis em vez de depender só de testes unitários.

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
  -d '{"name": "teste nome", "email": "teste@example.com", "password": "Senha@123"}'
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

## Roadmap

- [x] Autenticação com JWT e controle de acesso por usuário
- [x] CRUD de projetos e tarefas com paginação
- [x] Cache com Redis e eventos opcionais com Kafka
- [x] Pipeline de CI/CD com GitHub Actions
- [ ] Testes de integração com Testcontainers (PostgreSQL e Redis)
- [ ] Consumidor dos eventos de tarefa publicados no Kafka
- [ ] Filtros de listagem por status e prioridade

## Autor

Feito por André Felipe de Barros.

GitHub: [andrefelipebarros](https://github.com/andrefelipebarros)
