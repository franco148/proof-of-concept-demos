package com.francofral.opentelemetry.person;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/api/v1/people")
@RegisterRestClient(configKey = "ot-second-service")
public interface PersonServiceClient {

    @POST
    PersonResponse create(PersonDto payload);
}
