-- Doodle mini scheduling platform: initial schema.
-- SQLite-first DDL, using CREATE ... IF NOT EXISTS so re-running is safe.

CREATE TABLE IF NOT EXISTS users (
    id         VARCHAR(36)  NOT NULL,
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS calendars (
    id       VARCHAR(36) NOT NULL,
    owner_id VARCHAR(36) NOT NULL,
    timezone VARCHAR(64) NOT NULL,
    CONSTRAINT pk_calendars PRIMARY KEY (id),
    CONSTRAINT uk_calendars_owner UNIQUE (owner_id),
    CONSTRAINT fk_calendars_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS slots (
    id          VARCHAR(36) NOT NULL,
    calendar_id VARCHAR(36) NOT NULL,
    start_time  TIMESTAMP   NOT NULL,
    end_time    TIMESTAMP   NOT NULL,
    status      VARCHAR(16) NOT NULL,
    version     BIGINT      NOT NULL DEFAULT 0,
    created_at  TIMESTAMP   NOT NULL,
    updated_at  TIMESTAMP   NOT NULL,
    CONSTRAINT pk_slots PRIMARY KEY (id),
    CONSTRAINT fk_slots_calendar FOREIGN KEY (calendar_id) REFERENCES calendars (id)
);

CREATE INDEX IF NOT EXISTS idx_slots_calendar_time ON slots (calendar_id, start_time, end_time);
CREATE INDEX IF NOT EXISTS idx_slots_status ON slots (status);

CREATE TABLE IF NOT EXISTS meetings (
    id           VARCHAR(36)  NOT NULL,
    slot_id      VARCHAR(36)  NOT NULL,
    organizer_id VARCHAR(36)  NOT NULL,
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL,
    CONSTRAINT pk_meetings PRIMARY KEY (id),
    CONSTRAINT uk_meetings_slot UNIQUE (slot_id),
    CONSTRAINT fk_meetings_slot FOREIGN KEY (slot_id) REFERENCES slots (id),
    CONSTRAINT fk_meetings_organizer FOREIGN KEY (organizer_id) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_meetings_organizer ON meetings (organizer_id);

CREATE TABLE IF NOT EXISTS meeting_participants (
    id              VARCHAR(36) NOT NULL,
    meeting_id      VARCHAR(36) NOT NULL,
    user_id         VARCHAR(36) NOT NULL,
    response_status VARCHAR(16) NOT NULL,
    CONSTRAINT pk_meeting_participants PRIMARY KEY (id),
    CONSTRAINT uk_meeting_user UNIQUE (meeting_id, user_id),
    CONSTRAINT fk_participants_meeting FOREIGN KEY (meeting_id) REFERENCES meetings (id),
    CONSTRAINT fk_participants_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_participants_user ON meeting_participants (user_id);
