# OpenTelemetry POC - Arquitectura Centralizada

## Arquitectura

```
┌─────────────────────────────────────────────────────────────────┐
│                        Aplicaciones                              │
│  ot-initial-service  →  ot-second-service  →  ot-third-service  │
└────────────────────────┬────────────────────────────────────────┘
                         │ OTLP (gRPC/HTTP)
                         │ Traces + Metrics + Logs
                         ↓
            ┌────────────────────────────┐
            │   OpenTelemetry Collector   │
            │  (Procesamiento Central)    │
            └────────────┬────────────────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
        ↓                ↓                ↓
    ┌──────┐       ┌──────────┐      ┌──────┐
    │ Tempo │       │Prometheus│      │ Loki │
    │(Traces)│      │(Metrics) │      │(Logs)│
    └───┬───┘       └────┬─────┘      └──┬───┘
        │                │               │
        └────────────────┴───────────────┘
                         │
                         ↓
                   ┌──────────┐
                   │  Grafana │
                   │(Visualiz.)│
                   └──────────┘
```

## Ventajas de esta arquitectura

### 1. **Desacoplamiento total**
- Las aplicaciones solo conocen el protocolo OTLP
- No dependen de Prometheus, Loki, Tempo, etc.
- Cambiar backend es transparente para las apps

### 2. **Procesamiento centralizado**
- Filtrado de logs en un solo lugar
- Sampling de trazas configurable
- Enriquecimiento de métricas
- Batching y optimización

### 3. **Estándar de la industria**
- OpenTelemetry es el estándar CNCF
- Compatible con cualquier backend observability
- Futuro-proof para tu organización

### 4. **Simplicidad en las aplicaciones**
- Configuración mínima en cada microservicio
- Solo endpoint y protocol
- Habilitar signals (traces, metrics, logs)

## Configuración de aplicaciones

En cada microservicio (`application.properties`):

```properties
# Solo necesitas esto - punto único de configuración
quarkus.otel.exporter.otlp.endpoint=http://localhost:4317
quarkus.otel.exporter.otlp.protocol=grpc

# Habilitar todas las señales
quarkus.otel.traces.enabled=true
quarkus.otel.metrics.enabled=true
quarkus.otel.logs.enabled=true

# Exportar logs via OTLP
quarkus.otel.logs.exporter=otlp
```

**NO es necesario especificar endpoints individuales**:
- ❌ `quarkus.otel.exporter.otlp.traces.endpoint`
- ❌ `quarkus.otel.exporter.otlp.metrics.endpoint`
- ❌ `quarkus.otel.exporter.otlp.logs.endpoint`

El endpoint base se usa automáticamente para todas las señales.

## Dependencias necesarias

```xml
<dependencies>
    <!-- Core REST API -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-rest</artifactId>
    </dependency>
    
    <!-- OpenTelemetry (Traces + Metrics) -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-opentelemetry</artifactId>
    </dependency>
    
    <!-- OpenTelemetry Logs -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-opentelemetry-logs</artifactId>
    </dependency>
    
    <!-- REST Client (para llamadas entre servicios) -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-rest-client</artifactId>
    </dependency>
</dependencies>
```

**NO necesitas Micrometer**:
- ❌ `quarkus-micrometer-registry-prometheus`
- OpenTelemetry maneja métricas nativamente

## Flujo de datos

### Traces
```
App → OTLP → OTel Collector → Tempo → Grafana
                            → Jaeger (opcional)
```

### Metrics
```
App → OTLP → OTel Collector → Prometheus exporter → Prometheus → Grafana
```

### Logs
```
App → OTLP → OTel Collector → Loki → Grafana
```

## Iniciar el POC

### 1. Iniciar infraestructura
```bash
cd ot-initial-service
docker-compose up -d
```

Esto inicia:
- OpenTelemetry Collector (puertos 4317/4318/8889)
- Tempo (puerto 3200)
- Prometheus (puerto 9090)
- Loki (puerto 3100)
- Grafana (puerto 3000)
- Jaeger UI (puerto 16686)

### 2. Iniciar microservicios

Terminal 1:
```bash
cd ot-initial-service
./mvnw quarkus:dev
```

Terminal 2:
```bash
cd ot-second-service
./mvnw quarkus:dev
```

Terminal 3:
```bash
cd ot-third-service
./mvnw quarkus:dev
```

### 3. Generar tráfico
```bash
# Llamar al servicio inicial, que llamará a los demás
curl http://localhost:8080/hello
```

### 4. Visualizar en Grafana

Abrir http://localhost:3000 (admin/admin)

#### Ver Traces:
1. Ir a "Explore"
2. Seleccionar datasource "Tempo"
3. Buscar trazas por service name o trace ID

#### Ver Metrics:
1. Ir a "Explore"
2. Seleccionar datasource "Prometheus"
3. Buscar métricas: `otel_*` o `http_*`

#### Ver Logs:
1. Ir a "Explore"
2. Seleccionar datasource "Loki"
3. Query: `{job="otel-collector"}`
4. Correlacionar con traces usando traceId

## Configuración del OTel Collector

El archivo `otel-collector-config.yaml` define:

```yaml
receivers:
  otlp:  # Recibe OTLP en gRPC y HTTP

processors:
  batch:  # Agrupa datos para eficiencia
  resource:  # Añade atributos comunes

exporters:
  otlp/tempo:  # Trazas a Tempo
  prometheus:  # Métricas como endpoint Prometheus
  loki:  # Logs a Loki

service:
  pipelines:
    traces: [otlp] → [batch, resource] → [tempo, jaeger]
    metrics: [otlp] → [batch, resource] → [prometheus]
    logs: [otlp] → [batch, resource] → [loki]
```

## Recomendaciones para producción

### 1. Alta disponibilidad
- Deploy múltiples instancias del OTel Collector
- Load balancer delante del Collector
- Failover automático

### 2. Seguridad
- TLS para OTLP
- Autenticación con API keys/tokens
- Network policies para limitar acceso

### 3. Sampling
- Configurar sampling en el Collector
- Mantener 100% de trazas con errores
- Sample normal traffic (e.g., 10%)

### 4. Resource attributes
- Definir en el Collector, no en cada app
- `environment`, `region`, `cluster`, etc.
- Facilita queries y agregaciones

### 5. Monitoreo del Collector
- Prometheus scrape metrics del Collector mismo
- Alertas sobre queue size, dropped spans, etc.
- Health checks y readiness probes

## Troubleshooting

### No veo traces en Tempo
1. Verificar logs del OTel Collector: `docker logs ot-initial-service-otel-collector-1`
2. Verificar que las apps envían: buscar `otlp` en logs de la app
3. Verificar conectividad: `curl http://localhost:4317` (debe conectar)

### No veo metrics en Prometheus
1. Verificar targets en Prometheus: http://localhost:9090/targets
2. Debe aparecer `otel-collector:8889` como UP
3. Buscar métricas: `{job="otel-collector"}`

### No veo logs en Loki
1. Verificar que `quarkus.otel.logs.enabled=true`
2. Verificar que `quarkus.otel.logs.exporter=otlp`
3. Check logs del Collector por errores enviando a Loki

## Comparación: Antes vs Ahora

### ❌ Antes (sin OTel Collector centralizado)
```
App → Prometheus (métricas directas)
App → Tempo (trazas directas)
App → File → Promtail → Loki (logs)
```
Problemas:
- Apps conocen múltiples backends
- Configuración duplicada en cada app
- Difícil cambiar backends
- No hay procesamiento centralizado

### ✅ Ahora (con OTel Collector)
```
App → OTLP → OTel Collector → {Tempo, Prometheus, Loki}
```
Ventajas:
- Apps solo conocen OTLP
- Configuración en un solo lugar
- Cambiar backend es transparente
- Procesamiento, filtrado, sampling centralizado

## Preguntas frecuentes

**Q: ¿Necesito Micrometer?**
A: No. OpenTelemetry maneja métricas nativamente desde Quarkus 3.x

**Q: ¿Necesito especificar endpoint para cada signal?**
A: No. Con `quarkus.otel.exporter.otlp.endpoint` es suficiente.

**Q: ¿Puedo usar esto en producción?**
A: Sí, con las recomendaciones de HA, seguridad y sampling.

**Q: ¿Qué pasa si el Collector cae?**
A: Las apps almacenan en buffer temporalmente. Deploy múltiples Collectors.

**Q: ¿Cómo correlaciono logs con traces?**
A: Los logs incluyen `traceId` y `spanId` automáticamente en el contexto.

## Recursos adicionales

- [OpenTelemetry Docs](https://opentelemetry.io/docs/)
- [Quarkus OpenTelemetry Guide](https://quarkus.io/guides/opentelemetry)
- [OTel Collector Config](https://opentelemetry.io/docs/collector/configuration/)
- [CNCF OpenTelemetry](https://www.cncf.io/projects/opentelemetry/)

