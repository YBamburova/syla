# Git Workflow

Full decision: [ADR-0011](../adr/0011-git-workflow-and-versioning.md).

## Agent rules

- **Never commit unless the user asks.** Never push to `main`. Never force push, rebase shared branches, or run destructive commands (`reset --hard`, `clean -f`, `branch -D`) without explicit permission.
- Stage specific files, not `git add .`. Check that no secrets or `.env` files are staged.
- Keep git hooks active; never use `--no-verify`.
- Every commit is signed off: `git commit -s` (Developer Certificate of Origin, ADR-0007).

## Branching: trunk-based with short-lived branches

- `main` is always releasable and protected. Changes reach it only through pull requests with passing CI.
- One branch per task or spec task. Keep branches short-lived (ideally under 2 days of work).
- The documented baseline is `<type>/<short-description>`. Codex uses its configured `codex/` prefix, for example `codex/feat-gym-class-attendance`; do not rename an existing branch just for naming consistency.
  - `codex/feat-gym-class-attendance`
  - `codex/fix-42-amrap-score-rounding`
  - `codex/docs-adr-0012-authentication`
- Merge with **squash merge**. The PR title becomes the commit message on `main`, so it follows the commit style below.
- Unfinished features that must merge early are hidden behind a feature flag, not kept on long-lived branches.

## Commit messages: Conventional Commits

```
<type>(<scope>): <summary>

<body: what and why, wrapped at 72 characters>

<footer: BREAKING CHANGE: ..., Refs: #42>
Signed-off-by: Name <email>
```

- **Types:** `feat`, `fix`, `docs`, `refactor`, `test`, `perf`, `build`, `ci`, `chore`.
- **Scopes:** a service (`core-api`, `agent-service`, `web`, `mcp-server`), a core module (`workouts`, `progress`, `social`, `gyms`, `wearables`, `identity`, `privacy`), or `docs`, `infra`, `steering`.
- **Summary:** imperative mood, lowercase, no period, max 72 characters. "add" not "added".
- **Breaking changes:** `!` after the scope and a `BREAKING CHANGE:` footer.
- One logical change per commit.

Examples:

```
feat(gyms): add hidden scores to class results
fix(agent-service): handle time-capped AMRAP on whiteboard boards
docs(adr): add ADR-0012 for authentication
feat(core-api)!: rename workout score endpoint
```

## Pull requests

- Title in Conventional Commits format, under 70 characters.
- Description: summary of changes, link to the spec or issue, how it was tested, any ADR added or affected, new dependencies and why.
- Architecture-relevant changes link the ADR they implement or add.

## Versioning: Semantic Versioning

- One version for the whole platform, tagged on `main` as `vMAJOR.MINOR.PATCH` (for example `v0.3.0`).
- `0.x` until the MVP is complete; `v1.0.0` is the first MVP release.
- Version bumps follow the commits since the last tag: `fix` → patch, `feat` → minor, breaking change → major (minor while in `0.x`).
- Each release has notes generated from the commit history, grouped by type, in `CHANGELOG.md`.
- REST APIs are versioned in the path (`/api/v1/...`); event schemas carry a version field. Breaking either requires a new version, not an in-place change.
