package sd2526.trab.impl.rest.servers;

import java.net.URI;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import sd2526.trab.impl.discovery.Discovery;
import sd2526.trab.impl.java.servers.AbstractServer;
import sd2526.trab.impl.utils.IP;
import sd2526.trab.impl.utils.TLS;

public abstract class AbstractRestServer extends AbstractServer {
    private static final String SERVER_BASE_URI = "https://%s:%s%s";
    private static final String REST_CTX = "/rest";

    private SSLContext sslContext;

    protected AbstractRestServer(Logger log, String service, int port) {
        super(log, service, String.format(SERVER_BASE_URI, IP.hostname(), port, REST_CTX));
    }

    protected void setSSLContext(SSLContext ctx) {
        this.sslContext = ctx;
    }

    protected void start() {
        ResourceConfig config = new ResourceConfig();

        registerResources(config);

        if (sslContext == null)
            throw new RuntimeException("SSLContext not configured — call setSslContext() before start()");

        JdkHttpServerFactory.createHttpServer(
            URI.create(serverURI.replace(IP.hostAddress(), INETADDR_ANY)),
            config,
            sslContext
        );

        if (service != null)
            Discovery.getInstance().announce(serviceName(), super.serverURI);

        Log.info(String.format("%s Server ready @ %s\n", service, serverURI));
    }

    abstract void registerResources(ResourceConfig config);
}