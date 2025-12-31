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
import org.jboss.logging.Logger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

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

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() throws Exception {
        long startTime = System.currentTimeMillis();

        Span span = tracer.spanBuilder("hello-processing").startSpan();

        try (Scope scope = span.makeCurrent()) {
            LOG.info("Service3: Starting request");
            requestCounter.add(1);

            span.setAttribute("service.name", "ot-third-service");
            span.addEvent("Processing started");

            LOG.info("Service3: Finishing the business logic in service 3, back to service 2");

            DateFormat formatoDestino = new SimpleDateFormat("HH:mm:ss");
            String result = "Service3 [ " + formatoDestino.format(new Date()) + " ]";

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

