# Python Conventions (agent service)

Applies to `services/agent-service`. See also `coding-standards.md` and `architecture-rules.md`.

## Language

- Target the Python version pinned in `pyproject.toml`.
- Type hints on every function signature. `mypy --strict` must pass.
- Pydantic models for every input and output that crosses a boundary (HTTP, AMQP, LLM output). Never pass raw dicts across layers.
- Follow PEP 8 through Ruff. Names: `snake_case` functions and variables, `PascalCase` classes, `UPPER_SNAKE_CASE` constants.
- Docstrings (Google style) on public functions, classes and modules. Explain purpose and non-obvious behavior.
- `async` all the way for I/O (FastAPI handlers, LLM calls, AMQP). No blocking calls inside async code.

## Structure

- `api/` HTTP endpoints, `agents/` agent logic, `llm/` provider interface and implementations, `messaging/` AMQP consumers and publishers, `config.py` settings.
- Configuration through `pydantic-settings`, read from environment variables.
- The agent service has no database. Do not add one.

## LLM code

- Call LLMs only through the provider interface in `llm/`. Never import a provider SDK elsewhere.
- Prompts live in versioned files or constants with a clear name, not inline strings scattered through the code.
- Validate every LLM response against a Pydantic schema. Handle invalid output explicitly (retry once, then return a clear error); never pass unvalidated output on.
- Whiteboard images stay in memory. Never write them to disk, log them or attach them to traces.
- Never log prompts or responses that contain athlete data; log IDs, token counts, latency and cost.

## Testing and evals

- pytest. Unit tests mock the provider interface, never the network library.
- Evals in `evals/` run against labeled datasets and report accuracy. A change to prompts, models or parsing must not lower the eval score below the agreed threshold (90% exact match for the WOD parser).
- Eval datasets contain no personal data; whiteboard photos have people removed or blurred.
