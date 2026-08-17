import { useState } from 'react';
import { api } from '../api';
import type { Availability, User } from '../types';

interface Props {
  users: User[];
}

function toIso(local: string): string {
  return new Date(local + 'Z').toISOString();
}

export function AvailabilityPanel({ users }: Props) {
  const [selected, setSelected] = useState<string[]>([]);
  const [from, setFrom] = useState('2026-09-01T09:00');
  const [to, setTo] = useState('2026-09-01T18:00');
  const [result, setResult] = useState<Availability | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function check(e: React.FormEvent) {
    e.preventDefault();
    if (selected.length === 0) return;
    setBusy(true);
    setError(null);
    try {
      const r = await api.availability(selected, toIso(from), toIso(to));
      setResult(r);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="panel">
      <h2>Availability — find common free time</h2>

      <form onSubmit={check}>
        <div className="row" style={{ alignItems: 'flex-start' }}>
          <div className="hint" style={{ width: 90 }}>Users:</div>
          <div>
            {users.length === 0 && <span className="hint">Create some users first.</span>}
            {users.map(u => (
              <label key={u.id} style={{ display: 'inline-block', marginRight: 8 }}>
                <input
                  type="checkbox"
                  checked={selected.includes(u.id)}
                  onChange={e => {
                    setSelected(prev =>
                      e.target.checked ? [...prev, u.id] : prev.filter(id => id !== u.id)
                    );
                  }}
                />{' '}
                {u.name}
              </label>
            ))}
          </div>
        </div>
        <div className="row">
          <input
            type="datetime-local"
            value={from}
            onChange={e => setFrom(e.target.value)}
            required
          />
          <span>→</span>
          <input
            type="datetime-local"
            value={to}
            onChange={e => setTo(e.target.value)}
            required
          />
          <button disabled={busy || selected.length === 0}>Check</button>
        </div>
      </form>

      {error && <div className="error">{error}</div>}

      {result && (
        <>
          <h3 style={{ margin: '12px 0 4px', fontSize: 14 }}>Common free windows</h3>
          <ul className="list" style={{ maxHeight: 120 }}>
            {result.commonFree.length === 0 && (
              <li className="hint">No overlap in the selected window.</li>
            )}
            {result.commonFree.map((iv, i) => (
              <li key={i}>
                <span className="mono">
                  {iv.start.slice(0, 16).replace('T', ' ')} → {iv.end.slice(11, 16)}
                </span>
              </li>
            ))}
          </ul>

          <h3 style={{ margin: '12px 0 4px', fontSize: 14 }}>Per-user</h3>
          {result.users.map(ua => {
            const user = users.find(u => u.id === ua.userId);
            return (
              <div key={ua.userId} style={{ marginBottom: 6 }}>
                <strong>{user?.name ?? ua.userId.slice(0, 8)}</strong>{' '}
                <span className="hint">
                  {ua.free.length} free · {ua.busy.length} busy
                </span>
              </div>
            );
          })}
        </>
      )}
    </div>
  );
}
