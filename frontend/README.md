# Doodle frontend

Small Vite + React + TypeScript UI over the backend API. Covers the four main
flows the challenge asks for:

- Create users (each auto-provisions a calendar)
- Manage a user's slots (create, mark BUSY/FREE, delete)
- Book meetings from FREE slots (title, description, participants); cancel meetings
- **Availability** — the "mini Doodle" endpoint: pick a set of users and a time
  window and see their common free intervals

## Run it

### Recommended — as part of the compose stack

From the repo root:

```bash
docker compose up --build
```

Open `http://localhost:3000`. The compose stack builds this frontend into a
static bundle and serves it from nginx, which also reverse-proxies `/api/**`
and `/actuator/**` to the backend on the compose network. Same-origin from the
browser's point of view, no CORS needed.

### Alternatively — dev mode with hot reload

Backend needs to be up first (from the repo root):

```bash
docker compose up --build -d app postgres
```

Then in `frontend/`:

```bash
npm install
npm run dev
```

Open `http://localhost:5173`. The Vite dev server proxies `/api/**` and
`/actuator/**` to `http://localhost:8080`.

## Layout

```
src/
├── main.tsx              # React entry
├── App.tsx               # Composes the four panels
├── api.ts                # Typed fetch wrapper for /api/*
├── types.ts              # DTO mirrors of the backend records
├── styles.css            # A little vanilla CSS
└── components/
    ├── UsersPanel.tsx        # POST /api/users, list
    ├── SlotsPanel.tsx        # slot CRUD + status transitions
    ├── MeetingsPanel.tsx     # book/cancel meetings
    └── AvailabilityPanel.tsx # GET /api/availability
```

Kept deliberately small — no react-router, no state library, no toast library.
State is per-panel React state + a shared "selected user" in `App`.
