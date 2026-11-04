# ADR-0013: Vite-based React web tooling

- Status: Accepted
- Accepted by the user: 2026-11-04
- Date: 2026-11-04

## Context

React and TypeScript are accepted in ADR-0003. Syla initially serves authenticated, interactive workout forms backed by a Java API. It needs a mobile-friendly UI, explicit error/retry states, generated API contracts, and a small maintainable toolchain. There is no immediate requirement for server-rendered public pages.

## Decision

| Concern | Recommendation | Reason for Syla |
|---|---|---|
| Build | Vite, React, strict TypeScript, npm lockfile | Browser app development and static production assets without another application server |
| Routing | React Router declarative mode | History, editor, detail, and settings navigation; no competing data-loader cache |
| Server state | TanStack Query | Loading/error states, pagination, mutation invalidation, and bounded in-memory cache |
| Forms | React Hook Form | Ordered strength sets, controlled input/error state, and server field-error mapping |
| Components | Material UI community components | Existing inputs, dialogs, navigation, and responsive layout with a small Syla theme |
| API client | OpenAPI Generator `typescript-fetch` | Runtime client/types generated from core OpenAPI; no hand-maintained request DTO copies |
| Verification | Existing Vitest + Testing Library; add Playwright for browser flows | Component behavior plus actual login/save/reload/edit/delete validation |
| Formatting/lint | Existing ESLint/Prettier and TypeScript checks | Enforce the already documented conventions |

Pin exact compatible dependency and generator versions during M0, including Node runtime requirements; this ADR chooses tooling, not untested release numbers. Verify React/Router/MUI/TypeScript and Java OpenAPI generator interoperability in the scaffold. Include only required styling peers and components; no premium MUI dependency or charting system is introduced.

Use component/form state for unsaved input and Query for remote records. No Redux/global application store initially. Form-only view types may describe editing states, but API payloads/types are generated. The backend remains authoritative for domain validation. Do not add another schema library until a specific boundary-validation need justifies it.

### Network and privacy behavior

- Use same-origin API paths and the server session/CSRF contract accepted in ADR-0012; no provider SDK or secret in the web bundle.
- Generated fetch transport must preserve cookie handling, add CSRF and idempotency/revision headers as needed, and expose structured errors. Prove discriminated score schemas, decimal values, missing versus zero, and Problem Details work correctly through generation.
- Explicitly disable automatic create/update/delete retries unless the operation's idempotency/precondition design permits them. Query defaults are not the save semantics. Configure stale time and refetch behavior deliberately so focus changes cannot overwrite unsaved edits.
- Clear cached private records on logout, account change, and deletion. Namespace cache entries by authenticated account context. Do not persist the Query cache, workouts, or tokens to local storage. Do not cache private API responses in a service worker.
- Browser traces/screenshots can include training data: use fictional fixtures, exclude credentials, and define artifact retention/access in CI.

### Design and accessibility

Use a simple responsive theme and community components. A library does not establish accessibility: test labels, focus, dialogs, errors, touch layout, contrast, and keyboard use. MUI is a starting component system, not approval of final screens. Keep meaningful product copy free of backend jargon.

Build static assets for the web container. Vite's dev server is for development, not production serving. The packaged local environment routes static assets and core endpoints through one browser origin; choose its minimal serving configuration during the deployment/scaffold task without adding a second application backend.

## Alternatives considered

| Alternative | Why not selected now |
|---|---|
| Next.js/full-stack React framework | Valuable for server rendering and public content; its server/client and caching architecture adds responsibilities without an immediate need in Syla's private logging flow |
| React Router data/framework mode | Viable; using it for both loading and mutations would need a deliberate choice about overlap with Query. Declarative routing keeps one server-state owner |
| Manual fetch/useEffect everywhere | Fewer dependencies but requires repeated request lifecycle, invalidation, and pagination logic |
| Plain React forms | Viable for a tiny form; dynamic ordered sets and field/error handling justify a form library for this slice |
| Custom CSS and native components only | Fewer UI dependencies but more work building consistent dialogs, controls, and responsive states |

These are project judgments. Reconsider server rendering if public gym/profile discovery later creates a concrete need, rather than introducing it in anticipation.

## Consequences and acceptance evidence

Several focused dependencies replace handwritten routing/state/form/component infrastructure. Keep their responsibilities separate. MUI styling peers add dependencies; Query caching and form abstractions require explicit privacy and mutation behavior. Playwright needs browser binaries in local/CI environments.

Before committing the scaffold as complete: build static assets; pass strict type/lint/component checks; generate and compile a representative score API client; prove same-origin cookie/CSRF handling; run one Playwright smoke scenario. Before M1 completion verify all W1–W3 flows against the real API, auth, and PostgreSQL rather than only mocked requests. The CI topology is a separate pending decision under ADR-0011.

## Open questions

- Exact compatible versions, serving/proxy configuration, CI browser installation, and artifact retention are implementation prerequisites, not selected by this draft.

## Sources checked 2026-11-04

- [Vite guide](https://vite.dev/guide/): build/development roles and templates.
- [React Router modes](https://reactrouter.com/start/modes): declarative versus data/framework choices.
- [TanStack Query overview](https://tanstack.com/query/latest/docs/framework/react/overview): server-state responsibilities.
- [React Hook Form guide](https://react-hook-form.com/get-started): form values, errors, and UI-library integration.
- [Material UI overview](https://mui.com/material-ui/getting-started/): component library and theming.
- [TypeScript Fetch generator](https://openapi-generator.tech/docs/generators/typescript-fetch/): generated-client configuration.
- [Playwright installation](https://playwright.dev/docs/intro): browser testing setup.
- [Next.js server/client components](https://nextjs.org/docs/app/getting-started/server-and-client-components): alternative rendering architecture.
