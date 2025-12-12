# SUGERENCIAS PARA ENTORNOS DONDE EL DEPLOYMENT ESTA EN CLOUDRUN Y KUBERNETES

## Para servicios en Cloud Run (ot-initial-service y ot-second-service):

### Configuración de Tracing:

```
# application.properties
quarkus.otel.exporter.otlp.traces.endpoint=https://cloudtrace.googleapis.com/v1/projects/YOUR_PROJECT_ID/traces
quarkus.otel.exporter.otlp.traces.headers=Authorization=Bearer ${GOOGLE_CLOUD_ACCESS_TOKEN}
quarkus.otel.service.name=ot-initial-service
quarkus.otel.resource.attributes=service.version=1.0.0,service.environment=production
```

### Configuración de Métricas:

```
# Cloud Run usa Google Cloud Monitoring automáticamente
quarkus.micrometer.export.googlecloud.enabled=true
quarkus.micrometer.export.googlecloud.project-id=YOUR_PROJECT_ID
```

### Configuración de Logs:
```
# Cloud Run envía logs automáticamente a Google Cloud Logging
quarkus.log.console.json=true
quarkus.log.console.json.fields.timestamp=@timestamp
quarkus.log.console.json.fields.level=level
quarkus.log.console.json.fields.logger=logger
quarkus.log.console.json.fields.message=message
quarkus.log.console.json.fields.traceId=traceId
quarkus.log.console.json.fields.spanId=spanId

```

## Para servicios en Kubernetes (ot-third-service):

### Configuración de Tracing:

```
# application.properties
quarkus.otel.exporter.otlp.traces.endpoint=http://opentelemetry-collector.observability.svc.cluster.local:4318
quarkus.otel.service.name=ot-third-service
quarkus.otel.resource.attributes=service.version=1.0.0,service.environment=production,k8s.namespace.name=${KUBERNETES_NAMESPACE}

```

### Configuración de Métricas:

```
# Usar Prometheus para métricas en Kubernetes
quarkus.micrometer.export.prometheus.enabled=true
quarkus.micrometer.export.prometheus.path=/metrics

```

### Configuración de Logs:

```
# Logs estructurados para Fluent Bit
quarkus.log.console.json=true
quarkus.log.console.json.fields.timestamp=@timestamp
quarkus.log.console.json.fields.level=level
quarkus.log.console.json.fields.logger=logger
quarkus.log.console.json.fields.message=message
quarkus.log.console.json.fields.traceId=traceId
quarkus.log.console.json.fields.spanId=spanId
quarkus.log.console.json.fields.kubernetes.pod.name=${KUBERNETES_POD_NAME}
quarkus.log.console.json.fields.kubernetes.namespace.name=${KUBERNETES_NAMESPACE}

```


## Configuración de Infraestructura GCP

### OpenTelemetry Collector (para Kubernetes):

```yaml
# otel-collector-config.yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318

processors:
  batch:
    timeout: 1s
    send_batch_size: 1024

exporters:
  googlecloud:
    project: YOUR_PROJECT_ID
  loki:
    endpoint: http://loki.observability.svc.cluster.local:3100/loki/api/v1/push
  prometheus:
    endpoint: http://prometheus.observability.svc.cluster.local:9090/api/v1/write

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [googlecloud]
    metrics:
      receivers: [otlp]
      processors: [batch]
      exporters: [prometheus]
    logs:
      receivers: [otlp]
      processors: [batch]
      exporters: [loki]

```

### Fluent Bit para Kubernetes (para logs):

```
# fluent-bit-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: fluent-bit-config
  namespace: observability
data:
  fluent-bit.conf: |
    [SERVICE]
        Flush         5
        Log_Level     info
        Daemon        off

    [INPUT]
        Name              tail
        Path              /var/log/containers/*ot-third-service*.log
        Parser            docker
        Tag               kube.*
        Refresh_Interval  5

    [FILTER]
        Name                kubernetes
        Match               kube.*
        Kube_URL            https://kubernetes.default.svc:443
        Kube_CA_File        /var/run/secrets/kubernetes.io/serviceaccount/ca.crt
        Kube_Token_File     /var/run/secrets/kubernetes.io/serviceaccount/token

    [OUTPUT]
        Name  loki
        Match kube.*
        Host  loki.observability.svc.cluster.local
        Port  3100
        Labels job=quarkus-services,service=ot-third-service
        LabelKeys traceId,spanId

```

## Pasos para Verificar la Configuración

### Paso 1: Verificar Tracing

```
# Verificar que los traces llegan a Google Cloud Trace
gcloud trace list --project=YOUR_PROJECT_ID --limit=10

# Verificar Jaeger UI (si está configurado como proxy)
curl http://jaeger.your-domain.com/api/traces?service=ot-initial-service

```

### Paso 2: Verificar Métricas

```
# Para Cloud Run - verificar Google Cloud Monitoring
gcloud monitoring query "fetch k8s_container::kubernetes.io/container/cpu/core_usage_time" \
  --project=YOUR_PROJECT_ID \
  --filter="resource.labels.namespace_name:YOUR_NAMESPACE AND resource.labels.pod_name:ot-third-service"

# Para Kubernetes - verificar Prometheus
curl http://prometheus.your-domain.com/api/v1/query?query=up{job="quarkus-services"}

```

### Paso 3: Verificar Logs

```
# Verificar logs en Google Cloud Logging (Cloud Run)
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=ot-initial-service" --limit=10

# Verificar logs en Loki (Kubernetes)
curl "http://loki.your-domain.com/loki/api/v1/query_range?query={job=\"quarkus-services\",service=\"ot-third-service\"}&start=$(date -u +%s -d '1 hour ago')&end=$(date -u +%s)&limit=10"

```

### Paso 4: Verificar Correlación End-to-End

```
# Hacer una petición de prueba
curl http://ot-initial-service.your-domain.com/hello

# Verificar en Grafana que puedes:
# 1. Ver el trace completo en Tempo/Jaeger
# 2. Ver métricas en Prometheus
# 3. Ver logs correlacionados en Loki con el mismo traceId

```

### Paso 5: Verificar Dashboards en Grafana

```
# Verificar que los datasources están configurados
curl -u admin:password http://grafana.your-domain.com/api/datasources

# Verificar que puedes crear queries correlacionadas
# Ejemplo: Mostrar logs filtrados por traceId desde Tempo

```

## Consideraciones Adicionales

### Variables de Entorno por Entorno:

```
# Para Cloud Run
env:
  - name: QUARKUS_OTEL_EXPORTER_OTLP_TRACES_ENDPOINT
    value: "https://cloudtrace.googleapis.com/v1/projects/YOUR_PROJECT_ID/traces"
  - name: GOOGLE_CLOUD_PROJECT
    value: "YOUR_PROJECT_ID"

# Para Kubernetes
env:
  - name: QUARKUS_OTEL_EXPORTER_OTLP_TRACES_ENDPOINT
    value: "http://opentelemetry-collector.observability.svc.cluster.local:4318"
  - name: KUBERNETES_NAMESPACE
    valueFrom:
      fieldRef:
        fieldPath: metadata.namespace

```

### Configuración de Service Account:

```
# Para Cloud Run - IAM roles
gcloud projects add-iam-policy-binding YOUR_PROJECT_ID \
  --member="serviceAccount:YOUR_SERVICE_ACCOUNT@YOUR_PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/cloudtrace.agent"

# Para Kubernetes - RBAC
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: observability-collector
rules:
- apiGroups: [""]
  resources: ["pods", "nodes"]
  verbs: ["get", "list", "watch"]

```

### Monitoreo de Health Checks:

```
# Verificar que todos los servicios responden
curl http://ot-initial-service.your-domain.com/q/health
curl http://ot-second-service.your-domain.com/q/health  
curl http://ot-third-service.your-domain.com/q/health

# Verificar métricas de health
curl http://prometheus.your-domain.com/api/v1/query?query=up{job="quarkus-health"}

```

Esta configuración te permitirá tener observabilidad completa en tu entorno híbrido GCP, manteniendo la correlación entre traces, métricas y logs a través de todos los servicios desplegados en diferentes plataformas.