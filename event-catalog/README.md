# EventMaster - Sistema de Gestao de Eventos Escalavel (Java 17 + Maven)

## Relatorio Tecnico

### 1. Visao Geral

O EventMaster e a nova arquitetura proposta para substituir o sistema monolitico de venda de ingressos que sofre com:

- **Latencia** em picos de acesso durante grandes lancamentos
- **Inconsistencia** na base de dados
- **Vulnerabilidades** a ataques de injecao

---

## Fase 1: Design e Estrutura

### 1.1 Transicao Monolito para Microsservicos

```
+---------------------------------------------------------------------------+
|                              ANTES (Monolito)                             |
|   +-------------------------------------------------------------------+   |
|   |      UI + Catalogo + Vendas + Pagamento + Auth + Relatorios       |   |
|   |                          [DB unico]                               |   |
|   +-------------------------------------------------------------------+   |
+---------------------------------------------------------------------------+

                                || Migracao ||
                                     \/

+---------------------------------------------------------------------------+
|                          DEPOIS (Microsservicos)                          |
|                                                                           |
|  +--------+        +-------------------------------------------+          |
|  | Client |  -->   |        Cloudflare Waiting Room            |          |
|  +--------+        +---------------------+---------------------+          |
|                                          |                                |
|                                          v                                |
|                 +------------------------------------------------+        |
|                 |                    API Gateway                 |        |
|                 |          (Rate Limiting, Auth, Routing)        |        |
|                 +----+------------------+-------------------+----+        |
|                      |                  |                   |             |
|                      v                  v                   v             |
|                +----------+       +----------+         +----------+       |
|                | Catalogo |       |  Vendas/ |         |Relatorios|       |
|                |de Eventos|       | Pagamento|         | (Batch)  |       |
|                | (Ctx 1)  |       | (Ctx 2)  |         |          |       |
|                +----+-----+       +----+-----+         +----+-----+       |
|                     |                  |                    |             |
|                     |            +-----+------+             |             |
|                     v            v            v             v             |
|                +----------+ +----------+ +----------+  +----------+       |
|                |DB Catalog| |  REDIS   | | DB Vendas | |Data Lake |       |
|                |(Read-opt)| |  (TTL)   | | (Write-op)| |(S3/Batch)|       |
|                +----------+ +----------+ +----------+  +----------+       |
|                                                                           |
|                +--------------------------------------------------+       |
|                |           Message Broker (SQS/Kafka)             |       |
|                |        (Comunicacao assincrona entre svc)        |       |
|                +--------------------------------------------------+       |
+---------------------------------------------------------------------------+
```

### 1.2 DDD - Contextos Delimitados

| Contexto                               | Responsabilidade                                 | Entidades              |
| -------------------------------------- | ------------------------------------------------ | ---------------------- |
| **Catalogo de Eventos** (implementado) | CRUD de eventos, busca, disponibilidade          | Event                  |
| **Vendas/Pagamento** (futuro)          | Reserva de ingressos, processamento de pagamento | Order, Ticket, Payment |

### 1.3 Padroes de Resiliencia

**Cloudflare Waiting Room (Fila de Espera):**

- Camada mais externa de protecao, antes do API Gateway
- Ativa automaticamente em picos de trafego durante grandes lancamentos de ingressos
- Funcionalidades:
  - Controla o fluxo de requisicoes: permite apenas N usuarios por segundo
  - Mantem usuarios excedentes em uma fila virtual amigavel (spinner + contador)
  - Distribui o acesso de forma ordenatica (FIFO)
  - Valida bot/crawler automaticamente para evitar ataque de scraping
  - Retorna status em tempo real (posicao na fila, tempo estimado)
- Configuracao sugerida para lancamentos:
  - Capacidade: 500 requisicoes/segundo
  - Duracoes dinamicas conforme demanda

**API Gateway (Spring Cloud Gateway ou AWS API Gateway):**

- Ponto unico de entrada posicionado logo apos a Waiting Room
- Roteia `/events/**` para o servico de Catalogo e `/orders/**` para o servico de Vendas
- Autenticacao centralizada: valida o token JWT antes de encaminhar a requisicao
- Rate limiting: limita 1000 requisicoes/segundo por IP durante grandes lancamentos
- Protecao contra DDoS: bloqueia IPs com comportamento anomalo
- Exemplo de fluxo:

```
Cliente -> Cloudflare Waiting Room -> API Gateway (valida JWT, aplica rate limit) -> Microsservico
```

**Circuit Breaker (Resilience4j):**

- Aplicado na chamada do servico de Vendas ao gateway de pagamento externo (ex: Stripe, Adyen)
- Configuracao:
  - Janela deslizante: 10 chamadas
  - Threshold de falha: 50% (abre se 5 de 10 falharem)
  - Tempo em estado OPEN: 30 segundos
  - Chamadas de teste em HALF_OPEN: 3
- Fluxo de estados:

```
CLOSED (normal) -> falhas atingem 50% -> OPEN (rejeita tudo)
   -> espera 30s -> HALF_OPEN (permite 3 chamadas de teste)
      -> se passam -> volta pra CLOSED
      -> se falham -> volta pra OPEN
```

- Fallback quando aberto: retorna erro ao cliente e aciona compensacao SAGA (libera ingresso)

**Redis Redlock com TTL (Reserva de Ingressos):**

- Reserva ingressos durante o checkout com TTL de 7 minutos
- Evita double-selling: Cliente 1 tem 7 min para confirmar pagamento; Cliente 2 vê "Indisponível"
- Se confirmado: ingresso debitado; se TTL expira: volta ao estoque automaticamente
- Fallback: mutex em memória se Redis indisponível

**Padrao SAGA (Coreografia via eventos):**

- Resolve a consistencia entre o estoque de ingressos (Catalogo) e o pagamento (Vendas)
- Cada servico publica eventos e reage aos eventos dos outros
- Fluxo normal (sucesso):

```
1. Cliente solicita compra
2. Catalogo: decrementa estoque -> publica evento "IngressoReservado"
3. Vendas: recebe evento -> processa pagamento -> publica "PagamentoConfirmado"
4. Catalogo: recebe evento -> confirma venda (status SOLD)
```

- Fluxo de compensacao (falha no pagamento):

```
1. Vendas: pagamento falha -> publica evento "PagamentoFalhou"
2. Catalogo: recebe evento -> incrementa estoque (libera ingresso)
3. Cliente recebe notificacao de falha
```

- Dessa forma evitamos locks distribuidos e os servicos ficam desacoplados

---

## Fase 2: Processamento e Performance

| Tipo       | Caso de Uso                                                    | Tecnologia    |
| ---------- | -------------------------------------------------------------- | ------------- |
| **Lock**   | Distributed lock para evitar race condition na venda            | Redis Redlock |
| **Stream** | Monitoramento de acessos em tempo real, atualizacao de estoque | Kafka         |
| **Stream** | Deteccao de fraude em tempo real                               | Kafka Streams |
| **Batch**  | Relatorio financeiro diario para organizadores                 | Spark         |
| **Batch**  | Conciliacao de pagamentos                                      | Spring Batch  |

---

## Fase 3: Seguranca

### 3.1 Fluxo OAuth 2.0 / JWT

```
+--------+     +----------+     +----------+     +-----------+
| Client |---->| Auth Svc |---->| Identity |---->|API Gateway|
|        |     |(OAuth2.0)|     | Provider |     |(JWT Valid)|
+--------+     +----------+     +----------+     +-----------+
     |                                                  |
     |  1. Login (credentials)                          |
     |  2. Recebe access_token (JWT) + refresh_token    |
     |  3. Requisicoes com Bearer token                 |
     |  4. Gateway valida assinatura JWT (sem DB hit)   |
     +--------------------------------------------------+
```

**JWT Claims:** user_id, roles, exp, iss, aud

### 3.2 Protecao OWASP Top 10

| #   | Vulnerabilidade           | Mitigacao                                            |
| --- | ------------------------- | ---------------------------------------------------- |
| A01 | Broken Access Control     | RBAC + validacao em cada servico (Zero Trust)        |
| A02 | Cryptographic Failures    | TLS 1.3 entre servicos, AES-256 para dados sensiveis |
| A03 | Injection                 | Bean Validation, queries parametrizadas (JPA)        |
| A04 | Insecure Design           | Threat modeling, principio do menor privilegio       |
| A05 | Security Misconfiguration | IaC com Terraform, secrets no AWS Secrets Manager    |
| A06 | Vulnerable Components     | Dependabot, OWASP Dependency Check no Maven                |
| A07 | Auth Failures             | Rate limiting no login, MFA, tokens com TTL curto, Cloudflare WR |
| A08 | Data Integrity Failures   | Assinatura de payloads entre servicos (HMAC), Redis Redlock     |
| A09 | Logging Failures          | Logs estruturados centralizados (Datadog + SLF4J)    |
| A10 | SSRF                      | Whitelist de URLs, validacao de inputs de URL        |

**Zero Trust entre servicos:** mTLS + service mesh (cada chamada interna e autenticada).

---

## Fase 4: Qualidade e Testes

### 4.1 SOLID + Design Pattern

O microsservico de Catalogo de Eventos implementa:

- **SRP:** Cada classe tem uma responsabilidade (Entity, UseCase, Repository, Controller)
- **OCP:** Novos use cases podem ser adicionados sem alterar os existentes
- **LSP:** O adapter JpaEventRepositoryAdapter substitui qualquer EventRepository
- **ISP:** Interface EventRepository e enxuta (save, findById, findAll, findByCategory)
- **DIP:** Use cases dependem da interface EventRepository, nao da implementacao JPA

**Design Pattern:** Repository Pattern (GoF) para abstrair o acesso a dados.

**Lombok:** Utilizado na camada de infraestrutura (EventEntity) para reduzir boilerplate (@Getter, @Setter, @Builder, @NoArgsConstructor, @AllArgsConstructor).

### 4.2 Piramide de Testes

```
        /  E2E  \          <- Poucos (fluxo completo)
       /  Integr. \        <- Medios (API + DB)
      / Unitarios   \      <- Muitos (logica de negocio)
```

- **Unitarios (TDD):** JUnit 5 - validacoes de entidade, controle de estoque, use cases
- **BDD:** Cucumber + Gherkin - cenarios de cadastro e busca de eventos

---

## Decisoes Arquiteturais - Justificativa

| Decisao              | Impacto Escalabilidade           | Impacto Manutencao                   |
| -------------------- | -------------------------------- | ------------------------------------ |
| Microsservicos       | Scale independente por servico   | Deploy isolado, menor blast radius   |
| Repository Pattern   | Extensivel com novos adapters    | Troca de banco sem alterar dominio   |
| Clean Architecture   | Testabilidade                    | Independencia de frameworks          |
| Spring Boot 3        | Auto-scaling com containers      | Ecossistema maduro, comunidade ativa |
| Cloudflare WR        | Controla picos de trafego        | Nao requer config em cada microsvc   |
| Redis Redlock        | Evita race conditions escalavel  | TTL automatico, sem deadlocks        |

---

## Microsservico Implementado: Catalogo de Eventos (event-catalog)

Este e o microsservico completo do projeto. Ele gerencia o cadastro e consulta de eventos.

**Funcionalidades:**

1. Criar evento (com validacoes de dominio e publicacao automatica)
2. Listar todos os eventos
3. Buscar evento por ID
4. Filtrar eventos por categoria

**Endpoints:**

| Metodo | Rota                    | O que faz                     |
| ------ | ----------------------- | ----------------------------- |
| POST   | /events                 | Cria e publica um evento      |
| GET    | /events                 | Lista todos os eventos        |
| GET    | /events?category=TEATRO | Filtra eventos por categoria  |
| GET    | /events/{id}            | Busca evento por ID           |
| GET    | /events/health          | Verifica se o servico esta ok |

**Conceitos aplicados no codigo:**

- **Repository Pattern** — abstrai acesso a dados (interface no dominio, JPA na infra)
- **Clean Architecture** — dominio nao depende de Spring, JPA ou HTTP
- **DIP (SOLID)** — use cases dependem de interfaces, nao de implementacoes concretas
- **Validacoes de dominio** — regras de negocio na entidade (nome obrigatorio, preco >= 0, estoque > 0)

---

## Como Executar

### Pre-requisitos

- Java 17 (JDK)
- git

#### Instalacao dos pre-requisitos

1. Baixe e instale o JDK 17
2. Baixe e instale o Git

Para verificar se tudo esta instalado:

```cmd
java -version
git --version
```

### Passo a Passo

```cmd
git clone <url-do-repositorio>
cd event-catalog
```

Clona o projeto e entra na pasta do microsservico.

```cmd
mvnw.cmd spring-boot:run
```

Inicia a aplicacao na porta 8081. O banco H2 em memoria e criado automaticamente com dados de exemplo.

Em outro terminal, testar a API:

```cmd
curl http://localhost:8081/events/health
```

Verifica se o servico esta no ar.

```cmd
curl http://localhost:8081/events
```

Lista todos os eventos cadastrados.

```cmd
curl -X POST http://localhost:8081/events -H "Content-Type: application/json" -d "{\"name\": \"Romeu e Julieta\", \"venue\": \"Teatro FAAP\", \"date\": \"2025-12-01T20:00:00\", \"totalTickets\": 400, \"price\": 90.0, \"category\": \"TEATRO\"}"
```

Cria um novo evento.

```cmd
curl "http://localhost:8081/events?category=TEATRO"
```

Filtra eventos por categoria.

Rodar os testes:

```cmd
mvnw.cmd test
```

Executa todos os testes (unitarios + BDD).

```cmd
mvnw.cmd verify
```

Executa testes e gera relatorio de cobertura (JaCoCo) em target/site/jacoco/index.html.

### Banco de Dados

O projeto usa H2 em memoria. O banco e criado automaticamente ao iniciar a aplicacao com dados de exemplo (5 eventos de teatro). Os dados sao perdidos ao parar a aplicacao.

Console H2 disponivel em: http://localhost:8081/h2-console

- JDBC URL: `jdbc:h2:mem:eventmaster`
- User: `sa`
- Password: (vazio)

---

## Estrutura do Projeto

```
event-catalog/
├── README.md                          # Relatorio tecnico
├── .gitignore
├── mvnw                               # Maven Wrapper (Linux/macOS)
├── mvnw.cmd                           # Maven Wrapper (Windows)
├── .mvn/                              # Config do Maven Wrapper
├── pom.xml                            # Spring Boot 3 + JPA + Lombok + Cucumber + JaCoCo
├── src/
    ├── main/
    │   ├── java/com/eventmaster/catalog/
    │   │   ├── EventCatalogApplication.java    # Main (Spring Boot)
    │   │   ├── domain/
    │   │   │   ├── entity/
    │   │   │   │   ├── Event.java              # Entidade com regras de negocio
    │   │   │   │   └── EventStatus.java        # Enum (DRAFT, PUBLISHED, etc)
    │   │   │   ├── exception/
    │   │   │   │   ├── DomainValidationException.java
    │   │   │   │   ├── BusinessRuleException.java
    │   │   │   │   └── EventNotFoundException.java
    │   │   │   └── repository/
    │   │   │       └── EventRepository.java    # Interface (port)
    │   │   ├── application/
    │   │   │   ├── usecase/
    │   │   │   │   ├── CreateEventUseCase.java
    │   │   │   │   └── SearchEventsUseCase.java
    │   │   │   └── dto/
    │   │   │       ├── CreateEventRequest.java
    │   │   │       └── EventResponse.java
    │   │   └── infrastructure/
    │   │       ├── repository/
    │   │       │   ├── EventEntity.java              # JPA Entity (Lombok)
    │   │       │   ├── JpaEventRepository.java       # Spring Data
    │   │       │   └── JpaEventRepositoryAdapter.java # Adapter
    │   │       └── api/
    │   │           ├── EventController.java           # REST Controller
    │   │           └── GlobalExceptionHandler.java    # Tratamento de erros
    │   └── resources/
    │       ├── application.yml                 # Config H2
    │       └── data.sql                        # Dados iniciais (5 eventos)
    └── test/
        ├── java/com/eventmaster/catalog/
        │   ├── unit/
        │   │   ├── EventTest.java              # Testes de entidade (TDD)
        │   │   └── UseCaseTest.java            # Testes de use case (TDD)
        │   ├── integration/
        │   │   └── EventControllerIntegrationTest.java # Teste de integracao
        │   └── bdd/
        │       ├── CatalogStepDefinitions.java # Step definitions
        │       └── CucumberTest.java           # Runner
        └── resources/
            ├── application.yml                 # Config H2 (testes)
            └── features/
                └── catalog.feature             # Cenarios BDD (Gherkin)
```
