# ADR-0011: Git workflow and versioning

- Status: Accepted
- Date: 2026-11-04

## Context

Syla is a monorepo built by one developer working with AI coding agents. The history must be readable for reviewers, safe to work on with agents, and able to produce release notes automatically. The project may later have outside contributors (ADR-0007).

## Decision

1. **Trunk-based development with short-lived branches.** `main` is protected and always releasable. Every change goes through a pull request with passing CI. Branches are named `<type>/<short-description>`.
2. **Squash merge** into `main`. One PR becomes one commit on `main`.
3. **Conventional Commits** for commit messages and PR titles, with service or module scopes (for example `feat(gyms): ...`).
4. **Developer Certificate of Origin.** Every commit is signed off (`git commit -s`).
5. **Semantic Versioning for the whole platform.** One version, tagged `vMAJOR.MINOR.PATCH` on `main`. `0.x` during the MVP; `v1.0.0` is the first MVP release. Release notes are generated from commits into `CHANGELOG.md`.
6. **API versions are separate from the release version.** REST paths carry a major version (`/api/v1/`); event schemas carry a version field.
7. **AI agents** may commit only when asked, never push to `main`, and never run destructive git commands without permission.
8. **Hosting on GitHub.** The repository is the public portfolio, and GitHub is where reviewers look. CI runs on GitHub Actions; `main` is protected with required status checks and PR review settings suitable for a solo maintainer.

Details for daily use are in `.kiro/steering/git-workflow.md`.

## Alternatives Considered

- **GitFlow (develop, release and hotfix branches).** Rejected: built for scheduled releases with several teams; too much overhead for continuous delivery by one developer.
- **Independent versions per service.** Rejected for now: the services are released together, and one version is easier to understand. Revisit if services get separate release cycles.
- **Merge commits or rebase merge.** Rejected: squash merge gives a clean history where every commit on `main` is one reviewed change.

## Consequences

- A linear, readable history that maps one-to-one to pull requests.
- Release notes and version bumps can be automated (for example with release-please) once CI exists.
- Squash merge loses the individual commits of a branch. Accepted, because branches are short.
- Unfinished work merged early needs feature flags.
- GitHub Actions becomes the CI platform; the CI design gets its own ADR when the first pipeline is built.
