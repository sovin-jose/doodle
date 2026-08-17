# Mini Doodle

A small Spring Boot service that lets users define time slots on a personal calendar and turn free slots into meetings with participants. The word "calendar" only lives in the domain layer — there's no `/calendars` REST resource, per the brief.

## Stack

Java 21, Spring Boot 4.1, PostgreSQL 16, Flyway for schema migrations, JUnit + Mockito for tests, springdoc for OpenAPI, Micrometer/Prometheus for metrics. Frontend is a tiny Vite + React + TypeScript SPA served by nginx. Everything runs from one `docker compose up`.

## Running it

```bash
docker compose up --build
```

Three containers come up:

| Service | URL | What |
| --- | --- | --- |
| `postgres` | localhost:5432 | Postgres 16, db/user/pass all `doodle` |
| `app` | http://localhost:8080 | Spring Boot backend |
| `frontend` | http://localhost:3001 | React SPA + nginx, proxies `/api` and `/actuator` back to `app` |

Open http://localhost:3001. Data lives in the `postgres_data` named volume, so restarts keep your users and slots around. `docker compose down -v` wipes it.

If you'd rather run the JVM against your own Postgres:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/mydb \
SPRING_DATASOURCE_USERNAME=me \
SPRING_DATASOURCE_PASSWORD=secret \
./mvnw spring-boot:run
```

## Domain

```
User ──1:1── Calendar ──1:N── Slot ──0..1── Meeting ──1:N── MeetingParticipant
```

A slot has one of three statuses: `FREE`, `BUSY`, `BOOKED`. Only `FREE` slots can be booked. Booking creates a `Meeting` and flips the slot to `BOOKED`. Cancelling a meeting deletes it and returns the slot to `FREE`.

A few decisions worth calling out up front rather than in a design-notes appendix:

- Overlap detection runs at the service layer via a `count(*)` query against a composite index on `(calendar_id, start_time, end_time)`. Fast enough for the brief's "thousands of slots" but not race-proof under high concurrency. A Postgres `EXCLUDE USING GIST` constraint would be the bulletproof answer. See notes at the bottom for why I didn't add it.
- `@Version` on `Slot` catches the concurrent-booking race and returns a 409 instead of silently double-booking.
- `spring.jpa.open-in-view=false`. Lazy-load bugs surface in tests instead of hiding until an HTTP call trips them.

## API

Base URL http://localhost:8080. All JSON. Errors look like this:

```json
{ "timestamp": "…", "status": 409, "error": "Conflict", "message": "slot overlaps an existing slot" }
```

404 for not-found, 409 for conflicts (overlap or bad state transition), 400 for validation.

### Users

```http
POST /api/users        # { name, email, timezone? }
GET  /api/users        # list all
GET  /api/users/{id}
```

Creating a user also provisions their Calendar behind the scenes. The response includes the `calendarId` for reference, but you never address it directly.

### Slots

```http
POST   /api/users/{userId}/slots            # { startTime, endTime, status? }
GET    /api/users/{userId}/slots?from=&to=&status=
PATCH  /api/slots/{slotId}/status?status=BUSY|FREE
DELETE /api/slots/{slotId}
```

Two rules that come back as 409:

- You can't create a slot as `BOOKED` directly. Booking a meeting is what flips it.
- You can't delete a booked slot without cancelling its meeting first.

### Meetings

```http
POST   /api/meetings   # { slotId, organizerId, title, description?, participantIds? }
GET    /api/meetings?organizerId=…
GET    /api/meetings/{id}
DELETE /api/meetings/{id}
```

Booking is a single transaction: the slot goes `FREE` → `BOOKED`, the meeting is inserted, participant rows are written. Cancelling reverses it.

### Availability

The actual mini-Doodle bit — given a set of participants and a window, find the common free time:

```http
GET /api/availability?userIds=a,b,c&from=…&to=…
```

Response:

```json
{
  "from": "…",
  "to": "…",
  "users": [
    { "userId": "a", "free": [...], "busy": [...] }
  ],
  "commonFree": [
    { "start": "…", "end": "…" }
  ]
}
```

Implementation is one query that pulls every overlapping slot for the requested users (using the composite index), then a two-pointer sweep across each user's free intervals to compute the intersection. Slots that straddle the window boundary get clipped.

Interactive Swagger UI at http://localhost:8080/swagger-ui.html.

## Observability

`/actuator/health` — used by compose to gate the frontend on the backend being healthy.
`/actuator/prometheus` — Micrometer scrape endpoint, JVM + HTTP + Hikari metrics.

## Tests

```bash
./mvnw test
```

17 Mockito unit tests, run in about a second, no Postgres needed. They cover overlap detection, state transition rules, cancellation restoring `FREE`, and the interval-intersection math in the availability service. There's no integration test yet — see next steps.

## Assumptions and trade-offs

Things I decided about, some of which aren't obvious from the code:

- **No auth.** The API trusts the `userId` in the path — any caller can act as any user. In production you'd take identity from a JWT and gate writes to the caller's own calendar. Explicitly out of scope for a demo.
- **UTC internally, always.** The frontend `datetime-local` inputs are treated as UTC, which is technically wrong but avoids pulling in `date-fns-tz` for a demo. A real product needs proper user-timezone handling.
- **Flyway is wired manually in `FlywayBootstrap.java`.** Spring Boot 4.1's Flyway auto-configuration wasn't firing for this project (no Flyway logs at startup, no `flyway_schema_history` table ever created). I switched to defining the bean explicitly with `initMethod = "migrate"`. Non-idiomatic, but reliable, and the class comment explains it. Would revisit once Boot 4.x autoconfig behaviour is clearer to me.
- **App-level overlap detection.** See above.
- **Slots are `[start, end)`, not `(startAt, durationMinutes)`.** Same information, different shape. The brief phrases it as "configurable duration" — if the product actually needed fixed-duration templates you'd add a helper that materialises N slots of duration D.
- **No pagination on list endpoints.** Fine for a few thousand slots per user, would need cursor pagination past that.

## What I'd do with more time

Rough priority order:

1. **Testcontainers-Postgres integration test.** The unit tests mock the repository, which means the JPQL and the migration script aren't actually exercised. A single end-to-end test that boots a real Postgres, applies the migration, and runs `SlotRepository.existsOverlap` + `findOverlappingForUsers` would close that gap.
2. **`EXCLUDE USING GIST` constraint** in a V2 migration once `btree_gist` is enabled. Turns overlap prevention into a race-proof DB guarantee instead of an app-layer race.
3. **Participant response endpoint** — `PATCH /api/meetings/{id}/participants/{userId}?status=ACCEPTED`. The column and enum exist, the endpoint doesn't.
4. **Auth.**
5. **Pagination.**

## Notes on the frontend

The React app is intentionally minimal — no react-router, no state library, no toast library. It's four panels backed by four API areas, hydrated on mount from `GET /api/users`, and persists the currently-selected user in `localStorage` so a reload doesn't blank the UI. See [`frontend/README.md`](./frontend/README.md) if you want to run it in Vite dev mode with hot reload instead of the compose-served build.
