package com.francofral.opentelemetry;

import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
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

    private final LongCounter requestCounter;
    private final Tracer tracer;

    public TracedResource(Meter meter, Tracer tracer) {
        this.tracer = tracer;
        this.requestCounter = meter
                .counterBuilder("service_requests_total")
                .setDescription("Total number of service requests")
                .setUnit("requests")
                .build();
    }

    @RestClient
    ThirdServiceClient thirdServiceClient;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() throws Exception {
        long startTime = System.currentTimeMillis();

        Span span = tracer.spanBuilder("hello-processing").startSpan();

        try (Scope scope = span.makeCurrent()) {
            LOG.info("Service2: Starting request");
            requestCounter.add(1);

            span.setAttribute("service.name", "ot-second-service");
            span.addEvent("Processing started");

            TimeUnit.MILLISECONDS.sleep(150); // Simulate processing time

            span.addEvent("Calling third service");
            String response = thirdServiceClient.callThird();
            LOG.info("Service2: Received response from Service3: " + response);

            DateFormat formatoDestino = new SimpleDateFormat("HH:mm:ss");
            String result = "Service2 [ " + formatoDestino.format(new Date()) + " ] -> " + response;

            long duration = System.currentTimeMillis() - startTime;
            span.setAttribute("request.duration_ms", duration);
            span.addEvent("Processing completed");

            return result;
        } catch (Exception e) {
            span.recordException(e);
            span.setAttribute("error", true);
            throw e;
        } finally {
            span.end();
        }
    }
}