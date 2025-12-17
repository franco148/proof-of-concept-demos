package com.francofral.opentelemetry.person;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/people")
public class PersonResource {

    private final PersonService personService;

    public PersonResource(PersonService personService) {
        this.personService = personService;
    }

    @POST
    public Response create(PersonDto personDto) {
        var personResponse = personService.create(personDto);
        return Response.ok().entity(personResponse).build();
    }
}
