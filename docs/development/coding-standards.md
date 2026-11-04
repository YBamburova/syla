# Coding Standards

These apply to every language. Language-specific rules are in `java.md`, `python.md` and `typescript.md`.

## Readability first

Write code for the next person who reads it. Clear beats clever.

- **Names explain intent.** `estimatedOneRepMax`, not `e1` or `calc`. Use the domain glossary terms from `product.md` (WOD, AMRAP, gym class, attendance).
- **Small units.** A function does one thing. If it needs "and" to describe it, split it. Aim for functions that fit on one screen.
- **Flat control flow.** Use guard clauses and early returns instead of deep nesting.
- **No magic values.** Name constants: `MAX_PHOTO_EDGE_PX = 1600`, not `1600`.
- **Make illegal states unrepresentable.** Prefer precise types (enums, value objects, sealed types) over strings and loose maps.
- **Follow the existing style** of the file and module. Consistency beats personal preference.

## Comments

Code says *what* and *how*. Comments say *why*.

- Comment **why** a decision was made, a non-obvious business rule, a workaround, or a link to an ADR or spec.
- Do not restate the code. `// increment i` is noise.
- Public APIs (module APIs, REST endpoints, event types) get doc comments: purpose, parameters, errors.
- No commented-out code. Git keeps history.
- TODOs must reference an issue: `TODO(#42): handle time slots`.

Good:

```java
// Keep the athlete's saved workout unchanged when its class template is edited.
// See ADR-0010 for class correction rules.
```

Bad:

```java
// check if capped
if (score.capped()) { ... }
```

## Correctness and safety

- Validate input at the system boundary (REST controllers, message consumers). Trust validated data inside.
- Handle errors explicitly. Never swallow exceptions; either handle them or let them propagate with context.
- Use parameterized queries only. Never build SQL from strings.
- No secrets, tokens or keys in code, tests, logs or commits.
- No personal or health data in logs.
- Store event timestamps in UTC using time-zone-aware types. Keep workout/class calendar dates and their relevant time zone explicitly; do not shift a gym day through UTC conversion.

## Tests

- Add meaningful tests for new behavior and regression tests for bug fixes. Documentation-only changes need link and consistency checks, not application tests.
- Test behavior, not implementation details.
- Test names describe the behavior: `hiddenScoreIsExcludedFromClassRanking`.
- Integration tests use real dependencies through Testcontainers, not in-memory substitutes.
- Run the service's build and tests before declaring a task done. If they fail, fix them.

## Scope

- Do what the task or spec asks. No speculative features, abstractions or configuration.
- Do not reformat or refactor unrelated code in the same change.
- Remove dead code you create; do not leave unused functions, imports or flags.

## Accessibility (web)

- Semantic HTML, labels on every input, keyboard navigation, visible focus, sufficient contrast.
- Images and icons that carry meaning have text alternatives.
