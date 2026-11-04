# TypeScript Conventions (web app, MCP server)

Applies to `web/` and, later, `services/mcp-server/`. See also `coding-standards.md` and `architecture-rules.md`.

## Language

- `strict` mode on. No `any`; use `unknown` and narrow it. No non-null assertions (`!`) without a comment explaining why it is safe.
- Prefer `type` aliases and discriminated unions for domain data (for example score types).
- Named exports only; no default exports.
- Names: `camelCase` variables and functions, `PascalCase` types and components, `UPPER_SNAKE_CASE` constants. File names: `kebab-case.ts`, components `PascalCase.tsx`.
- JSDoc on exported functions, hooks and components whose purpose is not obvious from the name.

## API access

- The API client and types are generated from the core API's OpenAPI spec. Never hand-write API types or edit generated files.
- Validate data from outside the app (URL params, local storage) before using it.

## React (web)

- Function components and hooks only.
- Keep components small; move logic into custom hooks (`useGymClass`) and pure functions.
- Server data through the data-fetching library (TBD in an ADR); local UI state with React state. No global store unless an ADR adds one.
- Mobile-first, responsive layouts. Athletes log at the gym on their phone.
- Accessibility is required: semantic elements, labels for every input, keyboard support, visible focus, `alt` text.

## Whiteboard photos

- Before upload: resize the long edge to about 1600 px, compress to JPEG, strip EXIF metadata (including GPS). See ADR-0009.
- Do not keep the photo in local storage or caches after the parse request finishes.

## Testing

- Vitest + Testing Library. Test what the user sees and does (roles, labels), not component internals.
- Run tests once with `--run`; never start watch mode from the agent.
