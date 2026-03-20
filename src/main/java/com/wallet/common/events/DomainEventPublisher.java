package com.wallet.common.events;

public interface DomainEventPublisher {

    void publish(BaseEvent event);
}

