package com.capsule.delivery;

import com.capsule.capsule.CapsuleRepository;
import com.capsule.capsule.CapsuleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class DeliveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(DeliveryScheduler.class);

    private final CapsuleRepository capsuleRepository;
    private final DeliveryProducer producer;

    public DeliveryScheduler(CapsuleRepository capsuleRepository, DeliveryProducer producer) {
        this.capsuleRepository = capsuleRepository;
        this.producer = producer;
    }

    @Scheduled(fixedDelay = 60_000)
    public void pollDueCapsules() {
        var due = capsuleRepository.findByStateAndOpenAtBefore(CapsuleState.sealed, Instant.now());
        if (!due.isEmpty()) {
            log.info("Found {} capsules due for delivery", due.size());
        }
        due.forEach(c -> producer.sendCapsuleDue(c.getId()));
    }
}
