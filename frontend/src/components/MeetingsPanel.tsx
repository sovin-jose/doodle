import { useEffect, useState } from 'react';
import { api } from '../api';
import type { Meeting, Slot, User } from '../types';

interface Props {
  user: User | null;
  allUsers: User[];
}

function todayRange() {
  const from = new Date();
  from.setUTCHours(0, 0, 0, 0);
  const to = new Date(from);
  to.setUTCDate(to.getUTCDate() + 30);
  return { from: from.toISOString(), to: to.toISOString() };
}

export function MeetingsPanel({ user, allUsers }: Props) {
  const [freeSlots, setFreeSlots] = useState<Slot[]>([]);
  const [meetings, setMeetings] = useState<Meeting[]>([]);
  const [slotId, setSlotId] = useState('');
  const [title, setTitle] = useState('Sprint sync');
  const [description, setDescription] = useState('');
  const [participantIds, setParticipantIds] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function reload() {
    if (!user) return;
    setError(null);
    try {
      const range = todayRange();
      const [free, mine] = await Promise.all([
        api.listSlots(user.id, range.from, range.to, 'FREE'),
        api.listMeetings(user.id),
      ]);
      setFreeSlots(free);
      setMeetings(mine);
      if (free.length > 0 && !slotId) setSlotId(free[0].id);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    }
  }

  useEffect(() => {
    setFreeSlots([]);
    setMeetings([]);
    setSlotId('');
    setParticipantIds([]);
    if (user) reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.id]);

  async function book(e: React.FormEvent) {
    e.preventDefault();
    if (!user || !slotId) return;
    setBusy(true);
    setError(null);
    try {
      await api.bookMeeting({
        slotId,
        organizerId: user.id,
        title,
        description: description || undefined,
        participantIds,
      });
      setTitle('Sprint sync');
      setDescription('');
      setParticipantIds([]);
      await reload();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setBusy(false);
    }
  }

  async function cancel(id: string) {
    setError(null);
    try {
      await api.cancelMeeting(id);
      await reload();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    }
  }

  if (!user) {
    return (
      <div className="panel">
        <h2>Meetings</h2>
        <div className="hint">Select a user to organize meetings.</div>
      </div>
    );
  }

  const otherUsers = allUsers.filter(u => u.id !== user.id);

  return (
    <div className="panel">
      <h2>Meetings — organized by {user.name}</h2>

      <form onSubmit={book}>
        <div className="row">
          <select
            value={slotId}
            onChange={e => setSlotId(e.target.value)}
            required
            style={{ flex: 1 }}
          >
            <option value="" disabled>
              — pick a FREE slot —
            </option>
            {freeSlots.map(s => (
              <option key={s.id} value={s.id}>
                {s.startTime.slice(0, 16).replace('T', ' ')} → {s.endTime.slice(11, 16)}
              </option>
            ))}
          </select>
        </div>
        <div className="row">
          <input
            placeholder="Meeting title"
            value={title}
            onChange={e => setTitle(e.target.value)}
            required
            style={{ flex: 1 }}
          />
        </div>
        <div className="row">
          <input
            placeholder="Description (optional)"
            value={description}
            onChange={e => setDescription(e.target.value)}
            style={{ flex: 1 }}
          />
        </div>
        {otherUsers.length > 0 && (
          <div className="row" style={{ alignItems: 'flex-start' }}>
            <div className="hint" style={{ width: 90 }}>Participants:</div>
            <div>
              {otherUsers.map(u => (
                <label key={u.id} style={{ display: 'inline-block', marginRight: 8 }}>
                  <input
                    type="checkbox"
                    checked={participantIds.includes(u.id)}
                    onChange={e => {
                      setParticipantIds(prev =>
                        e.target.checked
                          ? [...prev, u.id]
                          : prev.filter(id => id !== u.id)
                      );
                    }}
                  />{' '}
                  {u.name}
                </label>
              ))}
            </div>
          </div>
        )}
        <div className="row">
          <button disabled={busy || !slotId}>Book meeting</button>
          <button type="button" className="secondary" onClick={reload}>
            Reload
          </button>
        </div>
      </form>

      {error && <div className="error">{error}</div>}

      <ul className="list">
        {meetings.length === 0 && <li className="hint">No meetings booked yet.</li>}
        {meetings.map(m => (
          <li key={m.id}>
            <div>
              <strong>{m.title}</strong>{' '}
              <span className="mono">
                {m.startTime.slice(0, 16).replace('T', ' ')} → {m.endTime.slice(11, 16)}
              </span>{' '}
              <span className="hint">· {m.participants.length} participant(s)</span>
            </div>
            <button className="danger" onClick={() => cancel(m.id)}>
              Cancel
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}
