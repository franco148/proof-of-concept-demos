package com.francofral.opentelemetry.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.smallrye.reactive.messaging.kafka.Record;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

import java.util.concurrent.TimeUnit;


@ApplicationScoped
public class PersonKafkaProducerService {

    private static final Logger LOG = Logger.getLogger(PersonKafkaProducerService.class);
    private final ObjectMapper objectMapper;

    public PersonKafkaProducerService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Inject
    @Channel("people-out")
    Emitter<Record<String, String>> emitter;

    public void sendPersonCreatedEvent(PersonCreatedEvent event) {
        try {
            // Manual JSON serialization to preserve trace context
            String jsonEvent = objectMapper.writeValueAsString(event);
            Record<String, String> record = Record.of(event.personId(), jsonEvent);
            TimeUnit.MILLISECONDS.sleep(100);
            emitter.send(record)
                    .thenAccept(result -> LOG.infof("Event sent to kafka: %s", event.eventId()))
                    .exceptionally(throwable -> {
                        LOG.errorf("Error event sent: %s", throwable.getMessage());
                        return null;
                    });
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (Exception e) {
            LOG.errorf("Error serializing event: %s", e.getMessage());
        }
    }
}
