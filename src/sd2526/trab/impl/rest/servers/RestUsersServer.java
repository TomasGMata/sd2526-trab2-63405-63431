package sd2526.trab.impl.rest.servers;

import java.util.logging.Logger;

import org.glassfish.jersey.server.ResourceConfig;

import sd2526.trab.api.java.Users;
import sd2526.trab.impl.utils.TLS;
import sd2526.trab.impl.utils.ServerConfig;
import sd2526.trab.impl.utils.SyncPoint;
import sd2526.trab.impl.utils.kafka.KafkaSubscriber;
import sd2526.trab.impl.utils.kafka.KafkaUtils;

public class RestUsersServer extends AbstractRestServer {
    public static final int PORT = 3456;

    private static Logger Log = Logger.getLogger(RestUsersServer.class.getName());

    RestUsersServer() {
        super(Log, Users.SERVICE_NAME, PORT);
    }

    @Override
    void registerResources(ResourceConfig config) {
        config.register(RestUsersResource.class);
    }

    public static void main(String[] args) {

        try {
            var server = new RestUsersServer();
            if (args.length > 2)
                server.setSSLContext(TLS.serverContextFromFile(args[1], args[2]));
            else
                throw new RuntimeException("Keystore args required: <secret> <keystore> <password>");
            server.start();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

}