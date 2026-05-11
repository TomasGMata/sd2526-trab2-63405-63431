package sd2526.trab.impl.rest.servers;

import java.nio.file.Paths;
import java.util.logging.Logger;

import org.glassfish.jersey.server.ResourceConfig;

import sd2526.trab.api.java.Messages;
import sd2526.trab.impl.utils.IP;
import sd2526.trab.impl.utils.ServerConfig;
import sd2526.trab.impl.utils.TLS;

public class RestZohoMessagesServer extends AbstractRestServer {

    public static final int PORT = 5567;

    private static Logger Log = Logger.getLogger(RestZohoMessagesServer.class.getName());

    RestZohoMessagesServer() {
        super(Log, Messages.SERVICE_NAME, PORT);
    }

    @Override
    void registerResources(ResourceConfig config) {
        config.register(RestZohoMessagesResource.class);  // ← subclasse Zoho
    }

    public static void main(String[] args) {
        try {
            if (args.length > 0) ServerConfig.setSecret(args[0]);

            var server = new RestZohoMessagesServer();
            if (args.length > 2) {
                String domain = IP.domain();
                String dir  = args[1].substring(0, args[1].lastIndexOf('/') + 1);
                String file = Paths.get(args[1]).getFileName().toString()
                                   .replaceAll("ourorg\\d+", domain);
                server.setSSLContext(TLS.serverContextFromFile(dir + file, args[2]));
            } else {
                throw new RuntimeException("Args: <secret> <keystore> <password>");
            }

            server.start();

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}