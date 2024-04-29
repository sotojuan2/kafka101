package io.confluent.csta.kafka101.avro;

import io.confluent.csta.kafka101.Customer;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;
import java.util.Random;

public class AvroProducer {
    private static final Logger log = LoggerFactory.getLogger(AvroProducer.class);
    private static boolean running = true;
    private static final long SLEEP_TIME_MS = 500;
    private static final String TOPIC = "customers";

    public static void main(String[] args) throws IOException {
        Properties properties = new Properties();
        properties.load(AvroProducer.class.getResourceAsStream("/configuration.properties"));
        properties.put("context.name.strategy",ExampleContextNameStrategy.class.getName());

        Producer<String, Customer> producer = new KafkaProducer<>(properties);

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
                String firstName = RandomStringUtils.randomAlphanumeric(3).toUpperCase();
                String lastName = RandomStringUtils.randomAlphanumeric(5).toUpperCase();
                String fullName = firstName + " " + lastName;
                int age = random.nextInt(101);
                Customer customer = Customer.newBuilder()
                        .setAge(age)
                        .setFirstName(firstName)
                        .setLastName(lastName)
                        .build();

                //kafka producer - asynchronous writes
                final ProducerRecord<String, Customer> record = new ProducerRecord<>(TOPIC, fullName, customer);
                //send with callback
                producer.send(record, (metadata, e) -> {
                    if (e != null)
                        log.info("Send failed for record {}", record, e);
                    else
                        log.info("Sent name={}, age={} - Partition-{} - Offset {}", record.key(),
                                record.value().getAge(), metadata.partition(), metadata.offset());
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
