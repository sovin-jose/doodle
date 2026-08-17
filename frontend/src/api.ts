import type {
  Availability,
  ApiError,
  Meeting,
  Slot,
  SlotStatus,
  User,
  UUID,
} from './types';

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(path, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      ...(init?.headers ?? {}),
    },
  });

  if (res.status === 204) {
    return undefined as T;
  }

  const text = await res.text();
  const body = text ? JSON.parse(text) : null;

  if (!res.ok) {
    const err = body as ApiError | null;
    throw new Error(err?.message ?? `HTTP ${res.status}`);
  }
  return body as T;
}

export const api = {
  createUser: (input: { name: string; email: string; timezone?: string }) =>
    request<User>('/api/users', { method: 'POST', body: JSON.stringify(input) }),

  getUser: (id: UUID) => request<User>(`/api/users/${id}`),

  listUsers: () => request<User[]>('/api/users'),

  createSlot: (
    userId: UUID,
    input: { startTime: string; endTime: string; status?: SlotStatus }
  ) =>
    request<Slot>(`/api/users/${userId}/slots`, {
      method: 'POST',
      body: JSON.stringify(input),
    }),

  listSlots: (userId: UUID, from: string, to: string, status?: SlotStatus) => {
    const params = new URLSearchParams({ from, to });
    if (status) params.set('status', status);
    return request<Slot[]>(`/api/users/${userId}/slots?${params.toString()}`);
  },

  deleteSlot: (slotId: UUID) =>
    request<void>(`/api/slots/${slotId}`, { method: 'DELETE' }),

  updateSlotStatus: (slotId: UUID, status: SlotStatus) =>
    request<Slot>(`/api/slots/${slotId}/status?status=${status}`, {
      method: 'PATCH',
    }),

  bookMeeting: (input: {
    slotId: UUID;
    organizerId: UUID;
    title: string;
    description?: string;
    participantIds?: UUID[];
  }) =>
    request<Meeting>('/api/meetings', {
      method: 'POST',
      body: JSON.stringify(input),
    }),

  cancelMeeting: (id: UUID) =>
    request<void>(`/api/meetings/${id}`, { method: 'DELETE' }),

  listMeetings: (organizerId: UUID) =>
    request<Meeting[]>(`/api/meetings?organizerId=${organizerId}`),

  availability: (userIds: UUID[], from: string, to: string) => {
    const params = new URLSearchParams({
      userIds: userIds.join(','),
      from,
      to,
    });
    return request<Availability>(`/api/availability?${params.toString()}`);
  },
};
