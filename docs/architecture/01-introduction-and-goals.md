# 1. Introduction and Goals

Syla is a social training platform for CrossFit and functional athletes. Athletes log strength and WOD sessions, track their progress, see Oura recovery data next to their training, and follow each other. Coaching features come in a later phase. See the [Product Vision](../vision.md) for the business view.

## 1.1 Requirements Overview (MVP)

| ID | Requirement |
|---|---|
| F1 | Log workouts: strength sets and WOD formats (AMRAP, EMOM, For Time, Tabata), RX or scaled |
| F2 | AI-assisted logging: parse a whiteboard photo or free text into a structured workout; the athlete picks the relevant parts, adds their score and confirms before saving |
| F3 | Progress tracking: automatic PR detection, estimated 1RM, benchmark WOD history |
| F4 | Athlete profile with progress charts |
| F5 | Social: follow athletes, feed of logged workouts, likes, comments |
| F6 | Oura integration: connect via OAuth2, import sleep, readiness and HRV, show them next to training |
| F7 | Privacy: explicit consent for health data, data export, account deletion |
| F8 | Gyms and classes: create or join public gyms (open) or private gyms (invite link or approved request); publish a parsed board as the gym's class for a day; log it with "I was there" plus your own score; see class results, with the option to hide your score |

## 1.2 Quality Goals

In priority order. Each goal becomes measurable scenarios in section 10.

| Priority | Quality goal | Motivation |
|---|---|---|
| 1 | **Usability (logging speed)** | A typical WOD is logged in under 30 seconds. AI parsing responds within 3 s (text) and 8 s (photo) at p95. Slow logging kills the product. |
| 2 | **Data protection** | Health data is GDPR special-category data: explicit consent, least privilege, encryption in transit and at rest, export and deletion. |
| 3 | **Modifiability** | A new wearable provider or LLM provider can be added without changing the core domain. Modules can be extracted into services later. |
| 4 | **Resilience** | Failures of external services (Oura, Anthropic) never block manual logging or viewing progress. |
| 5 | **AI trustworthiness** | AI output is always reviewed by a human before it is saved or applied. Parser accuracy is at least 90% on the eval set, and every agent run is traced. |

## 1.3 Stakeholders

| Stakeholder | Expectations |
|---|---|
| Athlete | Fast logging, visible progress, a community, control over their own data |
| Coach (later phase) | One view of athletes' progress and readiness, AI suggestions they control |
| Gym owner (later phase) | Community and engagement features for members |
| Author / architect | Showcase architecture and AI engineering skills; keep a path open to a real product |
| Portfolio reviewers (hiring managers, architects) | Traceable decisions, readable docs, a demo that runs with one command |
| Oura (API provider) | Use of the API within its terms |
| Anthropic (LLM provider) | Use within its usage policies |
| AI coding agents (Codex) | Clear repository instructions, specs and architecture rules to work from |
