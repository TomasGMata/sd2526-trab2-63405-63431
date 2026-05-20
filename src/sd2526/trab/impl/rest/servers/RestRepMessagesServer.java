package sd2526.trab.impl.rest.servers;

import java.util.List;
import java.util.logging.Logger;

import org.glassfish.jersey.server.ResourceConfig;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import sd2526.trab.api.Message;
import sd2526.trab.api.java.Messages;
import sd2526.trab.impl.java.servers.JavaMessages;
import sd2526.trab.impl.utils.ServerConfig;
import sd2526.trab.impl.utils.kafka.KafkaSubscriber;
import sd2526.trab.impl.utils.kafka.KafkaUtils;
import sd2526.trab.impl.utils.TLS;
import sd2526.trab.impl.utils.VersionHolder;
import sd2526.trab.impl.utils.VersionResponseFilter;


public class RestRepMessagesServer extends AbstractRestServer {

    public static final int PORT = 4567;

    public static final String KAFKA_TOPIC = "rep-messages-" +
        System.getenv().getOrDefault("DOMAIN", "default");

    private static Logger Log = Logger.getLogger(RestRepMessagesServer.class.getName());
    private static final Gson gson = new Gson();

    // Versão atual processada por esta réplica (= último offset processado)
    public static volatile long currentVersion = -1;

    RestRepMessagesServer() {
        super(Log, Messages.SERVICE_NAME, PORT);
    }

    @Override
    void registerResources(ResourceConfig config) {
        config.register(RestRepMessagesResource.class);
        config.register(VersionResponseFilter.class);
    }

    public static void main(String[] args) {
        try {
            if (args.length > 0) ServerConfig.setSecret(args[0]);

            var server = new RestRepMessagesServer();
            if (args.length > 2)
                server.setSSLContext(TLS.serverContextFromFile(args[1], args[2]));
            else
                throw new RuntimeException("Args required: <secret> <keystore> <password>");

            KafkaUtils.createTopic(KAFKA_TOPIC);

            // Subscreve o tópico — todas as réplicas processam as mesmas mensagens
            KafkaSubscriber.createSubscriber("kafka:9092", List.of(KAFKA_TOPIC))
                .start(record -> {
                    JsonObject obj = JsonParser.parseString(record.value()).getAsJsonObject();
                    String op = obj.get("op").getAsString();

                    switch (op) {
                        case "postMessage" -> {
                            String pwd = obj.get("pwd").getAsString();
                            Message msg = gson.fromJson(obj.get("msg"), Message.class);
                            JavaMessages.getInstance().postMessage(pwd, msg);
                        }
                        case "deleteMessage" -> {
                            String name = obj.get("name").getAsString();
                            String mid  = obj.get("mid").getAsString();
                            String pwd  = obj.get("pwd").getAsString();
                            JavaMessages.getInstance().deleteMessage(name, mid, pwd);
                        }
                        case "removeInboxMessage" -> {
                            String name = obj.get("name").getAsString();
                            String mid  = obj.get("mid").getAsString();
                            String pwd  = obj.get("pwd").getAsString();
                            JavaMessages.getInstance().removeInboxMessage(name, mid, pwd);
                        }
                    }

                    // Actualiza a versão local após processar
                    currentVersion = record.offset();
                    synchronized (RestRepMessagesServer.class) {
                        RestRepMessagesServer.class.notifyAll();
                    }
                });

            server.start();

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    // Bloqueia até esta réplica ter processado pelo menos a versão pedida
    public static void waitForVersion(long version) {
        if (version < 0) return;
        synchronized (RestRepMessagesServer.class) {
            while (currentVersion < version) {
                try {
                    RestRepMessagesServer.class.wait(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}