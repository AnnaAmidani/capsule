# Capsule — Backend MVP Design

**Date:** 2026-03-19
**Scope:** Backend service only. Frontend, IaC, and CI/CD pipelines are separate sub-projects to be specced independently.

---

## 1. Repository Structure

Monorepo with the following top-level directories:

```
capsule/
├── backend/       ← this spec
├── frontend/
├── infra/
├── pipelines/
└── docs/
```

---

## 2. Tech Stack

| Concern | Choice |
|---|---|
| Language / Runtime | Java 21 (Project Loom virtual threads) |
| Framework | Spring Boot 3 |
| Build tool | Maven (mvnw wrapper) |
| Database | PostgreSQL — primary store |
| Migrations | Flyway (`backend/src/main/resources/db/migration/`) |
| Cache / Sessions | Redis |
| Messaging | Apache Kafka |
| Scheduling | Quartz Scheduler with JDBC job store (PostgreSQL) |
| Object storage | AWS S3 (MinIO for local dev) |
| Payments | Stripe |
| Email | SendGrid or AWS SES (TBD — decide at IaC spec time) |
| Auth | Spring Security — JWT + OAuth2 (Google, Apple) |
| API | REST, versioned under `/api/v1/` |
| Docs | OpenAPI / Swagger UI (auto-generated) |
| Observability | Spring Actuator, structured JSON logging (Logback + logstash-logback-encoder), correlation ID per request |

---

## 3. Architecture

A **modular monolith** structured as vertical feature slices. Each module maps to a future microservice. A shared layer holds cross-cutting concerns.

```
backend/src/main/java/com/capsule/
├── user/
│   ├── UserController
│   ├── UserService
│   ├── UserRepository
│   └── User (domain)
├── capsule/
│   ├── CapsuleController
│   ├── CapsuleService
│   ├── CapsuleRepository
│   ├── CapsuleItemRepository
│   ├── Capsule (domain)
│   └── CapsuleItem (domain)
├── delivery/
│   ├── DeliveryScheduler       ← Quartz job
│   ├── DeliveryConsumer        ← Kafka listener
│   ├── RecipientRepository
│   └── Recipient (domain)
├── billing/
│   ├── BillingController
│   ├── StripeWebhookController
│   ├── SubscriptionService
│   └── Subscription (domain)
└── shared/
    ├── security/               ← JWT filter, OAuth2 config
    ├── config/                 ← Kafka, S3, Redis config beans
    └── web/                    ← correlation ID filter, error handling
```

Infrastructure: pre-signed S3 URLs — the backend never handles binary upload traffic. The frontend uploads directly to S3; the backend generates the URL and records the confirmed item.

S3 Glacier lifecycle policies and CloudFront CDN are deferred to the IaC spec. Note: CloudFront signed URLs vs. S3 pre-signed URLs is a decision that must be made at IaC time, as it affects how the frontend constructs media URLs.

---

## 4. Domain Model

### Capsule state machine

```
draft → sealed → accessible → archived
```

- **draft** — editable, not yet locked
- **sealed** — locked, awaiting `open_at`; contents inaccessible
- **accessible** — `open_at` has passed, delivery attempted, capsule is viewable by recipients
- **archived** — soft-deleted, hidden from all views pending hard deletion

State is about the **capsule's accessibility**, not delivery outcomes. Delivery success/failure per recipient is tracked on `Recipient` rows independently.

Archival policy: **direction C** — owner-initiated manual archival + automatic archival after a period of inactivity. Grace period, notification sequence, and subscription lapse rules are **deferred — must be defined before launch.**

Double-delivery prevention: Quartz JDBC job store (cluster-safe, no extra state column required).

### Encryption

MVP: AES-256 server-side encryption for private capsule items. `encryption_key_hint` stores a hint to the key derivation (key stored server-side in a secrets manager, not in the DB). What is encrypted: `CapsuleItem.content` (text bodies) and S3 objects for `image` and `keepsake` types. Sealed state enforcement is architectural (service layer blocks reads) + encryption (data is ciphertext at rest). Zero-knowledge cryptography is a post-MVP target.

### Entities

**user.User**
```
id                   UUID PK
email                VARCHAR UNIQUE NOT NULL
password_hash        VARCHAR nullable          -- null if OAuth-only
oauth_provider       VARCHAR nullable
oauth_subject        VARCHAR nullable
tier                 ENUM (seed, vessel, legacy) NOT NULL
stripe_customer_id   VARCHAR nullable
created_at           TIMESTAMPTZ NOT NULL
updated_at           TIMESTAMPTZ NOT NULL
```

**capsule.Capsule**
```
id                   UUID PK
owner_id             UUID FK → User NOT NULL
title                VARCHAR NOT NULL
state                ENUM (draft, sealed, accessible, archived) NOT NULL
visibility           ENUM (public, private) NOT NULL
open_at              TIMESTAMPTZ NOT NULL
encryption_key_hint  VARCHAR nullable
created_at           TIMESTAMPTZ NOT NULL
updated_at           TIMESTAMPTZ NOT NULL
```
Indexes: `(state, open_at)` for Quartz poll query; `(owner_id)`.

**capsule.CapsuleItem**
```
id           UUID PK
capsule_id   UUID FK → Capsule NOT NULL
type         ENUM (text, image, video_link, music_link, keepsake) NOT NULL
content      TEXT nullable          -- text body or external URL; encrypted at rest for private capsules
s3_key       VARCHAR nullable       -- image and keepsake types only
sort_order   INT NOT NULL
created_at   TIMESTAMPTZ NOT NULL
updated_at   TIMESTAMPTZ NOT NULL
```
Note: `video_link` covers YouTube embed URLs only (user-generated video upload is out of scope for MVP). `music_link` covers Spotify/YouTube playlist links only — no audio file storage.

**delivery.Recipient**
```
id                UUID PK
capsule_id        UUID FK → Capsule NOT NULL
email             VARCHAR NOT NULL
user_id           UUID nullable              -- if recipient is registered
access_token      VARCHAR nullable           -- HMAC-signed, expires after 7 days
token_expires_at  TIMESTAMPTZ nullable
notified_at       TIMESTAMPTZ nullable       -- email successfully dispatched
delivery_error    VARCHAR nullable           -- failure reason if email dispatch failed
accessed_at       TIMESTAMPTZ nullable       -- first time recipient viewed the capsule; also used as single-use token guard (see §7)
```
Index: `(capsule_id)`.

**billing.Subscription**
```
id                       UUID PK
user_id                  UUID FK → User NOT NULL
stripe_subscription_id   VARCHAR NOT NULL
tier                     ENUM (seed, vessel, legacy) NOT NULL
billing_cycle            ENUM (monthly, yearly) NOT NULL
status                   ENUM (active, past_due, cancelled) NOT NULL
current_period_end       TIMESTAMPTZ NOT NULL
```

---

## 5. Authentication

Two paths converge to the same JWT pair:

**Email / password**
1. `POST /api/v1/auth/register` — hash password (BCrypt), create user
2. `POST /api/v1/auth/login` — verify hash → issue access token + refresh token
3. `POST /api/v1/auth/refresh` — validate refresh token in Redis → rotate, issue new access token
4. `POST /api/v1/auth/logout` — delete refresh token from Redis (session invalidation)

**OAuth2 (Google / Apple)**
1. Spring Security handles redirect and token exchange
2. Callback URI: `/login/oauth2/code/{provider}` — handled by Spring Security, must be registered in Google/Apple OAuth2 app config
3. Upsert user by `(oauth_provider, oauth_subject)` — create on first login
4. Issue same JWT pair as email path

**JWT payload**
```json
{
  "sub": "<user UUID>",
  "email": "<email>",
  "tier": "seed | vessel | legacy",
  "iat": "<issued at>",
  "exp": "<expires at>",
  "correlation_id": "<request trace ID>"
}
```

- Access token: 15-minute expiry
- Refresh token: opaque, 30-day sliding window, stored in Redis keyed by user ID

**JWT tier staleness:** The `tier` claim reflects the user's tier at token issuance time. Maximum staleness is 15 minutes (access token TTL). On subscription downgrade or cancellation, the Stripe webhook handler must immediately delete the user's refresh token from Redis, forcing re-authentication and issuance of a new JWT with the correct tier. This bounds tier staleness to the remaining lifetime of the current access token (max 15 min), which is the accepted trade-off.

**Access control rules**
- Sealed capsule contents blocked at service layer for any request where `state != accessible`, regardless of ownership
- Private capsules visible only to owner and designated recipients (Vessel+ tier required)
- Stripe webhook route: verified by `Stripe-Signature` header, no JWT
- `DELETE /api/v1/users/me`: deletes account and all `draft` capsules immediately. `sealed` and `accessible` capsules are transitioned to `archived` state pending the deferred GDPR retention policy (see §9).

---

## 6. API Surface

All endpoints under `/api/v1/`. Auth via JWT Bearer token unless noted.

### user module
```
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
POST   /api/v1/auth/logout
GET    /oauth2/authorize/{provider}              -- no JWT, Spring Security redirect
                                                 -- callback: /login/oauth2/code/{provider}
GET    /api/v1/users/me
PATCH  /api/v1/users/me
DELETE /api/v1/users/me                          -- GDPR: see §5 for deletion behaviour
```

### capsule module
```
POST   /api/v1/capsules                              -- create (draft)
GET    /api/v1/capsules?page=&size=&state=           -- list own capsules, sorted by created_at desc; default page size 20
GET    /api/v1/capsules/{id}                         -- get capsule (enforces state; items returned inline)
PATCH  /api/v1/capsules/{id}                         -- update metadata (draft only)
POST   /api/v1/capsules/{id}/seal                    -- draft → sealed
DELETE /api/v1/capsules/{id}                         -- delete (draft only)
POST   /api/v1/capsules/{id}/items/upload-url        -- generate pre-signed S3 URL
POST   /api/v1/capsules/{id}/items                   -- add item (text / link / confirm upload)
DELETE /api/v1/capsules/{id}/items/{itemId}          -- remove item (draft only)
GET    /api/v1/capsules/public?page=&size=           -- community feed (accessible + public), cursor-based pagination, default page size 20
```

`GET /api/v1/capsules/{id}` also accepts `?token={token}` for unauthenticated recipient access (see §7).
Items are returned inline in the capsule response. A separate items endpoint is not required for MVP.

### delivery module
```
POST   /api/v1/capsules/{id}/recipients              -- add recipient email(s) (draft or sealed)
GET    /api/v1/capsules/{id}/recipients              -- list recipients (owner only)
DELETE /api/v1/capsules/{id}/recipients/{recipientId} -- remove recipient (draft only)
```

### billing module
```
POST   /api/v1/billing/checkout       -- create Stripe checkout session
GET    /api/v1/billing/subscription   -- current subscription status
POST   /api/v1/billing/portal         -- Stripe customer portal link
POST   /api/v1/billing/webhook        -- Stripe webhook (no JWT); idempotent — check Stripe event ID before processing
```

---

## 7. Delivery Flow

Quartz polls every minute (JDBC job store prevents double-firing in clustered deployments):

```sql
SELECT id FROM capsules WHERE state = 'sealed' AND open_at <= now()
```

For each result, publish `capsule.due` event to Kafka `{ capsuleId, ownerId, openAt }`.

`DeliveryConsumer` (Kafka listener) processes each event:

1. Transition capsule state `sealed → accessible` **first**, atomically. If this step fails, Kafka retries the event — the capsule remains `sealed` and no emails are sent. This is the idempotency boundary.
2. For each recipient where `notified_at IS NULL`:
   - Generate signed access token: `HMAC-SHA256(capsuleId + email + expiresAt, serverSecret)`, 7-day expiry
   - Persist `access_token` + `token_expires_at` on Recipient row
   - Send email with link: `https://capsule.app/open/{capsuleId}?token={token}`
   - On success: record `notified_at`
   - On failure: record `delivery_error`; retry via Kafka dead-letter topic; capsule remains `accessible`
3. Publish `capsule.opened` event (audit log)

Step 2 is keyed on `notified_at IS NULL` so retries skip already-notified recipients — no duplicate emails.

**Acceptable delivery lag:** Kafka consumer lag should be monitored. No formal SLA is defined for MVP; operational alerting on consumer lag is a deferred observability concern.

**Unauthenticated recipient access** (`GET /api/v1/capsules/{id}?token={token}`):
1. Verify HMAC signature
2. Check `token_expires_at > now()` — return `410 Gone` if expired
3. Atomically update: `UPDATE recipients SET accessed_at = now() WHERE id = ? AND accessed_at IS NULL` — check affected row count; if 0, the token has already been used
4. Return capsule contents — no JWT required

`accessed_at` is both the first-view timestamp and the single-use guard. Recipients can only view via the token link once. After viewing, the frontend prompts sign-up; authenticated users with a matching registered email can access the capsule via standard JWT auth thereafter.

---

## 8. Standard Maven Targets

Per AGENTS.md:

```sh
./mvnw clean verify           # Full build with tests
./mvnw spring-boot:run        # Start dev server
./mvnw test                   # Unit tests only
./mvnw verify -Pintegration   # Integration tests
./mvnw checkstyle:check       # Style enforcement
```

---

## 9. Open / Deferred Decisions

| Decision | Status | Notes |
|---|---|---|
| Archival policy details | Deferred — must resolve before launch | Direction: owner-initiated + automatic. Grace period, notification sequence, subscription lapse rules TBD. |
| Email provider | TBD | SendGrid or AWS SES — decide at IaC spec time |
| S3 Glacier lifecycle + CloudFront | Deferred to IaC spec | CloudFront signed URLs vs S3 pre-signed URLs affects frontend media URL construction — decide before frontend spec |
| Local dev setup (Docker Compose) | Deferred to implementation | Stack: PostgreSQL, Redis, Kafka, MinIO |
| GDPR retention basis for sealed capsules | Requires legal review before launch | Sealed capsules on account deletion → `archived`; full policy TBD |
| Content moderation workflow | Required before public launch | Flagging + human review — legal exposure |
| Kafka consumer lag SLA | Deferred | No formal delivery latency SLA defined for MVP; add operational alerting post-launch |
| accessed_at multi-visit policy | Deferred | Current design: single-use token (first view only). Authenticated users with matching email can re-access via JWT. Revisit if UX feedback requires token re-use. |
