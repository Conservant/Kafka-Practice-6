package ru.mitenev.consumer;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

import static ru.mitenev.common.Constants.BOOTSTRAP_SERVERS;

public class KafkaConsumerApp {

    public static void main(String[] args) {

        Consumer<String, String> consumer = getSingleMessageConsumer();
        consumer.subscribe(List.of(MESSAGES_TOPIC));

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(10));
            records.forEach(record -> {
                try {
                    System.out.println("Got message: " + record.value());
                } catch (Exception e) {
                    System.err.print("Error occurred");
                    e.printStackTrace();
                }
            });
        }
    }

    private static Consumer<String, String> getSingleMessageConsumer() {
        Properties props = new Properties();
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "my-secured-consumer");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put("security.protocol", "SSL");
        props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, "/Users/spb-amitenev/IdeaProjects/Kafka-Practice-6/security/kafka-0-creds/kafka-0.truststore.jks"); // Truststore
        props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "your-password-0"); // Truststore password
        props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, "/Users/spb-amitenev/IdeaProjects/Kafka-Practice-6/security/kafka-0-creds/kafka-0.keystore.jks"); // Keystore
        props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, "password"); // Keystore password
        props.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, "your-password-0"); // Key password
        props.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        return new KafkaConsumer<>(properties);
    }
}
