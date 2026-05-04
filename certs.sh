#!/bin/bash
mkdir -p tls
PWD_STORE="sdtp2-2526"
CLIENT_TS="tls/client-truststore.jks"

SERVERS=(
  "users.ourorg0:tls/users0.jks"
  "messages0.ourorg0:tls/messages0-0.jks"
  "messages1.ourorg0:tls/messages1-0.jks"
  "messages2.ourorg0:tls/messages2-0.jks"
  "users.ourorg1:tls/users1.jks"
  "messages0.ourorg1:tls/messages0-1.jks"
  "messages1.ourorg1:tls/messages1-1.jks"
  "messages2.ourorg1:tls/messages2-1.jks"
  "users.ourorg2:tls/users2.jks"
  "messages.ourorg2:tls/messages-2.jks"
)

for entry in "${SERVERS[@]}"; do
  HOST="${entry%%:*}"
  KS="${entry##*:}"
  keytool -genkeypair -alias server -keyalg RSA -keysize 2048 \
    -validity 3650 -keystore "$KS" \
    -storepass "$PWD_STORE" -keypass "$PWD_STORE" \
    -dname "CN=$HOST, OU=SD, O=FCT, L=Lisboa, ST=Lisboa, C=PT" \
    -ext "SAN=dns:$HOST"
  keytool -exportcert -alias server -keystore "$KS" \
    -storepass "$PWD_STORE" -file /tmp/${HOST}.cer
  keytool -importcert -alias "$HOST" -file /tmp/${HOST}.cer \
    -keystore "$CLIENT_TS" -storepass "$PWD_STORE" -noprompt
done
echo "Certificados gerados em tls/"