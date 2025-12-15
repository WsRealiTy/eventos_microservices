#!/bin/bash
echo "--- Iniciando Compilação dos Microservices ---"

echo "1. Compilando API Gateway..."
mvn clean package -DskipTests -f api_gateway/pom.xml

echo "2. Compilando User Service..."
mvn clean package -DskipTests -f user-service/pom.xml

echo "3. Compilando Event Service..."
mvn clean package -DskipTests -f event-service/pom.xml

echo "4. Compilando Registration Service..."
mvn clean package -DskipTests -f registration-service/pom.xml

echo "5. Compilando Attendance Service..."
mvn clean package -DskipTests -f attendance-service/pom.xml

echo "6. Compilando Certificate Service..."
mvn clean package -DskipTests -f certificate-service/pom.xml

echo "--- Compilação Finalizada! Agora pode rodar o docker compose up ---"
