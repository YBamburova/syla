# ADR-0007: Apache 2.0 license

- Status: Accepted
- Date: 2026-11-04

## Context

The repository is public to show the code and architecture. Syla may become a real product later, so the license must be easy for reviewers and companies to accept while keeping commercial options open.

## Decision

- License the code under Apache License 2.0 (`LICENSE` in the repository root).
- Keep a `NOTICE` file with the copyright line. The "Syla" name is not licensed.
- Outside contributions use the Developer Certificate of Origin: commits are signed off with `git commit -s`, confirming the contributor has the right to submit the code.

## Alternatives Considered

- **MIT.** Simpler, but has no explicit patent grant.
- **AGPL-3.0.** Stronger protection against hosted forks, but many companies avoid it, which weakens the portfolio value.
- **Business Source License.** Strongest commercial protection, but not open source. Can still be considered for future versions.
- **Private code, public docs.** Reviewers could not see the code.

## Consequences

- Anyone can use, modify and host the code, including commercially. A competitor could run a copy. The main value of a future product is the brand, users and data, not the code.
- Apache 2.0 includes a patent grant and allows sublicensing, so later versions can be distributed under different terms while keeping the required notices.
- This is not legal advice. A lawyer should review licensing before any commercial launch.
