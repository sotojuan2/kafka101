package io.confluent.csta.kafka101.avro;

import io.confluent.csta.kafka101.Customer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;



public class AvroConsumer {
    private static final Logger log = LoggerFactory.getLogger(AvroConsumer.class);
    //private static final String TOPIC = "test-customers";
    private static final List<String> TOPICS = Arrays.asList( "customers");

    public static void main(String[] args) throws IOException {
        Properties properties = new Properties();
        properties.load(AvroConsumer.class.getResourceAsStream("/configuration.properties"));
        properties.put("group.id", "jsoto-consumer-group");
        //properties.put("context.name.strategy",ExampleContextNameStrategy.class.getName());

        KafkaConsumer<String, Customer> consumer = new KafkaConsumer<>(properties);

        try {
            consumer.subscribe(TOPICS);

            final Thread mainThread = Thread.currentThread();

            // adding the shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    log.info("Detected a shutdown, let's exit by calling consumer.wakeup()...");
                    consumer.wakeup();

                    // join the main thread to allow the execution of the code in the main thread
                    try {
                        mainThread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });

            while (true) {
                ConsumerRecords<String, Customer> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, Customer> record : records) {
                    log.info("Message with name={}, age={} - Partition-{} - Offset {} - topic {}", record.key(),
                            record.value().getAge(),
                            record.partition(),record.offset(),record.topic());
                }
            }
        } catch (WakeupException e) {
            log.info("Wake up exception!");
            // we ignore this as this is an expected exception when closing a consumer
        } catch (Exception e) {
            log.error("Unexpected exception", e);
        } finally {
            log.info("The consumer is now gracefully closed.");
            consumer.close();
        }
    }
}
