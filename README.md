# Matchday

A football league management application built with Java and Spring Boot. Browse teams, players and match results, follow league standings, and manage a football competition through an authenticated admin dashboard.

The application is a server-rendered Spring MVC project with Thymeleaf views, a transactional service layer and MariaDB persistence.

## Features

- **Football management:** leagues, teams, squad members, match scheduling, results and match events.
- **Standings and statistics:** league tables calculated from completed matches, plus team and player statistics.
- **Search:** filters, sorting and pagination across leagues, teams, players and matches.
- **User accounts:** registration, login, favorite teams, profile pages and threaded match comments.
- **Administration:** role-based access to management screens, comment moderation and user blocking.
- **Wikipedia integration:** image lookup and asynchronous retrieval with rate-limit retries.

## Technology

| Area | Tools |
| --- | --- |
| Language and build | Java 25, Maven Wrapper |
| Application | Spring Boot 4.0.6, Spring MVC |
| Authentication | Spring Security, BCrypt, session-based login, CSRF protection |
| Persistence | Spring Data JPA, Hibernate, MariaDB 12.1.2 |
| UI | Thymeleaf, Bootstrap 5, JavaScript, HTML/CSS |
| Local infrastructure | Docker Compose, optional phpMyAdmin |
| Verification | JUnit 5, Mockito, Spring Boot integration tests, GitHub Actions |

## Run locally

### Prerequisites

- JDK 25 (`java -version` and `javac -version` should both show 25).
- Docker with the Compose plugin and a running Docker engine.
- Network access for the first Maven dependency download and Docker image pull.

Maven is included through the wrapper; a separate Maven installation is unnecessary.

### 1. Configure your environment

From the project root, copy the example:

**PowerShell**

```powershell
Copy-Item .env.example .env
```

**macOS / Linux**

```bash
cp .env.example .env
```

Edit `.env` and set **three different passwords** for `DB_PASSWORD`, `DB_ROOT_PASSWORD` and `ADMIN_PASSWORD`. Use at least 16 unquoted letters, digits, hyphens or underscores. The administrator password must contain at least 12 characters and fit within BCrypt's 72-byte limit.

The file is read by Docker Compose and imported by Spring Boot as a properties file. Do not use `export`, shell expressions, or quoted values. Environment variables override values from the file. `.env` is excluded from Git.

### 2. Start MariaDB

```bash
docker compose up -d --wait mariadb
```

The database port is bound to the local machine only. The application connects with the `matchday` database user rather than the database root account.

### 3. Start the application

**PowerShell**

```powershell
.\mvnw.cmd spring-boot:run
```

**macOS / Linux**

```bash
sh ./mvnw spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080).

Sign in using `ADMIN_USERNAME` and the password you set in `.env`. Register through the UI to create a regular user. Administrator creation is opt-in through `ADMIN_ENABLED=true`; there is no bundled administrator password. Existing accounts are never overwritten by startup configuration. After creating the administrator, you can set `ADMIN_ENABLED=false` and remove `ADMIN_PASSWORD` from `.env`.

To stop, press `Ctrl+C` in the application terminal and run `docker compose down`. The named database volume is retained.

### Optional database interface

```bash
docker compose --profile tools up -d phpmyadmin
```

Open [http://localhost:8081](http://localhost:8081) and sign in with `DB_USER` and `DB_PASSWORD`. phpMyAdmin is not started by the default command.

## Demo data

The example configuration enables `SEED_DEMO_DATA=true`. On a fresh database, the application imports football records from `src/main/resources/demo-data.sql`, including teams, leagues, players, matches and events. These are demonstration fixtures, not a current sports data feed.

The seed contains **no user accounts, password hashes, comments or favorites**. It preserves foreign-key checks and runs in a transaction. On subsequent starts, it skips loading when teams already exist. Do not use this seed to merge data into an existing database. Set `SEED_DEMO_DATA=false` for an empty installation.

## Tests and build

Integration tests use a **separate, disposable MariaDB database** on port 3307. They create and drop its schema; never point `TEST_DB_URL` at a database whose data you want to keep.

After configuring `.env` as above, start the test database:

```bash
docker compose --profile test up -d --wait mariadb-test
```

**PowerShell**

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress clean verify
```

**macOS / Linux**

```bash
sh ./mvnw --batch-mode --no-transfer-progress clean verify
```

The test database uses a documented test-only credential and temporary storage. To remove that test container and its temporary data after testing:

```bash
docker compose --profile test rm --stop --force mariadb-test
```

For a different dedicated test database, configure `TEST_DB_URL`, `TEST_DB_USER` and `TEST_DB_PASSWORD`. The default test URL is `jdbc:mariadb://127.0.0.1:3307/matchday_test`.

The suite checks administrator bootstrap validation, password encoding, preservation of existing accounts, opt-in administrator creation, sanitized demo imports, repeat-startup behavior, public page rendering and login requirements for private routes. It is not a comprehensive test suite for all application features.

A successful build produces `target/matchday-0.0.1-SNAPSHOT.jar`. Run it from the project root so the local `.env` file is found:

```bash
java -jar target/matchday-0.0.1-SNAPSHOT.jar
```

GitHub Actions runs `clean verify` against a disposable MariaDB service on pushes to `main` and pull requests. It requires no repository secrets. Dependabot checks Maven dependencies and Actions monthly.

## Project structure

```text
src/main/java/il/ac/jmc/matchday/
  config/        Security, async execution and shared configuration
  controller/    Public and authenticated routes, admin screens
  dto/           Form validation and view models
  entity/        JPA domain entities
  repository/    Persistence and filtered queries
  service/       Transactions, business rules, statistics, integrations
  security/      User details and login redirect handling
  init/          Opt-in admin bootstrap and football demo seed
  util/          Shared URL, formatting and pagination helpers
src/main/resources/
  templates/     Thymeleaf pages and reusable fragments
  static/        CSS, JavaScript and favicon
  demo-data.sql  Football-only demonstration fixtures
src/test/        Bootstrap and MariaDB integration tests
.github/         Continuous integration and dependency updates
```

A request passes from a controller to a service and repository, then returns a Thymeleaf view. Form DTOs validate input, services implement business rules and transaction boundaries, and repositories handle database queries. Small JSON endpoints support interactive UI features; this is primarily an MVC application, not a standalone REST API.

## Configuration reference

| Variable | Purpose / default |
| --- | --- |
| `DB_NAME`, `DB_USER` | Database and application user; both default to `matchday` |
| `DB_PASSWORD` | Required local application database password |
| `DB_ROOT_PASSWORD` | Required when creating the Compose database |
| `DB_PORT` | Local MariaDB port; `3306` |
| `DB_URL` | Optional JDBC URL override for the application |
| `ADMIN_ENABLED` | Create an administrator on startup; defaults to `false` |
| `ADMIN_USERNAME`, `ADMIN_EMAIL` | Bootstrap identity; `admin`, `admin@example.test` |
| `ADMIN_PASSWORD` | Required when creating a new administrator |
| `SEED_DEMO_DATA` | Import football fixtures into a fresh database; `false` |
| `SERVER_ADDRESS`, `SERVER_PORT` | Application listener; `127.0.0.1`, `8080` |
| `WIKIPEDIA_USER_AGENT` | Optional public project URL/contact for Wikipedia requests |

## Troubleshooting and scope

- **Port already in use:** change `DB_PORT` or `SERVER_PORT` in `.env`. For tests, change the test service's port mapping and set `TEST_DB_URL` accordingly.
- **Database access denied after changing `.env`:** MariaDB initializes credentials only when its volume is first created. Changing `.env` does not update credentials in an existing volume.
- **Administrator login fails:** bootstrap configuration does not reset an existing user's password. Use the credentials supplied on the first successful startup.
- **Images unavailable:** external image URLs and Wikipedia requests depend on network access and upstream availability. Browsing locally stored football data does not require the image integration.
- This is an academic portfolio application configured for local use. Hosted deployment needs environment-specific configuration, HTTPS and a reviewed schema migration and backup process; automatic `ddl-auto=update` is retained for local development.

## Authors and acknowledgments

Originally developed by **Amnon Yaakov and Roi Mazon** for Internet Programming B at Jerusalem Multidisciplinary College.

Frameworks and libraries retain their respective licenses. Football names, logos and externally hosted images belong to their respective owners; demo image URLs are references to external resources. This repository does not claim ownership of those assets.
