package com.francofral.opentelemetry.kafka;

import com.francofral.opentelemetry.person.PersonDto;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Path("/api/v1/people/async")
public class PersonKafkaResource {

    private final PersonKafkaProducerService producerService;

    public PersonKafkaResource(PersonKafkaProducerService producerService) {
        this.producerService = producerService;
    }

    @POST
    public CompletionStage<Response> create(PersonDto personDto) {
        String personId = UUID.randomUUID().toString();
        var personCreatedEvent = new PersonCreatedEvent(personId, personDto);
        producerService.sendPersonCreatedEvent(personCreatedEvent);
        return CompletableFuture.completedFuture(
                Response.accepted()
                        .entity("User creation in progress with event: %s".formatted(personCreatedEvent.eventId()))
                        .build()
        );
    }
}
