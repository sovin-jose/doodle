// Mirrors the Java DTOs on the backend. Kept flat and manual — small enough
// that codegen would be overkill.

export type UUID = string;
export type Instant = string; // ISO-8601, e.g. "2026-09-01T09:00:00Z"

export type SlotStatus = 'FREE' | 'BUSY' | 'BOOKED';
export type ResponseStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED';

export interface User {
  id: UUID;
  name: string;
  email: string;
  calendarId: UUID;
  timezone: string;
  createdAt: Instant;
}

export interface Slot {
  id: UUID;
  calendarId: UUID;
  startTime: Instant;
  endTime: Instant;
  status: SlotStatus;
}

export interface MeetingParticipant {
  userId: UUID;
  responseStatus: ResponseStatus;
}

export interface Meeting {
  id: UUID;
  slotId: UUID;
  organizerId: UUID;
  title: string;
  description: string | null;
  startTime: Instant;
  endTime: Instant;
  participants: MeetingParticipant[];
}

export interface Interval {
  start: Instant;
  end: Instant;
}

export interface UserAvailability {
  userId: UUID;
  free: Interval[];
  busy: Interval[];
}

export interface Availability {
  from: Instant;
  to: Instant;
  users: UserAvailability[];
  commonFree: Interval[];
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
}
