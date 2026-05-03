#!/bin/bash
# gerar-certs.sh — corre na raiz do projeto
keytool -genkeypair -alias server -keyalg RSA -keysize 2048 \
  -validity 3650 -keystore src/main/resources/server.ks \
  -storepass sdtp1-2526 -keypass sdtp1-2526 \
  -dname "CN=sd2526, OU=SD, O=FCT, L=Lisboa, ST=Lisboa, C=PT"

keytool -exportcert -alias server \
  -keystore src/main/resources/server.ks \
  -storepass sdtp1-2526 -file /tmp/server.cer

keytool -importcert -alias server -file /tmp/server.cer \
  -keystore src/main/resources/client.ts \
  -storepass sdtp1-2526 -noprompt