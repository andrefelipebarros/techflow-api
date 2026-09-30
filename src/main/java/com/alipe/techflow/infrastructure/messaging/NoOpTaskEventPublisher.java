package com.alipe.techflow.infrastructure.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpTaskEventPublisher implements TaskEventPublisher {
    @Override
    public void publish(TaskEvent event) {
    }
}
