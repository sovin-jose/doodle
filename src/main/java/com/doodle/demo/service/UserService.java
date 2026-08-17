package com.doodle.demo.service;

import com.doodle.demo.domain.Calendar;
import com.doodle.demo.domain.User;
import com.doodle.demo.repository.CalendarRepository;
import com.doodle.demo.repository.UserRepository;
import com.doodle.demo.web.dto.CreateUserRequest;
import com.doodle.demo.web.dto.UserResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository users;
    private final CalendarRepository calendars;

    public UserService(UserRepository users, CalendarRepository calendars) {
        this.users = users;
        this.calendars = calendars;
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (users.existsByEmail(request.email())) {
            throw new ConflictException("email already registered: " + request.email());
        }
        User user = users.save(new User(request.name(), request.email()));
        ZoneId tz = request.timezone() == null ? ZoneId.of("UTC") : ZoneId.of(request.timezone());
        Calendar calendar = calendars.save(new Calendar(user, tz));
        return UserResponse.of(user, calendar);
    }

    @Transactional(readOnly = true)
    public UserResponse get(UUID id) {
        User user = users.findById(id)
                .orElseThrow(() -> new NotFoundException("user not found: " + id));
        Calendar calendar = calendars.findByOwnerId(user.getId())
                .orElseThrow(() -> new NotFoundException("calendar not found for user: " + id));
        return UserResponse.of(user, calendar);
    }
}
