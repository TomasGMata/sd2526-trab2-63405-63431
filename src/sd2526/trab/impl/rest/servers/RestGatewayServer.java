package sd2526.trab.impl.rest.servers;

import java.nio.file.Paths;
import java.util.logging.Logger;

import org.glassfish.jersey.server.ResourceConfig;

import sd2526.trab.impl.utils.IP;
import sd2526.trab.impl.utils.ServerConfig;
import sd2526.trab.impl.utils.TLS;

public class RestGatewayServer extends AbstractRestServer {

    public static final int PORT = 6666;

    private static Logger Log = Logger.getLogger(RestGatewayServer.class.getName());

    RestGatewayServer() {
        super(Log, null, PORT);
    }

    @Override
    void registerResources(ResourceConfig config) {
        RestUsersResource.isGateway = true;
        RestMessagesResource.isGateway = true;
        config.register(RestUsersResource.class);
        config.register(RestMessagesResource.class);
    }

    public static void main(String[] args) {
        try {
            if (args.length > 0) ServerConfig.setSecret(args[0]);

            var server = new RestGatewayServer();

            if (args.length > 2) {
                String domain = IP.domain();
                String dir = args[1].substring(0, args[1].lastIndexOf('/') + 1);
                String file = Paths.get(args[1]).getFileName().toString()
                                   .replaceAll("ourorg\\d+", domain);
                server.setSSLContext(TLS.serverContextFromFile(dir + file, args[2]));
            } else {
                server.setSSLContext(TLS.serverContext());
            }

            server.start();
        } catch (Throwable t) {
            System.err.println("GATEWAY ERROR: " + t.getMessage());
            t.printStackTrace(System.err);
        }
    }
}