package com.francofral.opentelemetry;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(baseUri = "http://localhost:8082")
public interface ThirdServiceClient {

    @GET
    @Path("/hello")
    String callThird();
}