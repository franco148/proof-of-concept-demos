#!/bin/bash

# Script para iniciar el POC de OpenTelemetry
# Uso: ./start-poc.sh

set -e

echo "🚀 Iniciando POC de OpenTelemetry"
echo "=================================="
echo ""

# Verificar que estamos en el directorio correcto
if [ ! -f "docker-compose.yml" ]; then
    echo "❌ Error: Este script debe ejecutarse desde el directorio ot-initial-service"
    exit 1
fi

# Detener contenedores previos
echo "🧹 Limpiando contenedores previos..."
docker-compose down -v 2>/dev/null || true

# Iniciar infraestructura
echo ""
echo "📦 Iniciando infraestructura de observabilidad..."
docker-compose up -d

# Esperar a que los servicios estén listos
echo ""
echo "⏳ Esperando a que los servicios estén listos..."
sleep 10

# Verificar que los contenedores estén corriendo
echo ""
echo "✅ Estado de los contenedores:"
docker-compose ps

# Mostrar URLs importantes
echo ""
echo "🌐 URLs de acceso:"
echo "   - Grafana:    http://localhost:3000 (admin/admin)"
echo "   - Prometheus: http://localhost:9090"
echo "   - Jaeger UI:  http://localhost:16686"
echo "   - Tempo:      http://localhost:3200"
echo "   - Loki:       http://localhost:3100"
echo ""
echo "📡 OpenTelemetry Collector:"
echo "   - OTLP gRPC:  localhost:4317"
echo "   - OTLP HTTP:  localhost:4318"
echo "   - Prometheus: localhost:8889"
echo ""
echo "🎯 Microservicios (iniciar manualmente en terminales separadas):"
echo "   Terminal 1: cd ot-initial-service && ./mvnw quarkus:dev"
echo "   Terminal 2: cd ot-second-service && ./mvnw quarkus:dev"
echo "   Terminal 3: cd ot-third-service && ./mvnw quarkus:dev"
echo ""
echo "🧪 Generar tráfico:"
echo "   curl http://localhost:8080/hello"
echo ""
echo "✨ Infraestructura lista!"

