# Java Conventions (core API)

Applies to `services/core-api`. See also `coding-standards.md` and `architecture-rules.md`.

## Language

- Java 21. Use modern features where they make code clearer:
  - `record` for value objects, DTOs and events.
  - `sealed` interfaces and pattern-matching `switch` for closed hierarchies (for example score types).
  - `var` only when the type is obvious from the right-hand side.
- Prefer immutability: `final` fields, unmodifiable collections, no setters on domain objects.
- No `null` in public APIs. Return `Optional` for "maybe absent" queries; use empty collections instead of `null`.
- Use `BigDecimal` for loads and anything compared exactly; `Duration` for times; `Instant` for timestamps.

## Spring

- Constructor injection only. No field injection, no `@Autowired` on fields.
- Keep controllers thin: validate, map, delegate to the module's service, map the result.
- Transactions (`@Transactional`) belong on application services, not controllers or repositories.
- Cross-module side effects use application events (`@ApplicationModuleListener`), not direct calls.
- Configuration through `@ConfigurationProperties` records, bound from environment variables.
- Use virtual threads for blocking I/O; do not add reactive (WebFlux) code.

## Persistence

- JPA entities stay in the module's `internal` package and are never exposed in public APIs or REST responses. Map to DTOs.
- One Flyway migration per schema change; never edit a migration that has been merged.
- No cross-schema joins, foreign keys or JPA relations (ADR-0002).
- Use JSONB only for format-specific workout details; everything queried or filtered gets a real column.

## REST

- Paths under `/api/v1/`, plural nouns, kebab-case: `/api/v1/gym-classes/{id}/attendances`.
- Errors as RFC 9457 Problem Details (`ProblemDetail`).
- Every endpoint documented through springdoc-openapi annotations where the defaults are not enough.
- Every endpoint checks authorization in the service layer, not only in the controller.

## Testing

- JUnit 5 + AssertJ. Testcontainers for PostgreSQL and RabbitMQ.
- Unit tests for domain logic (PR detection, ranking, e1RM) without Spring.
- `@ApplicationModuleTest` for module-level tests; the `ApplicationModules.verify()` test must stay green.
- Test class names end with `Test` (unit) or `IT` (integration).
