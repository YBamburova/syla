# ADR-0012: OIDC login with Keycloak and server-side sessions

- Status: Accepted
- Accepted by the user: 2026-11-04
- Date: 2026-11-04

## Context

Syla needs private personal logging with reproducible local authentication, reliable ownership, and account deletion. The Java core is already the application backend; ADR-0003 rejects a separate backend-for-frontend until needed. AWS is a deployment target, not a prerequisite to a local demo.

The user accepted this setup; it is not implemented configuration. Exact supported versions will be pinned and compatibility-tested during M0.

## Decision

Use Keycloak as the local OIDC identity provider and Spring Security OAuth2 Login in the existing core API. The core completes Authorization Code login with PKCE, validates the provider response, and creates a server-side session. React receives an opaque session cookie; access/refresh/ID tokens and client secrets stay server-side. Do not implement password storage or an authorization server in Syla.

Keycloak supplies hosted login, logout, and credential management. Syla owns athlete profiles, consent, permissions, and account state. Map `(issuer, subject)` to an internal athlete ID with a unique constraint. Never identify an owner by mutable email or auto-link accounts on matching email addresses.

### Local runtime and persistence

- Add Keycloak to Compose with an isolated database/user in the existing PostgreSQL engine. Its credentials and tables are provider-owned; core services cannot read or write them. This extends the platform with identity infrastructure; it does not allow another service to write Syla domain tables.
- Import a nonsecret development realm/client definition. Provision clearly labeled local test users through a development-only mechanism; no reusable production passwords or client secrets in Git.
- Configure an issuer hostname resolvable consistently from browser and containers. Verify discovery, callback, issuer validation, and logout from both networks. Do not rewrite issuer claims or disable verification to work around Docker hostnames.
- Start with in-memory application sessions for one local core instance. Core restart logs users out; workout data persists. Shared/durable sessions and multi-instance deployment require a deployment decision before AWS rollout.
- A development Keycloak startup mode is only for the local demo. Production requires hardened configuration, TLS, backup/restore, patching, and a deployment-specific cost/operations review.

### Browser security and session lifecycle

- Serve web and core paths through one browser origin: development proxy locally; reverse proxy/static hosting arrangement for packaged Compose. This is HTTP routing, not a new application service. Restrict trusted forwarded headers and callback URLs.
- Use an HttpOnly, host-only session cookie; Secure in HTTPS environments; explicit SameSite policy compatible with the OIDC callback. Limit plain HTTP to local development. Rotate the session ID on login and require state/nonce validation plus PKCE. Validate redirect destinations.
- Keep Spring CSRF protection for mutations and logout. Expose a narrowly scoped CSRF bootstrap mechanism; React sends the token in the required header. Refresh it after login/logout. A readable CSRF token is not an authentication credential. Do not disable CSRF because the UI uses JSON.
- Session defaults: 30-minute idle timeout and 12-hour absolute lifetime, configurable and verified server-side. These are configurable defaults for the initial setup. In M1, reauthenticate after expiry; no silent background refresh keeping the app session alive indefinitely.
- Expired API sessions return JSON 401 instead of redirecting fetch requests to HTML login. Interactive login is an explicit browser navigation. Return 403 for failed CSRF checks and follow the owner-scoped resource policy for object access.
- Logout invalidates the application session, clears sensitive in-memory UI/query state, and uses the supported OIDC provider logout flow. Test both application logout and immediate re-entry. Password reset and provider-session revocation must not be assumed to invalidate existing app sessions automatically; configure/test provider logout integration or bound the limitation explicitly before real-user launch.
- Mark private responses no-store; keep tokens and workout drafts out of persistent browser storage. Cookie protection does not eliminate XSS: render notes as text and apply the application's content-security policy.

### Account lifecycle

Only an active internal account can use a valid session to mutate data. Consent is checked separately; successful provider login is not health-data consent.

Account deletion first disables the internal account's access and invalidates app sessions, then orchestrates retryable module cleanup and provider-account cleanup through a server-side administrative adapter with minimal permissions. Keep a documented deletion barrier so concurrent requests cannot restore data. A still-valid provider session must not silently reprovision the deleted account. Define minimal subject-blocking records, retention, and deliberate re-registration behavior in the privacy design before implementing deletion. Identity-provider cleanup failure is visible/retryable; do not report complete deletion prematurely.

Provider management operations are an explicit Keycloak-specific adapter, unlike ordinary OIDC login. Oura authorization is a separate connection with separate credentials/consent; it does not become Syla's login identity.

## Alternatives considered

| Option | Fit and tradeoff |
|---|---|
| Keycloak + Spring sessions — recommended | Local container-based login and provider-managed credentials; adds server resource use, administration, upgrades, and operational responsibility |
| Cognito managed login + Spring sessions | Managed identity and AWS alignment; requires an AWS user pool/domain/configuration and network for real login. Reconsider if reducing production identity operations outweighs local independence |
| Spring Authorization Server | A framework for building an identity/authorization product; leaves more account management and security workflow implementation to this project |
| Browser-held tokens with a public OIDC client | Useful for independently deployed/native clients; adds browser token handling and refresh concerns that the current same-origin web client does not need |
| Custom username/password in the core | Avoids an identity container but makes Syla responsible for credentials, reset, verification, and security lifecycle; not justified for this slice |

These are project-specific judgments, not claims that one option is universally safer. A later provider change needs more than a configuration flip: subjects, accounts, management adapters, and user migration must be planned.

## Consequences and acceptance evidence

The browser has a simple cookie-based relationship with the core and backend ownership enforcement. Local login needs no paid cloud account, but Keycloak is not operationally free. The C4 target view and runtime documentation must add Keycloak, its provider-owned persistence, login redirects, and the session boundary as part of recording this acceptance.

Before M1 completion demonstrate: clean Compose login with two users; guessed-ID denial; missing/invalid CSRF denial; session expiry; logout; stale provider session after account deletion; retryable provider cleanup; and core restart requiring login without losing workouts. No commercial-launch security or compliance guarantee follows from choosing a provider.

## Open questions

- Finalize registration, email verification/reset delivery for local versus real users, provider logout integration, and deletion/re-registration policy before the related implementation.
- Confirm the pinned Java/Spring/Keycloak/PostgreSQL combination and issuer routing in the M0 spike.

## Sources checked 2026-11-04

- [Keycloak Docker quickstart](https://www.keycloak.org/getting-started/getting-started-docker): local provider/realm/user setup.
- [Keycloak database configuration](https://www.keycloak.org/server/db): PostgreSQL support and development database limitations.
- [Spring OAuth2 Login](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/core.html): core login integration.
- [Spring CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html): SPA integration and token renewal considerations.
- [Spring OIDC logout](https://docs.spring.io/spring-security/reference/7.0/servlet/oauth2/login/logout.html): application/provider logout integration.
- [Cognito authentication approaches](https://docs.aws.amazon.com/cognito/latest/developerguide/cognito-authentication-approach.html): hosted authentication alternative.
- [Spring Authorization Server overview](https://docs.spring.io/spring-authorization-server/reference/overview.html): framework scope.
