package com.francofral.opentelemetry;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(baseUri = "http://localhost:8081")
public interface SecondServiceClient {

    @GET
    @Path("/hello")
    String callSecond();
}