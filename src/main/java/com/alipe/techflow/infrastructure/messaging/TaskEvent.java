package com.alipe.techflow.infrastructure.messaging;

import java.time.OffsetDateTime;

public record TaskEvent(String eventType, Long taskId, Long projectId,
                        String title, String status, OffsetDateTime occurredAt) {}
