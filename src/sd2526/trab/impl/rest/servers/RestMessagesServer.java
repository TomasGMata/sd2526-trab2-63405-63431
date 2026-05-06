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
import sd2526.trab.impl.utils.SyncPoint;
import sd2526.trab.impl.utils.kafka.KafkaSubscriber;
import sd2526.trab.impl.utils.kafka.KafkaUtils;

public class RestMessagesServer extends AbstractRestServer {
    public static final int PORT = 4567;

    public static final String KAFKA_TOPIC = "messages-" +
        System.getenv().getOrDefault("DOMAIN", "default");

    private static Logger Log = Logger.getLogger(RestMessagesServer.class.getName());
    private static final Gson gson = new Gson();

    RestMessagesServer() {
        super(Log, Messages.SERVICE_NAME, PORT);
    }

    @Override
    void registerResources(ResourceConfig config) {
        config.register(RestMessagesResource.class);
    }

    public static void main(String[] args) {
        if (args.length > 0) ServerConfig.setSecret(args[0]);
        if (args.length > 1) System.setProperty("javax.net.ssl.keyStore", args[1]);
        if (args.length > 2) System.setProperty("javax.net.ssl.keyStorePassword", args[2]);

        KafkaUtils.createTopic(KAFKA_TOPIC);

        KafkaSubscriber.createSubscriber("kafka:9092", List.of(KAFKA_TOPIC))
            .start(record -> {
                JsonObject obj = JsonParser.parseString(record.value()).getAsJsonObject();
                String op = obj.get("op").getAsString();
                SyncPoint sp = SyncPoint.getSyncPoint();

                switch (op) {
                    case "postMessage" -> {
                        String pwd = obj.get("pwd").getAsString();
                        Message msg = gson.fromJson(obj.get("msg"), Message.class);
                        var result = JavaMessages.getInstance().postMessage(pwd, msg);
                        sp.setResult(record.offset(), result.isOK() ? result.value() : null);
                    }
                    case "deleteMessage" -> {
                        String pwd = obj.get("pwd").getAsString();
                        String mid = obj.get("mid").getAsString();
                        String name = obj.get("name").getAsString();
                        var result = JavaMessages.getInstance().deleteMessage(name, mid, pwd);
                        sp.setResult(record.offset(), result.isOK() ? null : result.error().name());
                    }
                }
            });

        new RestMessagesServer().start();
    }
}