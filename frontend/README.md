# Frontend

Tiny Vite + React + TypeScript UI over the backend API. Four panels covering the four flows the challenge asks for:

- Create users (each auto-provisions a calendar behind the scenes)
- Manage a user's slots — create, toggle BUSY/FREE, delete
- Book meetings from FREE slots and cancel them
- Availability — pick a set of users and a window, see their common free intervals

Kept deliberately small: no react-router, no state library, no toast library. Per-panel `useState`, one shared "selected user" hoisted into `App`, and `localStorage` for the selected-user id so a browser reload doesn't blank the UI.

## Running it

Easiest way is via the compose stack from the repo root, which builds the SPA into static files and serves them from nginx on port 3001:

```bash
docker compose up --build
```

Open http://localhost:3001. Nginx also reverse-proxies `/api` and `/actuator` back to the `app` service, so the browser sees same-origin requests. No CORS setup needed on the Java side.

If you want hot reload, run the backend from compose and Vite locally:

```bash
docker compose up -d app postgres     # from repo root
cd frontend
npm install
npm run dev
```

Then http://localhost:5173. The Vite dev server proxies `/api` and `/actuator` to `http://localhost:8080` (see `vite.config.ts`).

## Layout

- `src/api.ts` — typed fetch wrapper. Throws with the backend's `message` on non-2xx.
- `src/types.ts` — mirrors the Java DTOs by hand. Small enough that codegen would be overkill.
- `src/App.tsx` — hydrates users from `GET /api/users` on mount, composes the four panels.
- `src/components/*Panel.tsx` — one panel per API area.

## A few things worth knowing

`datetime-local` inputs are treated as UTC when sent to the backend. That's technically wrong (they're actually local time in the browser) but it keeps the demo focused on scheduling logic rather than timezone plumbing. A real product needs proper zone handling here.

The selected-user id is persisted in `localStorage` under `doodle:selectedUserId` so reloading the tab keeps context. If the persisted user no longer exists on the server (e.g. after wiping the Postgres volume), the persisted selection is dropped silently.
