package com.francofral.opentelemetry;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@Path("/hello")
public class TracedResource {

    private static final Logger LOG = Logger.getLogger(TracedResource.class);

    @RestClient
    SecondServiceClient secondServiceClient;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        LOG.info("Service1: Starting request");
        String response = secondServiceClient.callSecond();
        LOG.info("Service1: Received response from Service2: " + response);
        return "Service1 -> " + response;
    }
}

