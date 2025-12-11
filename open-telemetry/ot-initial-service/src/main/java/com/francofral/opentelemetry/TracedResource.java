package com.francofral.opentelemetry;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Path("/hello")
public class TracedResource {

    private static final Logger LOG = Logger.getLogger(TracedResource.class);

    private final Counter requestCounter;
    private final Timer requestTimer;

    public TracedResource(MeterRegistry registry) {
        this.requestCounter = Counter.builder("service_requests_total")
                .description("Total number of service requests")
                .tag("service", "ot-initial-service")
                .register(registry);

        this.requestTimer = Timer.builder("service_request_duration")
                .description("Service request duration")
                .tag("service", "ot-initial-service")
                .register(registry);
    }

    @RestClient
    SecondServiceClient secondServiceClient;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() throws Exception {
        return requestTimer.recordCallable(() -> {
            LOG.info("Service1: Starting request");
            requestCounter.increment();

            TimeUnit.MILLISECONDS.sleep(100); // Simulate processing time
            String response = secondServiceClient.callSecond();
            LOG.info("Service1: Received response from Service2: " + response);

            DateFormat formatoDestino = new SimpleDateFormat("HH:mm:ss");
            return "Service1 [ " + formatoDestino.format(new Date()) + " ]";
        });
    }
}

