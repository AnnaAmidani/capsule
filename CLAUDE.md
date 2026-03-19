@~/.claude/AGENTS.md

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working in this repository.

## Project Status

This project is in early planning. The only file currently present is `context.md`, a strategic brainstorming document. No source code exists yet. All architecture and stack decisions below come from that document.

---

## Intended Architecture

### Overview

Full-stack SaaS web application for creating and delivering time capsules. A capsule groups media (text, images, video/music links) and is delivered to recipients at a scheduled date.

### Backend — Java / Spring Boot

- **Java 21 + Spring Boot 3**
- **Project Loom virtual threads** — preferred over reactive programming for async I/O
- **PostgreSQL** — primary database; JSON columns for flexible capsule metadata
- **Redis** — session caching and rate limiting
- **Quartz Scheduler** — time-based capsule delivery
- **Apache Kafka** — durable event queue (replay-capable audit log; post-MVP trigger delivery)
- **Spring Security** — authentication and authorization
- **Stripe** — payments (yearly discounts must be built in from the start)

### Storage

- **AWS S3** (or MinIO for local dev) — object storage for capsule media
- **S3 Glacier lifecycle policies** — automatically move sealed capsules to cold storage after a threshold; essential for long-term cost model
- **CloudFront CDN** — in front of S3 for media delivery

### Frontend — Next.js

- **Next.js** — SSR for SEO and fast landing pages
- **Three.js / React Three Fiber** — 3D capsule visualization (post-MVP; static SVG acceptable at MVP)
- **Framer Motion** — transitions and animations
- **Tailwind CSS** — styling

### Infrastructure

- **Docker** from day one
- **Kubernetes** when scaling is needed
- AWS is the natural cloud provider given S3/Glacier usage

---

## Key Domain Concepts

### Capsule States

Capsules have two meaningful UI states: **sealed** (before open date, contents inaccessible) and **opened** (after delivery). Architecture — not just policy — must enforce that sealed capsule contents cannot be read before the open date.

### Encryption

- **MVP:** AES-256 server-side encryption for private capsules
- **Post-MVP target:** Zero-knowledge cryptography (server never holds decryption key)

### Sharing Modes

- **Public** — community-visible
- **Private** — designated recipients only (requires paid tier)

### Subscription Tiers

| Tier | Storage | Sharing |
|------|---------|---------|
| Seed (free) | 5 GB | Public only, 1 capsule |
| Vessel | 25 GB | Public + Private, multiple capsules |
| Legacy | 100 GB+ | All features + digital executor |

---

## Hard Constraints

### Media / Copyright

- **Music:** Link-only (Spotify/YouTube embeds). Never store audio files or allow MP3 uploads.
- **Video:** User-generated video is hostable. External video via YouTube embeds. DMCA safe harbor policy required before public launch.
- **E-tickets:** Static screenshot keepsakes only. Do not store or render functional QR codes.
- **Photos:** User-owned content only; ToS must enforce this.

### Content Moderation

Public capsules require a flagging mechanism and human moderation workflow before any public launch — this is a legal requirement, not optional.

### GDPR

Sealed capsules (scheduled for future delivery) must have a separate legal retention basis. Right-to-erasure requests against sealed future capsules require legal counsel before launch.

---

## MVP Scope

**In scope:**
- User auth
- Capsule creation: text, images, Spotify/YouTube links
- Scheduled delivery date + recipient email designation
- Sealed/open UI states
- Email delivery notification
- Free tier + one paid tier

**Explicitly out of scope for MVP:**
- Trigger-based delivery (death/life events)
- Full 3D visualization
- Multi-tier pricing beyond free/paid
- Digital executor system
- Zero-knowledge encryption
- Complex social/community features
