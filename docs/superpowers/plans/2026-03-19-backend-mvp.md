# Backend MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Capsule backend — a Spring Boot modular monolith exposing REST APIs for user auth, capsule CRUD, time-based delivery, and billing.

**Architecture:** Vertical feature slice modules (`user`, `capsule`, `delivery`, `billing`) each owning their domain, service, repository, and controller. A `shared` layer provides security, config, and web infrastructure. Quartz + Kafka drive capsule delivery. PostgreSQL is the primary store; Redis holds sessions; MinIO stands in for S3 locally.

**Tech Stack:** Java 21, Spring Boot 3, Maven, PostgreSQL, Redis, Apache Kafka, Quartz (JDBC store), AWS S3 / MinIO, Stripe, Spring Security (JWT + OAuth2), Flyway, Testcontainers, JUnit 5, MockMvc

---

## File Map

```
backend/
├── pom.xml
├── docker-compose.yml
└── src/
    ├── main/
    │   ├── java/com/capsule/
    │   │   ├── CapsuleApplication.java
    │   │   ├── shared/
    │   │   │   ├── security/
    │   │   │   │   ├── SecurityConfig.java
    │   │   │   │   ├── JwtService.java
    │   │   │   │   ├── JwtAuthFilter.java
    │   │   │   │   └── OAuth2SuccessHandler.java
    │   │   │   ├── config/
    │   │   │   │   ├── KafkaConfig.java
    │   │   │   │   ├── RedisConfig.java
    │   │   │   │   ├── S3Config.java
    │   │   │   │   └── QuartzConfig.java
    │   │   │   └── web/
    │   │   │       ├── CorrelationIdFilter.java
    │   │   │       ├── GlobalExceptionHandler.java
    │   │   │       └── ApiError.java
    │   │   ├── user/
    │   │   │   ├── User.java
    │   │   │   ├── UserTier.java           (enum)
    │   │   │   ├── UserRepository.java
    │   │   │   ├── UserService.java
    │   │   │   ├── AuthController.java
    │   │   │   ├── UserController.java
    │   │   │   └── dto/
    │   │   │       ├── RegisterRequest.java
    │   │   │       ├── LoginRequest.java
    │   │   │       ├── TokenResponse.java
    │   │   │       └── UserResponse.java
    │   │   ├── capsule/
    │   │   │   ├── Capsule.java
    │   │   │   ├── CapsuleState.java       (enum)
    │   │   │   ├── CapsuleVisibility.java  (enum)
    │   │   │   ├── CapsuleItem.java
    │   │   │   ├── ItemType.java           (enum)
    │   │   │   ├── CapsuleRepository.java
    │   │   │   ├── CapsuleItemRepository.java
    │   │   │   ├── CapsuleService.java
    │   │   │   ├── CapsuleController.java
    │   │   │   └── dto/
    │   │   │       ├── CreateCapsuleRequest.java
    │   │   │       ├── UpdateCapsuleRequest.java
    │   │   │       ├── CapsuleResponse.java
    │   │   │       ├── AddItemRequest.java
    │   │   │       └── UploadUrlResponse.java
    │   │   ├── delivery/
    │   │   │   ├── Recipient.java
    │   │   │   ├── RecipientRepository.java
    │   │   │   ├── DeliveryService.java
    │   │   │   ├── RecipientController.java
    │   │   │   ├── DeliveryScheduler.java
    │   │   │   ├── DeliveryProducer.java
    │   │   │   ├── DeliveryConsumer.java
    │   │   │   ├── TokenService.java
    │   │   │   └── dto/
    │   │   │       ├── AddRecipientsRequest.java
    │   │   │       └── RecipientResponse.java
    │   │   └── billing/
    │   │       ├── Subscription.java
    │   │       ├── SubscriptionRepository.java
    │   │       ├── SubscriptionService.java
    │   │       ├── BillingController.java
    │   │       └── StripeWebhookController.java
    │   └── resources/
    │       ├── application.yml
    │       ├── application-local.yml
    │       └── db/migration/
    │           ├── V1__create_users.sql
    │           ├── V2__create_capsules.sql
    │           ├── V3__create_capsule_items.sql
    │           ├── V4__create_recipients.sql
    │           └── V5__create_subscriptions.sql
    └── test/
        └── java/com/capsule/
            ├── shared/security/JwtServiceTest.java
            ├── user/
            │   ├── UserServiceTest.java
            │   └── AuthControllerTest.java
            ├── capsule/
            │   ├── CapsuleServiceTest.java
            │   └── CapsuleControllerTest.java
            ├── delivery/
            │   ├── TokenServiceTest.java
            │   ├── DeliveryServiceTest.java
            │   └── DeliveryConsumerTest.java
            └── billing/
                └── StripeWebhookControllerTest.java
```

---

## Task 1: Project Scaffolding

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/capsule/CapsuleApplication.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/resources/application-local.yml`
- Create: `backend/docker-compose.yml`

- [ ] **Step 1: Create `backend/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.4</version>
  </parent>

  <groupId>com.capsule</groupId>
  <artifactId>capsule-backend</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <name>capsule-backend</name>

  <properties>
    <java.version>21</java.version>
    <jjwt.version>0.12.5</jjwt.version>
    <stripe.version>24.6.0</stripe.version>
    <aws.sdk.version>2.25.28</aws.sdk.version>
    <testcontainers.version>1.19.7</testcontainers.version>
    <logstash.version>7.4</logstash.version>
  </properties>

  <dependencies>
    <!-- Web -->
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
    <!-- Security -->
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-oauth2-client</artifactId></dependency>
    <!-- Data -->
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-redis</artifactId></dependency>
    <dependency><groupId>org.postgresql</groupId><artifactId>postgresql</artifactId><scope>runtime</scope></dependency>
    <!-- Migrations -->
    <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-core</artifactId></dependency>
    <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-database-postgresql</artifactId></dependency>
    <!-- Messaging -->
    <dependency><groupId>org.springframework.kafka</groupId><artifactId>spring-kafka</artifactId></dependency>
    <!-- Scheduling -->
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-quartz</artifactId></dependency>
    <!-- JWT -->
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-api</artifactId><version>${jjwt.version}</version></dependency>
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-impl</artifactId><version>${jjwt.version}</version><scope>runtime</scope></dependency>
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-jackson</artifactId><version>${jjwt.version}</version><scope>runtime</scope></dependency>
    <!-- AWS S3 -->
    <dependency><groupId>software.amazon.awssdk</groupId><artifactId>s3</artifactId><version>${aws.sdk.version}</version></dependency>
    <!-- Stripe -->
    <dependency><groupId>com.stripe</groupId><artifactId>stripe-java</artifactId><version>${stripe.version}</version></dependency>
    <!-- Observability -->
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-actuator</artifactId></dependency>
    <dependency><groupId>net.logstash.logback</groupId><artifactId>logstash-logback-encoder</artifactId><version>${logstash.version}</version></dependency>
    <!-- OpenAPI -->
    <dependency><groupId>org.springdoc</groupId><artifactId>springdoc-openapi-starter-webmvc-ui</artifactId><version>2.4.0</version></dependency>
    <!-- Validation -->
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>

    <!-- Test -->
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
    <dependency><groupId>org.springframework.security</groupId><artifactId>spring-security-test</artifactId><scope>test</scope></dependency>
    <dependency><groupId>org.springframework.kafka</groupId><artifactId>spring-kafka-test</artifactId><scope>test</scope></dependency>
    <dependency><groupId>org.testcontainers</groupId><artifactId>junit-jupiter</artifactId><version>${testcontainers.version}</version><scope>test</scope></dependency>
    <dependency><groupId>org.testcontainers</groupId><artifactId>postgresql</artifactId><version>${testcontainers.version}</version><scope>test</scope></dependency>
    <dependency><groupId>org.testcontainers</groupId><artifactId>kafka</artifactId><version>${testcontainers.version}</version><scope>test</scope></dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-checkstyle-plugin</artifactId>
        <version>3.3.1</version>
        <configuration>
          <configLocation>google_checks.xml</configLocation>
          <failsOnError>true</failsOnError>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

- [ ] **Step 2: Create `CapsuleApplication.java`**

```java
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
```

- [ ] **Step 3: Create `application.yml`**

```yaml
spring:
  application:
    name: capsule-backend
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
  kafka:
    bootstrap-servers: ${KAFKA_BROKERS}
    consumer:
      group-id: capsule-backend
      auto-offset-reset: earliest
  quartz:
    job-store-type: jdbc
    jdbc:
      initialize-schema: never
  threads:
    virtual:
      enabled: true

jwt:
  secret: ${JWT_SECRET}
  access-token-expiry-minutes: 15
  refresh-token-expiry-days: 30

aws:
  s3:
    bucket: ${S3_BUCKET}
    region: ${AWS_REGION:eu-west-1}
    endpoint: ${S3_ENDPOINT:}          # override for MinIO

stripe:
  secret-key: ${STRIPE_SECRET_KEY}
  webhook-secret: ${STRIPE_WEBHOOK_SECRET}

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info
  endpoint:
    health:
      show-details: always

logging:
  structured:
    format:
      console: logstash
```

- [ ] **Step 4: Create `application-local.yml`**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/capsule
    username: capsule
    password: capsule
  data:
    redis:
      host: localhost
      port: 6379
  kafka:
    bootstrap-servers: localhost:9092

aws:
  s3:
    bucket: capsule-local
    endpoint: http://localhost:9000

jwt:
  secret: local-dev-secret-at-least-32-chars-long!!

stripe:
  secret-key: sk_test_placeholder
  webhook-secret: whsec_placeholder
```

- [ ] **Step 5: Create `backend/docker-compose.yml`**

```yaml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: capsule
      POSTGRES_USER: capsule
      POSTGRES_PASSWORD: capsule
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  kafka:
    image: confluentinc/cp-kafka:7.6.0
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT
      CLUSTER_ID: MkU3OEVBNTcwNTJENDM2Qk
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    ports:
      - "9092:9092"

  minio:
    image: minio/minio
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio_data:/data

volumes:
  postgres_data:
  minio_data:
```

- [ ] **Step 6: Start local stack and verify**

```bash
cd backend
docker compose up -d
docker compose ps   # all services should be healthy
```

- [ ] **Step 7: Verify project compiles**

```bash
./mvnw compile
```
Expected: `BUILD SUCCESS`

- [ ] **Step 8: Commit**

```bash
git add backend/
git commit -m "[capsule] scaffold: Spring Boot project, Docker Compose local stack"
```

---

## Task 2: Shared Web Infrastructure

**Files:**
- Create: `backend/src/main/java/com/capsule/shared/web/CorrelationIdFilter.java`
- Create: `backend/src/main/java/com/capsule/shared/web/ApiError.java`
- Create: `backend/src/main/java/com/capsule/shared/web/GlobalExceptionHandler.java`

- [ ] **Step 1: Write failing test for CorrelationIdFilter**

```java
// backend/src/test/java/com/capsule/shared/web/CorrelationIdFilterTest.java
package com.capsule.shared.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@SpringBootTest
@AutoConfigureMockMvc
class CorrelationIdFilterTest {

    @Autowired MockMvc mockMvc;

    @Test
    void responseIncludesCorrelationIdHeader() throws Exception {
        mockMvc.perform(get("/actuator/health"))
               .andExpect(header().exists("X-Correlation-Id"));
    }
}
```

- [ ] **Step 2: Run to confirm it fails**

```bash
./mvnw test -Dtest=CorrelationIdFilterTest
```
Expected: FAIL — header not present

- [ ] **Step 3: Implement `CorrelationIdFilter`**

```java
package com.capsule.shared.web;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
public class CorrelationIdFilter implements Filter {

    private static final String HEADER = "X-Correlation-Id";
    private static final String MDC_KEY = "correlationId";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        var request = (HttpServletRequest) req;
        var response = (HttpServletResponse) res;
        String id = request.getHeader(HEADER);
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, id);
        response.setHeader(HEADER, id);
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
```

- [ ] **Step 4: Implement `ApiError` and `GlobalExceptionHandler`**

```java
// ApiError.java
package com.capsule.shared.web;

import java.time.Instant;

public record ApiError(int status, String error, String message, Instant timestamp) {
    public static ApiError of(int status, String error, String message) {
        return new ApiError(status, error, message, Instant.now());
    }
}
```

```java
// GlobalExceptionHandler.java
package com.capsule.shared.web;

import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex) {
        var error = ApiError.of(ex.getStatusCode().value(), ex.getReason(), ex.getMessage());
        return ResponseEntity.status(ex.getStatusCode()).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(403)
                .body(ApiError.of(403, "Forbidden", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        return ResponseEntity.status(500)
                .body(ApiError.of(500, "Internal Server Error", "An unexpected error occurred"));
    }
}
```

- [ ] **Step 5: Run tests**

```bash
./mvnw test -Dtest=CorrelationIdFilterTest
```
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add backend/src/
git commit -m "[capsule] shared: correlation ID filter, global error handler"
```

---

## Task 3: Database Migrations

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__create_users.sql`
- Create: `backend/src/main/resources/db/migration/V2__create_capsules.sql`
- Create: `backend/src/main/resources/db/migration/V3__create_capsule_items.sql`
- Create: `backend/src/main/resources/db/migration/V4__create_recipients.sql`
- Create: `backend/src/main/resources/db/migration/V5__create_subscriptions.sql`

- [ ] **Step 1: Write `V1__create_users.sql`**

```sql
CREATE TYPE user_tier AS ENUM ('seed', 'vessel', 'legacy');

CREATE TABLE users (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email               VARCHAR(255) UNIQUE NOT NULL,
    password_hash       VARCHAR(255),
    oauth_provider      VARCHAR(50),
    oauth_subject       VARCHAR(255),
    tier                user_tier NOT NULL DEFAULT 'seed',
    stripe_customer_id  VARCHAR(255),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT oauth_or_password CHECK (
        password_hash IS NOT NULL OR (oauth_provider IS NOT NULL AND oauth_subject IS NOT NULL)
    )
);

CREATE UNIQUE INDEX idx_users_oauth ON users (oauth_provider, oauth_subject)
    WHERE oauth_provider IS NOT NULL;
```

- [ ] **Step 2: Write `V2__create_capsules.sql`**

```sql
CREATE TYPE capsule_state AS ENUM ('draft', 'sealed', 'accessible', 'archived');
CREATE TYPE capsule_visibility AS ENUM ('public', 'private');

CREATE TABLE capsules (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id            UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    title               VARCHAR(255) NOT NULL,
    state               capsule_state NOT NULL DEFAULT 'draft',
    visibility          capsule_visibility NOT NULL DEFAULT 'public',
    open_at             TIMESTAMPTZ NOT NULL,
    encryption_key_hint VARCHAR(255),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_capsules_owner ON capsules (owner_id);
CREATE INDEX idx_capsules_delivery ON capsules (state, open_at)
    WHERE state = 'sealed';
```

- [ ] **Step 3: Write `V3__create_capsule_items.sql`**

```sql
CREATE TYPE item_type AS ENUM ('text', 'image', 'video_link', 'music_link', 'keepsake');

CREATE TABLE capsule_items (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    capsule_id  UUID NOT NULL REFERENCES capsules(id) ON DELETE CASCADE,
    type        item_type NOT NULL,
    content     TEXT,
    s3_key      VARCHAR(1024),
    sort_order  INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_items_capsule ON capsule_items (capsule_id);
```

- [ ] **Step 4: Write `V4__create_recipients.sql`**

```sql
CREATE TABLE recipients (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    capsule_id        UUID NOT NULL REFERENCES capsules(id) ON DELETE CASCADE,
    email             VARCHAR(255) NOT NULL,
    user_id           UUID REFERENCES users(id) ON DELETE SET NULL,
    access_token      VARCHAR(512),
    token_expires_at  TIMESTAMPTZ,
    notified_at       TIMESTAMPTZ,
    delivery_error    TEXT,
    accessed_at       TIMESTAMPTZ,
    CONSTRAINT unique_recipient_per_capsule UNIQUE (capsule_id, email)
);

CREATE INDEX idx_recipients_capsule ON recipients (capsule_id);
```

- [ ] **Step 5: Write `V5__create_subscriptions.sql`**

```sql
CREATE TYPE billing_cycle AS ENUM ('monthly', 'yearly');
CREATE TYPE subscription_status AS ENUM ('active', 'past_due', 'cancelled');

CREATE TABLE subscriptions (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    stripe_subscription_id   VARCHAR(255) NOT NULL UNIQUE,
    tier                     user_tier NOT NULL,
    billing_cycle            billing_cycle NOT NULL,
    status                   subscription_status NOT NULL,
    current_period_end       TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX idx_subscriptions_user ON subscriptions (user_id);
```

- [ ] **Step 6: Run migrations against local DB**

```bash
cd backend
SPRING_PROFILES_ACTIVE=local ./mvnw flyway:migrate
```
Expected: `Successfully applied 5 migrations`

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/resources/db/
git commit -m "[capsule] db: Flyway migrations for all entities"
```

---

## Task 4: JWT Service

**Files:**
- Create: `backend/src/main/java/com/capsule/shared/security/JwtService.java`
- Test: `backend/src/test/java/com/capsule/shared/security/JwtServiceTest.java`

- [ ] **Step 1: Write failing tests**

```java
package com.capsule.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "jwt.secret=test-secret-must-be-at-least-32-chars!!",
    "jwt.access-token-expiry-minutes=15",
    "jwt.refresh-token-expiry-days=30"
})
class JwtServiceTest {

    @Autowired JwtService jwtService;

    @Test
    void generatedAccessTokenIsValidatable() {
        var userId = UUID.randomUUID();
        var token = jwtService.generateAccessToken(userId, "test@example.com", "seed");
        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    void extractsSubjectFromToken() {
        var userId = UUID.randomUUID();
        var token = jwtService.generateAccessToken(userId, "test@example.com", "seed");
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
    }

    @Test
    void extractsTierFromToken() {
        var userId = UUID.randomUUID();
        var token = jwtService.generateAccessToken(userId, "test@example.com", "vessel");
        assertThat(jwtService.extractTier(token)).isEqualTo("vessel");
    }

    @Test
    void invalidTokenFailsValidation() {
        assertThat(jwtService.isValid("not.a.token")).isFalse();
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```bash
./mvnw test -Dtest=JwtServiceTest
```

- [ ] **Step 3: Implement `JwtService`**

```java
package com.capsule.shared.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final long accessTokenMinutes;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiry-minutes}") long accessTokenMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenMinutes = accessTokenMinutes;
    }

    public String generateAccessToken(UUID userId, String email, String tier) {
        var now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("tier", tier)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).getPayload().getSubject());
    }

    public String extractTier(String token) {
        return parseClaims(token).getPayload().get("tier", String.class);
    }

    public String extractEmail(String token) {
        return parseClaims(token).getPayload().get("email", String.class);
    }

    private Jws<Claims> parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
    }
}
```

- [ ] **Step 4: Run tests**

```bash
./mvnw test -Dtest=JwtServiceTest
```
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/
git commit -m "[capsule] shared/security: JwtService — generate and validate access tokens"
```

---

## Task 5: Security Config + JWT Auth Filter

**Files:**
- Create: `backend/src/main/java/com/capsule/shared/security/JwtAuthFilter.java`
- Create: `backend/src/main/java/com/capsule/shared/security/SecurityConfig.java`

- [ ] **Step 1: Implement `JwtAuthFilter`**

```java
package com.capsule.shared.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        var header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            var token = header.substring(7);
            if (jwtService.isValid(token)) {
                var userId = jwtService.extractUserId(token);
                var tier = jwtService.extractTier(token);
                var auth = new UsernamePasswordAuthenticationToken(
                        userId, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + tier.toUpperCase()))
                );
                auth.setDetails(token);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }
}
```

- [ ] **Step 2: Implement `SecurityConfig`**

```java
package com.capsule.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/login/oauth2/**", "/oauth2/**").permitAll()
                        .requestMatchers("/api/v1/billing/webhook").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/capsules/public").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/capsules/{id}").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth.successHandler(null)) // wired in Task 6
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

- [ ] **Step 3: Verify app starts with local profile**

```bash
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run &
sleep 10
curl -s http://localhost:8080/actuator/health | grep UP
kill %1
```
Expected: `{"status":"UP",...}`

- [ ] **Step 4: Commit**

```bash
git add backend/src/
git commit -m "[capsule] shared/security: JWT auth filter, security config"
```

---

## Task 6: User Domain + Email/Password Auth

**Files:**
- Create: `backend/src/main/java/com/capsule/user/User.java`
- Create: `backend/src/main/java/com/capsule/user/UserTier.java`
- Create: `backend/src/main/java/com/capsule/user/UserRepository.java`
- Create: `backend/src/main/java/com/capsule/user/UserService.java`
- Create: `backend/src/main/java/com/capsule/user/AuthController.java`
- Create: `backend/src/main/java/com/capsule/user/UserController.java`
- Create: `backend/src/main/java/com/capsule/user/dto/*.java`
- Create: `backend/src/main/java/com/capsule/shared/config/RedisConfig.java`
- Test: `backend/src/test/java/com/capsule/user/UserServiceTest.java`
- Test: `backend/src/test/java/com/capsule/user/AuthControllerTest.java`

- [ ] **Step 1: Create enums and User entity**

```java
// UserTier.java
package com.capsule.user;
public enum UserTier { seed, vessel, legacy }
```

```java
// User.java
package com.capsule.user;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    private String passwordHash;
    private String oauthProvider;
    private String oauthSubject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "user_tier")
    private UserTier tier = UserTier.seed;

    private String stripeCustomerId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    // getters and setters
    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getOauthProvider() { return oauthProvider; }
    public void setOauthProvider(String oauthProvider) { this.oauthProvider = oauthProvider; }
    public String getOauthSubject() { return oauthSubject; }
    public void setOauthSubject(String oauthSubject) { this.oauthSubject = oauthSubject; }
    public UserTier getTier() { return tier; }
    public void setTier(UserTier tier) { this.tier = tier; }
    public String getStripeCustomerId() { return stripeCustomerId; }
    public void setStripeCustomerId(String stripeCustomerId) { this.stripeCustomerId = stripeCustomerId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
```

```java
// UserRepository.java
package com.capsule.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByOauthProviderAndOauthSubject(String provider, String subject);
    boolean existsByEmail(String email);
}
```

- [ ] **Step 2: Create DTOs**

```java
// RegisterRequest.java
package com.capsule.user.dto;
import jakarta.validation.constraints.*;
public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8) String password
) {}

// LoginRequest.java
package com.capsule.user.dto;
import jakarta.validation.constraints.NotBlank;
public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

// TokenResponse.java
package com.capsule.user.dto;
public record TokenResponse(String accessToken, String refreshToken) {}

// UserResponse.java
package com.capsule.user.dto;
import com.capsule.user.UserTier;
import java.util.UUID;
public record UserResponse(UUID id, String email, UserTier tier) {}
```

- [ ] **Step 3: Create `RedisConfig`**

```java
package com.capsule.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
}
```

- [ ] **Step 4: Write failing UserService tests**

```java
package com.capsule.user;

import com.capsule.user.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @InjectMocks UserService userService;

    @Test
    void registerCreatesUserWithHashedPassword() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var user = userService.register(new RegisterRequest("test@example.com", "password123"));

        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getPasswordHash()).isNotEqualTo("password123");
        assertThat(new BCryptPasswordEncoder().matches("password123", user.getPasswordHash())).isTrue();
    }

    @Test
    void registerThrowsWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(new RegisterRequest("test@example.com", "password123")))
                .isInstanceOf(ResponseStatusException.class);
    }
}
```

- [ ] **Step 5: Run to confirm failure**

```bash
./mvnw test -Dtest=UserServiceTest
```

- [ ] **Step 6: Implement `UserService`**

```java
package com.capsule.user;

import com.capsule.shared.security.JwtService;
import com.capsule.user.dto.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.UUID;

@Service
public class UserService {

    private static final Duration REFRESH_TTL = Duration.ofDays(30);
    private static final String REFRESH_KEY_PREFIX = "refresh:";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final StringRedisTemplate redis;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, StringRedisTemplate redis) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.redis = redis;
    }

    public User register(com.capsule.user.dto.RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        var user = new User();
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        return userRepository.save(user);
    }

    public TokenResponse login(LoginRequest req) {
        var user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return issueTokens(user);
    }

    public TokenResponse refresh(String refreshToken) {
        var key = REFRESH_KEY_PREFIX + refreshToken;
        var userIdStr = redis.opsForValue().get(key);
        if (userIdStr == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
        }
        redis.delete(key);
        var user = userRepository.findById(UUID.fromString(userIdStr))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        return issueTokens(user);
    }

    public void logout(String refreshToken) {
        redis.delete(REFRESH_KEY_PREFIX + refreshToken);
    }

    public void invalidateRefreshTokenForUser(UUID userId) {
        // called by Stripe webhook on downgrade — scan for user's token key
        var keys = redis.keys(REFRESH_KEY_PREFIX + "*");
        if (keys != null) {
            keys.stream()
                .filter(k -> userId.toString().equals(redis.opsForValue().get(k)))
                .forEach(redis::delete);
        }
    }

    public User upsertOAuthUser(String provider, String subject, String email) {
        return userRepository.findByOauthProviderAndOauthSubject(provider, subject)
                .orElseGet(() -> {
                    var user = new User();
                    user.setEmail(email);
                    user.setOauthProvider(provider);
                    user.setOauthSubject(subject);
                    return userRepository.save(user);
                });
    }

    public TokenResponse issueTokens(User user) {
        var accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getTier().name());
        var refreshToken = UUID.randomUUID().toString();
        redis.opsForValue().set(REFRESH_KEY_PREFIX + refreshToken, user.getId().toString(), REFRESH_TTL);
        return new TokenResponse(accessToken, refreshToken);
    }

    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public User updateEmail(UUID id, String newEmail) {
        var user = findById(id);
        user.setEmail(newEmail);
        return userRepository.save(user);
    }

    public void deleteUser(UUID id) {
        userRepository.deleteById(id);
    }
}
```

- [ ] **Step 7: Add `UpdateUserRequest` DTO**

```java
// UpdateUserRequest.java
package com.capsule.user.dto;
public record UpdateUserRequest(String email) {}
```

- [ ] **Step 8: Implement `AuthController` and `UserController`**

```java
// AuthController.java
package com.capsule.user;

import com.capsule.user.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest req) {
        var user = userService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UserResponse(user.getId(), user.getEmail(), user.getTier()));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(userService.login(req));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody String refreshToken) {
        return ResponseEntity.ok(userService.refresh(refreshToken.trim()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody String refreshToken) {
        userService.logout(refreshToken.trim());
        return ResponseEntity.noContent().build();
    }
}
```

```java
// UserController.java
package com.capsule.user;

import com.capsule.user.dto.UserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication auth) {
        var userId = (UUID) auth.getPrincipal();
        var user = userService.findById(userId);
        return ResponseEntity.ok(new UserResponse(user.getId(), user.getEmail(), user.getTier()));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateMe(@RequestBody UpdateUserRequest req,
                                                  Authentication auth) {
        var userId = (UUID) auth.getPrincipal();
        var user = userService.updateEmail(userId, req.email());
        return ResponseEntity.ok(new UserResponse(user.getId(), user.getEmail(), user.getTier()));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(Authentication auth) {
        var userId = (UUID) auth.getPrincipal();
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 8: Run user tests**

```bash
./mvnw test -Dtest=UserServiceTest
```
Expected: PASS

- [ ] **Step 9: Smoke test register + login**

```bash
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run &
sleep 10
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"anna@example.com","password":"password123"}' | jq .

curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"anna@example.com","password":"password123"}' | jq .
kill %1
```
Expected: user object, then `{"accessToken":"...","refreshToken":"..."}`

- [ ] **Step 10: Commit**

```bash
git add backend/src/
git commit -m "[capsule] user: User entity, auth endpoints (register/login/refresh/logout)"
```

---

## Task 7: OAuth2 Integration

**Files:**
- Create: `backend/src/main/java/com/capsule/shared/security/OAuth2SuccessHandler.java`
- Modify: `backend/src/main/java/com/capsule/shared/security/SecurityConfig.java`

- [ ] **Step 1: Implement `OAuth2SuccessHandler`**

```java
package com.capsule.shared.security;

import com.capsule.user.UserService;
import jakarta.servlet.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserService userService;

    public OAuth2SuccessHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        var oauthUser = (OAuth2User) authentication.getPrincipal();
        // registrationId is stored in the OAuth2AuthenticationToken
        var token = (org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) authentication;
        var provider = token.getAuthorizedClientRegistrationId();
        var subject = oauthUser.getAttribute("sub") != null
                ? oauthUser.getAttribute("sub").toString()
                : oauthUser.getAttribute("id").toString();
        var email = oauthUser.<String>getAttribute("email");

        var user = userService.upsertOAuthUser(provider, subject, email);
        var tokens = userService.issueTokens(user);

        // Redirect to frontend with tokens as query params (frontend exchanges for cookie/storage)
        var redirectUrl = String.format("/oauth2/success?accessToken=%s&refreshToken=%s",
                tokens.accessToken(), tokens.refreshToken());
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
```

- [ ] **Step 2: Wire handler into SecurityConfig**

Replace `oauth2Login` line:
```java
.oauth2Login(oauth -> oauth.successHandler(oauth2SuccessHandler))
```

Add constructor parameter:
```java
private final OAuth2SuccessHandler oauth2SuccessHandler;
```

- [ ] **Step 3: Add OAuth2 client config to `application-local.yml`**

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID:placeholder}
            client-secret: ${GOOGLE_CLIENT_SECRET:placeholder}
            scope: openid,email,profile
```

- [ ] **Step 4: Verify app still starts**

```bash
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run &
sleep 10
curl -s http://localhost:8080/actuator/health | grep UP
kill %1
```

- [ ] **Step 5: Commit**

```bash
git add backend/src/ backend/src/main/resources/
git commit -m "[capsule] user: OAuth2 login (Google/Apple) with JWT handoff"
```

---

## Task 8: Capsule Module

**Files:**
- Create: `backend/src/main/java/com/capsule/capsule/` (all files)
- Test: `backend/src/test/java/com/capsule/capsule/CapsuleServiceTest.java`

- [ ] **Step 1: Create enums and entities**

```java
// CapsuleState.java
package com.capsule.capsule;
public enum CapsuleState { draft, sealed, accessible, archived }

// CapsuleVisibility.java
package com.capsule.capsule;
public enum CapsuleVisibility { public_, private_ }  // avoid reserved words

// ItemType.java
package com.capsule.capsule;
public enum ItemType { text, image, video_link, music_link, keepsake }
```

```java
// Capsule.java
package com.capsule.capsule;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "capsules")
public class Capsule {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "capsule_state")
    private CapsuleState state = CapsuleState.draft;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "capsule_visibility")
    private CapsuleVisibility visibility = CapsuleVisibility.public_;

    @Column(nullable = false)
    private Instant openAt;

    private String encryptionKeyHint;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "capsule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    private List<CapsuleItem> items = new ArrayList<>();

    @PreUpdate void onUpdate() { this.updatedAt = Instant.now(); }

    // getters/setters omitted for brevity — generate with IDE
    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public CapsuleState getState() { return state; }
    public void setState(CapsuleState state) { this.state = state; }
    public CapsuleVisibility getVisibility() { return visibility; }
    public void setVisibility(CapsuleVisibility visibility) { this.visibility = visibility; }
    public Instant getOpenAt() { return openAt; }
    public void setOpenAt(Instant openAt) { this.openAt = openAt; }
    public String getEncryptionKeyHint() { return encryptionKeyHint; }
    public void setEncryptionKeyHint(String encryptionKeyHint) { this.encryptionKeyHint = encryptionKeyHint; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<CapsuleItem> getItems() { return items; }
}
```

```java
// CapsuleItem.java
package com.capsule.capsule;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "capsule_items")
public class CapsuleItem {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "capsule_id", nullable = false)
    private Capsule capsule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "item_type")
    private ItemType type;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String s3Key;
    private int sortOrder;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate void onUpdate() { this.updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public Capsule getCapsule() { return capsule; }
    public void setCapsule(Capsule capsule) { this.capsule = capsule; }
    public ItemType getType() { return type; }
    public void setType(ItemType type) { this.type = type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getS3Key() { return s3Key; }
    public void setS3Key(String s3Key) { this.s3Key = s3Key; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
```

```java
// CapsuleRepository.java
package com.capsule.capsule;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.*;

public interface CapsuleRepository extends JpaRepository<Capsule, UUID> {
    Page<Capsule> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId, Pageable pageable);
    Page<Capsule> findByOwnerIdAndStateOrderByCreatedAtDesc(UUID ownerId, CapsuleState state, Pageable pageable);
    Page<Capsule> findByStateAndVisibilityOrderByCreatedAtDesc(CapsuleState state, CapsuleVisibility visibility, Pageable pageable);
    List<Capsule> findByStateAndOpenAtBefore(CapsuleState state, Instant now);
}

// CapsuleItemRepository.java
package com.capsule.capsule;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CapsuleItemRepository extends JpaRepository<CapsuleItem, UUID> {}
```

- [ ] **Step 2: Create DTOs**

```java
// CreateCapsuleRequest.java
package com.capsule.capsule.dto;
import com.capsule.capsule.CapsuleVisibility;
import jakarta.validation.constraints.*;
import java.time.Instant;

public record CreateCapsuleRequest(
    @NotBlank String title,
    @NotNull CapsuleVisibility visibility,
    @NotNull @Future Instant openAt
) {}

// UpdateCapsuleRequest.java
package com.capsule.capsule.dto;
import java.time.Instant;
public record UpdateCapsuleRequest(String title, Instant openAt) {}

// AddItemRequest.java
package com.capsule.capsule.dto;
import com.capsule.capsule.ItemType;
import jakarta.validation.constraints.NotNull;
public record AddItemRequest(@NotNull ItemType type, String content, String s3Key, int sortOrder) {}

// UploadUrlResponse.java
package com.capsule.capsule.dto;
public record UploadUrlResponse(String uploadUrl, String s3Key) {}

// CapsuleResponse.java — build from Capsule entity, items inline
package com.capsule.capsule.dto;
import com.capsule.capsule.*;
import java.time.Instant;
import java.util.*;

public record CapsuleResponse(
    UUID id, UUID ownerId, String title,
    CapsuleState state, CapsuleVisibility visibility,
    Instant openAt, Instant createdAt, List<ItemResponse> items
) {
    public record ItemResponse(UUID id, ItemType type, String content, String s3Key, int sortOrder) {}

    public static CapsuleResponse from(Capsule c) {
        var items = c.getItems().stream()
            .map(i -> new ItemResponse(i.getId(), i.getType(), i.getContent(), i.getS3Key(), i.getSortOrder()))
            .toList();
        return new CapsuleResponse(c.getId(), c.getOwnerId(), c.getTitle(),
            c.getState(), c.getVisibility(), c.getOpenAt(), c.getCreatedAt(), items);
    }
}
```

- [ ] **Step 3: Write failing CapsuleService tests**

```java
package com.capsule.capsule;

import com.capsule.capsule.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CapsuleServiceTest {

    @Mock CapsuleRepository capsuleRepository;
    @Mock CapsuleItemRepository itemRepository;
    @InjectMocks CapsuleService capsuleService;

    @Test
    void createCapsuleSetsDraftState() {
        var ownerId = UUID.randomUUID();
        var req = new CreateCapsuleRequest("My Capsule", CapsuleVisibility.public_,
                Instant.now().plus(1, ChronoUnit.DAYS));
        when(capsuleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var capsule = capsuleService.create(ownerId, req);

        assertThat(capsule.getState()).isEqualTo(CapsuleState.draft);
        assertThat(capsule.getOwnerId()).isEqualTo(ownerId);
    }

    @Test
    void sealTransitionsDraftToSealed() {
        var capsule = new Capsule();
        capsule.setState(CapsuleState.draft);
        var ownerId = UUID.randomUUID();
        capsule.setOwnerId(ownerId);
        when(capsuleRepository.findById(any())).thenReturn(Optional.of(capsule));
        when(capsuleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var sealed = capsuleService.seal(UUID.randomUUID(), ownerId);

        assertThat(sealed.getState()).isEqualTo(CapsuleState.sealed);
    }

    @Test
    void sealRejectsNonOwner() {
        var capsule = new Capsule();
        capsule.setState(CapsuleState.draft);
        capsule.setOwnerId(UUID.randomUUID());
        when(capsuleRepository.findById(any())).thenReturn(Optional.of(capsule));

        assertThatThrownBy(() -> capsuleService.seal(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class);
    }
}
```

- [ ] **Step 4: Run to confirm failure**

```bash
./mvnw test -Dtest=CapsuleServiceTest
```

- [ ] **Step 5: Implement `CapsuleService`**

```java
package com.capsule.capsule;

import com.capsule.capsule.dto.*;
import com.capsule.shared.config.S3Config;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.Duration;
import java.util.UUID;

@Service
@Transactional
public class CapsuleService {

    private final CapsuleRepository capsuleRepository;
    private final CapsuleItemRepository itemRepository;
    private final S3Presigner s3Presigner;
    private final String s3Bucket;

    public CapsuleService(CapsuleRepository capsuleRepository,
                          CapsuleItemRepository itemRepository,
                          S3Presigner s3Presigner,
                          @org.springframework.beans.factory.annotation.Value("${aws.s3.bucket}") String s3Bucket) {
        this.capsuleRepository = capsuleRepository;
        this.itemRepository = itemRepository;
        this.s3Presigner = s3Presigner;
        this.s3Bucket = s3Bucket;
    }

    public Capsule create(UUID ownerId, CreateCapsuleRequest req) {
        var capsule = new Capsule();
        capsule.setOwnerId(ownerId);
        capsule.setTitle(req.title());
        capsule.setVisibility(req.visibility());
        capsule.setOpenAt(req.openAt());
        return capsuleRepository.save(capsule);
    }

    public Page<Capsule> listOwn(UUID ownerId, CapsuleState stateFilter, Pageable pageable) {
        if (stateFilter != null) {
            return capsuleRepository.findByOwnerIdAndStateOrderByCreatedAtDesc(ownerId, stateFilter, pageable);
        }
        return capsuleRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId, pageable);
    }

    @Transactional(readOnly = true)
    public Capsule getForOwner(UUID id, UUID ownerId) {
        var capsule = findOrThrow(id);
        if (!capsule.getOwnerId().equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your capsule");
        }
        return capsule;
    }

    @Transactional(readOnly = true)
    public Capsule getAccessible(UUID id) {
        var capsule = findOrThrow(id);
        if (capsule.getState() != CapsuleState.accessible) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Capsule is not accessible");
        }
        return capsule;
    }

    public Capsule update(UUID id, UUID ownerId, UpdateCapsuleRequest req) {
        var capsule = getForOwner(id, ownerId);
        requireDraft(capsule);
        if (req.title() != null) capsule.setTitle(req.title());
        if (req.openAt() != null) capsule.setOpenAt(req.openAt());
        return capsuleRepository.save(capsule);
    }

    public Capsule seal(UUID id, UUID ownerId) {
        var capsule = getForOwner(id, ownerId);
        requireDraft(capsule);
        capsule.setState(CapsuleState.sealed);
        return capsuleRepository.save(capsule);
    }

    public void delete(UUID id, UUID ownerId) {
        var capsule = getForOwner(id, ownerId);
        requireDraft(capsule);
        capsuleRepository.delete(capsule);
    }

    public UploadUrlResponse generateUploadUrl(UUID capsuleId, UUID ownerId, String contentType) {
        getForOwner(capsuleId, ownerId); // ownership check
        var s3Key = "capsules/" + capsuleId + "/" + UUID.randomUUID();
        var presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(r -> r.bucket(s3Bucket).key(s3Key).contentType(contentType))
                .build();
        var url = s3Presigner.presignPutObject(presignRequest).url().toString();
        return new UploadUrlResponse(url, s3Key);
    }

    public CapsuleItem addItem(UUID capsuleId, UUID ownerId, AddItemRequest req) {
        var capsule = getForOwner(capsuleId, ownerId);
        requireDraft(capsule);
        var item = new CapsuleItem();
        item.setCapsule(capsule);
        item.setType(req.type());
        item.setContent(req.content());
        item.setS3Key(req.s3Key());
        item.setSortOrder(req.sortOrder());
        return itemRepository.save(item);
    }

    public void deleteItem(UUID capsuleId, UUID itemId, UUID ownerId) {
        var capsule = getForOwner(capsuleId, ownerId);
        requireDraft(capsule);
        var item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
        itemRepository.delete(item);
    }

    public Page<Capsule> publicFeed(Pageable pageable) {
        return capsuleRepository.findByStateAndVisibilityOrderByCreatedAtDesc(
                CapsuleState.accessible, CapsuleVisibility.public_, pageable);
    }

    private Capsule findOrThrow(UUID id) {
        return capsuleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Capsule not found"));
    }

    private void requireDraft(Capsule capsule) {
        if (capsule.getState() != CapsuleState.draft) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Capsule is not in draft state");
        }
    }
}
```

- [ ] **Step 6: Add `S3Config`**

```java
package com.capsule.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class S3Config {

    @Bean
    public S3Presigner s3Presigner(
            @Value("${aws.s3.region}") String region,
            @Value("${aws.s3.endpoint:}") String endpoint) {
        var builder = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create());
        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }
}
```

- [ ] **Step 7: Implement `CapsuleController`**

```java
package com.capsule.capsule;

import com.capsule.capsule.dto.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/capsules")
public class CapsuleController {

    private final CapsuleService capsuleService;

    public CapsuleController(CapsuleService capsuleService) {
        this.capsuleService = capsuleService;
    }

    @PostMapping
    public ResponseEntity<CapsuleResponse> create(@Valid @RequestBody CreateCapsuleRequest req,
                                                   Authentication auth) {
        var capsule = capsuleService.create(userId(auth), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(CapsuleResponse.from(capsule));
    }

    @GetMapping
    public ResponseEntity<Page<CapsuleResponse>> list(
            @RequestParam(required = false) CapsuleState state,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        var pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                capsuleService.listOwn(userId(auth), state, pageable).map(CapsuleResponse::from));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CapsuleResponse> get(@PathVariable UUID id,
                                                @RequestParam(required = false) String token,
                                                Authentication auth) {
        Capsule capsule;
        if (token != null) {
            capsule = capsuleService.getAccessible(id); // token validated in delivery module
        } else {
            capsule = capsuleService.getForOwner(id, userId(auth));
        }
        return ResponseEntity.ok(CapsuleResponse.from(capsule));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CapsuleResponse> update(@PathVariable UUID id,
                                                   @RequestBody UpdateCapsuleRequest req,
                                                   Authentication auth) {
        return ResponseEntity.ok(CapsuleResponse.from(capsuleService.update(id, userId(auth), req)));
    }

    @PostMapping("/{id}/seal")
    public ResponseEntity<CapsuleResponse> seal(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(CapsuleResponse.from(capsuleService.seal(id, userId(auth))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication auth) {
        capsuleService.delete(id, userId(auth));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/items/upload-url")
    public ResponseEntity<UploadUrlResponse> uploadUrl(@PathVariable UUID id,
                                                        @RequestParam String contentType,
                                                        Authentication auth) {
        return ResponseEntity.ok(capsuleService.generateUploadUrl(id, userId(auth), contentType));
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<CapsuleResponse.ItemResponse> addItem(@PathVariable UUID id,
                                                                  @Valid @RequestBody AddItemRequest req,
                                                                  Authentication auth) {
        var item = capsuleService.addItem(id, userId(auth), req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CapsuleResponse.ItemResponse(item.getId(), item.getType(),
                        item.getContent(), item.getS3Key(), item.getSortOrder()));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable UUID id, @PathVariable UUID itemId,
                                            Authentication auth) {
        capsuleService.deleteItem(id, itemId, userId(auth));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/public")
    public ResponseEntity<Page<CapsuleResponse>> publicFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(capsuleService.publicFeed(pageable).map(CapsuleResponse::from));
    }

    private UUID userId(Authentication auth) {
        return (UUID) auth.getPrincipal();
    }
}
```

- [ ] **Step 8: Run capsule tests**

```bash
./mvnw test -Dtest=CapsuleServiceTest
```
Expected: PASS

- [ ] **Step 9: Commit**

```bash
git add backend/src/
git commit -m "[capsule] capsule: CRUD, sealing, S3 pre-signed upload URLs"
```

---

## Task 9: Delivery Module — Recipients + Scheduler + Kafka

**Files:**
- Create: `backend/src/main/java/com/capsule/delivery/` (all files)
- Create: `backend/src/main/java/com/capsule/shared/config/KafkaConfig.java`
- Create: `backend/src/main/java/com/capsule/shared/config/QuartzConfig.java`
- Test: `backend/src/test/java/com/capsule/delivery/TokenServiceTest.java`
- Test: `backend/src/test/java/com/capsule/delivery/DeliveryConsumerTest.java`

- [ ] **Step 1: Create `Recipient` entity**

```java
package com.capsule.delivery;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recipients")
public class Recipient {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID capsuleId;

    @Column(nullable = false)
    private String email;

    private UUID userId;
    private String accessToken;
    private Instant tokenExpiresAt;
    private Instant notifiedAt;
    private String deliveryError;
    private Instant accessedAt;

    public UUID getId() { return id; }
    public UUID getCapsuleId() { return capsuleId; }
    public void setCapsuleId(UUID capsuleId) { this.capsuleId = capsuleId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public Instant getTokenExpiresAt() { return tokenExpiresAt; }
    public void setTokenExpiresAt(Instant tokenExpiresAt) { this.tokenExpiresAt = tokenExpiresAt; }
    public Instant getNotifiedAt() { return notifiedAt; }
    public void setNotifiedAt(Instant notifiedAt) { this.notifiedAt = notifiedAt; }
    public String getDeliveryError() { return deliveryError; }
    public void setDeliveryError(String deliveryError) { this.deliveryError = deliveryError; }
    public Instant getAccessedAt() { return accessedAt; }
    public void setAccessedAt(Instant accessedAt) { this.accessedAt = accessedAt; }
}
```

```java
// RecipientRepository.java
package com.capsule.delivery;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface RecipientRepository extends JpaRepository<Recipient, UUID> {
    List<Recipient> findByCapsuleId(UUID capsuleId);
    List<Recipient> findByCapsuleIdAndNotifiedAtIsNull(UUID capsuleId);

    @Modifying
    @Query("UPDATE Recipient r SET r.accessedAt = CURRENT_TIMESTAMP WHERE r.id = :id AND r.accessedAt IS NULL")
    int markAccessed(@Param("id") UUID id);
}
```

- [ ] **Step 2: Write failing `TokenService` tests**

```java
package com.capsule.delivery;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class TokenServiceTest {

    TokenService tokenService = new TokenService("test-secret-32-chars-minimum-here!!");

    @Test
    void generatedTokenIsVerifiable() {
        var capsuleId = UUID.randomUUID();
        var email = "test@example.com";
        var expires = Instant.now().plus(7, ChronoUnit.DAYS);
        var token = tokenService.generate(capsuleId, email, expires);
        assertThat(tokenService.verify(token, capsuleId, email, expires)).isTrue();
    }

    @Test
    void tamperedTokenFailsVerification() {
        var capsuleId = UUID.randomUUID();
        var email = "test@example.com";
        var expires = Instant.now().plus(7, ChronoUnit.DAYS);
        var token = tokenService.generate(capsuleId, email, expires);
        assertThat(tokenService.verify(token + "x", capsuleId, email, expires)).isFalse();
    }
}
```

- [ ] **Step 3: Run to confirm failure**

```bash
./mvnw test -Dtest=TokenServiceTest
```

- [ ] **Step 4: Implement `TokenService`**

```java
package com.capsule.delivery;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class TokenService {

    private final String secret;

    public TokenService(@Value("${jwt.secret}") String secret) {
        this.secret = secret;
    }

    public String generate(UUID capsuleId, String email, Instant expiresAt) {
        return hmac(payload(capsuleId, email, expiresAt));
    }

    public boolean verify(String token, UUID capsuleId, String email, Instant expiresAt) {
        var expected = generate(capsuleId, email, expiresAt);
        return expected.equals(token);
    }

    private String payload(UUID capsuleId, String email, Instant expiresAt) {
        return capsuleId + "|" + email + "|" + expiresAt.toEpochMilli();
    }

    private String hmac(String data) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC error", e);
        }
    }
}
```

- [ ] **Step 5: Run token tests**

```bash
./mvnw test -Dtest=TokenServiceTest
```
Expected: PASS

- [ ] **Step 6: Add `KafkaConfig`**

```java
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
```

- [ ] **Step 7: Implement `DeliveryScheduler` and `DeliveryProducer`**

```java
// DeliveryProducer.java
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
```

```java
// DeliveryScheduler.java
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
```

- [ ] **Step 8: Implement `DeliveryService` and `DeliveryConsumer`**

```java
// DeliveryService.java
package com.capsule.delivery;

import com.capsule.capsule.*;
import com.capsule.delivery.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DeliveryService {

    private final RecipientRepository recipientRepository;
    private final CapsuleRepository capsuleRepository;
    private final TokenService tokenService;

    public DeliveryService(RecipientRepository recipientRepository,
                           CapsuleRepository capsuleRepository,
                           TokenService tokenService) {
        this.recipientRepository = recipientRepository;
        this.capsuleRepository = capsuleRepository;
        this.tokenService = tokenService;
    }

    public List<Recipient> addRecipients(UUID capsuleId, UUID ownerId, List<String> emails) {
        var capsule = capsuleRepository.findById(capsuleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Capsule not found"));
        if (!capsule.getOwnerId().equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (capsule.getState() == CapsuleState.accessible || capsule.getState() == CapsuleState.archived) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot add recipients to an accessible or archived capsule");
        }
        return emails.stream().map(email -> {
            var r = new Recipient();
            r.setCapsuleId(capsuleId);
            r.setEmail(email);
            return recipientRepository.save(r);
        }).toList();
    }

    public List<Recipient> listRecipients(UUID capsuleId, UUID ownerId) {
        var capsule = capsuleRepository.findById(capsuleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Capsule not found"));
        if (!capsule.getOwnerId().equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return recipientRepository.findByCapsuleId(capsuleId);
    }

    public void removeRecipient(UUID capsuleId, UUID recipientId, UUID ownerId) {
        var capsule = capsuleRepository.findById(capsuleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!capsule.getOwnerId().equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (capsule.getState() != CapsuleState.draft) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Can only remove recipients from draft capsules");
        }
        recipientRepository.deleteById(recipientId);
    }

    /** Called by DeliveryConsumer. Transitions state first, then sends emails. */
    public void deliverCapsule(UUID capsuleId) {
        var capsule = capsuleRepository.findById(capsuleId).orElse(null);
        if (capsule == null || capsule.getState() != CapsuleState.sealed) return;

        // Step 1: transition state — idempotency boundary
        capsule.setState(CapsuleState.accessible);
        capsuleRepository.save(capsule);

        // Step 2: send emails to un-notified recipients
        var recipients = recipientRepository.findByCapsuleIdAndNotifiedAtIsNull(capsuleId);
        for (var recipient : recipients) {
            try {
                var expires = Instant.now().plus(7, ChronoUnit.DAYS);
                var token = tokenService.generate(capsuleId, recipient.getEmail(), expires);
                recipient.setAccessToken(token);
                recipient.setTokenExpiresAt(expires);
                sendEmail(recipient.getEmail(), capsuleId, token);
                recipient.setNotifiedAt(Instant.now());
            } catch (Exception e) {
                recipient.setDeliveryError(e.getMessage());
            }
            recipientRepository.save(recipient);
        }
    }

    public Recipient validateTokenAndMarkAccessed(UUID capsuleId, String token) {
        var recipients = recipientRepository.findByCapsuleId(capsuleId);
        var recipient = recipients.stream()
                .filter(r -> token.equals(r.getAccessToken()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

        if (recipient.getTokenExpiresAt() != null && Instant.now().isAfter(recipient.getTokenExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Token has expired");
        }

        int updated = recipientRepository.markAccessed(recipient.getId());
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.GONE, "Token has already been used");
        }
        return recipient;
    }

    private void sendEmail(String email, UUID capsuleId, String token) {
        // Email sending — wire JavaMailSender or SendGrid SDK in production
        // For MVP: log the URL; wire real sender at IaC time
        var link = "https://capsule.app/open/" + capsuleId + "?token=" + token;
        org.slf4j.LoggerFactory.getLogger(DeliveryService.class)
                .info("Delivery link for {}: {}", email, link);
    }
}
```

```java
// DeliveryConsumer.java
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
            throw e; // let Kafka retry
        }
    }
}
```

- [ ] **Step 9: Implement `RecipientController`**

```java
package com.capsule.delivery;

import com.capsule.delivery.dto.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/capsules/{capsuleId}/recipients")
public class RecipientController {

    private final DeliveryService deliveryService;

    public RecipientController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @PostMapping
    public ResponseEntity<List<RecipientResponse>> add(
            @PathVariable UUID capsuleId,
            @RequestBody AddRecipientsRequest req,
            Authentication auth) {
        var recipients = deliveryService.addRecipients(capsuleId, userId(auth), req.emails());
        var response = recipients.stream()
                .map(r -> new RecipientResponse(r.getId(), r.getEmail(), r.getNotifiedAt(), r.getAccessedAt()))
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<RecipientResponse>> list(
            @PathVariable UUID capsuleId, Authentication auth) {
        var recipients = deliveryService.listRecipients(capsuleId, userId(auth));
        var response = recipients.stream()
                .map(r -> new RecipientResponse(r.getId(), r.getEmail(), r.getNotifiedAt(), r.getAccessedAt()))
                .toList();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{recipientId}")
    public ResponseEntity<Void> remove(
            @PathVariable UUID capsuleId,
            @PathVariable UUID recipientId,
            Authentication auth) {
        deliveryService.removeRecipient(capsuleId, recipientId, userId(auth));
        return ResponseEntity.noContent().build();
    }

    private UUID userId(Authentication auth) { return (UUID) auth.getPrincipal(); }
}
```

- [ ] **Step 10: Create DTOs**

```java
// AddRecipientsRequest.java
package com.capsule.delivery.dto;
import java.util.List;
public record AddRecipientsRequest(List<String> emails) {}

// RecipientResponse.java
package com.capsule.delivery.dto;
import java.time.Instant;
import java.util.UUID;
public record RecipientResponse(UUID id, String email, Instant notifiedAt, Instant accessedAt) {}
```

- [ ] **Step 11: Write `DeliveryServiceTest`**

```java
package com.capsule.delivery;

import com.capsule.capsule.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock RecipientRepository recipientRepository;
    @Mock CapsuleRepository capsuleRepository;
    @Mock TokenService tokenService;
    @InjectMocks DeliveryService deliveryService;

    @Test
    void addRecipientsRejectsNonOwner() {
        var capsule = new Capsule();
        capsule.setOwnerId(UUID.randomUUID());
        capsule.setState(CapsuleState.draft);
        when(capsuleRepository.findById(any())).thenReturn(Optional.of(capsule));

        assertThatThrownBy(() ->
            deliveryService.addRecipients(UUID.randomUUID(), UUID.randomUUID(), List.of("a@b.com")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void deliverCapsuleSkipsAlreadyAccessible() {
        var capsule = new Capsule();
        capsule.setState(CapsuleState.accessible);
        when(capsuleRepository.findById(any())).thenReturn(Optional.of(capsule));

        deliveryService.deliverCapsule(UUID.randomUUID()); // must not throw or re-transition

        verify(capsuleRepository, never()).save(any());
    }

    @Test
    void deliverCapsuleTransitionsToAccessibleBeforeSendingEmails() {
        var capsuleId = UUID.randomUUID();
        var capsule = new Capsule();
        capsule.setState(CapsuleState.sealed);

        var recipient = new Recipient();
        recipient.setEmail("recipient@example.com");
        recipient.setCapsuleId(capsuleId);

        when(capsuleRepository.findById(capsuleId)).thenReturn(Optional.of(capsule));
        when(capsuleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(recipientRepository.findByCapsuleIdAndNotifiedAtIsNull(capsuleId))
            .thenReturn(List.of(recipient));
        when(tokenService.generate(any(), any(), any())).thenReturn("test-token");
        when(recipientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        deliveryService.deliverCapsule(capsuleId);

        // State must be transitioned first
        assertThat(capsule.getState()).isEqualTo(CapsuleState.accessible);
        verify(capsuleRepository).save(capsule);
        // Recipient must be notified
        assertThat(recipient.getNotifiedAt()).isNotNull();
    }

    @Test
    void validateTokenRejectsMissingToken() {
        when(recipientRepository.findByCapsuleId(any())).thenReturn(List.of());

        assertThatThrownBy(() ->
            deliveryService.validateTokenAndMarkAccessed(UUID.randomUUID(), "bad-token"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void validateTokenRejectsExpiredToken() {
        var recipient = new Recipient();
        recipient.setAccessToken("my-token");
        recipient.setTokenExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        when(recipientRepository.findByCapsuleId(any())).thenReturn(List.of(recipient));

        assertThatThrownBy(() ->
            deliveryService.validateTokenAndMarkAccessed(UUID.randomUUID(), "my-token"))
                .isInstanceOf(ResponseStatusException.class);
    }
}
```

- [ ] **Step 12: Run delivery tests**

```bash
./mvnw test -Dtest=TokenServiceTest,DeliveryServiceTest
```
Expected: PASS

- [ ] **Step 12: Commit**

```bash
git add backend/src/
git commit -m "[capsule] delivery: recipients, Quartz scheduler, Kafka producer/consumer, token service"
```

---

## Task 10: Billing Module

**Files:**
- Create: `backend/src/main/java/com/capsule/billing/` (all files)

- [ ] **Step 1: Create `Subscription` entity**

```java
package com.capsule.billing;

import com.capsule.user.UserTier;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true)
    private String stripeSubscriptionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "user_tier")
    private UserTier tier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "billing_cycle")
    private BillingCycle billingCycle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "subscription_status")
    private SubscriptionStatus status;

    @Column(nullable = false)
    private Instant currentPeriodEnd;

    // getters/setters
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getStripeSubscriptionId() { return stripeSubscriptionId; }
    public void setStripeSubscriptionId(String id) { this.stripeSubscriptionId = id; }
    public UserTier getTier() { return tier; }
    public void setTier(UserTier tier) { this.tier = tier; }
    public BillingCycle getBillingCycle() { return billingCycle; }
    public void setBillingCycle(BillingCycle billingCycle) { this.billingCycle = billingCycle; }
    public SubscriptionStatus getStatus() { return status; }
    public void setStatus(SubscriptionStatus status) { this.status = status; }
    public Instant getCurrentPeriodEnd() { return currentPeriodEnd; }
    public void setCurrentPeriodEnd(Instant currentPeriodEnd) { this.currentPeriodEnd = currentPeriodEnd; }
}
```

```java
package com.capsule.billing;
public enum BillingCycle { monthly, yearly }

package com.capsule.billing;
public enum SubscriptionStatus { active, past_due, cancelled }
```

```java
// SubscriptionRepository.java
package com.capsule.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    Optional<Subscription> findByUserId(UUID userId);
    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);
}
```

- [ ] **Step 2: Implement `SubscriptionService`**

```java
package com.capsule.billing;

import com.capsule.user.*;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                                UserRepository userRepository,
                                UserService userService,
                                @Value("${stripe.secret-key}") String stripeKey) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        Stripe.apiKey = stripeKey;
    }

    public String createCheckoutSession(UUID userId, String priceId, String successUrl, String cancelUrl)
            throws com.stripe.exception.StripeException {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomerEmail(user.getEmail())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(priceId).setQuantity(1L).build())
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .build();
        return Session.create(params).getUrl();
    }

    public String createPortalSession(UUID userId, String returnUrl)
            throws com.stripe.exception.StripeException {
        // Requires stripeCustomerId on User — populated when checkout session completes
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (user.getStripeCustomerId() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No Stripe customer found");
        }
        var params = com.stripe.param.billingportal.SessionCreateParams.builder()
                .setCustomer(user.getStripeCustomerId())   // cus_... not sub_...
                .setReturnUrl(returnUrl)
                .build();
        return com.stripe.model.billingportal.Session.create(params).getUrl();
    }

    public Subscription getSubscription(UUID userId) {
        return subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No subscription found"));
    }

    /** Called by webhook handler — idempotent. */
    public void handleSubscriptionUpdated(String stripeSubId, String status,
                                           String tier, long periodEnd) {
        var existing = subscriptionRepository.findByStripeSubscriptionId(stripeSubId);
        var sub = existing.orElseGet(Subscription::new);
        sub.setStripeSubscriptionId(stripeSubId);
        sub.setStatus(SubscriptionStatus.valueOf(status));
        sub.setTier(UserTier.valueOf(tier));
        sub.setCurrentPeriodEnd(java.time.Instant.ofEpochSecond(periodEnd));
        subscriptionRepository.save(sub);

        // Sync user tier and revoke refresh token on downgrade
        if (sub.getUserId() != null) {
            var user = userRepository.findById(sub.getUserId()).orElse(null);
            if (user != null && user.getTier() != sub.getTier()) {
                user.setTier(sub.getTier());
                userRepository.save(user);
                userService.invalidateRefreshTokenForUser(sub.getUserId());
            }
        }
    }
}
```

- [ ] **Step 3: Implement `StripeWebhookController`**

```java
package com.capsule.billing;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.*;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/billing")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);
    private static final Set<String> HANDLED_EVENTS = Set.of(
            "customer.subscription.created",
            "customer.subscription.updated",
            "customer.subscription.deleted"
    );

    private final SubscriptionService subscriptionService;
    private final String webhookSecret;

    public StripeWebhookController(SubscriptionService subscriptionService,
                                    @Value("${stripe.webhook-secret}") String webhookSecret) {
        this.subscriptionService = subscriptionService;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String payload,
                                         @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe signature");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        if (!HANDLED_EVENTS.contains(event.getType())) {
            return ResponseEntity.ok().build();
        }

        var dataObject = event.getDataObjectDeserializer().getObject();
        if (dataObject.isEmpty()) return ResponseEntity.ok().build();

        var sub = (com.stripe.model.Subscription) dataObject.get();
        // Map Stripe status and metadata to internal tier — tier stored in Stripe price metadata
        var status = mapStatus(sub.getStatus());
        var tier = "seed"; // resolve from price metadata in production
        subscriptionService.handleSubscriptionUpdated(
                sub.getId(), status, tier, sub.getCurrentPeriodEnd());

        return ResponseEntity.ok().build();
    }

    private String mapStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "active" -> "active";
            case "past_due" -> "past_due";
            default -> "cancelled";
        };
    }
}
```

```java
// BillingController.java
package com.capsule.billing;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    private final SubscriptionService subscriptionService;

    public BillingController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<Map<String, String>> checkout(
            @RequestParam String priceId,
            @RequestParam String successUrl,
            @RequestParam String cancelUrl,
            Authentication auth) throws com.stripe.exception.StripeException {
        var url = subscriptionService.createCheckoutSession(userId(auth), priceId, successUrl, cancelUrl);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/subscription")
    public ResponseEntity<?> subscription(Authentication auth) {
        return ResponseEntity.ok(subscriptionService.getSubscription(userId(auth)));
    }

    @PostMapping("/portal")
    public ResponseEntity<Map<String, String>> portal(
            @RequestParam String returnUrl, Authentication auth)
            throws com.stripe.exception.StripeException {
        var url = subscriptionService.createPortalSession(userId(auth), returnUrl);
        return ResponseEntity.ok(Map.of("url", url));
    }

    private UUID userId(Authentication auth) { return (UUID) auth.getPrincipal(); }
}
```

- [ ] **Step 4: Run full build**

```bash
./mvnw clean verify
```
Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add backend/src/
git commit -m "[capsule] billing: Stripe checkout, webhook handler, subscription sync"
```

---

## Task 11: Integration Test Baseline

**Files:**
- Create: `backend/src/test/java/com/capsule/BaseIntegrationTest.java`
- Create: `backend/src/test/java/com/capsule/user/AuthControllerTest.java`

- [ ] **Step 1: Write `BaseIntegrationTest` with Testcontainers**

```java
package com.capsule;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("capsule_test")
            .withUsername("capsule")
            .withPassword("capsule");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");
        registry.add("jwt.secret", () -> "integration-test-secret-32-chars!!");
        registry.add("jwt.access-token-expiry-minutes", () -> "15");
        registry.add("jwt.refresh-token-expiry-days", () -> "30");
        registry.add("aws.s3.bucket", () -> "test-bucket");
        registry.add("aws.s3.endpoint", () -> "http://localhost:9000");
        registry.add("stripe.secret-key", () -> "sk_test_placeholder");
        registry.add("stripe.webhook-secret", () -> "whsec_placeholder");
    }
}
```

- [ ] **Step 2: Write auth integration test**

```java
package com.capsule.user;

import com.capsule.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest extends BaseIntegrationTest {

    @Autowired MockMvc mockMvc;

    @Test
    void registerAndLoginReturnsTokens() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"integration@example.com","password":"password123"}
                    """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("integration@example.com"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"integration@example.com","password":"password123"}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"bad@example.com","password":"password123"}
                    """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"bad@example.com","password":"wrongpassword"}
                    """))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 3: Run integration tests (requires Docker)**

```bash
./mvnw verify -Pintegration
```
Expected: PASS (Redis must be running locally for integration tests, or add Redis Testcontainer)

- [ ] **Step 4: Run full test suite**

```bash
./mvnw clean verify
```
Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add backend/src/test/
git commit -m "[capsule] test: Testcontainers integration baseline, auth flow tests"
```

---

## Task 12: Final Verification

- [ ] **Step 1: Run linter**

```bash
./mvnw checkstyle:check
```
Fix any violations before proceeding.

- [ ] **Step 2: Run full build and test suite**

```bash
./mvnw clean verify
```
Expected: `BUILD SUCCESS` with all tests green.

- [ ] **Step 3: Start local stack and smoke test all modules**

```bash
cd backend
docker compose up -d
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run &
sleep 15

# Register
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"anna@example.com","password":"password123"}' | jq -r .accessToken)

# Create capsule
curl -s -X POST http://localhost:8080/api/v1/capsules \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"My First Capsule","visibility":"public_","openAt":"2030-01-01T00:00:00Z"}' | jq .

# Actuator health
curl -s http://localhost:8080/actuator/health | jq .

# OpenAPI docs
curl -s http://localhost:8080/v3/api-docs | jq '.info'

kill %1
```

- [ ] **Step 4: Final commit**

```bash
git add .
git commit -m "[capsule] backend MVP complete — all modules wired, tests green"
```
