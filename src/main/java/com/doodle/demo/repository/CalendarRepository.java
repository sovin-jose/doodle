package com.doodle.demo.repository;

import com.doodle.demo.domain.Calendar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CalendarRepository extends JpaRepository<Calendar, UUID> {
    Optional<Calendar> findByOwnerId(UUID ownerId);
}
