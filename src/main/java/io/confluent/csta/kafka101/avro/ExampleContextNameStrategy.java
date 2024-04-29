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
 
