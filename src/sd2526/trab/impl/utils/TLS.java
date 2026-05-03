package sd2526.trab.impl.utils;

import javax.net.ssl.*;
import java.io.InputStream;
import java.security.KeyStore;

public class TLS {

    private static final String KEYSTORE_PATH   = "/tls/server.ks";
    private static final String TRUSTSTORE_PATH = "/tls/client.ts";
    private static final String PASSWORD        = "sdtp1-2526";

    /** SSLContext para o servidor (tem a chave privada) */
    public static SSLContext serverContext() {
        try {
            KeyStore ks = loadStore(KEYSTORE_PATH);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, PASSWORD.toCharArray());

            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(kmf.getKeyManagers(), null, null);
            return ctx;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create server SSLContext", e);
        }
    }

    /** SSLContext para o cliente (confia no certificado do servidor) */
    public static SSLContext clientContext() {
        try {
            KeyStore ts = loadStore(TRUSTSTORE_PATH);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ts);

            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, tmf.getTrustManagers(), null);
            return ctx;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create client SSLContext", e);
        }
    }

    private static KeyStore loadStore(String path) throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        try (InputStream is = TLS.class.getResourceAsStream(path)) {
            if (is == null) throw new RuntimeException("TLS store not found: " + path);
            ks.load(is, PASSWORD.toCharArray());
        }
        return ks;
    }
}