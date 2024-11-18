# KIP 848 New Consumer Group Protocol

The new CG protocol should be truly incremental and cooperative and should not rely on a global synchronization barrier anymore. 
 
[New CG Protocol animation](https://www.youtube.com/watch?v=HUm1ZU-ICGo&t=35s)

## Run the demo

### Docker
In the folder kip-848 there is a docker-compos.yml file
You can see the following changes
1- KIP 848 is only supported by kraft
2- Each broker has the following new properties

````
      # KIP-848
      # group.consumer.migration.policy
      KAFKA_GROUP_CONSUMER_MIGRATION_POLICY: 'BIDIRECTIONAL'
      #group.coordinator.rebalance.protocols
      KAFKA_GROUP_COORDINATOR_REBALANCE_PROTOCOLS: 'consumer, classic'
      #group.coordinator.new.enable
      KAFKA_GROUP_COORDINATOR_NEW_ENABLE: 'true'
````

### Update client configuration

Update the file `src/main/resources/configuration.properties`.
The property `group.protocol`will start with value `CLASSIC`.

### Compile

```bash
mvn package
```

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

### Execute consumer 1

```bash
java -cp /workspaces/kafka101/target/kafka101-1.0-SNAPSHOT-jar-with-dependencies.jar io.confluent.csta.kafka101.avro.AvroConsumer
```
The consumer is using the old consumer Group Protocol
### Execute consumer 2

```bash
java -cp /workspaces/kafka101/target/kafka101-1.0-SNAPSHOT-jar-with-dependencies.jar io.confluent.csta.kafka101.avro.AvroConsumer
```
A second consumer is using the old consumer Group Protocol

### Update client configuration

Update the file `src/main/resources/configuration.properties`.
The property `group.protocol`will start with value `CONSUMER`.

### Compile

```bash
mvn package
```

### Stop one of the classic consumer

### Execute consumer 1

```bash
java -cp /workspaces/kafka101/target/kafka101-1.0-SNAPSHOT-jar-with-dependencies.jar io.confluent.csta.kafka101.avro.AvroConsumer
```

You can trace in the broker side something similar to:
```bash
[2024-11-11 09:37:20,677] INFO [GroupCoordinator id=1 topic=__consumer_offsets partition=33] [GroupId avro-consumer2] Computed a new target assignment for epoch 4 with 'uniform' assignor in 10ms: {avro-producer-b8f7c567-6711-4cc6-8720-29ce8dee2034=MemberAssignment(partitions={7l8rBsX6TMqbHE6QgWshdw=[0, 1, 2, 3, 4, 5]}), gjWzAEmmRlOfypB2_GB_9Q=MemberAssignment(partitions={8iWjCLGESE6MUiR5ANkArg=[0, 1, 2, 3, 4, 5]})}. (org.apache.kafka.coordinator.group.GroupMetadataManager)
```

After few seconds the rebalance is completed by two consumers being stabillized

````
[2024-11-11 09:37:20,961] INFO [GroupCoordinator id=1 topic=__consumer_offsets partition=33] [GroupId avro-consumer2] Member avro-producer-b8f7c567-6711-4cc6-8720-29ce8dee2034 new assignment state: epoch=4, previousEpoch=3, state=STABLE, assignedPartitions=[7l8rBsX6TMqbHE6QgWshdw-0, 7l8rBsX6TMqbHE6QgWshdw-1, 7l8rBsX6TMqbHE6QgWshdw-2, 7l8rBsX6TMqbHE6QgWshdw-3, 7l8rBsX6TMqbHE6QgWshdw-4, 7l8rBsX6TMqbHE6QgWshdw-5] and revokedPartitions=[]. (org.apache.kafka.coordinator.group.GroupMetadataManager)
[2024-11-11 09:37:25,723] INFO [GroupCoordinator id=1 topic=__consumer_offsets partition=33] [GroupId avro-consumer2] Member gjWzAEmmRlOfypB2_GB_9Q new assignment state: epoch=4, previousEpoch=4, state=STABLE, assignedPartitions=[8iWjCLGESE6MUiR5ANkArg-0, 8iWjCLGESE6MUiR5ANkArg-1, 8iWjCLGESE6MUiR5ANkArg-2, 8iWjCLGESE6MUiR5ANkArg-3, 8iWjCLGESE6MUiR5ANkArg-4, 8iWjCLGESE6MUiR5ANkArg-5] and revokedPartitions=[]. 
````


### Stop second classic consumer

The other classic consumer `avro-producer-b8f7c567-6711-4cc6-8720-29ce8dee2034` leaves. A new assignment is computed. This time the only consumer protocol consumer has all the partitions


````
[2024-11-11 09:38:25,810] INFO [GroupCoordinator id=1 topic=__consumer_offsets partition=33] [GroupId avro-consumer2] Computed a new target assignment for epoch 5 with 'uniform' assignor in 1ms: {gjWzAEmmRlOfypB2_GB_9Q=MemberAssignment(partitions={7l8rBsX6TMqbHE6QgWshdw=[0, 1, 2, 3, 4, 5], 8iWjCLGESE6MUiR5ANkArg=[0, 1, 2, 3, 4, 5]})}. (org.apache.kafka.coordinator.group.GroupMetadataManager)
````
### Execute consumer 2

```bash
java -cp /workspaces/kafka101/target/kafka101-1.0-SNAPSHOT-jar-with-dependencies.jar io.confluent.csta.kafka101.avro.AvroConsumer
```

A new consumer protocol consumer `6nc1qt8rSeyTwBh__vZELQ` joins. A new assignment is computed and the assignment is evenly split.

```
[2024-11-11 09:38:40,249] INFO [GroupCoordinator id=1 topic=__consumer_offsets partition=33] [GroupId avro-consumer2] Computed a new target assignment for epoch 6 with 'uniform' assignor in 1ms: {6nc1qt8rSeyTwBh__vZELQ=MemberAssignment(partitions={8iWjCLGESE6MUiR5ANkArg=[0, 1, 2, 3, 4, 5]}), gjWzAEmmRlOfypB2_GB_9Q=MemberAssignment(partitions={7l8rBsX6TMqbHE6QgWshdw=[0, 1, 2, 3, 4, 5]})}. (org.apache.kafka.coordinator.group.GroupMetadataManager)
```

The rebalance is completed

```
[2024-11-11 09:38:40,966] INFO [GroupCoordinator id=1 topic=__consumer_offsets partition=33] [GroupId avro-consumer2] Member gjWzAEmmRlOfypB2_GB_9Q new assignment state: epoch=6, previousEpoch=5, state=STABLE, assignedPartitions=[7l8rBsX6TMqbHE6QgWshdw-0, 7l8rBsX6TMqbHE6QgWshdw-1, 7l8rBsX6TMqbHE6QgWshdw-2, 7l8rBsX6TMqbHE6QgWshdw-3, 7l8rBsX6TMqbHE6QgWshdw-4, 7l8rBsX6TMqbHE6QgWshdw-5] and revokedPartitions=[]. (org.apache.kafka.coordinator.group.GroupMetadataManager)
[2024-11-11 09:38:45,273] INFO [GroupCoordinator id=1 topic=__consumer_offsets partition=33] [GroupId avro-consumer2] Member 6nc1qt8rSeyTwBh__vZELQ new assignment state: epoch=6, previousEpoch=6, state=STABLE, assignedPartitions=[8iWjCLGESE6MUiR5ANkArg-0, 8iWjCLGESE6MUiR5ANkArg-1, 8iWjCLGESE6MUiR5ANkArg-2, 8iWjCLGESE6MUiR5ANkArg-3, 8iWjCLGESE6MUiR5ANkArg-4, 8iWjCLGESE6MUiR5ANkArg-5] and revokedPartitions=[]. (org.apache.kafka.coordinator.group.GroupMetadataManager)
```

## Cleanup

From the root of the project:

```bash
docker compose down -v
```