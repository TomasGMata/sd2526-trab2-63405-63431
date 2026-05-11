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

# 4. Exportar .crt e .pem para cada host (necessário para gRPC TLS)
for HOST in "${HOSTS[@]}"; do
  echo "Exportando .crt e .pem para $HOST..."
  keytool -exportcert \
    -alias $HOST \
    -keystore tls/$HOST-server.ks \
    -storepass password \
    -file tls/$HOST-server.crt \
    -rfc \
    -noprompt

  openssl pkcs12 \
    -in tls/$HOST-server.ks \
    -nocerts -nodes \
    -out tls/$HOST-server.pem \
    -passin pass:password
done

# 5. Criar keystores numerados que o tester usa
# users0.jks → users.ourorg0, users1.jks → users.ourorg1, users2.jks → users.ourorg2
echo "Criando keystores numerados para o tester..."
cp tls/users.ourorg0-server.ks tls/users0.jks
cp tls/users.ourorg1-server.ks tls/users1.jks
cp tls/users.ourorg2-server.ks tls/users2.jks

# messages0-0.jks → messages0.ourorg0, messages0-1.jks → messages0.ourorg1
cp tls/messages0.ourorg0-server.ks tls/messages0-0.jks
cp tls/messages0.ourorg1-server.ks tls/messages0-1.jks

cp tls/messages1.ourorg0-server.ks tls/messages1-0.jks
cp tls/messages1.ourorg1-server.ks tls/messages1-1.jks

cp tls/messages2.ourorg0-server.ks tls/messages2-0.jks
cp tls/messages2.ourorg1-server.ks tls/messages2-1.jks

# client-truststore.jks → truststore.ks
cp tls/truststore.ks tls/client-truststore.jks

echo "Todos os certificados gerados em tls/"
ls tls/