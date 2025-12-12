#!/bin/bash
echo "=== VERIFICACIÓN COMPLETA ==="

echo "1. Servicios Docker:"
docker-compose ps --format "table {{.Name}}\t{{.Status}}"

echo -e "\n2. Procesos Quarkus:"
ps aux | grep quarkus | grep -v grep

echo -e "\n3. Endpoints de métricas:"
curl -s http://localhost:8080/q/metrics | grep -c "service_requests_total" || echo "No metrics"
curl -s http://localhost:8081/q/metrics | grep -c "service_requests_total" || echo "No metrics"  
curl -s http://localhost:8082/q/metrics | grep -c "service_requests_total" || echo "No metrics"

echo -e "\n4. Tempo status:"
curl -s http://localhost:3200/status | head -1

echo -e "\n5. Prueba de traza:"
curl -s http://localhost:8080/hello
