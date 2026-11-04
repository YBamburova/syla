# Syla: Product Vision

> Status: Draft v0.2. Initial product choices are recorded; technical decisions and detailed behavior remain open. See the [readiness review](architecture/review-2026-11-04.md) and [proposed milestones](milestones.md).
>
> **Syla** (Ukrainian: Сила, "strength, power"). Pronounced *SEE-lah*.

## 1. Problem

Athletes who train across disciplines (CrossFit, functional fitness, strength, endurance) have no single place to track their progress and share it with others.

- **Social fitness apps are built around GPS.** Strava and similar apps work well for running, cycling and swimming. Strength and CrossFit sessions show up as a generic "Workout" with no meaningful data.
- **Strength and CrossFit trackers are isolated.** Tools like SugarWOD, BTWB and Hevy track lifts and WODs well, but they have limited social features and ignore recovery data.
- **Wearable data has no training context.** Oura, Whoop and Garmin know how an athlete slept and recovered, but not what they lifted or how a WOD went. Nobody links "HRV dropped" to "the squat went down".
- **Coaches work from scattered sources.** Online coaches piece together spreadsheets, chat messages and screenshots to understand how their athletes are doing.
- **Logging takes too long.** Recording a complex WOD (movements, loads, rounds, scaling) takes long enough that many athletes stop logging altogether.

## 2. Vision

> One place where every athlete, from runners to CrossFitters, tracks real progress, sees how recovery drives performance, and trains with their community and coach.

## 3. Target Users

| Persona | Description | Primary need |
|---|---|---|
| **Hybrid athlete** | Mixes CrossFit, lifting and endurance training | One profile showing progress across all disciplines |
| **CrossFit / functional athlete** | Trains at a box, follows benchmark WODs | Fast logging, PR history, benchmark progress, leaderboards |
| **Online coach** | Programs for remote athletes | One dashboard with each athlete's compliance, progress and readiness |
| **Gym / box** *(owner features later)* | Posts a daily WOD for its members | Community, leaderboards, member engagement |

MVP primary persona: **CrossFit / functional athlete**. This is the clearest gap in the market, and it puts the hardest modelling problem (WOD formats and scoring) at the center of the MVP. Coaches and gym owners build on that foundation in later phases. The MVP already includes a light gym concept driven by members: one athlete publishes the day's board as a gym class, and the others log it with "I was there".

## 4. Value Proposition

- **For athletes:** see real progress (working weights, estimated 1RM, AMRAP rounds, benchmark times) in one profile, alongside recovery data.
- **For coaches:** see each athlete's progress and readiness without chasing spreadsheets, with AI-assisted programming suggestions that the coach always approves.
- **Differentiators:**
  1. A data model built for strength, CrossFit and functional training, not just GPS activities
  2. AI-assisted logging from a photo of the gym whiteboard or free text, taking seconds instead of minutes
  3. Wearable recovery data linked to training performance
  4. Shared gym classes: one athlete scans the board, the rest of the class logs it with "I was there" and their score
  5. Coaching built into the same platform as the social and tracking features

## 5. Key Capabilities

| Capability | MVP | Later |
|---|---|---|
| Workout logging (strength, WOD formats: AMRAP, EMOM, For Time, Tabata) | ✅ | |
| PR detection, estimated 1RM, benchmark WOD history | ✅ | |
| AI WOD parser: whiteboard photo or free text to a structured workout | ✅ | Voice input |
| Athlete profile and progress charts | ✅ | |
| Social feed (follow, feed of logged workouts, like, comment) | ✅ | Gym feeds, challenges |
| Wearable integration | Oura (cloud API v2, OAuth2) | Whoop, Garmin, Apple Health, Health Connect |
| Coach dashboard and athlete management | | ✅ |
| Multi-agent coach assistant (human-in-the-loop) | | ✅ |
| MCP server ("ask your training data") | | ✅ |
| Gyms (public or private), gym classes from a parsed board, "I was there" co-attendance, class results with optional hidden score | ✅ | |
| Gym owner accounts, verification, programming, full leaderboards | | ✅ |
| Payments and coaching marketplace | | ✅ |

## 6. Scope Boundaries (Non-Goals)

- No medical diagnosis or health advice. Insights are about training, not medicine.
- No GPS route tracking or maps in the MVP. Endurance activities are imported from wearables or entered manually.
- AI agents never change an athlete's program on their own. A human approves every change.
- No native iOS or Android app in phase 1. The MVP is a responsive React web app. Apple Health and Health Connect need a native app, so they wait for the mobile phase, which gets its own ADR.

## 7. Long-Term Intent and Business Context

The project starts as an architecture portfolio, but it is designed so it can become a real product. The architecture must therefore account for:

- **Business model (hypothesis):** free for athletes. Revenue comes from coach subscriptions (athlete management, AI assistant) and later from gym plans and a coaching marketplace that takes a commission.
- **Multi-tenancy:** coaches and gyms act as tenants with their own athletes. The data model supports this from the start, even though tenant features ship later.
- **Compliance:** health and wearable data falls under GDPR special-category data. Consent, export and deletion are MVP requirements, not later additions.
- **Operating cost:** LLM and hosting cost per active user is tracked and documented, so pricing can be based on real numbers.
- **Provider terms:** the Oura API terms (and every later wearable provider's) are reviewed before any commercial launch.

## 8. Success Metrics

**Product (once real users exist):**
- Average time to log a WOD: under 30 seconds
- Week-4 logging retention: at least 40% of users who log in week 1 are still logging in week 4
- AI parser accuracy: at least 90% exact match on the eval dataset

**Portfolio (current primary goal):**
- Architecture fully documented (arc42, C4 diagrams, ADRs, threat model)
- Every significant decision recorded as an ADR
- A working end-to-end demo runnable with `docker compose up`
- The AI-assisted development workflow (repository instructions, specs, automated checks) captured in the repo

## 9. Guiding Principles

1. **Logging speed comes first.** If logging is slow, nothing else matters.
2. **AI assists, humans decide.** Agents propose changes; athletes and coaches approve them.
3. **Health data is sensitive by default.** Design for GDPR from day one: explicit consent, export and deletion.
4. **Every technology choice needs a justification.** Each language and service in the stack has an ADR explaining why.
5. **Grow the architecture gradually.** Start as a modular monolith and extract services only when a real need appears.

## 10. Decisions Log

| ID | Question | Decision |
|---|---|---|
| Q1 | Product name | **Syla** (Сила, "strength"); repo name `syla` |
| Q2 | MVP primary persona | CrossFit / functional athlete |
| Q3 | MVP client | Responsive React web app; mobile in a later phase |
| Q4 | First wearable integration | Oura (direct API integration, no aggregator) |
| Q5 | Social feed in the MVP | Yes, a simple version in v1 |
| Q6 | Long-term intent | Portfolio first, designed to become a real product |
| Q7 | Logging input | Whiteboard photo and free text in the MVP; voice later. Photos are processed and discarded ([ADR-0009](adr/0009-whiteboard-photo-parsing.md)) |
| Q8 | Shared gym WODs | Members publish a parsed board as a gym class; others log it with "I was there" ([ADR-0010](adr/0010-gym-classes-and-co-attendance.md)) |
