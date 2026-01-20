package com.francofral.opentelemetry.kafka;

import com.francofral.opentelemetry.person.PersonDto;

import java.time.LocalDateTime;
import java.util.UUID;

public record PersonCreatedEvent(
        String eventId,
        String personId,
        String personName,
        String personLastname,
        int personAge,
        LocalDateTime timestamp
) {

    public PersonCreatedEvent(String personId, PersonDto personDto) {
        this(
                UUID.randomUUID().toString(),
                personId,
                personDto.name(),
                personDto.lastname(),
                personDto.age(),
                LocalDateTime.now()
        );
    }
}
