# ADR-0008: Docker Compose locally, AWS as target cloud

- Status: Accepted
- Date: 2026-11-04

## Context

Reviewers must be able to run the whole system with one command. The system should later run in a real cloud without a rewrite. AWS is the chosen target.

## Decision

- Every container (core API, agent service, web app, PostgreSQL, RabbitMQ) runs locally with `docker compose up`.
- Services follow twelve-factor configuration: everything environment-specific comes from environment variables.
- No AWS-specific APIs in application code without an interface in front of them.
- The AWS deployment architecture (compute, managed services, region, infrastructure as code) gets its own ADR before the first deployment. Likely candidates: ECS Fargate, RDS for PostgreSQL, Amazon MQ for RabbitMQ, an EU region for GDPR.

## Alternatives Considered

- **Kubernetes locally (kind, minikube).** Rejected for now: too heavy for reviewers who just want to run the demo.
- **Design for AWS from day one (LocalStack, managed services).** Rejected: slows down early development and ties it to AWS.

## Consequences

- Containers and environment-based configuration exist from the first commit, so moving to AWS is a deployment task, not a rewrite.
- Some AWS behavior (IAM, networking) is only tested once the AWS ADR is implemented.
