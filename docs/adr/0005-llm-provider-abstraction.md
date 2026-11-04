# ADR-0005: Anthropic API behind a provider interface

- Status: Accepted
- Date: 2026-11-04

## Context

The agent service needs LLMs: first for WOD parsing (fast and cheap), later for the coach assistant (stronger reasoning). Providers, models and prices change often. Health-related data must be minimized before it leaves the system.

## Decision

- Use the Anthropic API, called directly from the agent service.
- All LLM calls go through a provider interface inside the agent service. No other service calls an LLM.
- The model is chosen per task through configuration, so parsing and planning can use different models.
- Only the minimum data is sent: workout text and the training context the task needs, never names, emails or account identifiers.
- The API key comes from environment variables or a secrets manager and is never committed.
- Token usage, latency and cost are recorded for every call.

## Alternatives Considered

- **Amazon Bedrock (Claude on AWS).** Benefits: IAM authentication, data stays in the AWS account. Deferred: more setup for local development. To be reconsidered in the AWS deployment ADR. The provider interface keeps this a configuration change.
- **OpenAI.** Viable; not chosen. Can be added behind the same interface.
- **Local models via Ollama.** Useful for offline work; can be added behind the same interface later.

## Consequences

- Switching or adding providers does not affect the core API or the prompts' callers.
- Per-task model routing keeps parsing cheap and fast.
- The system depends on an external API for AI features. Manual logging must keep working when it is down (quality goal: resilience).
