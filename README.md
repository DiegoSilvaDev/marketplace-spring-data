# 🎟️ Event Marketplace

Aplicação backend de um marketplace de eventos construída com **Java 25** e **Spring Boot 4.1.2**, aplicando **Arquitetura Modular (Modular Monolith)**, **Clean / Layered Architecture**, **Persistência Poliglota** e **Comunicação Orientada a Eventos**.

## 🏗️ Arquitetura e Módulos

O sistema é estruturado em contextos delimitados (*Bounded Contexts*) independentes dentro de `br.com.ia.marketplace`, com isolamento completo de bancos de dados, configurações de persistência dedicadas e integração assíncrona via eventos:

* **`registration`**: Gerenciamento e cadastro de clientes. Após persistir um novo cliente, publica o evento de integração `CustomerCreated`.

* **`catalog`**: Gestão de eventos, setores, assentos e vitrine pública (*Showcase*). Combina persistência relacional para dados estruturados do evento com persistência orientada a documentos para metadados flexíveis e cache distribuído para otimizar consultas de leitura. Publica `EventUpdated` quando os metadados e assentos são cadastrados ou atualizados.

* **`ticketing`**: Operação de ingressos, projeção local de clientes, eventos, setores e assentos, além da seleção de lugares com bloqueio distribuído (*Locking*) para evitar reservas concorrentes.

* **`common`**: Contratos compartilhados de eventos de integração (`DTOs`) que desacoplam publicadores e consumidores entre os módulos.

## 🗄️ Persistência Poliglota & Infraestrutura

Cada módulo utiliza a tecnologia de armazenamento mais adequada ao seu domínio, orquestrada via **Docker Compose** (`compose.yml`) com gerenciamento de ciclo de vida integrado ao Spring Boot (`spring.docker.compose.lifecycle-management=start-only`):

| Módulo / Finalidade | Serviço (Compose) | Tecnologia | Porta (Host) | Detalhes | 
| ----- | ----- | ----- | ----- | ----- | 
| **`registration`** | `registration-database` | MySQL 9.6 | `3307` | Database: `registration` | 
| **`catalog` (Eventos)** | `catalog-database` | MySQL 9.6 | `3308` | Database: `catalog` | 
| **`catalog` (Metadados)** | `catalog-metadata-database` | MongoDB 8.2 | `27018` | UUIDs padrão (`standard`) | 
| **`catalog` (Cache)** | `catalog-cache` | Redis 8.6 (Jedis) | `6380` | Cache distribuído da vitrine (*Showcase*) | 
| **`ticketing` (Dados)** | `ticketing-database` | PostgreSQL 18.3 | `5433` | Database: `ticketing` | 
| **`ticketing` (Locking)** | `ticketing-locking` | Redis 8.6 (Jedis) | `6381` | Lock distribuído de reserva de assentos | 

## 🔄 Fluxo Principal de Integração e Reserva

1. **Cadastro de Cliente (`registration`** $\rightarrow$ **`ticketing`):**

   * Um novo cliente é criado via `POST /customers` no MySQL.

   * O evento `CustomerCreated` é publicado e consumido assincronamente pelo `TicketingEventListener`, replicando o cliente no PostgreSQL do módulo `ticketing`.

2. **Criação de Evento e Metadados (`catalog`** $\rightarrow$ **`ticketing`):**

   * O evento base é cadastrado via `POST /events` no MySQL.

   * Os detalhes técnicos, setores e assentos são salvos via `POST /eventMetadatas` no MongoDB.

   * O evento `EventUpdated` é disparado e processado pelo `CreateEventUseCase` no `ticketing`, persistindo a hierarquia relacional de `Event`, `Sector` e `Seat` no PostgreSQL.

3. **Consulta da Vitrine (`catalog`):**

   * O endpoint `GET /showcase` consolida os dados dos eventos disponíveis utilizando cache distribuído no Redis (`6380`).

4. **Seleção e Bloqueio de Assento (`ticketing`):**

   * O cliente solicita a reserva temporária via `POST /ticketing/events/{eventId}/seats/select` enviando o header `X-CUSTOMER-ID` e o identificador do assento.

   * O `WorkOfUnitEventRepository` valida a existência do assento no PostgreSQL (`5433`) e registra o bloqueio (`SeatLock`) na instância Redis dedicada (`6381`), impedindo que múltiplos usuários reservem o mesmo assento simultaneamente.

## 🛠️ Stack Tecnológica

* **Java 25** (Toolchain configurado no Gradle e **Virtual Threads** habilitadas via `spring.threads.virtual.enabled=true`)

* **Spring Boot 4.1.2**

  * Spring Web & Spring Validation

  * Spring Data JPA (Hibernate com múltiplos `DataSource`, `EntityManagerFactory` e `TransactionManager` isolados por módulo)

  * Spring Data MongoDB

  * Spring Data Redis com cliente **Jedis** (`@EnableRedisRepositories` dedicado para locking + `RedisCacheManager` para cache)

  * Spring Data REST & **HAL Explorer**

  * Spring Boot Actuator (`/actuator/health` com detalhes completos habilitados)

  * Spring Boot Docker Compose

* **Lombok** (`io.freefair.lombok`)

* **Gradle** & **Docker Compose**

## ⚙️ Como Executar

### Pré-requisitos

* **Java 25** (JDK 25)

* **Docker** e **Docker Compose**

### Passo a passo

1. **Clone o repositório:**

   ```bash
   git clone https://github.com/DiegoSilvaDev/marketplace-spring-data.git
   cd marketplace-spring-data
   ```

2. **Suba os containers de bancos de dados e cache:**

   ```bash
   docker compose up -d
   ```

   *(Opcional: ao executar `./gradlew bootRun` diretamente, o Spring Boot Docker Compose também inicia os serviços automaticamente).*

3. **Execute a aplicação:**

   ```bash
   ./gradlew bootRun
   ```

4. **Endpoints de Observabilidade e Exploração:**

   * **Actuator Health:** `http://localhost:8080/actuator/health`

   * **HAL Explorer (Spring Data REST):** `http://localhost:8080/explorer/index.html`

## 🔌 Guia de Endpoints e Exemplos de Requisição

### 1. Cadastrar Cliente (`registration` $\rightarrow$ `ticketing`)

* **POST** `http://localhost:8080/customers`

```json
{
    "firstName": "João",
    "lastName": "Silva",
    "email": "joao.silva@email.com",
    "address": {
        "street": "223 Arbour Stone Place",
        "postalCode": "T3G SG1",
        "city": "Calgary",
        "state": "Alberta"
    }
}
```

### 2. Criar Evento (`catalog`)

* **POST** `http://localhost:8080/events`

```json
{
    "title": "Turnê de Despedida - Show Ao Vivo",
    "date": "2026-12-20T21:00:00Z"
}
```

### 3. Cadastrar Metadados, Setores e Assentos (`catalog` $\rightarrow$ `ticketing`)

* **POST** `http://localhost:8080/eventMetadatas`

```json
{
    "eventId": "<UUID_DO_EVENTO>",
    "eventDescription": "Turnê de despedida da banda X. Um show épico com 3 horas de duração",
    "technicalRequirements": {
        "sound_system": "Line Array L-Acoustics",
        "min_age": 16,
        "backstage_pass_available": true
    },
    "sectors": [
        {
            "name": "Pista Premium",
            "price": 450.00
        },
        {
            "name": "Arquibancada",
            "price": 200.00
        }
    ],
    "seats": [
        {
            "code": "PRE-01",
            "sectorName": "Pista Premium"
        },
        {
            "code": "ARQ-10",
            "sectorName": "Arquibancada"
        }
    ]
}
```

### 4. Consultar Vitrine (`catalog` + Redis Cache)

* **GET** `http://localhost:8080/showcase`

### 5. Selecionar e Travar Assento (`ticketing` + Redis Locking)

* **POST** `http://localhost:8080/ticketing/events/{eventId}/seats/select`

* **Header:** `X-CUSTOMER-ID: <UUID_DO_CLIENTE>`

```json
{
    "id": "ARQ-10"
}
```

* **Comportamento esperado:** Retorna `201 Created` na primeira seleção (criando o registro `SeatLock` no Redis na porta `6381`). Tentativas subsequentes para o mesmo assento lançam `SeatAlreadyReservedException`.

### 📦 Collection do Postman

Para testar o fluxo completo diretamente no Postman, importe o arquivo [`docs/postman/marketplace.postman_collection.json`](docs/postman/marketplace.postman_collection.json) e execute as requisições na seguinte ordem:

1. **`POST - customers`** (copie o UUID gerado do cliente)
2. **`POST - events`** (copie o UUID gerado do evento)
3. **`POST - eventMetadatas`** (cole o `eventId` no corpo da requisição)
4. **`GET - showcase`** (valida a vitrine e o cache no Redis)
5. **`POST - ticketing/events/{eventId}/seats/select`** (cole o `eventId` na URL e o `customerId` no header `X-CUSTOMER-ID`)