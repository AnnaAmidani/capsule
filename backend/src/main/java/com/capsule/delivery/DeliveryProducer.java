package com.capsule.delivery;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class DeliveryProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public DeliveryProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendCapsuleDue(UUID capsuleId) {
        kafkaTemplate.send("capsule.due", capsuleId.toString(), capsuleId.toString());
    }

    public void sendCapsuleOpened(UUID capsuleId) {
        kafkaTemplate.send("capsule.opened", capsuleId.toString(), capsuleId.toString());
    }
}
