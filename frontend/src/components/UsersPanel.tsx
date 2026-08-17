import { useState } from 'react';
import { api } from '../api';
import type { User, UUID } from '../types';

interface Props {
  users: User[];
  onUsersChanged: (users: User[]) => void;
  selectedUserId: UUID | null;
  onSelect: (id: UUID | null) => void;
}

export function UsersPanel({ users, onUsersChanged, selectedUserId, onSelect }: Props) {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [tz, setTz] = useState('UTC');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      const user = await api.createUser({ name, email, timezone: tz });
      onUsersChanged([...users, user]);
      onSelect(user.id);
      setName('');
      setEmail('');
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="panel">
      <h2>Users</h2>
      <form onSubmit={submit}>
        <div className="row">
          <input
            placeholder="Name"
            value={name}
            onChange={e => setName(e.target.value)}
            required
          />
          <input
            type="email"
            placeholder="email@example.com"
            value={email}
            onChange={e => setEmail(e.target.value)}
            required
          />
          <input
            placeholder="Timezone (UTC)"
            value={tz}
            onChange={e => setTz(e.target.value)}
            style={{ width: 130 }}
          />
          <button disabled={busy}>Create</button>
        </div>
      </form>

      {error && <div className="error">{error}</div>}

      <ul className="list">
        {users.length === 0 && <li className="hint">No users yet — create one above.</li>}
        {users.map(u => (
          <li key={u.id}>
            <div>
              <input
                type="radio"
                name="selectedUser"
                checked={u.id === selectedUserId}
                onChange={() => onSelect(u.id)}
                style={{ marginRight: 8 }}
              />
              <strong>{u.name}</strong>{' '}
              <span className="mono">{u.email}</span>
            </div>
            <span className="mono">{u.timezone}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
