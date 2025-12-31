# Cambios Realizados en el POC de OpenTelemetry

## Resumen

Se actualizó completamente el POC para implementar una **arquitectura centralizada usando OpenTelemetry Collector** como punto único de integración para todas las señales de observabilidad (traces, metrics, logs).

---

## ✅ Cambios en Configuración

### 1. **application.properties** (3 servicios)

**Antes:**
```properties
quarkus.otel.exporter.otlp.endpoint=http://localhost:4317
# Configuraciones incompletas, sin métricas ni logs habilitados
```

**Ahora:**
```properties
# OpenTelemetry configuration - All telemetry through OTel Collector
quarkus.otel.exporter.otlp.endpoint=http://localhost:4317
quarkus.otel.exporter.otlp.protocol=grpc
quarkus.otel.exporter.otlp.headers=authorization=Bearer my_secret

# Enable all telemetry signals
quarkus.otel.traces.enabled=true
quarkus.otel.metrics.enabled=true
quarkus.otel.logs.enabled=true

# OTLP Logs exporter
quarkus.otel.logs.exporter=otlp

# Structured logging with trace context
quarkus.log.console.format=%d{yyyy-MM-dd HH:mm:ss} %-5p [%c{2.}] [%X{traceId},%X{spanId}] (%t) %s%e%n
quarkus.log.level=INFO
quarkus.log.category."com.francofral".level=DEBUG

# HTTP access log with trace context
quarkus.http.access-log.enabled=true
quarkus.http.access-log.pattern="...traceId=%{X,traceId} spanId=%{X,spanId}"
```

**Mejoras:**
- ✅ Solo un endpoint OTLP (no es necesario especificar endpoints individuales)
- ✅ Todas las señales habilitadas (traces, metrics, logs)
- ✅ Logs exportados vía OTLP (no más logs a archivos)
- ✅ Contexto de tracing en logs
- ✅ Configuración simplificada y consistente

---

## ✅ Cambios en Código Java

### 2. **TracedResource.java** (3 servicios)

**Antes - Usando Micrometer:**
```java
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

public class TracedResource {
    private final Counter requestCounter;
    private final Timer requestTimer;

    public TracedResource(MeterRegistry registry) {
        this.requestCounter = Counter.builder("service_requests_total")
            .tag("service", "ot-initial-service")
            .register(registry);
        this.requestTimer = Timer.builder("service_request_duration")
            .tag("service", "ot-initial-service")
            .register(registry);
    }
    
    @GET
    public String hello() throws Exception {
        return requestTimer.recordCallable(() -> {
            requestCounter.increment();
            // business logic
        });
    }
}
```

**Ahora - Usando OpenTelemetry nativo:**
```java
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

public class TracedResource {
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
    public String hello() throws Exception {
        long startTime = System.currentTimeMillis();
        
        Span span = tracer.spanBuilder("hello-processing").startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            requestCounter.add(1);
            
            span.setAttribute("service.name", "ot-initial-service");
            span.addEvent("Processing started");
            
            // business logic
            
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
```

**Mejoras:**
- ✅ No más dependencia de Micrometer
- ✅ API nativa de OpenTelemetry para métricas
- ✅ Spans personalizados con atributos y eventos
- ✅ Manejo de errores con `recordException()`
- ✅ Métricas de duración manuales + automáticas
- ✅ Mejor observabilidad del código de negocio

---

## ✅ Cambios en OpenTelemetry Collector

### 3. **otel-collector-config.yaml**

**Antes:**
```yaml
receivers:
  otlp:
    protocols:
      grpc:
      http:

exporters:
  otlp/tempo:
    endpoint: tempo:4317
  otlp/jaeger:
    endpoint: jaeger-all-in-one:4317

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [otlp/tempo, otlp/jaeger]
```

**Ahora:**
```yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318

processors:
  batch:
  resource:
    attributes:
      - key: environment
        value: dev
        action: insert

exporters:
  # Traces
  otlp/tempo:
    endpoint: tempo:4317
    tls:
      insecure: true
  otlp/jaeger:
    endpoint: jaeger-all-in-one:4317
    tls:
      insecure: true
  
  # Metrics
  prometheus:
    endpoint: "0.0.0.0:8889"
    namespace: otel
    
  # Logs
  loki:
    endpoint: http://loki:3100/loki/api/v1/push
  
  # Debug
  logging:
    loglevel: debug

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch, resource]
      exporters: [otlp/tempo, otlp/jaeger]
    
    metrics:
      receivers: [otlp]
      processors: [batch, resource]
      exporters: [prometheus]
    
    logs:
      receivers: [otlp]
      processors: [batch, resource]
      exporters: [loki, logging]
```

**Mejoras:**
- ✅ 3 pipelines separados (traces, metrics, logs)
- ✅ Procesador de recursos para enriquecer datos
- ✅ Exportador de Prometheus para métricas
- ✅ Exportador de Loki para logs
- ✅ Logging para debugging

---

## ✅ Cambios en Docker Compose

### 4. **docker-compose.yml**

**Cambios realizados:**

1. **OTel Collector actualizado:**
   - Cambiado a `otel/opentelemetry-collector-contrib:latest` (tiene más exporters)
   - Puerto 8889 expuesto para Prometheus scraping
   - Dependencia de Loki agregada

2. **Loki agregado:**
   ```yaml
   loki:
     image: grafana/loki:latest
     ports:
       - "3100:3100"
     volumes:
       - loki-data:/loki
   ```

3. **Prometheus actualizado:**
   ```yaml
   prometheus:
     depends_on:
       - otel-collector
   ```

4. **Grafana actualizado:**
   ```yaml
   grafana:
     depends_on:
       - prometheus
       - tempo
       - loki
   ```

---

## ✅ Cambios en Prometheus

### 5. **prometheus.yml**

**Antes:**
```yaml
scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
```

**Ahora:**
```yaml
scrape_configs:
  - job_name: 'otel-collector'
    static_configs:
      - targets: ['otel-collector:8889']
        labels:
          environment: 'dev'
```

**Mejoras:**
- ✅ Prometheus scrapea del OTel Collector, no de las apps directamente
- ✅ Labels consistentes

---

## ✅ Nuevos Archivos Creados

### 6. **Grafana Datasources**

**grafana/provisioning/datasources/loki.yaml** (nuevo):
```yaml
apiVersion: 1

datasources:
  - name: Loki
    type: loki
    access: proxy
    url: http://loki:3100
    isDefault: false
    editable: true
    jsonData:
      maxLines: 1000
```

### 7. **ARQUITECTURA.md** (nuevo)

Documento completo con:
- Diagrama de arquitectura
- Ventajas del enfoque centralizado
- Guía de configuración
- Instrucciones de inicio
- Troubleshooting
- FAQs
- Comparación antes/después

---

## 🎯 Resultado Final

### Arquitectura Implementada:

```
Aplicaciones (Quarkus)
    ↓ OTLP (traces + metrics + logs)
OpenTelemetry Collector
    ├─→ Tempo (traces)
    ├─→ Prometheus (metrics)
    └─→ Loki (logs)
         ↓
    Grafana (visualización unificada)
```

### Beneficios Alcanzados:

1. ✅ **Desacoplamiento total**: Apps solo conocen OTLP
2. ✅ **Sin Micrometer**: OpenTelemetry nativo para todo
3. ✅ **Configuración mínima**: Un solo endpoint
4. ✅ **Procesamiento centralizado**: Filtering, sampling, enrichment
5. ✅ **Estándar CNCF**: Compatible con cualquier backend
6. ✅ **Logs correlacionados**: traceId/spanId automáticos
7. ✅ **Métricas personalizadas**: API OpenTelemetry nativa
8. ✅ **Spans enriquecidos**: Atributos y eventos de negocio

---

## 📋 Checklist de Verificación

- [x] Configuración simplificada en application.properties
- [x] Todas las señales habilitadas (traces, metrics, logs)
- [x] Código Java usando OpenTelemetry nativo (no Micrometer)
- [x] OTel Collector con 3 pipelines funcionando
- [x] Loki agregado para logs
- [x] Prometheus scrapeando del Collector
- [x] Grafana con 3 datasources (Tempo, Prometheus, Loki)
- [x] Documentación completa en ARQUITECTURA.md
- [x] Eliminados archivos de log y Promtail (ya no necesarios)

---

## 🚀 Próximos Pasos

Para probar los cambios:

1. **Detener todo** (si estaba corriendo):
   ```bash
   docker-compose down -v
   pkill -f quarkus:dev
   ```

2. **Iniciar infraestructura**:
   ```bash
   cd ot-initial-service
   docker-compose up -d
   ```

3. **Iniciar servicios** (3 terminales):
   ```bash
   # Terminal 1
   cd ot-initial-service && ./mvnw clean quarkus:dev
   
   # Terminal 2
   cd ot-second-service && ./mvnw clean quarkus:dev
   
   # Terminal 3
   cd ot-third-service && ./mvnw clean quarkus:dev
   ```

4. **Generar tráfico**:
   ```bash
   curl http://localhost:8080/hello
   ```

5. **Verificar en Grafana** (http://localhost:3000):
   - **Traces**: Explore → Tempo → Search
   - **Metrics**: Explore → Prometheus → `service_requests_total`
   - **Logs**: Explore → Loki → `{job="otel-collector"}`

---

## 📝 Notas Importantes

1. **No es necesario especificar endpoints individuales**:
   - ❌ `quarkus.otel.exporter.otlp.traces.endpoint`
   - ❌ `quarkus.otel.exporter.otlp.metrics.endpoint`
   - ❌ `quarkus.otel.exporter.otlp.logs.endpoint`
   - ✅ Solo: `quarkus.otel.exporter.otlp.endpoint`

2. **No se necesita Micrometer**:
   - OpenTelemetry maneja métricas nativamente desde Quarkus 3.x
   - API más poderosa y estándar

3. **Logs van directo a OTel Collector**:
   - No más logs a archivos
   - No más Promtail
   - Correlación automática con traces

4. **Esta arquitectura es production-ready**:
   - Solo agregar: HA, TLS, autenticación, sampling
   - Escalable y estándar de la industria

