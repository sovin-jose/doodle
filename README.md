# Doodle — Mini Meeting Scheduling Platform

A small Spring Boot service that simulates the core of a Doodle-style scheduling
platform: users own calendars, expose available time slots, and turn free slots
into meetings with participants.

The "calendar" is an internal domain concept — it is created 1:1 with each user
and is never exposed directly as a REST resource.

---

## Tech stack

| Layer          | Choice                                           |
| -------------- | ------------------------------------------------ |
| Language       | Java 21                                          |
| Framework      | Spring Boot 4.1                                  |
| Persistence    | Spring Data JPA / Hibernate                      |
| Database       | PostgreSQL 16                                    |
| Migrations     | Flyway                                           |
| Build          | Maven (wrapper included: `./mvnw`)               |
| Packaging      | Multi-stage Docker image (Temurin JDK 21 → JRE)  |
| Orchestration  | docker compose                                   |

---

## Run it — one command

Prerequisites: Docker Desktop (or Docker Engine) with the `compose` plugin.

```bash
docker compose up --build
```

That's it. The compose file:

1. Boots `postgres:16-alpine` with database `doodle` (user/password: `doodle`).
2. Waits until Postgres is healthy (`pg_isready`).
3. Builds the app image from the multi-stage `Dockerfile`.
4. Starts the app on `http://localhost:8080` with `SPRING_DATASOURCE_URL`,
   `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` wired to Postgres.
5. Flyway automatically runs `V1__init_schema.sql` on first startup.

Postgres data is persisted in the named volume `postgres_data`, so restarts
keep your data. To wipe everything:

```bash
docker compose down -v
```

---

## Running without Docker (optional)

If you'd rather run the JVM locally against a Postgres you already have:

```bash
# 1. Have a Postgres reachable at localhost:5432 with db/user/password 'doodle'
# 2. Build and run
./mvnw spring-boot:run
```

Override the datasource with env vars if needed:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://my-host:5432/mydb \
SPRING_DATASOURCE_USERNAME=me \
SPRING_DATASOURCE_PASSWORD=secret \
./mvnw spring-boot:run
```

---

## Domain model

```
User ─┬─(1:1)─ Calendar ─(1:N)─ Slot ─(0..1)─ Meeting ─(1:N)─ MeetingParticipant
      │                                                        │
      └────────────────── organizer / participant ──────────────┘
```

- **User** — `id`, `name`, unique `email`.
- **Calendar** — 1:1 with a User; carries the owner's `timezone`.
- **Slot** — belongs to a Calendar; `start_time`, `end_time`, `status`
  (`FREE` / `BUSY` / `BOOKED`), plus `@Version` for optimistic locking.
- **Meeting** — 1:1 with a `BOOKED` Slot; has `title`, `description`, organizer.
- **MeetingParticipant** — join between Meeting and User with
  `response_status` (`PENDING` / `ACCEPTED` / `DECLINED`).

`slots` has a composite index on `(calendar_id, start_time, end_time)` for fast
range queries; overlap detection uses the classic
`start < :end AND end > :start` predicate.

---

## HTTP API

Base URL: `http://localhost:8080`

### Users

```http
POST /api/users
Content-Type: application/json

{
  "name": "Ada Lovelace",
  "email": "ada@example.com",
  "timezone": "Europe/London"
}
```

Response `201 Created` — includes the auto-created `calendarId`.

```http
GET /api/users/{id}
```

### Slots

```http
POST /api/users/{userId}/slots
Content-Type: application/json

{
  "startTime": "2026-09-01T09:00:00Z",
  "endTime":   "2026-09-01T09:30:00Z",
  "status":    "FREE"
}
```

Rejects overlaps with the user's existing slots (`409 Conflict`). Direct
creation as `BOOKED` is not allowed — book a meeting instead.

```http
GET /api/users/{userId}/slots?from=2026-09-01T00:00:00Z&to=2026-09-02T00:00:00Z&status=FREE
PATCH /api/slots/{slotId}/status?status=BUSY
DELETE /api/slots/{slotId}
```

`status` on the list endpoint is optional. Booked slots cannot be deleted
without cancelling their meeting first.

### Meetings

```http
POST /api/meetings
Content-Type: application/json

{
  "slotId":       "…",
  "organizerId":  "…",
  "title":        "Sprint planning",
  "description":  "Weekly review",
  "participantIds": ["…", "…"]
}
```

Transitions the slot from `FREE` → `BOOKED` atomically. Returns the meeting
with participant response statuses (all start as `PENDING`).

```http
GET /api/meetings/{id}
GET /api/meetings?organizerId={userId}
```

### Error shape

```json
{
  "timestamp": "2026-08-16T12:34:56.789Z",
  "status": 409,
  "error": "Conflict",
  "message": "slot overlaps an existing slot"
}
```

Mapping: `404` for not-found, `409` for conflict (overlap / bad state
transition), `400` for validation errors.

---

## Database migrations

Managed by Flyway from `src/main/resources/db/migration`.

- Migrations run automatically at application startup.
- Hibernate is set to `ddl-auto=none` — Flyway is the single source of truth
  for schema changes.
- The V1 script uses `CREATE TABLE IF NOT EXISTS`, so pointing the app at a
  Postgres that already has the tables is safe.

To add a new migration: drop `V2__something.sql` (or `V3__…`) into the
migration folder and restart. `spring.flyway.baseline-on-migrate=true` is set
so Flyway can adopt a pre-existing empty schema without complaining.

---

## Quick smoke test

With `docker compose up` running:

```bash
# 1. Create a user (returns the user + calendarId)
USER_ID=$(curl -sS -X POST http://localhost:8080/api/users \
  -H 'Content-Type: application/json' \
  -d '{"name":"Ada","email":"ada@example.com","timezone":"UTC"}' \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])')

# 2. Create a free slot
SLOT_ID=$(curl -sS -X POST http://localhost:8080/api/users/$USER_ID/slots \
  -H 'Content-Type: application/json' \
  -d '{"startTime":"2026-09-01T09:00:00Z","endTime":"2026-09-01T09:30:00Z"}' \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])')

# 3. Book it as a meeting
curl -sS -X POST http://localhost:8080/api/meetings \
  -H 'Content-Type: application/json' \
  -d "{\"slotId\":\"$SLOT_ID\",\"organizerId\":\"$USER_ID\",\"title\":\"Kickoff\"}"
```

---

## Project layout

```
src/main/java/com/doodle/demo
├── DemoApplication.java
├── domain/          # JPA entities and enums
├── repository/      # Spring Data repositories
├── service/         # Transactional business logic
└── web/             # REST controllers + DTOs + exception mapper
src/main/resources
├── application.properties
└── db/migration/    # Flyway SQL scripts
Dockerfile
docker-compose.yml
```

---

## Design notes

- **Overlap detection** happens at the service layer with a single `count`
  query; a stronger guarantee would be a Postgres `EXCLUDE USING GIST` range
  constraint — deferred as it needs the `btree_gist` extension.
- **Optimistic locking** (`@Version`) on `Slot` prevents two organizers from
  double-booking the same slot under contention.
- **`open-in-view=false`** — DB sessions don't leak into the web layer, so
  lazy-load errors show up early in tests instead of at HTTP time.
- **Calendar not exposed as REST** — per the brief, "Calendar" is a domain
  term only. Slots are addressed through `/api/users/{userId}/slots`.
- **Env-driven datasource** — the same jar runs unchanged in compose, locally,
  or against a staging DB just by swapping `SPRING_DATASOURCE_*` env vars.

## Next steps (not yet implemented)

- Aggregated free/busy endpoint for a given time window across multiple users.
- Integration tests with Testcontainers-Postgres.
- Actuator + Micrometer metrics, OpenAPI/Swagger UI.
- Meeting cancellation flow that returns a `BOOKED` slot to `FREE`.
