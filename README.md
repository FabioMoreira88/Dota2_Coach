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
5. Resumo público de um jogador pela OpenDota (account id de 32 bits, o "ID de amigo" do Dota):
   <http://localhost:8080/api/v1/players/34839344/summary>
6. Login pela Steam: abra <http://localhost:8080/api/v1/auth/steam/login>, entre na Steam e, de volta
   (o front ainda não existe, então a página de retorno em `localhost:3000` não abre), acesse
   <http://localhost:8080/api/v1/me> no mesmo navegador para ver seu nome e medalha.

## Raio-X do perfil (linha de comando)

Gera um relatório em Markdown com heróis, funções, rotas, fundamentos, itens e aderência ao meta de uma conta,
usando só dados públicos da OpenDota. Não precisa de banco nem do Redis. Na pasta `backend`:

```
./mvnw -pl api -am spring-boot:run "-Dspring-boot.run.arguments=--spring.main.web-application-type=none --dota2coach.raio-x.account-id=34839344"
```

O relatório sai em `docs/fase0/raio-x-34839344.md`. Opções extras, no mesmo `arguments`:

- `--dota2coach.raio-x.output=caminho.md`: outro arquivo de saída (relativo a `backend/api`).
- `--dota2coach.raio-x.parse-limit=0`: não pedir análise de replays (padrão: até 20 partidas recentes).

Na primeira execução o comando pede à OpenDota a análise dos replays recentes ainda não analisados. Rode de novo
uns 15 minutos depois para que rotas e itens dessas partidas entrem no relatório.

## Testes

```
cd backend
./mvnw verify
```

## Fluxo de trabalho

Uma branch por tarefa, pull request pequeno, CI verde e aprovação antes do merge na `main`.
