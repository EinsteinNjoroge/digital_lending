# Digital Lending Platform Backend

A Spring Boot backend for a simplified lending platform.

It supports:
- customer onboarding
- loan product configuration
- credit scoring
- loan application and account lifecycle management
- payment/disbursal callbacks
- notifications
- basic servicing and delinquency handling

For a simple explanation of how the system fits together, see [`architecture-overview.md`](./architecture-overview.md).

## What this project is

This project is a **modular monolith** built for the lending-platform case study. It focuses on the core loan journey:

1. create a customer profile
2. define or use an existing loan product
3. submit a loan application
4. underwrite it with basic credit scoring and exposure checks
5. disburse funds through a stubbed payment flow
6. accept repayments
7. move overdue loans into servicing buckets and notify customers

## Tech stack

- Java 21
- Spring Boot 3
- Spring Data JPA
- PostgreSQL
- Flyway
- Springdoc / Swagger UI
- Spring Modulith (lightly enforced through an architecture verification test)

## Before you start

You will need:
- Java 21
- Maven wrapper included in the repo
- Docker + Docker Compose

## Environment setup

Do **not** use a real secrets file from another environment.

Create your local env file from the example:

```bash
cp .env.example .env
```

Then update any values you need, especially:
- database credentials
- SMTP credentials if you want real email delivery
- servicing schedule

Important env values:
- `POSTGRES_DB` - database name, default example is `digital_lending_db`
- `APP_PORT` - application port
- `SERVICING_ENABLED` - enables the scheduled servicing job
- `SERVICING_CRON` - cron expression for servicing
- `SERVICING_BASE_URL` - base URL the scheduler uses to call the internal servicing endpoint

Example default servicing schedule:
- `0 */15 * * * *` = every 15 minutes

## Database creation

If you run the project with Docker Compose, the PostgreSQL container creates the database named in `POSTGRES_DB` automatically the first time the database volume is initialized. With the provided example env file, that database is:

- `digital_lending_db`

If the container is already running and you want a Docker-based fallback command to create the database only if it is missing, run:

```bash
docker compose exec postgres psql -U digital_lending_user -d postgres -c "SELECT 'CREATE DATABASE digital_lending_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'digital_lending_db')\\gexec"
```

If you are using your own local PostgreSQL instance instead of Docker, create the database before starting the app. One simple way is:

```bash
psql -U postgres -d postgres -c "SELECT 'CREATE DATABASE digital_lending_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'digital_lending_db')\\gexec"
```

After that, start the application and Flyway will create the schema objects inside `digital_lending_db`.

## Run with Docker Compose

From the project root:

```bash
docker compose --env-file .env up --build
```

The app will start after PostgreSQL is healthy and Flyway migrations have run.

## Run locally without Docker for the app

If you want PostgreSQL in Docker but the Spring Boot app on your machine:

```bash
docker compose --env-file .env up postgres
./mvnw spring-boot:run
```

Make sure your `.env` values match the local database connection.

## Useful URLs

Once the app is running:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Actuator: `http://localhost:8080/actuator`

If you change `APP_PORT`, use that port instead.

## Seeded data

The project includes Flyway migrations and seed data for:
- loan product families
- loan products
- credit scoring models
- payment providers/statuses/categories
- notification templates

Helpful files:
- `src/main/resources/db/migration`
- `postman_collection.json`

## Servicing and delinquency job

A scheduled job now calls the internal loan-account servicing endpoint:

- endpoint: `POST /api/v1/internal/loan-accounts/servicing/run`

What it does:
- finds active loans past their simplified repayment due date
- updates `daysPastDue`
- moves accounts into `WATCH` or `DOUBTFUL`
- writes audit history
- publishes overdue events
- triggers notification flow

This is intentionally simple for the assignment. A production system would usually use a fuller repayment schedule and stronger retry/outbox patterns.

## Authentication note

The intended production direction is **JWT-based authentication**.

JWT dependencies are present, but auth is **not implemented in this submission** because it is out of scope for this case study. In the current dev setup, requests are open so the functional loan flows are easy to review.


## Run tests

```bash
./mvnw test -Dspring.profiles.active=test
```

## Project structure

Main modules live under:

- `src/main/java/com/digital/lending/profile`
- `src/main/java/com/digital/lending/loanproduct`
- `src/main/java/com/digital/lending/creditscoring`
- `src/main/java/com/digital/lending/loanaccount`
- `src/main/java/com/digital/lending/payment`
- `src/main/java/com/digital/lending/notification`
- `src/main/java/com/digital/lending/events`

## Reviewer note

This project aims to be a clear, maintainable first version of the platform rather than a fully exhaustive lending system. Some parts are intentionally simplified and called out in [`architecture-overview.md`](./architecture-overview.md).
