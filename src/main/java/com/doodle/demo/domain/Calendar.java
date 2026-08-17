package com.doodle.demo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "calendars")
public class Calendar {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, unique = true)
    private User owner;

    @Column(name = "timezone", nullable = false)
    private String timezone;

    protected Calendar() {
    }

    public Calendar(User owner, ZoneId timezone) {
        this.id = UUID.randomUUID();
        this.owner = owner;
        this.timezone = (timezone == null ? ZoneId.of("UTC") : timezone).getId();
    }

    public UUID getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public ZoneId getTimezone() {
        return ZoneId.of(timezone);
    }

    public void setTimezone(ZoneId timezone) {
        this.timezone = timezone.getId();
    }
}
