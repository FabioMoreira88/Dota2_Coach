# Dota 2 Coach

Coach pessoal de Dota 2: analisa o histórico público do jogador (heróis, roles, itens, aderência ao meta), indica até 3 focos de melhoria e acompanha a evolução a cada ciclo de treino.

> Projeto não afiliado à Valve. Dota 2 é marca registrada da Valve Corporation.

## Estrutura

```
backend/    Java 21 + Spring Boot 3.5, Maven multi-módulo (core, collector, analysis, coach-ai, api)
frontend/   Next.js + TypeScript (a partir da sprint 5)
infra/      docker-compose para desenvolvimento local
docs/adr/   decisões de arquitetura
docs/fase0/ relatórios da fase de validação
```

A organização dos pacotes e as regras de camadas estão em [docs/adr/0001-estrutura-backend.md](docs/adr/0001-estrutura-backend.md).

## Rodando localmente

Pré-requisitos: JDK 21, Docker Desktop, Node 22.

1. Copie `.env.example` para `.env` na raiz e preencha (o `.env` nunca vai para o Git).
2. Suba banco e Redis:
   ```
   docker compose --env-file .env -f infra/docker-compose.yml up -d
   ```
3. Rode a API:
   ```
   cd backend
   ./mvnw -pl api -am spring-boot:run
   ```
   No Windows use `mvnw.cmd`.
4. Teste: <http://localhost:8080/api/v1/ping> deve responder `{"status":"ok", ...}`.

## Testes

```
cd backend
./mvnw verify
```

## Fluxo de trabalho

Uma branch por tarefa, pull request pequeno, CI verde e aprovação antes do merge na `main`.
