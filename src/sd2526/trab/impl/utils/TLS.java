package sd2526.trab.impl.utils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class TLS {

    private static String TRUSTSTORE_PATH = "/home/sd/tls/truststore.ks";
    private static String TRUSTSTORE_PWD  = "changeit";

    public static SSLContext serverContext() {
        try {
            return SSLContext.getDefault();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create server SSLContext", e);
        }
    }

    public static SSLContext clientContext() {
        try {
            KeyStore ts = loadStore(TRUSTSTORE_PATH, TRUSTSTORE_PWD, "JKS");
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ts);

            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, tmf.getTrustManagers(), null);
            return ctx;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create client SSLContext", e);
        }
    }

    private static KeyStore loadStore(String path, String pwd, String type) throws Exception {
        KeyStore ks = KeyStore.getInstance(type);
        try (InputStream is = new FileInputStream(path)) {
            ks.load(is, pwd.toCharArray());
        }
        return ks;
    }
}