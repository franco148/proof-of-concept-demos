package com.francofral.opentelemetry.person;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class PersonService {

    private static final Logger LOGGER = Logger.getLogger(PersonService.class);
    private static final List<String> ALLOWED_NAMES = List.of("Franco", "Juan", "Jose", "Maria", "Michael");

    private final PersonServiceClient client;

    public PersonService(@RestClient PersonServiceClient client) {
        this.client = client;
    }

    public PersonResponse create(PersonDto payload) {
        var name = payload.name();
        boolean isValidName = ALLOWED_NAMES.contains(name);

        if (isValidName) LOGGER.info("Person name %s is valid".formatted(name));
        else LOGGER.error("Person name %s is not valid".formatted(name));

        LOGGER.info(payload.age() < 18 ? "Person is underage" : "Person is overage");
        return client.create(payload);
    }
}
