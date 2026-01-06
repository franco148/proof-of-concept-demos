package com.francofral.opentelemetry;

import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.Meter;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.io.IOException;

@Provider
public class ActiveRequestsMetricsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger(ActiveRequestsMetricsFilter.class);

    @Inject
    Meter meter;

    private LongUpDownCounter activeRequestsCounter;

    private LongUpDownCounter getActiveRequestsCounter() {
        if (activeRequestsCounter == null) {
            activeRequestsCounter = meter
                    .upDownCounterBuilder("http_server_active_requests")
                    .setDescription("Number of active HTTP server requests")
                    .setUnit("requests")
                    .build();
        }
        return activeRequestsCounter;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LOG.debug("Request started - incrementing active requests");
        getActiveRequestsCounter().add(1);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        LOG.debug("Request completed - decrementing active requests");
        getActiveRequestsCounter().add(-1);
    }
}
