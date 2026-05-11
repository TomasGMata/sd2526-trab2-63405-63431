package sd2526.trab.impl.grpc.servers;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;

import sd2526.trab.api.java.Messages;
import sd2526.trab.impl.db.Hibernate;
import sd2526.trab.impl.utils.IP;
import sd2526.trab.impl.utils.ServerConfig;

public class GrpcMessagesServer extends AbstractGrpcServer {

    public static final int PORT = 14567;

    private static Logger Log =
            Logger.getLogger(GrpcMessagesServer.class.getName());

    public GrpcMessagesServer() {
        super(Log, Messages.SERVICE_NAME, PORT);
    }

    @Override
    protected List<GrpcController> controllers(String uri) {
        return List.of(
                new GrpcMessagesController(),
                new GrpcAdminMessagesController());
    }

    public static void main(String[] args) {
        try {
            if (args.length >= 1)
                ServerConfig.setSecret(args[0]);

            String domain = IP.domain();

            if (args.length >= 2) {
                String dir = args[1].substring(0, args[1].lastIndexOf('/') + 1);
                String file = Paths.get(args[1]).getFileName().toString()
                        .replaceAll("ourorg\\d+", domain);
                System.setProperty("javax.net.ssl.keyStore", dir + file);
            }

            if (args.length >= 3)
                System.setProperty("javax.net.ssl.keyStorePassword", args[2]);

            Hibernate.getInstance();
            new GrpcMessagesServer().start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}