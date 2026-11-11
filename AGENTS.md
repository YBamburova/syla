# Working on Syla

Syla is a social training network for CrossFit athletes and gym people. Read [README](README.md) first. Read the relevant existing docs: [product](docs/product.md), [architecture](docs/architecture.md), [decisions](docs/decisions.md), [development](docs/development.md), and [workouts](docs/workouts.md) for domain changes.

- Implement small coherent changes and run relevant checks. Report actual results and limitations.
- Do not create a document for each task. Scope, acceptance criteria, failure cases, and verification belong in the issue/PR or task conversation. Update existing docs only for lasting changes. No mandatory requirements/design/tasks files or milestone reports.
- Significant architecture changes use a Proposed row in the single decision table. Only the user accepts decisions; retain historical IDs and supersession links.
- Only the Java core writes domain data. Python returns validated proposals that the athlete confirms. Preserve module/schema ownership and API authorization.
- Manual logging must work when AI/wearables fail. Whiteboard photos stay in memory and are discarded; never persist/log their contents.
- Include relevant consent, authorization, export, and deletion. Never log secrets or health values.
- Never commit unless asked, never push to main, and never perform destructive Git operations without explicit authorization. Leave unrelated local changes untouched.
- Do not add speculative frameworks, infrastructure, custom skills, or agents. Use subagents only when the user asks for delegation/parallel work.

No application source/build is tracked yet. Do not claim planned commands, tests, or services work. CI is a minimal GitHub workflow; wire actual tests in when adding code.
