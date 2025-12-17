package com.francofral.opentelemetry.person;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class PersonService {

    private final PersonServiceClient client;

    public PersonService(@RestClient PersonServiceClient client) {
        this.client = client;
    }

    public PersonResponse create(PersonDto payload) {
        return client.create(payload);
    }
}
