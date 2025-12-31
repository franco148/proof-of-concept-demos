# OpenTelemetry POC con Quarkus

POC para demostrar una arquitectura de observabilidad centralizada usando **OpenTelemetry Collector** como punto único de integración para traces, metrics y logs.

## 🎯 Objetivo

Implementar observabilidad completa (traces, metrics, logs) en microservicios Quarkus usando:
- **OpenTelemetry** como estándar único
- **OTel Collector** como punto central de procesamiento
- **Sin dependencias directas** a Prometheus, Loki, o Tempo en las aplicaciones

## 🏗️ Arquitectura

```
Aplicaciones (3 microservicios)
    ↓ OTLP (gRPC)
OpenTelemetry Collector
    ├─→ Tempo (traces)
    ├─→ Prometheus (metrics)
    └─→ Loki (logs)
         ↓
    Grafana (visualización)
```

**Ver [ARQUITECTURA.md](./ARQUITECTURA.md) para detalles completos.**

## 🚀 Inicio Rápido

### 1. Iniciar infraestructura

```bash
./start-poc.sh
```

Esto inicia:
- OpenTelemetry Collector
- Tempo (traces)
- Prometheus (metrics)
- Loki (logs)
- Grafana (visualización)
- Jaeger (opcional)

### 2. Iniciar microservicios

**Terminal 1:**
```bash
cd /Users/francofral/Code/POCs/open-telemetry/open-telemetry/ot-initial-service
./mvnw quarkus:dev
```

**Terminal 2:**
```bash
cd /Users/francofral/Code/POCs/open-telemetry/open-telemetry/ot-second-service
./mvnw quarkus:dev
```

**Terminal 3:**
```bash
cd /Users/francofral/Code/POCs/open-telemetry/open-telemetry/ot-third-service
./mvnw quarkus:dev
```

### 3. Generar tráfico

```bash
./generate-traffic.sh 10
```

O manualmente:
```bash
curl http://localhost:8080/hello
```

### 4. Visualizar en Grafana

Abrir: **http://localhost:3000** (admin/admin)

- **Traces**: Explore → Tempo
- **Metrics**: Explore → Prometheus → `service_requests_total`
- **Logs**: Explore → Loki → `{job="otel-collector"}`

## 📊 URLs de acceso

| Servicio | URL | Credenciales |
|----------|-----|--------------|
| Grafana | http://localhost:3000 | admin/admin |
| Prometheus | http://localhost:9090 | - |
| Jaeger UI | http://localhost:16686 | - |
| Tempo | http://localhost:3200 | - |
| Loki | http://localhost:3100 | - |
| OTel Collector (gRPC) | localhost:4317 | - |
| OTel Collector (HTTP) | localhost:4318 | - |
| Initial Service | http://localhost:8080 | - |
| Second Service | http://localhost:8081 | - |
| Third Service | http://localhost:8082 | - |

## 🔧 Configuración

### Aplicaciones

Cada microservicio solo necesita:

```properties
# OpenTelemetry - punto único de configuración
quarkus.otel.exporter.otlp.endpoint=http://localhost:4317
quarkus.otel.exporter.otlp.protocol=grpc

# Habilitar señales
quarkus.otel.traces.enabled=true
quarkus.otel.metrics.enabled=true
quarkus.otel.logs.enabled=true

quarkus.otel.logs.exporter=otlp
```

### Dependencias

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-opentelemetry</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-opentelemetry-logs</artifactId>
</dependency>
```

**NO se necesita Micrometer** - OpenTelemetry maneja métricas nativamente.

## ✅ Ventajas de esta arquitectura

1. **Desacoplamiento**: Apps solo conocen OTLP, no los backends
2. **Flexibilidad**: Cambiar Prometheus/Loki/Tempo sin tocar código
3. **Procesamiento centralizado**: Filtering, sampling, enrichment
4. **Estándar**: OpenTelemetry es el estándar CNCF
5. **Simplicidad**: Configuración mínima en cada app
6. **Correlación**: Logs con traceId/spanId automáticos

## 📚 Documentación

- **[ARQUITECTURA.md](./ARQUITECTURA.md)** - Arquitectura completa y guía detallada
- **[CAMBIOS_REALIZADOS.md](./CAMBIOS_REALIZADOS.md)** - Resumen de cambios en el POC
- **[otel-collector-config.yaml](./otel-collector-config.yaml)** - Configuración del Collector

## 🧪 Testing

### Generar tráfico sostenido

```bash
# 50 requests con pausa entre cada uno
./generate-traffic.sh 50
```

### Verificar métricas en Prometheus

```bash
# Verificar que Prometheus está scrapeando
curl http://localhost:9090/api/v1/targets

# Query de ejemplo
curl -G http://localhost:9090/api/v1/query \
  --data-urlencode 'query=service_requests_total'
```

### Verificar logs en Loki

```bash
# Query logs del collector
curl -G http://localhost:3100/loki/api/v1/query_range \
  --data-urlencode 'query={job="otel-collector"}' \
  --data-urlencode 'limit=10'
```

## 🐛 Troubleshooting

### No veo traces

```bash
# Verificar logs del OTel Collector
docker logs ot-initial-service-otel-collector-1 --tail 50

# Verificar que las apps están enviando
# Buscar "otlp" en logs de la aplicación
```

### No veo métricas

```bash
# Verificar targets en Prometheus
open http://localhost:9090/targets

# Debe aparecer otel-collector:8889 como UP
```

### No veo logs

```bash
# Verificar que el exporter está habilitado
grep "quarkus.otel.logs" src/main/resources/application.properties

# Verificar logs del Loki
docker logs ot-initial-service-loki-1 --tail 50
```

## 🧹 Limpieza

```bash
# Detener infraestructura
docker-compose down -v

# Detener aplicaciones
pkill -f quarkus:dev
```

## 📖 Recursos adicionales

- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [Quarkus OpenTelemetry Guide](https://quarkus.io/guides/opentelemetry)
- [OTel Collector Configuration](https://opentelemetry.io/docs/collector/configuration/)

---

**Nota**: Este es un POC para desarrollo/testing. Para producción, agregar:
- TLS para OTLP
- Autenticación
- Alta disponibilidad del Collector
- Sampling configurado
- Resource attributes centralizados

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/ot-initial-service-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Related Guides

- REST ([guide](https://quarkus.io/guides/rest)): A Jakarta REST implementation utilizing build time processing and Vert.x. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it.
- OpenTelemetry ([guide](https://quarkus.io/guides/opentelemetry)): Use OpenTelemetry to trace services
