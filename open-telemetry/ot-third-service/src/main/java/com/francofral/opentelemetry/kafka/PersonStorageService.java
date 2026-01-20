package com.francofral.opentelemetry.kafka;

import com.francofral.opentelemetry.person.Person;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.smallrye.reactive.messaging.kafka.Record;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class PersonStorageService {

    private static final Logger LOGGER = Logger.getLogger(PersonStorageService.class);
    private final ObjectMapper objectMapper;
    List<Person> people = new ArrayList<>();

    public PersonStorageService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Incoming("validated-people-in")
    public void store(Record<String, String> event) {
        try {
            String jsonValue = event.value();
            LOGGER.infof("Received message for storage: %s", jsonValue);
            
            PersonCreatedEvent personCreatedEvent = objectMapper.readValue(jsonValue, PersonCreatedEvent.class);
            LOGGER.infof("Storing person: %s", personCreatedEvent);
            
            Person personToPersist = new Person(
                    personCreatedEvent.personId(),
                    personCreatedEvent.personName(),
                    personCreatedEvent.personLastname(),
                    personCreatedEvent.personAge()
            );
            TimeUnit.MILLISECONDS.sleep(100);
            people.add(personToPersist);
            TimeUnit.MILLISECONDS.sleep(100);
            LOGGER.infof("Successfully stored person with ID: %s", personCreatedEvent.personId());
        } catch (Exception e) {
            LOGGER.error("Error processing event for storage", e);
        }
    }
}
