package com.francofral.opentelemetry.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.smallrye.reactive.messaging.kafka.Record;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class PersonKafkaConsumerService {

    private static final Logger LOGGER = Logger.getLogger(PersonKafkaConsumerService.class);
    private static final List<String> ALLOWED_NAMES = List.of("Franco", "Juan", "Jose", "Maria", "Michael");
    private final ObjectMapper objectMapper;

    public PersonKafkaConsumerService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Inject
    @Channel("people-validation-out")
    Emitter<Record<String, String>> validationEmitter;
    
    @Incoming("people-in")
    public void process(Record<String, String> record) {
        try {
            // Manual JSON deserialization to preserve trace context
            String jsonValue = record.value();
            LOGGER.infof("Received message: %s", jsonValue);
            
            PersonCreatedEvent personCreatedEvent = objectMapper.readValue(jsonValue, PersonCreatedEvent.class);
            String name = personCreatedEvent.personName();
            int age = personCreatedEvent.personAge();

            boolean isValidName = ALLOWED_NAMES.contains(name);

            if (isValidName) LOGGER.info("Person name %s is valid".formatted(name));
            else LOGGER.error("Person name %s is not valid".formatted(name));

            LOGGER.info(age < 18 ? "Person is underage" : "Person is overage");

            sendToThirdService(personCreatedEvent.personId(), jsonValue);

        } catch (Exception e) {
            LOGGER.error("Error processing event from first service", e);
        }
    }

    private void sendToThirdService(String personId, String jsonEvent) {
        validationEmitter.send(Record.of(personId, jsonEvent))
                .thenAccept(unused -> LOGGER.infof("Event sent to third service: %s", personId))
                .exceptionally(throwable -> {
                    LOGGER.errorf("Error event sent to third service: %s", throwable.getMessage());
                    return null;
                });
    }

}
