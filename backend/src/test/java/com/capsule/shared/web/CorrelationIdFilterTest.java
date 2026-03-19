package com.capsule.shared.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:test",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.flyway.enabled=false",
    "spring.kafka.bootstrap-servers=localhost:9092",
    "jwt.secret=test-secret-at-least-32-chars-long!!",
    "aws.s3.bucket=test",
    "aws.s3.endpoint=http://localhost:9000",
    "stripe.secret-key=sk_test_x",
    "stripe.webhook-secret=whsec_x",
    "spring.quartz.job-store-type=memory"
})
class CorrelationIdFilterTest {

    @Autowired MockMvc mockMvc;

    @Test
    void responseIncludesCorrelationIdHeader() throws Exception {
        mockMvc.perform(get("/actuator/health"))
               .andExpect(header().exists("X-Correlation-Id"));
    }
}
