# IMPORTANT NOTES

## Arquitectura Implementada

**Flujo de llamadas:** Service1 → Service2 → Service3

### Servicios Configurados:
1. ot-initial-service (Service1 - Puerto 8080)
   - Llama a Service2 
   - Tiempo de procesamiento simulado: 100ms

2. ot-second-service (Service2 - Puerto 8081)
   - Llama a Service3
   - Tiempo de procesamiento simulado: 150ms
3. ot-third-service (Service3 - Puerto 8082)
   - Servicio final
   - Tiempo de procesamiento simulado: 200ms

### Configuración OpenTelemetry:
- Todos los servicios envían traces a Jaeger en `localhost:4317`
- Logging configurado con traceId, spanId, etc.
- Cliente REST configurado para llamadas entre servicios

### Visualiza los traces en Jaeger:
- Abre http://localhost:16686
- Busca por servicio "ot-initial-service"
- Verás el trace completo con tiempos de cada span

El trace mostrará el tiempo total de ejecución (~450ms) dividido en cada servicio, permitiéndote ver el rendimiento de cada componente en la cadena de llamadas.
























