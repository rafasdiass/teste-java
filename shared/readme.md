
# Serviço FIPE — Magnum Bank • README

Repositório: `git@github.com:rafasdiass/teste-java.git`
Autor: **Rafael de Souza Dias** · [LinkedIn](https://www.linkedin.com/in/rdrafaeldias/) · WhatsApp: **+55 85 99444-8858**

## 1) Visão Geral

* **Arquitetura**

    * **API-1**: façade REST, carga inicial FIPE, cache Redis, autenticação, producer de fila.
    * **API-2**: consumer da fila, integra FIPE, persiste em SQL, utilitários JWT.
* **Stack**: Java 17, Quarkus 3.6.x, Hibernate ORM + Panache, PostgreSQL/H2, RabbitMQ, Redis, SmallRye OpenAPI, JWT, Fault Tolerance.
* **Módulos**: `api-1`, `api-2`, `shared`.

Estrutura:

```
servico-fipe-parent/
├─ api-1/
├─ api-2/
└─ shared/
```

## 2) Rodando local (sem Docker)

### 2.1 Pré-requisitos

* Java 17+ e Maven 3.9+
* PostgreSQL 15+ (ou H2 em modo dev)
* Redis 7+
* RabbitMQ 3.x (com plugin management padrão)

Instalação rápida (macOS Homebrew):

```bash
brew install openjdk@17 maven postgresql redis rabbitmq
brew services start postgresql
brew services start redis
brew services start rabbitmq
```

Crie o banco:

```bash
psql -U postgres -c "CREATE DATABASE fipe;"
psql -U postgres -c "CREATE USER fipe WITH PASSWORD 'fipe';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE fipe TO fipe;"
```

### 2.2 Build do monorepo

```bash
mvn -q -U -DskipTests clean install
```

### 2.3 Executar API-1 (REST + cache + producer)

Opção A — passando propriedades via CLI:

```bash
cd api-1
mvn quarkus:dev \
  -Dquarkus.http.port=8080 \
  -Dquarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/fipe \
  -Dquarkus.datasource.username=fipe \
  -Dquarkus.datasource.password=fipe \
  -Dquarkus.redis.hosts=redis://localhost:6379
```

Opção B — arquivo `api-1/src/main/resources/application.properties`:

```properties
# HTTP
quarkus.http.port=8080

# DB
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/fipe
quarkus.datasource.username=fipe
quarkus.datasource.password=fipe
quarkus.hibernate-orm.database.generation=none

# Flyway (migra no start, se houver scripts)
quarkus.flyway.migrate-at-start=true

# Redis (cache)
quarkus.redis.hosts=redis://localhost:6379

# OpenAPI
quarkus.smallrye-openapi.path=/q/openapi

# Messaging (producer -> RabbitMQ)
mp.messaging.outgoing.marcas-out.connector=smallrye-rabbitmq
mp.messaging.outgoing.marcas-out.exchange.name=marcas
mp.messaging.outgoing.marcas-out.host=localhost
mp.messaging.outgoing.marcas-out.port=5672
mp.messaging.outgoing.marcas-out.username=guest
mp.messaging.outgoing.marcas-out.password=guest

# JWT (se aplicável nos endpoints protegidos)
mp.jwt.verify.issuer=https://magnum.local/issuer
```

Depois:

```bash
cd api-1 && mvn quarkus:dev
```

### 2.4 Executar API-2 (consumer + persistência)

Opção A — via CLI:

```bash
cd api-2
mvn quarkus:dev \
  -Dquarkus.http.port=8081 \
  -Dquarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/fipe \
  -Dquarkus.datasource.username=fipe \
  -Dquarkus.datasource.password=fipe
```

Opção B — `api-2/src/main/resources/application.properties`:

```properties
# HTTP
quarkus.http.port=8081

# DB
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/fipe
quarkus.datasource.username=fipe
quarkus.datasource.password=fipe
quarkus.hibernate-orm.database.generation=update

# Messaging (consumer <- RabbitMQ)
mp.messaging.incoming.marcas-in.connector=smallrye-rabbitmq
mp.messaging.incoming.marcas-in.queue.name=marcas.queue
mp.messaging.incoming.marcas-in.host=localhost
mp.messaging.incoming.marcas-in.port=5672
mp.messaging.incoming.marcas-in.username=guest
mp.messaging.incoming.marcas-in.password=guest

# FIPE client (exemplo de timeouts/retry)
fipe.processing.delay-between-requests=100
fipe.processing.max-retries=3
fipe.processing.retry-delay=5000

# JWT utils
mp.jwt.verify.issuer=https://magnum.local/issuer
jwt.token.expiration=PT1H
```

Depois:

```bash
cd api-2 && mvn quarkus:dev
```

### 2.5 Modo rápido sem Postgres (H2 em memória) — opcional

Para validações rápidas, você pode subir H2 localmente:

API-1 (CLI):

```bash
mvn quarkus:dev \
  -Dquarkus.datasource.db-kind=h2 \
  -Dquarkus.datasource.jdbc.url=jdbc:h2:mem:fipe;DB_CLOSE_DELAY=-1 \
  -Dquarkus.datasource.username=sa \
  -Dquarkus.datasource.password=sa
```

API-2 igual, trocando as mesmas chaves. Observação: dados se perdem ao parar o processo.

## 3) Uso dos endpoints

### 3.1 Swagger/OpenAPI

* API-1: `http://localhost:8080/q/swagger-ui`
* API-2: `http://localhost:8081/q/swagger-ui`

### 3.2 Fluxo típico

1. **Carga inicial de marcas** (API-1 → publica na fila):

```bash
curl -X POST "http://localhost:8080/api/v1/carga-inicial?tipoVeiculo=carros" \
  -H "Authorization: Bearer <TOKEN>"
```

2. **Consumer processa e persiste modelos** (API-2). Logs mostram progresso.

3. **Consultar marcas** (API-1):

```bash
curl "http://localhost:8080/api/v1/marcas?tipoVeiculo=carros&page=0&size=50" \
  -H "Authorization: Bearer <TOKEN>"
```

4. **Consultar modelos por marca** (API-1):

```bash
curl "http://localhost:8080/api/v1/marcas/59/modelos?page=0&size=50" \
  -H "Authorization: Bearer <TOKEN>"
```

5. **Atualizar modelo/observações** (API-1):

```bash
curl -X PUT "http://localhost:8080/api/v1/modelos/004557-0" \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"nome":"Gol 1.0 8v","observacoes":"Revisado em 2025-09-06"}'
```

## 4) Autenticação

* **JWT Bearer** via SmallRye JWT.
* API-2 expõe utilitário de geração de token via `AuthService` no código.
* Papéis sugeridos: `user`, `monitoring`, `fipe-access`, `fipe-admin`.

## 5) Cache

* **API-1** usa `quarkus-cache` + **Redis**.
* Configure `quarkus.redis.hosts=redis://localhost:6379`.
* Invalidação ocorre em updates e por TTL no serviço.

## 6) Testes

```bash
mvn test
```

* H2 em memória nos testes.
* REST-Assured para endpoints.
* Repositórios com Panache e cenários de integração FIPE stubáveis.

## 7) Decisões e trade-offs

* Dois serviços isolam latência da FIPE e falhas transitórias (fila).
* Idempotência por `codigoFipe` reduz duplicidade.
* Cache em Redis evita pressão de leitura.
* Riscos: rate limit FIPE mitigado com retry/backoff configuráveis.

## 8) Comandos úteis

```bash
# Build
mvn -DskipTests clean install

# API-1
(cd api-1 && mvn quarkus:dev)

# API-2
(cd api-2 && mvn quarkus:dev)
```

