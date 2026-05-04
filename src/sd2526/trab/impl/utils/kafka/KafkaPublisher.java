package sd2526.trab.impl.utils.kafka;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer; // CORRETO

public class KafkaPublisher {

    public static KafkaPublisher createPublisher(String addr) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, addr);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaPublisher(new KafkaProducer<>(props));
    }

    private final KafkaProducer<String, String> producer;

    private KafkaPublisher(KafkaProducer<String, String> producer) {
        this.producer = producer;
    }

    public long publish(String topic, String value) {
        try {
            RecordMetadata rec = producer.send(
                new ProducerRecord<>(topic, value)).get();
            return rec.offset();
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public void close() {
        producer.close();
    }
}