#!/bin/bash

# Script para generar tráfico de prueba
# Uso: ./generate-traffic.sh [numero_de_requests]

REQUESTS=${1:-10}
URL="http://localhost:8080/hello"

echo "🚦 Generando tráfico de prueba"
echo "==============================="
echo "URL: $URL"
echo "Requests: $REQUESTS"
echo ""

for i in $(seq 1 $REQUESTS); do
    echo -n "Request $i/$REQUESTS: "

    RESPONSE=$(curl -s -w "\n%{http_code}" "$URL" 2>/dev/null)
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | head -n-1)

    if [ "$HTTP_CODE" = "200" ]; then
        echo "✅ OK - $BODY"
    else
        echo "❌ ERROR - HTTP $HTTP_CODE"
    fi

    # Pequeña pausa entre requests
    sleep 0.5
done

echo ""
echo "✨ Tráfico generado!"
echo ""
echo "📊 Ahora puedes ver los datos en:"
echo "   - Traces:  http://localhost:3000/explore (seleccionar Tempo)"
echo "   - Metrics: http://localhost:3000/explore (seleccionar Prometheus)"
echo "   - Logs:    http://localhost:3000/explore (seleccionar Loki)"

