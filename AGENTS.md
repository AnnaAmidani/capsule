# AGENTS.md — capsule

Produce clean, reusable, well-architected code. Everything is version-controlled.

---

## Rules

- Run all commands from the project root.
- Never use `sudo` for package manager installs.
- No em dashes in generated content or documentation.

---

## Git Workflow

- GitHub org: https://github.com/AnnaAmidani
- Run `gh auth status` before any GitHub CLI commands. If unavailable, provide the manual URL.
- Linters and tests must pass (green) before committing.
- Use git hooks.
- PRs require at least 1 reviewer. All comments must be resolved before merge.

## Branching Strategy

Trunk-based, team of 2.

| Situation | Action |
|---|---|
| Small, self-contained change (config, docs, single-file fix) | Commit directly to `main` |
| Multi-commit, touches production files, or carries meaningful risk | Short-lived branch → PR → squash merge → delete branch |

When in doubt, use a branch.

## PR Format

- **Title:** `[capsule] <Title>`
- **Body:** meaningful description + testing criteria used
- Attach an image for design changes, diagrams, or schema updates.

---

## Backend — Java 21 / Spring Boot 3

### Principles

1. **Contracts first** — Define and version APIs upfront; validate at the boundary; never break consumers silently.
2. **Pure core, impure shell** — Business logic is deterministic and side-effect-free. I/O, DB calls, and integrations live at the edges.
3. **Observability by default** — Structured logs, metrics, and distributed traces from day one. Every request carries a correlation ID.
4. **Predictable failure** — Timeouts, retries, and circuit breakers are explicit. Errors are typed and carry enough context to act on.
5. **Clear data ownership** — No shared databases across services. Data is exposed only through the owning service's API.

### Standards

- Layered structure: `controller` → `service` → `repository` → `domain`
- Every service must have:
  - Spring Actuator (`/actuator/health`, `/actuator/metrics`)
  - Structured JSON logging (Logback + `logstash-logback-encoder`)
  - `application.yml` for config (not `.properties`)
  - Multi-stage `Dockerfile`
- Use **virtual threads** (Project Loom) over reactive programming for async I/O.

### Maven commands

```sh
./mvnw clean verify           # Full build with tests
./mvnw spring-boot:run        # Start dev server
./mvnw test                   # Unit tests only
./mvnw verify -Pintegration   # Integration tests
./mvnw checkstyle:check       # Style enforcement
```

---

## Frontend — Next.js

### Principles

1. **State hygiene** — Keep state minimal; never duplicate derivable data. Lift state only as far as necessary.
2. **Component discipline** — Small, single-purpose components with explicit, typed props. No god components.
3. **Accessibility first** — Semantic HTML throughout. Every interactive element must be keyboard-navigable and screen-reader-friendly.
4. **Performance by design** — No unnecessary re-renders, over-fetching, or bundle bloat. Retrofitting is expensive.
5. **Explicit async states** — Every async flow (fetch, form submit) must handle loading, empty, error, and success explicitly.

### Standards

- Linters, unit tests, and E2E tests are required.
- Standard scripts in `package.json`:

```sh
npm run dev        # Start dev server (localhost:3000)
npm run build      # Production build
npm run lint       # ESLint
npm run test       # Unit tests (Jest / Vitest)
npm run test:e2e   # Playwright E2E
```

---

## Role-Specific Criteria

These are project-specific constraints that apply when acting in a given role. Generic role behavior is handled by the skills system; this section captures only what is unique to capsule.

### Code Reviewer

- **Sealed capsule enforcement** — content must be unreadable before the open date at the architecture level (access control, encryption), not just by policy or UI gating. Flag any path that could bypass this.
- **Stripe idempotency** — webhook handlers must be idempotent and replay-safe. Check for duplicate event handling.
- **Media constraints** — music is link-only (Spotify/YouTube). Reject any code that stores, serves, or accepts audio file uploads.
- **Encryption** — private capsule content must be AES-256 encrypted at rest. Flag unencrypted storage of private content.
- **Content moderation hook** — public capsules must have a flagging path. Flag any public capsule flow that has no route to moderation.
- **Kafka as audit log** — delivery events go through Kafka. Flag direct delivery logic that bypasses the event queue.

### Designer

- **Animation** — Framer Motion for transitions. Three.js / React Three Fiber for 3D (post-MVP only; static SVG is acceptable at MVP).
- **Styling** — Tailwind CSS only. No additional CSS frameworks.
- **Two primary UI states** — every capsule view is either sealed or opened. Design must make this distinction visually unambiguous.
- **Async states** — loading, empty, error, and success must each have a designed state. No bare spinners or silent failures.

### Developer

- **Concurrency model** — use virtual threads (Project Loom). Do not introduce reactive chains (`Mono`, `Flux`, `CompletableFuture` pipelines) unless there is a specific reason.
- **Kafka** — delivery events are the durable audit log. Do not route capsule delivery outside Kafka.
- **S3 Glacier** — sealed capsules migrate to cold storage. Retrieval latency is real; account for it in any feature that reads sealed content before the open date.
- **Encryption scope** — MVP uses server-side AES-256. Do not design APIs that assume zero-knowledge (post-MVP) without flagging the dependency.
