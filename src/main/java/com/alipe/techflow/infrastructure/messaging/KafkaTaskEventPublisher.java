package com.alipe.techflow.infrastructure.messaging;

import org.apache.kafka.common.KafkaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class KafkaTaskEventPublisher implements TaskEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(KafkaTaskEventPublisher.class);

    private final KafkaTemplate<String, TaskEvent> kafkaTemplate;
    private final String topic;

    public KafkaTaskEventPublisher(KafkaTemplate<String, TaskEvent> kafkaTemplate,
                                   @Value("${app.kafka.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publish(TaskEvent event) {
        try {
            kafkaTemplate.send(topic, String.valueOf(event.taskId()), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Falha ao publicar evento da tarefa {}", event.taskId(), ex);
                        }
                    });
        } catch (KafkaException ex) {
            log.warn("Falha ao publicar evento da tarefa {}", event.taskId(), ex);
        }
    }
}
