# ADR 0001: Estrutura do back-end

- **Data:** 10/11/2026
- **Status:** aceita

## Decisão

Back-end em **Java 21 + Spring Boot 3.5**, build com **Maven multi-módulo** (POM pai em `backend/pom.xml`), organizado em camadas MVC com pacotes bem separados.

Maven foi escolhido no lugar de Gradle porque é a ferramenta que o mantenedor (Fabio) já domina; para um projeto deste tamanho a diferença de velocidade de build não compensa.

## Módulos

| Módulo | Responsabilidade | Depende de |
| --- | --- | --- |
| `core` | Domínio: entidades, repositórios, serviços de negócio, enums | — |
| `collector` | Clientes HTTP externos (OpenDota, STRATZ, Steam) e jobs de coleta | `core` |
| `analysis` | Métricas, meta, focos e comparação de ciclos de evolução | `core` |
| `coach-ai` | Prompts e cliente da API de IA | `core` |
| `api` | Aplicação Spring Boot: controllers, DTOs, segurança, configuração | todos |

## Pacotes (raiz `com.dota2coach`)

```
api/
  controller/          REST controllers: recebem request, chamam service, devolvem DTO
  dto/request/         contratos de entrada (records com Bean Validation)
  dto/response/        contratos de saída (records)
  mapper/              conversão entidade <-> DTO
  security/            login Steam OpenID, sessão, filtros
  config/              beans e @ConfigurationProperties
  exception/           @RestControllerAdvice e formato padrão de erro
core/
  model/               entidades JPA
  repository/          Spring Data repositories
  service/             regras de negócio
  enums/               Role, GameMode, FocusStatus, Lane...
  exception/           exceções de negócio
collector/
  client/opendota/     cliente OpenDota
  client/steam/        cliente Steam Web API
  dto/                 formato das respostas externas (inclui enums/códigos externos)
  mapper/              conversão resposta externa -> entidade do core
  job/                 sincronização em fila
analysis/
  metric/  meta/  focus/  evolution/
coachai/
  prompt/  client/  service/
```

## Regras

1. Controller nunca acessa repository diretamente; sempre passa por um service.
2. Entidade JPA nunca sai da API: a resposta é sempre um DTO.
3. Regra de negócio fica em service, não em controller nem em entidade anêmica com lógica espalhada.
4. Dependências só apontam para `core`; `core` não conhece web, HTTP externo nem IA.
5. Formatos de APIs externas ficam em `collector/dto` e são convertidos antes de entrar no domínio.
6. Injeção por construtor; nada de `@Autowired` em campo.
