# Specifying a context name for clients

When using a client to talk to Schema Registry, you may want the client to use a particular context. 
The easy approach is to simply change the Schema Registry URL used by the client from https://<host1> to https://<host2>/contexts/.mycontext.

In case you have a client where you have to read or write in two or more topics in different context, you can use the following example.

To achieve this, you can specify a context name strategy to the serializer or deserializer.

There are two main iteam in this example:

1 - Client side
Add the property or cunsumer property context.name.strategy 

```java
    properties.load(AvroProducer.class.getResourceAsStream("/configuration.properties"));
    properties.put("context.name.strategy",ExampleContextNameStrategy.class.getName());
```

2 - ContextName 

The class named **ExampleContextNameStrategy** has the implementation
```java
package io.confluent.csta.kafka101.avro;

import java.util.Map;
import io.confluent.kafka.serializers.context.strategy.ContextNameStrategy;

public class ExampleContextNameStrategy implements ContextNameStrategy {
    public void configure(Map<String, ?> configs) {
    }
    public String contextName(String topic) {
      if (topic.startsWith("test-")) {
        return "test";
      } else {
          return "";
      }
    }
}
 


```
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



### Update configuration properties

Update the configuration properties if it is need it

### Compile java

```bash
mvn package
```

## Avro Schema Based Producer

### Register Schema

First lets register our schema against Schema Registry on the Default context:

```bash
jq '. | {schema: tojson}' src/main/resources/avro/customer.avsc | \
curl -X POST http://localhost:8081/subjects/customers-value/versions \
-H "Content-Type: application/vnd.schemaregistry.v1+json" \
-d @-
```

You should see as response:

```text
{"id":1}
```

```bash
jq '. | {schema: tojson}' src/main/resources/avro/customer.avsc | \
curl -X POST http://localhost:8081/subjects/:.test:test-customers-value/versions \
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

```bash
curl -s http://localhost:8081/contexts
```

### Create Topic

Let's create our topic:

```bash
kafka-topics --bootstrap-server localhost:19092 --create \
--topic customers \
--replication-factor 3 \
--partitions 6
```

Let's create our second topic

```bash
kafka-topics --bootstrap-server localhost:19092 --create \
--topic test-customers \
--replication-factor 3 \
--partitions 6
```

### Execute first producer

```bash
java -cp /workspaces/kafka101/target/kafka101-1.0-SNAPSHOT-jar-with-dependencies.jar io.confluent.csta.kafka101.avro.AvroProducer customers
```

### Execute second producer

```bash
java -cp /workspaces/kafka101/target/kafka101-1.0-SNAPSHOT-jar-with-dependencies.jar io.confluent.csta.kafka101.avro.AvroProducer test-customers
```

### Execute consumer

```bash
java -cp /workspaces/kafka101/target/kafka101-1.0-SNAPSHOT-jar-with-dependencies.jar io.confluent.csta.kafka101.avro.AvroConsumer
```


## Cleanup

From the root of the project:

```bash
docker compose down -v
```

# Addendum 

## KIP 848

In a different branch there is an example of the new [KIP-848](http://192.168.1.200/Juan/kafka101/-/blob/KIP-848/kip-848/readme.md?ref_type=heads)