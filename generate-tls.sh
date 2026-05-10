#!/bin/bash

mkdir -p tls

HOSTS=(
  "users.ourorg0"
  "messages0.ourorg0"
  "messages1.ourorg0"
  "messages2.ourorg0"
  "users.ourorg1"
  "messages0.ourorg1"
  "messages1.ourorg1"
  "messages2.ourorg1"
  "users.ourorg2"
  "messages.ourorg2"
)

# 1. Gerar keystores para cada host
for HOST in "${HOSTS[@]}"; do
  echo "Gerando keystore para $HOST..."
  keytool -ext SAN=dns:$HOST \
    -genkeypair -alias $HOST \
    -keyalg RSA -validity 365 \
    -keystore tls/$HOST-server.ks \
    -storetype pkcs12 \
    -storepass password \
    -keypass password \
    -dname "CN=$HOST, OU=SD, O=NOVA, L=Lisboa, ST=Lisboa, C=PT" \
    -noprompt
done

# 2. Criar truststore a partir do cacerts
echo "Criando truststore..."
cp /Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/lib/security/cacerts tls/truststore.ks

# 3. Exportar e importar certificados para o truststore
for HOST in "${HOSTS[@]}"; do
  echo "Exportando certificado de $HOST..."
  keytool -exportcert \
    -alias $HOST \
    -keystore tls/$HOST-server.ks \
    -storepass password \
    -file tls/$HOST.cert \
    -noprompt

  echo "Importando certificado de $HOST para truststore..."
  keytool -importcert \
    -file tls/$HOST.cert \
    -alias $HOST \
    -keystore tls/truststore.ks \
    -storepass changeit \
    -noprompt
done

echo "Certificados em tls/"
ls tls/

# 4. Exportar .crt e .pem para cada host (necessário para gRPC TLS)
for HOST in "${HOSTS[@]}"; do
  echo "Exportando .crt e .pem para $HOST..."
  
  # Exportar certificado em formato PEM (.crt)
  keytool -exportcert \
    -alias $HOST \
    -keystore tls/$HOST-server.ks \
    -storepass password \
    -file tls/$HOST-server.crt \
    -rfc \
    -noprompt

  # Exportar chave privada em formato PEM (.pem)
  openssl pkcs12 \
    -in tls/$HOST-server.ks \
    -nocerts -nodes \
    -out tls/$HOST-server.pem \
    -passin pass:password
done