package com.wallet.common.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.events.broker", havingValue = "kafka")
public class KafkaEventPublisher implements DomainEventPublisher {

    private static final String TOPIC = "wallet.domain.events";

    private final KafkaTemplate<String, BaseEvent> kafkaTemplate;

    @Override
    public void publish(BaseEvent event) {
        log.info("Publishing event {} via Kafka",
                event.getClass().getSimpleName());
        kafkaTemplate.send(TOPIC, event.getEventId().toString(), event);
    }
}

