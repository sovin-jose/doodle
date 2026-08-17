import { useEffect, useState } from 'react';
import { api } from '../api';
import type { Slot, SlotStatus, User } from '../types';

interface Props {
  user: User | null;
}

function defaultRange() {
  const now = new Date();
  const from = new Date(now);
  from.setUTCHours(0, 0, 0, 0);
  const to = new Date(from);
  to.setUTCDate(to.getUTCDate() + 7);
  return { from: from.toISOString(), to: to.toISOString() };
}

function toIso(local: string): string {
  // datetime-local returns "YYYY-MM-DDTHH:mm" without timezone; treat as UTC.
  return new Date(local + 'Z').toISOString();
}

function toLocalInput(iso: string): string {
  return iso.slice(0, 16);
}

export function SlotsPanel({ user }: Props) {
  const [slots, setSlots] = useState<Slot[]>([]);
  const [range] = useState(defaultRange());
  const [start, setStart] = useState('2026-09-01T09:00');
  const [end, setEnd] = useState('2026-09-01T09:30');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function reload() {
    if (!user) return;
    setError(null);
    try {
      const s = await api.listSlots(user.id, range.from, range.to);
      setSlots(s);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    }
  }

  useEffect(() => {
    setSlots([]);
    if (user) reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.id]);

  async function create(e: React.FormEvent) {
    e.preventDefault();
    if (!user) return;
    setBusy(true);
    setError(null);
    try {
      await api.createSlot(user.id, {
        startTime: toIso(start),
        endTime: toIso(end),
        status: 'FREE',
      });
      await reload();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setBusy(false);
    }
  }

  async function updateStatus(id: string, status: SlotStatus) {
    setError(null);
    try {
      await api.updateSlotStatus(id, status);
      await reload();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    }
  }

  async function del(id: string) {
    setError(null);
    try {
      await api.deleteSlot(id);
      await reload();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    }
  }

  if (!user) {
    return (
      <div className="panel">
        <h2>Slots</h2>
        <div className="hint">Select a user to manage their slots.</div>
      </div>
    );
  }

  return (
    <div className="panel">
      <h2>Slots — {user.name}</h2>
      <form onSubmit={create}>
        <div className="row">
          <input
            type="datetime-local"
            value={start}
            onChange={e => setStart(e.target.value)}
            required
          />
          <span>→</span>
          <input
            type="datetime-local"
            value={end}
            onChange={e => setEnd(e.target.value)}
            required
          />
          <button disabled={busy}>Add FREE slot</button>
          <button type="button" className="secondary" onClick={reload}>
            Reload
          </button>
        </div>
        <div className="hint">
          Times are treated as UTC. Range shown: {toLocalInput(range.from)} → {toLocalInput(range.to)}
        </div>
      </form>

      {error && <div className="error">{error}</div>}

      <ul className="list">
        {slots.length === 0 && <li className="hint">No slots in this range.</li>}
        {slots.map(s => (
          <li key={s.id}>
            <div>
              <span className="mono">
                {s.startTime.slice(11, 16)}–{s.endTime.slice(11, 16)}
              </span>{' '}
              <span className="mono">{s.startTime.slice(0, 10)}</span>{' '}
              <span className={`badge ${s.status}`}>{s.status}</span>
            </div>
            <div className="row" style={{ marginBottom: 0 }}>
              {s.status === 'FREE' && (
                <button className="secondary" onClick={() => updateStatus(s.id, 'BUSY')}>
                  Mark busy
                </button>
              )}
              {s.status === 'BUSY' && (
                <button className="secondary" onClick={() => updateStatus(s.id, 'FREE')}>
                  Mark free
                </button>
              )}
              {s.status !== 'BOOKED' && (
                <button className="danger" onClick={() => del(s.id)}>
                  Delete
                </button>
              )}
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}
