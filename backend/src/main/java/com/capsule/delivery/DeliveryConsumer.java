package com.capsule.delivery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DeliveryConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeliveryConsumer.class);

    private final DeliveryService deliveryService;
    private final DeliveryProducer producer;

    public DeliveryConsumer(DeliveryService deliveryService, DeliveryProducer producer) {
        this.deliveryService = deliveryService;
        this.producer = producer;
    }

    @KafkaListener(topics = "capsule.due", groupId = "capsule-backend")
    public void onCapsuleDue(String capsuleIdStr) {
        var capsuleId = UUID.fromString(capsuleIdStr);
        log.info("Processing delivery for capsule {}", capsuleId);
        try {
            deliveryService.deliverCapsule(capsuleId);
            producer.sendCapsuleOpened(capsuleId);
        } catch (Exception e) {
            log.error("Delivery failed for capsule {}: {}", capsuleId, e.getMessage());
            throw e;
        }
    }
}
