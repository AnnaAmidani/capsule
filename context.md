# Time Capsule Platform — Brainstorming Context

## Concept Overview

A web portal where users create one or more "time capsules" — curated, visually uniform digital spaces that group together memories, texts, sounds, and visuals meaningful to an individual. Capsules can be shared publicly with the community or privately with specific users, either immediately or at a scheduled date and time. The platform serves as a living testament — something handed to someone else to convey who you are, or who you've been.

**Tagline direction:** A pre-defined, personal space for memories — sealed and sent when the time is right.

---

## Core Features

- **Capsule creation** with multiple media types: text documents, images, video links, music playlist links, event keepsakes (e-ticket images)
- **Delivery options:** immediate, scheduled (date/time), or trigger-based (post-MVP)
- **Sharing modes:** public (community-visible) or private (designated recipients only)
- **Strong visual identity:** the capsule as a recognizable brand object — sealed vs. open states, spatial/temporal aesthetic
- **Subscription model:** monthly or yearly, 3 tiers based on storage and access level

---

## Use Cases / Buyer Personas

1. **Legacy** — parents to children, individuals facing illness, digital estate planning
2. **Celebration** — future-dated love letters, birthday drops, graduation messages
3. **Community memory** — groups capturing a shared moment in time (weddings, trips, milestones)

---

## Pricing Tiers (Draft)

| Tier | Storage | Features | Sharing |
|------|---------|----------|---------|
| Seed | 5 GB | Text + images, 1 capsule | Public only |
| Vessel | 25 GB | + Video links, playlists, multiple capsules | Public + Private |
| Legacy | 100 GB+ | All features, trusted-contact executor, priority support | Public + Private |

- Monthly and yearly billing (yearly ~20% discount, push hard in onboarding)
- Pricing varies by: storage tier, public vs. private access level

---

## Identified Strengths

- Emotionally resonant concept with timeless appeal
- Multiple distinct use cases and buyer personas
- Recurring subscription revenue
- Community and social angle (public capsules)
- The "trigger" mechanic (event-based delivery) is a genuine differentiator

---

## Identified Weaknesses

### Long-term survivability promise
If someone creates a capsule to open in 15 years, there's an implicit commitment to exist for 15 years. This is the hardest trust problem. Mitigation: export mechanism, legal escrow arrangement, or a partner institution (library, notary service).

### Trigger activation complexity
Who verifies a life event (e.g., death)? How is fraud prevented? This is technically and ethically complex. **Decision for MVP: park triggers entirely, use time-based delivery only.** Post-MVP: trusted third-party model with identity verification (digital executor system).

### Account lapse policy
What happens to sealed capsules if a subscription lapses? Needs a clear, humane grace period policy — visible upfront during onboarding. Users placing legacy content will be anxious about this.

### Content moderation
Public capsules will eventually contain problematic content. A flagging mechanism and human moderation process are required from day one — not optional, this is legal exposure.

### The morbidity barrier
Most users arrive via a triggering life event (diagnosis, new baby, milestone birthday), not casual browsing. Organic social growth will be slow. Acquisition strategy must meet people at those emotional moments.

---

## Copyright Constraints

### Music
- **Cannot store audio files directly.** Spotify, Apple Music, YouTube licensing prohibit third-party storage.
- Playlist feature must be **link-based** — render via Spotify embeddable widgets.
- Do not allow MP3 uploads under any circumstances.

### Video
- User-generated video (personal messages to camera) = hostable as original content.
- Commercial clips, TV recordings, copyrighted performances = DMCA safe harbor policy + takedown process required.
- YouTube embeds are the recommended approach for external video.

### E-tickets
- Frame as **"event keepsakes"** (the visual memory of attending), not functional ticket transfer.
- Static screenshots are fine; functional QR codes are not appropriate to host.
- Modern ticketing uses dynamic tokens anyway — static images lose validity.

### Photos
- User-owned photos: fine under user-generated content model.
- Professional/copyrighted photography: ToS must clearly require user-owned content only.

---

## Security Implications

### Encryption
- Private capsules require genuine encryption — ideally **even the platform cannot read sealed private capsule contents**.
- MVP approach: AES-256 encryption with key derived from user-held secret or time-lock mechanism.
- Gold standard (post-MVP): zero-knowledge cryptography where the server never holds the decryption key.
- This is both a technical feature and a major trust differentiator.

### Pre-open access
- Architecture — not just policy — should enforce that sealed capsules cannot be read before their open date.
- Users placing legacy content will be sensitive to this.

### GDPR / Right to Erasure
- If an EU user requests data deletion but their capsule is sealed and scheduled for 2040 — what happens?
- Sealed capsules may need to be treated as a separate data class with a distinct retention legal basis.
- **Requires legal counsel before launch.**

---

## Tech Stack

### Backend (Java)
- **Java 21** with **Spring Boot 3**
- **Project Loom (virtual threads)** — handles async, I/O-heavy media uploads without reactive programming complexity
- **Quartz Scheduler** — time-based capsule delivery, integrates cleanly with Spring
- **Apache Kafka** — durable event queue for triggered delivery; provides replay-capable audit log
- **PostgreSQL** — primary database; JSON columns for flexible capsule metadata, relational structure for users/subscriptions/permissions
- **Redis** — session caching, rate limiting
- **Spring Security** — authentication and authorization
- **Stripe** — payments (only option at MVP stage; build yearly discounts in from the start)

### Storage
- **AWS S3** (or MinIO for self-hosted) for object storage
- **S3 Glacier lifecycle policies** — automatically move older sealed capsules to cold storage (dramatically reduces long-term cost; essential for the economics of 20-year capsules)
- **CloudFront CDN** in front of S3 for media delivery

### Frontend
- **Next.js** — SSR for SEO and fast landing pages
- **Three.js / React Three Fiber** — 3D interactive capsule object (seal animation, drift into starfield)
- **Framer Motion** — transition animations
- **Tailwind CSS** — utility-first styling

### Infrastructure
- **Docker** from day one
- **Kubernetes** when scaling is needed
- Cloud provider: AWS / GCP / Azure (AWS most natural given S3/Glacier usage)

---

## MVP Scope

**Goal to validate:** Can a person create an emotionally satisfying capsule, seal it, and have it delivered to someone at a future date in a way that feels meaningful?

### In scope
- User authentication
- Create a capsule with text, images, Spotify/YouTube links
- Set a delivery date
- Designate recipients by email
- Sealed / open capsule UI states
- Email delivery notification
- One free tier (capsule limit) + one paid tier
- Simple but visually distinctive capsule design (beautiful static SVG acceptable at MVP)

### Explicitly out of scope for MVP
- Trigger-based delivery (death, life events)
- Complex social/community features
- Full 3D interactive capsule visualization
- Multi-tier pricing beyond basic free/paid
- Trusted-contact / digital executor system
- Zero-knowledge encryption (use AES-256 server-side for MVP)

### Estimated timeline
3–4 months for a small team.

### What you're testing
- Do people complete capsule creation?
- Does the "sealing" moment feel meaningful enough to convert to paid?
- If yes → build the rest. If no conversion → monetization problem. If low completion → onboarding/UX problem.

---

## Brand & Visual Identity

The visual identity of the capsule is the product's most important strategic asset. The capsule form — whatever it takes — must do emotional work instantly.

**References for emotional direction:**
- Spotify Wrapped (year-end nostalgia)
- Polaroid (impermanence, physical-feeling digital objects)
- Space/time aesthetic: sealed vessel drifting into a starfield

**Design principles:**
- The "sealed" vs. "opened" capsule states create a natural, meaningful UI moment
- Deep space palette: blues, purples, gold accents
- Consistent capsule object across all touchpoints (icon, loading state, email header, share card)
- Invest in a designer before engineering — the visual metaphor will guide every product decision downstream

---

## Open Questions / Next Steps

1. Legal review: GDPR retention basis for sealed capsules, right-to-erasure edge cases
2. Choose cloud provider and finalize S3 Glacier lifecycle policy design
3. Define exact Stripe pricing (monthly vs. yearly, tier amounts)
4. Establish content moderation workflow before any public launch
5. Design the sealed/open capsule visual before writing a line of code
6. Determine grace period policy for lapsed subscriptions
7. Map user acquisition channels for the three primary personas (legacy, celebration, community)
