package com.capsule;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CapsuleApplication {
    public static void main(String[] args) {
        SpringApplication.run(CapsuleApplication.class, args);
    }
}
