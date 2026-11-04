# Development instructions

Codex starts from the root [AGENTS.md](../../AGENTS.md), which explicitly routes it to these supporting instructions. This replaces Kiro steering metadata; these files are ordinary Markdown, not automatically discovered steering files.

- [Architecture boundaries](architecture-rules.md)
- [Coding standards](coding-standards.md)
- [Git workflow](git-workflow.md)
- [Repository structure](structure.md)
- [Technology and planned commands](tech.md)
- [Java](java.md), [Python](python.md), [TypeScript](typescript.md)
- [Documentation](docs.md)
- [Product summary and glossary](product.md)

The vision owns product scope; ADRs own architecture decisions. These instructions describe how to apply them. Accepted ADRs remain unchanged during this migration. Git workflow instructions document the Codex branch-prefix adaptation to ADR-0011.

Feature specs live in `docs/specs/<feature>/` and contain requirements, design, and tasks. Create them when work begins; there are no feature specs yet. Add reusable skills only after a recurring workflow justifies one.
