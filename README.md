# CQRS Event Sourcing Banking Ledger

A banking ledger API built with Spring Boot implementing CQRS (Command Query Responsibility Segregation) and Event Sourcing patterns.

## Architecture

```
+------------------+     +------------------+
|    Commands      |     |     Queries      |
|   (Write Side)   |     |   (Read Side)    |
+--------+---------+     +--------+---------+
         |                        |
         v                        v
+------------------+     +------------------+
|   Event Store    |---->|   Projections    |
|   (PostgreSQL)   |     |  (Read Models)   |
+------------------+     +------------------+
```

## Tech Stack

| Technology | Version |
|------------|---------|
| Java | 21 |
| Spring Boot | 3.2.5 |
| PostgreSQL | Production |
| H2 | Development |
| Flyway | 10.10.0 |
| JWT | 0.12.5 |
| Lombok | Latest |
| SpringDoc OpenAPI | 2.5.0 |

## Prerequisites

- Java 21 JDK
- Maven 3.9+
- Docker (for PostgreSQL)
- Git

## Build

```bash
# Clone the repository
git clone <repository-url>
cd cqrs-event-sourcing-ledger

# Build the project
mvn clean package -DskipTests

# Build with tests
mvn clean package
```

## Run

### Development (H2 in-memory)

```bash
# Run with dev profile (default)
mvn spring-boot:run

# Or with jar
java -jar target/cqrs-event-sourcing-ledger-1.0.0.jar
```

The application will start on `http://localhost:8080`.

### Docker

```bash
# Build the image
docker build -t ledger:latest .

# Run with PostgreSQL
docker run -d \
  --name ledger \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=postgres \
  -e DB_PORT=5432 \
  -e DB_NAME=ledger \
  -e DB_USERNAME=ledger_user \
  -e DB_PASSWORD=secure_password \
  -e JWT_SECRET=your-256-bit-secret-key-here \
  ledger:latest
```

### Quick Start with Docker Compose

```bash
# Clone the repository
git clone <repository-url>
cd cqrs-event-sourcing-ledger

# Copy environment template
cp .env.example .env

# Edit .env with your values (at minimum, set JWT_SECRET)
# nano .env

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f app

# Application available at http://localhost:8080
# Swagger UI at http://localhost:8080/swagger-ui.html

# Stop services
docker-compose down
```

### Docker Compose (Manual Configuration)

## API Documentation

Access Swagger UI at: `http://localhost:8080/swagger-ui.html`

OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## API Endpoints

### Account Commands (Write Side)

#### Open Account
```bash
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "accountNumber": "ACC123456789",
    "accountHolder": "John Doe",
    "initialBalance": 1000.00,
    "currency": "USD"
  }'
```

#### Credit Account
```bash
curl -X POST http://localhost:8080/api/v1/accounts/{id}/credit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "amount": 500.00,
    "description": "Deposit"
  }'
```

#### Debit Account
```bash
curl -X POST http://localhost:8080/api/v1/accounts/{id}/debit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "amount": 200.00,
    "description": "Withdrawal"
  }'
```

#### Close Account
```bash
curl -X DELETE "http://localhost:8080/api/v1/accounts/{id}?reason=Account%20closed%20by%20request" \
  -H "Authorization: Bearer <token>"
```

### Money Transfer

#### Transfer Money
```bash
curl -X POST http://localhost:8080/api/v1/transfers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "fromAccountId": "550e8400-e29b-41d4-a716-446655440000",
    "toAccountId": "660e8400-e29b-41d4-a716-446655440000",
    "amount": 250.00,
    "description": "Payment for services"
  }'
```

### Account Queries (Read Side)

#### Get All Accounts (Non-paginated)
```bash
curl http://localhost:8080/api/v1/accounts \
  -H "Authorization: Bearer <token>"
```

#### Get All Accounts (Paginated)
```bash
curl "http://localhost:8080/api/v1/accounts/paged?page=0&size=20" \
  -H "Authorization: Bearer <token>"
```

#### Get Account by ID
```bash
curl http://localhost:8080/api/v1/accounts/{id} \
  -H "Authorization: Bearer <token>"
```

#### Get Account Balance
```bash
curl http://localhost:8080/api/v1/accounts/{id}/balance \
  -H "Authorization: Bearer <token>"
```

#### Get Transaction History (Non-paginated)
```bash
curl http://localhost:8080/api/v1/accounts/{id}/transactions \
  -H "Authorization: Bearer <token>"
```

#### Get Transaction History (Paginated)
```bash
curl "http://localhost:8080/api/v1/accounts/{id}/transactions/paged?page=0&size=20" \
  -H "Authorization: Bearer <token>"
```

#### Get Account Event History
```bash
curl http://localhost:8080/api/v1/accounts/{id}/events \
  -H "Authorization: Bearer <token>"
```

#### Get All Events (Paginated)
```bash
curl "http://localhost:8080/api/v1/events?page=0&size=20" \
  -H "Authorization: Bearer <token>"
```

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Server port | 8080 |
| `SPRING_PROFILES_ACTIVE` | Active profile | dev |
| `DB_HOST` | Database host | localhost |
| `DB_PORT` | Database port | 5432 |
| `DB_NAME` | Database name | ledger |
| `DB_USERNAME` | Database username | - |
| `DB_PASSWORD` | Database password | - |
| `JWT_SECRET` | JWT signing key | - |

### Application Properties

Key configurations in `application.properties`:

```properties
# Response compression
server.compression.enabled=true

# HikariCP connection pool
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.connection-timeout=30000

# JPA
spring.jpa.open-in-view=false
```

## Testing

```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report

# Run specific test class
mvn test -Dtest=AccountControllerIntegrationTest
```

## License

MIT License
