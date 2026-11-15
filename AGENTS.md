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

The Java API and React frontend foundation exist. Use the commands in docs/development.md. Private running/swimming/cycling/general workout logging, PostgreSQL persistence, and Keycloak login exist. Automatic profiles, search, following/approval, blocking, and limited follower-only activity sharing exist. Feeds, reactions/comments, CrossFit formats, AI and wearables remain planned; do not claim they work. Update real CI checks alongside new application behavior. Generated TypeScript files come from the core OpenAPI contract; never edit them by hand.
