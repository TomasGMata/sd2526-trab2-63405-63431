package sd2526.trab.impl.rest.servers;

import java.net.InetAddress;
import java.nio.file.Paths;
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
import sd2526.trab.impl.utils.TLS;
import sd2526.trab.impl.utils.VersionResponseFilter;
import sd2526.trab.impl.utils.kafka.KafkaPublisher;
import sd2526.trab.impl.utils.kafka.KafkaSubscriber;
import sd2526.trab.impl.utils.kafka.KafkaUtils;

public class RestRepMessagesServer extends AbstractRestServer {

    public static final int PORT = 4567;

    public static final String KAFKA_TOPIC = "rep-messages-" +
        System.getenv().getOrDefault("DOMAIN", "default");

    public static final String RESULT_TOPIC = "rep-results-" +
        System.getenv().getOrDefault("DOMAIN", "default");

    private static Logger Log = Logger.getLogger(RestRepMessagesServer.class.getName());
    private static final Gson gson = new Gson();

    private static KafkaPublisher resultPublisher;

    private static synchronized KafkaPublisher getResultPublisher() {
        if (resultPublisher == null)
            resultPublisher = KafkaPublisher.createPublisher("kafka:9092");
        return resultPublisher;
    }

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
            if (args.length > 0)
                ServerConfig.setSecret(args[0]);

            var server = new RestRepMessagesServer();

            if (args.length > 2) {
                String host = InetAddress.getLocalHost().getHostName();
                String dir = args[1].substring(0, args[1].lastIndexOf('/') + 1);
                String file = Paths.get(args[1]).getFileName().toString()
                        .replaceFirst("messages\\d+\\.ourorg\\d+", host);
                String correctedPath = dir + file;
                server.setSSLContext(TLS.serverContextFromFile(correctedPath, args[2]));
            } else {
                throw new RuntimeException("Args required: <secret> <keystore> <password>");
            }

            KafkaUtils.createTopic(KAFKA_TOPIC);
            KafkaUtils.createTopic(RESULT_TOPIC);

            String sharedGroup = "rep-group-" +
                System.getenv().getOrDefault("DOMAIN", "default");

            KafkaSubscriber.createSubscriber("kafka:9092", List.of(KAFKA_TOPIC), sharedGroup)
                .start(record -> {
                    JsonObject obj = JsonParser.parseString(record.value()).getAsJsonObject();
                    String op = obj.get("op").getAsString();
                    String res = null;
                    Message processedMsg = null;

                    switch (op) {
                        case "postMessage" -> {
                            String pwd = obj.get("pwd").getAsString();
                            Message msg = gson.fromJson(obj.get("msg"), Message.class);
                            var result = JavaMessages.getInstance().postMessage(pwd, msg);
                            if (result.isOK()) {
                                res = result.value();
                                processedMsg = JavaMessages.getInstance().getCachedMessage(res).value();
                            }
                        }
                        case "deleteMessage" -> {
                            String name = obj.get("name").getAsString();
                            String mid = obj.get("mid").getAsString();
                            String pwd = obj.get("pwd").getAsString();
                            var result = JavaMessages.getInstance().replicateDelete(mid, name);
                            res = result.isOK() ? "OK" : result.error().name();
                        }
                        case "removeInboxMessage" -> {
                            String name = obj.get("name").getAsString();
                            String mid = obj.get("mid").getAsString();
                            String pwd = obj.get("pwd").getAsString();
                            var result = JavaMessages.getInstance().replicateRemove(mid, name);
                            res = result.isOK() ? "OK" : result.error().name();
                        }
                    }

                    JsonObject resultObj = new JsonObject();
                    resultObj.addProperty("offset", record.offset());
                    resultObj.addProperty("op", op);
                    resultObj.addProperty("res", res != null ? res : "NULL");
                    if (processedMsg != null)
                        resultObj.add("msg", gson.toJsonTree(processedMsg));
                    if (obj.has("mid"))
                        resultObj.addProperty("mid", obj.get("mid").getAsString());
                    if (obj.has("name"))
                        resultObj.addProperty("name", obj.get("name").getAsString());

                    getResultPublisher().publish(RESULT_TOPIC, resultObj.toString());
                });

            KafkaSubscriber.createSubscriber("kafka:9092", List.of(RESULT_TOPIC))
                .start(record -> {
                    JsonObject obj = JsonParser.parseString(record.value()).getAsJsonObject();
                    long offset = obj.get("offset").getAsLong();
                    String op = obj.get("op").getAsString();
                    String res = obj.get("res").getAsString();

                    switch (op) {
                        case "postMessage" -> {
                            if (obj.has("msg")) {
                                Message msg = gson.fromJson(obj.get("msg"), Message.class);
                                JavaMessages.getInstance().replicatePost(msg);
                            }
                        }
                        case "deleteMessage" -> {
                            if (obj.has("mid") && obj.has("name")) {
                                String mid = obj.get("mid").getAsString();
                                String name = obj.get("name").getAsString();
                                JavaMessages.getInstance().replicateDelete(mid, name);
                            }
                        }
                        case "removeInboxMessage" -> {
                            if (obj.has("mid") && obj.has("name")) {
                                String mid = obj.get("mid").getAsString();
                                String name = obj.get("name").getAsString();
                                JavaMessages.getInstance().replicateRemove(mid, name);
                            }
                        }
                    }

                    String result = res.equals("NULL") ? null : res;
                    SyncPoint.getSyncPoint().setResult(offset, result);
                });

            server.start();

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}