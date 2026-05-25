package sd2526.trab.impl.utils.kafka;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer; // CORRETO

public class KafkaSubscriber {

    public static KafkaSubscriber createSubscriber(String addr, List<String> topics) {
        return createSubscriber(addr, topics, UUID.randomUUID().toString());
    }

    public static KafkaSubscriber createSubscriber(String addr, List<String> topics, String groupId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, addr);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return new KafkaSubscriber(new KafkaConsumer<>(props), topics);
    }

    private static final long POLL_TIMEOUT = 1L;
    private final KafkaConsumer<String, String> consumer;

    private KafkaSubscriber(KafkaConsumer<String, String> consumer, List<String> topics) {
        this.consumer = consumer;
        this.consumer.subscribe(topics);
    }

    public void start(RecordProcessor rp) {
        new Thread(() -> {
            for (;;) {
                var records = consumer.poll(Duration.ofSeconds(POLL_TIMEOUT));
                for (var r : records)
                    rp.onReceive(r);
            }
        }).start();
    }

    public interface RecordProcessor {
        void onReceive(ConsumerRecord<String, String> r);
    }
}