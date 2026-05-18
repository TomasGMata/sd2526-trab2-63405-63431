package sd2526.trab.impl.rest.servers;

import java.nio.file.Paths;
import java.util.logging.Logger;

import org.glassfish.jersey.server.ResourceConfig;

import sd2526.trab.api.java.Messages;
import sd2526.trab.impl.java.servers.JavaZohoMessages;
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
        config.register(RestZohoMessagesResource.class);
    }

    public static void main(String[] args) {
        try {
            // args: [0]=cleanState(true/false), [1]=secret, [2]=keystore, [3]=password
            boolean cleanState = args.length > 0 && Boolean.parseBoolean(args[0]);

            if (args.length > 1)
                ServerConfig.setSecret(args[1]);

            JavaZohoMessages.setCleanState(cleanState);

            var server = new RestZohoMessagesServer();

            if (args.length > 3) {
                String domain = IP.domain();
                String dir = args[2].substring(0, args[2].lastIndexOf('/') + 1);
                String file = Paths.get(args[2]).getFileName().toString()
                        .replaceAll("ourorg\\d+", domain);

                server.setSSLContext(TLS.serverContextFromFile(dir + file, args[3]));
            } else {
                throw new RuntimeException("Args: <cleanState> <secret> <keystore> <password>");
            }

            server.start();

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}