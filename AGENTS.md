# Working on Syla

Syla is a social training platform for CrossFit and functional athletes. It is currently documentation-only. The primary goal is an architecture and AI engineering portfolio with a path to a real product.

## Read before working

- Read `docs/vision.md` for product scope and `docs/adr/README.md` for accepted decisions relevant to the task.
- Always follow `docs/development/architecture-rules.md`, `docs/development/coding-standards.md`, and `docs/development/git-workflow.md`.
- Read `docs/development/structure.md` before adding files and `docs/development/tech.md` before changing tooling or dependencies.
- For Java, Python, or TypeScript changes, read the corresponding `docs/development/java.md`, `python.md`, or `typescript.md`.
- For documentation changes, read `docs/development/docs.md`. Domain terminology is in `docs/development/product.md`.

These supporting files are repository instructions. Their paths are relative to this repository root; read them explicitly rather than assuming tool-specific steering discovery.

## Work loop

1. Inspect the existing files and clarify the requested outcome. Continue with reasonable routine implementation choices.
2. For a new feature, create or update `docs/specs/<feature>/requirements.md`, `design.md`, and `tasks.md` in proportion to its size. Include observable acceptance criteria, authorization, failure cases, and verification.
3. Resolve significant architecture changes through a Proposed ADR. Only the user accepts an ADR; preserve Accepted decision history.
4. Implement a small coherent change, run relevant checks, inspect the diff, and update affected documentation.
5. Report the outcome, verification results, and remaining limitations. Do not claim planned services or commands work before they exist.

Keep routine fixes lightweight. Do not add frameworks, infrastructure, custom skills, or multiple agents without a task-based reason. Use subagents only when the user requests delegation or parallel work.

## Essential boundaries

- Only the Java core API writes domain data. Python returns validated proposals; the athlete confirms before saving.
- Preserve module boundaries and schema ownership. Manual logging must work when AI or wearables fail.
- Process whiteboard photos in memory and discard them; never persist or log their contents.
- Build consent, authorization, export, and deletion into relevant features. Do not log secrets or health values.
- Never commit unless asked, never push to main, and never run destructive Git operations without explicit authorization.

## Verification status

No application build or Docker Compose configuration exists yet. Repository CI scripts now exist; use the commands in `docs/development/ci.md` for documentation/checker changes. The GitHub workflow passed on PR #1; see `docs/development/ci.md` for evidence and remaining rollout steps. Application commands in `docs/development/tech.md` are planned until the associated services are created.
