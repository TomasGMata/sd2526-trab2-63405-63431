package sd2526.trab.impl.rest.servers;

import java.nio.file.Paths;
import java.util.logging.Logger;

import org.glassfish.jersey.server.ResourceConfig;

import sd2526.trab.api.java.Users;
import sd2526.trab.impl.db.Hibernate;
import sd2526.trab.impl.utils.IP;
import sd2526.trab.impl.utils.ServerConfig;
import sd2526.trab.impl.utils.TLS;

public class RestUsersServer extends AbstractRestServer {
    public static final int PORT = 3456;

    private static Logger Log = Logger.getLogger(RestUsersServer.class.getName());

    RestUsersServer() {
        super(Log, Users.SERVICE_NAME, PORT);
    }

    @Override
    void registerResources(ResourceConfig config) {
        RestUsersResource.isGateway = false;
        config.register(RestUsersResource.class);
    }

    public static void main(String[] args) {
        try {
            if (args.length > 0) ServerConfig.setSecret(args[0]);

            // inicializa Hibernate antes de aceitar requests
            Hibernate.getInstance();

            var server = new RestUsersServer();
            if (args.length > 2) {
                String domain = IP.domain();
                String dir = args[1].substring(0, args[1].lastIndexOf('/') + 1);
                String file = Paths.get(args[1]).getFileName().toString()
                                   .replaceAll("ourorg\\d+", domain);
                String correctedPath = dir + file;
                server.setSSLContext(TLS.serverContextFromFile(correctedPath, args[2]));
            } else {
                throw new RuntimeException("Keystore args required: <secret> <keystore> <password>");
            }
            server.start();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}