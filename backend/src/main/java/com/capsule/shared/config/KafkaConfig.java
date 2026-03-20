package com.capsule.shared.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic capsuleDueTopic() {
        return TopicBuilder.name("capsule.due").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic capsuleOpenedTopic() {
        return TopicBuilder.name("capsule.opened").partitions(3).replicas(1).build();
    }
}
