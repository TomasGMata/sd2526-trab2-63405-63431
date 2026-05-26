package sd2526.trab.impl.rest.servers;

import java.net.URI;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import sd2526.trab.impl.discovery.Discovery;
import sd2526.trab.impl.java.servers.AbstractServer;
import sd2526.trab.impl.utils.IP;

public abstract class AbstractRestServer extends AbstractServer {

    private static final String SERVER_BASE_URI = "https://%s:%s%s";
    private static final String REST_CTX        = "/rest";
    private static final String INETADDR_ANY    = "0.0.0.0";

    private SSLContext sslContext;

    protected AbstractRestServer(Logger log, String service, int port) {
        super(log, service,
            String.format(SERVER_BASE_URI, IP.hostname(), port, REST_CTX));
    }

    protected void setSSLContext(SSLContext ctx) {
        this.sslContext = ctx;
    }

    protected void start() {
        if (sslContext == null)
            throw new RuntimeException(
                "SSLContext not configured — call setSSLContext() before start()");

        ResourceConfig config = new ResourceConfig();
        registerResources(config);

        URI bindURI = URI.create(
            String.format(SERVER_BASE_URI, INETADDR_ANY,
                URI.create(serverURI).getPort(), REST_CTX));

        JdkHttpServerFactory.createHttpServer(bindURI, config, sslContext);

        if (service != null)
            Discovery.getInstance().announce(serviceName(), super.serverURI);

        Log.info(String.format("%s Server ready @ %s\n", service, serverURI));
    }

    abstract void registerResources(ResourceConfig config);
}