package com.wallet.common.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.events.broker", havingValue = "spring",
        matchIfMissing = true)
public class SpringEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(BaseEvent event) {
        log.info("Publishing event {} via Spring",
                event.getClass().getSimpleName());
        applicationEventPublisher.publishEvent(event);
    }
}

