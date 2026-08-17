import { useEffect, useState } from 'react';
import { api } from './api';
import type { User, UUID } from './types';
import { UsersPanel } from './components/UsersPanel';
import { SlotsPanel } from './components/SlotsPanel';
import { MeetingsPanel } from './components/MeetingsPanel';
import { AvailabilityPanel } from './components/AvailabilityPanel';

const SELECTED_USER_KEY = 'doodle:selectedUserId';

export default function App() {
  const [users, setUsers] = useState<User[]>([]);
  const [selectedUserId, setSelectedUserIdRaw] = useState<UUID | null>(
    () => localStorage.getItem(SELECTED_USER_KEY)
  );
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  function setSelectedUserId(id: UUID | null) {
    setSelectedUserIdRaw(id);
    if (id) localStorage.setItem(SELECTED_USER_KEY, id);
    else localStorage.removeItem(SELECTED_USER_KEY);
  }

  useEffect(() => {
    api
      .listUsers()
      .then(fetched => {
        setUsers(fetched);
        // Drop the persisted selection if it no longer exists on the server.
        if (selectedUserId && !fetched.some(u => u.id === selectedUserId)) {
          setSelectedUserId(null);
        }
      })
      .catch(err => setError(err instanceof Error ? err.message : String(err)))
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const selectedUser = users.find(u => u.id === selectedUserId) ?? null;

  return (
    <div className="container">
      <h1>Mini Doodle</h1>
      <p className="hint">
        A small React UI over the Doodle API. Requests are proxied to
        <span className="mono"> http://localhost:8080</span>.
      </p>

      {loading && <p className="hint">Loading users…</p>}
      {error && <div className="error">Failed to load users: {error}</div>}

      <div className="grid">
        <UsersPanel
          users={users}
          onUsersChanged={setUsers}
          selectedUserId={selectedUserId}
          onSelect={setSelectedUserId}
        />
        <SlotsPanel user={selectedUser} />
        <MeetingsPanel user={selectedUser} allUsers={users} />
        <AvailabilityPanel users={users} />
      </div>
    </div>
  );
}
