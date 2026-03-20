package com.wallet.common.events;

import java.time.Instant;
import java.util.UUID;

public abstract class BaseEvent {

    private final UUID eventId;
    private final Instant occurredAt;

    protected BaseEvent() {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
    }

    public UUID getEventId() {
        return eventId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}

