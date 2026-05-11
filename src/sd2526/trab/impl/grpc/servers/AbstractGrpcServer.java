package sd2526.trab.impl.grpc.servers;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import sd2526.trab.impl.discovery.Discovery;
import sd2526.trab.impl.java.servers.AbstractServer;
import sd2526.trab.impl.utils.IP;

public abstract class AbstractGrpcServer extends AbstractServer {

    private static final String SERVER_BASE_URI = "grpc://%s:%s%s";
    private static final String GRPC_CTX = "/grpc";

    protected final Server server;

    protected AbstractGrpcServer(Logger log, String service, int port) {

        super(log, service,
                String.format(SERVER_BASE_URI, IP.hostname(), port, GRPC_CTX));

        var builder = NettyServerBuilder.forPort(port);

        var keystore = System.getProperty("javax.net.ssl.keyStore");

        if (keystore == null) {
            keystore = "/home/sd/tls/" + IP.hostname() + "-server";
            System.setProperty("javax.net.ssl.keyStore", keystore);
            Log.info("keyStore not set, defaulting to: " + keystore);
        }

        if (keystore.endsWith(".ks"))
            keystore = keystore.substring(0, keystore.length() - 3);

        var cert = new File(keystore + ".crt");
        var key  = new File(keystore + ".pem");

        if (cert.exists() && key.exists()) {
            try {
                builder.useTransportSecurity(cert, key);
                Log.info("gRPC TLS enabled using: " + cert.getAbsolutePath());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException(
                "Ficheiros TLS não encontrados:\n  cert: " + cert.getAbsolutePath()
                + "\n  key:  " + key.getAbsolutePath());
        }

        for (var s : controllers(super.serverURI))
            builder.addService(s);

        this.server = builder.build();
    }

    protected abstract List<GrpcController> controllers(String uri);

    protected void start() throws IOException {

        Discovery.getInstance().announce(serviceName(), super.serverURI);

        server.start();

        Log.info(String.format("%s gRPC Server ready @ %s\n", service, serverURI));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            server.shutdownNow();
            System.err.println("*** server shut down");
        }));

        try {
            server.awaitTermination(); // ← bloqueia o processo
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.warning("gRPC server interrupted: " + e.getMessage());
        }
    }
}