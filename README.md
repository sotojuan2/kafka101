# Kafka 1o1

Introduction Kafka 1o1.

## Setup

### Start Docker Compose

```bash
docker compose up -d
```

Check logs for confirming all services are running:

```bash
docker compose logs -f
```

It may be some services fail starting at first. You can check with:

```bash
docker stats
```

If everything is fine you should get something like this with all services listed:

```text
CONTAINER ID   NAME              CPU %     MEM USAGE / LIMIT     MEM %     NET I/O           BLOCK I/O        PIDS
c8e46a3a782e   control-center    4.67%     535.7MiB / 11.68GiB   4.48%     3.51MB / 1.31MB   717kB / 14.9MB   134
fe3cb66ebf47   connect           4.78%     1.658GiB / 11.68GiB   14.19%    934kB / 774kB     160kB / 352kB    46
e4aedc35125a   schema-registry   0.97%     288.1MiB / 11.68GiB   2.41%     177kB / 151kB     0B / 360kB       40
aac8afd891b3   kafka1            5.97%     643.1MiB / 11.68GiB   5.38%     1.11MB / 1.92MB   0B / 1.68MB      120
242b24a11f28   kafka4            11.47%    795.5MiB / 11.68GiB   6.65%     1.79MB / 2.62MB   0B / 1.5MB       119
0d907b9b87ac   kafka3            6.75%     659.9MiB / 11.68GiB   5.52%     1.18MB / 1.98MB   0B / 1.53MB      118
f40ffddfb298   kafka2            4.69%     664MiB / 11.68GiB     5.55%     1.54MB / 2.04MB   0B / 1.62MB      142
0c81cfe2dc13   zookeeper         0.25%     102.1MiB / 11.68GiB   0.85%     230kB / 207kB     0B / 1.27MB      60

```

If that's not the case in general executing `up -d` again should suffice.

### Check Control Center

Open http://localhost:9021 and check cluster is healthy

### Create First Topic

```bash
kafka-topics --bootstrap-server localhost:19092 --create \
--topic my-topic \
--replication-factor 3 \
--partitions 6
```

You can describe the topic to check how partitions got distributed:

```bash
kafka-topics --bootstrap-server localhost:19092 --topic my-topic --describe
```

You should get something like this:

```text
Topic: my-topic	TopicId: z7s7bS2oTGiTvmTDuShdmQ	PartitionCount: 6	 ReplicationFactor: 3	 Configs:
	Topic: my-topic	Partition: 0	Leader: 4	Replicas: 4,3,1	 Isr: 4,3,1	 Offline:
	Topic: my-topic	Partition: 1	Leader: 1	Replicas: 1,4,2	 Isr: 1,4,2	 Offline:
	Topic: my-topic	Partition: 2	Leader: 2	Replicas: 2,1,3	 Isr: 2,1,3	 Offline:
	Topic: my-topic	Partition: 3	Leader: 3	Replicas: 3,2,4	 Isr: 3,2,4	 Offline:
	Topic: my-topic	Partition: 4	Leader: 4	Replicas: 4,1,2	 Isr: 4,1,2	 Offline:
	Topic: my-topic	Partition: 5	Leader: 1	Replicas: 1,2,3	 Isr: 1,2,3	 Offline:
```

### Command line producer-consumer

To produce some test messages in one shell we can execute:

```bash
kafka-producer-perf-test --topic my-topic --num-records 600000 --record-size 100 --throughput 10000 --producer-props bootstrap.servers=localhost:19092
```

And in another shell we can execute the console consumer:

```bash
kafka-console-consumer --bootstrap-server localhost:19092 --topic my-topic --from-beginning --property print.timestamp=true --property print.value=true
```

After messages produced you can see the consumer console will still be waiting for more messages.

We can run another smaller batch of messages production and see our console consumer will consume them:

```bash
kafka-producer-perf-test --topic my-topic --num-records 10000 --record-size 100 --throughput 10000 --producer-props bootstrap.servers=localhost:19092
```

## Java Basic Producer

Now let's execute the class io.confluent.csta.kafka101.basic.BasicProducer.

You should see on the console output something like this:

```text
INFO - 2023-11-28 22:01:59,494 - io.confluent.csta.kafka101.basic.BasicProducer:50 - Sent key=10, value=ohYtUMGLBl - Partition-1 - Offset 99048  
INFO - 2023-11-28 22:01:59,992 - io.confluent.csta.kafka101.basic.BasicProducer:50 - Sent key=19, value=GEKp5GZH1z - Partition-2 - Offset 99156   
```

And in one of our shells lets execute the consumer:

```bash
kafka-console-consumer --bootstrap-server localhost:19092 \
--topic my-topic --property print.timestamp=true \
--property print.value=true \
--property print.key=true \
--property key.deserializer=org.apache.kafka.common.serialization.IntegerDeserializer
```

In another shell describe the topic:

```bash
kafka-topics --bootstrap-server localhost:19092 --topic my-topic --describe
```

Now meanwhile producer and consumer are executing let's stop one of the kafka broker instances:

```bash
docker compose stop kafka4
```

Check producer and consumer still executing.

Execute describe of the topic again.

## Avro Schema Based Producer

### Register Schema

First lets register our schema against Schema Registry:

```bash
jqjq '. | {schema: tojson}' src/main/resources/avro/customer.avsc | \
curl -X POST http://localhost:8081/subjects/customers-value/versions \
-H "Content-Type: application/vnd.schemaregistry.v1+json" \
-d @-
```

You should see as response:

```text
{"id":1}
```

You can also check schema was registered by executing:

```bash
curl -s http://localhost:8081/subjects/
```

```bash
curl -s http://localhost:8081/subjects/customers-value/versions
```

```bash
curl -s http://localhost:8081/subjects/customers-value/versions/1
```

### Create Topic

Let's create our topic:

```bash
kafka-topics --bootstrap-server localhost:19092 --create \
--topic customers \
--replication-factor 3 \
--partitions 6
```

### Run Producer

Now let's run our producer io.confluent.csta.kafka101.avro.AvroProducer.

And check with consumer:

```bash
kafka-avro-console-consumer --topic customers \
--bootstrap-server 127.0.0.1:19092 \
--property schema.registry.url=http://127.0.0.1:8081 \
--from-beginning
```

## Consumers

### Basic Consumer

Run the io.confluent.csta.kafka101.basic.BasicConsumer

Once it has consumed all messages in the topic run in parallel the io.confluent.csta.kafka101.basic.BasicProducer

### Avro Schema Based Consumer

Run the io.confluent.csta.kafka101.avro.AvroConsumer

Once it has consumed all messages in the topic run in parallel the io.confluent.csta.kafka101.avro.AvroProducer

## Consumer Groups

You could now stop the containers:

```bash
docker compose down
```

And check the demo https://github.com/tomasalmeida/kafka-partition-assignment-examples



## Cleanup

```bash
docker compose down -v
```