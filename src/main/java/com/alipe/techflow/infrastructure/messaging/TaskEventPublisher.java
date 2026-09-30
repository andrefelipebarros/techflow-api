package com.alipe.techflow.infrastructure.messaging;

public interface TaskEventPublisher {
    void publish(TaskEvent event);
}
