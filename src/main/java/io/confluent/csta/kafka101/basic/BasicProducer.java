package io.confluent.csta.kafka101.basic;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.Random;

public class BasicProducer {
    private static final Logger log = LoggerFactory.getLogger(BasicProducer.class);
    public static final String BOOTSTRAP_SERVERS = "localhost:19092";
    private static boolean running = true;
    private static final long SLEEP_TIME_MS = 500;
    private static final String TOPIC = "my-topic";

    public static void main(String[] args) {
        //initialization
        Properties config = new Properties();
        config.put("client.id", "basic-producer");
        config.put("bootstrap.servers", BOOTSTRAP_SERVERS);
        config.put("acks", "all");
        Producer<Integer, String> producer = new KafkaProducer<>(config, new IntegerSerializer(),
                new StringSerializer());

        final Thread mainThread = Thread.currentThread();
        // adding the shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Detected a shutdown, let's set running to false and interrupt thread...");
            running = false;
            mainThread.interrupt();
        }));

        Random random = new Random();

        try {
            while (running) {
                Integer key = random.nextInt(101);
                String value = RandomStringUtils.randomAlphanumeric(10);

                //kafka producer - asynchronous writes
                final ProducerRecord<Integer, String> record = new ProducerRecord<>(TOPIC, key, value);
                //send with callback
                producer.send(record, (metadata, e) -> {
                    if (e != null)
                        log.info("Send failed for record {}", record, e);
                    else
                        log.info("Sent key={}, value={} - Partition-{} - Offset {}", record.key(), record.value(),
                                metadata.partition(),metadata.offset());
                });
                //sleep
                Thread.sleep(SLEEP_TIME_MS);
            }
        } catch (InterruptedException e) {
            log.info("Thread interrupted.");
        } finally {
            log.info("Closing producer");
            producer.close();
        }

    }
}
