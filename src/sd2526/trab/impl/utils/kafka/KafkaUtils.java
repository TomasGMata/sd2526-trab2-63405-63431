package sd2526.trab.impl.utils.kafka;

import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;

public class KafkaUtils {

    public static void createTopic(String topic) {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        try (AdminClient admin = AdminClient.create(props)) {
            admin.createTopics(List.of(new NewTopic(topic, 1, (short) 1)));
        } catch (Exception e) {
            
        }
    }
}