# Visualize traces
### In Grafana:
- URL: http://localhost:3000
- User: `admin` / Pass: `admin`
- Go to **Explore** → Select **Tempo**
- Query: `{resource.service.name="ot-initial-service"}`

### In Jaeger:
- URL: http://localhost:16686
- Select Service: ot-initial-service
- Look for recent traces

### ✅ Result:
You see the same traces in both places:
- **Grafana + Tempo:** Advanced visualization with metrics
- **Jaeger:** Simple but effective UI
