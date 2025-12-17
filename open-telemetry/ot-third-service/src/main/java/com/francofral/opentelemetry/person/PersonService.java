package com.francofral.opentelemetry.person;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PersonService {

    private static final Logger LOGGER = Logger.getLogger(PersonService.class);

    List<Person> people = new ArrayList<>();

    public PersonResponse create(PersonDto payload) {
        String randomUUID = UUID.randomUUID().toString();
        LOGGER.infof("Generated UUID to persist a person: %s", randomUUID);

        LOGGER.info("Saving one person");
        var person = new Person(randomUUID, payload.name(), payload.lastname(), payload.age());
        people.add(person);

        var fullname = String.format("%s %s", person.name(), person.lastname());
        PersonResponse personResponse = new PersonResponse(randomUUID, fullname);
        LOGGER.infof("Person response created %s", personResponse);
        return personResponse;
    }
}
