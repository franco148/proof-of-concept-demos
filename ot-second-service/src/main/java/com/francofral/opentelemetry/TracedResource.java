package com.francofral.opentelemetry;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.concurrent.TimeUnit;

@Path("/hello")
public class TracedResource {

    private static final Logger LOG = Logger.getLogger(TracedResource.class);

    @RestClient
    ThirdServiceClient thirdServiceClient;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() throws InterruptedException {
        LOG.info("Service2: Starting request");
        TimeUnit.MILLISECONDS.sleep(150); // Simulate processing time
        String response = thirdServiceClient.callThird();
        LOG.info("Service2: Received response from Service3: " + response);
        return "Service2 -> " + response;
    }
}