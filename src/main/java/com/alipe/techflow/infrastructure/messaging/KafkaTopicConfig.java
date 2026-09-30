package com.alipe.techflow.infrastructure.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class KafkaTopicConfig {
    @Bean
    NewTopic techFlowEventsTopic() {
        return TopicBuilder.name("techflow.events").partitions(3).replicas(1).build();
    }
}
