# Product scope

Syla (Сила, “strength”) is a social training network for CrossFit athletes and gym people: a Strava-like place where strength and functional training have meaningful workout data. Hybrid and endurance training also fit. Social connection is a core part of the product, alongside useful logging and progress tracking.

## Product direction

- Log strength sets, WODs, loads, scaling, and scores quickly.
- Follow athletes, share permitted workouts, like and comment, and train with a gym community.
- Track PRs, estimated 1RM, and comparable benchmark results.
- Turn text or a whiteboard photo into an editable workout proposal; the athlete confirms before saving.
- Publish a reviewed gym class once so members can log their own result with “I was there.”
- Show optional wearable recovery data next to training, starting with Oura.

The first usable slice is private manual logging: sign in, save a strength/AMRAP/For Time workout, view history, edit, and delete. Initial defaults approved on 2026-11-04: one section per workout, optional unscored logging, and today/past training dates. Include ownership checks, applicable consent, export, and account deletion. This is a starting slice, not the complete social product.

Then add reviewed AI logging, shared gym training, progress, social interactions, and Oura. This is the previously agreed delivery order, not a fixed release schedule. Agree the next small slice in an issue or PR; no milestone documents or feature-spec folders are required. EMOM, Tabata, and mixed sessions remain intended MVP scope; detailed rules are open where noted in [workouts](workouts.md).

## Later and outside the first release

Coaching dashboards, coach-approved AI programming, verified gym owners, payments, challenges, MCP access, other wearables, and native mobile apps come later. Start with a responsive web app. GPS recording/maps and medical advice are outside the initial scope. Revenue ideas (coach/gym subscriptions and a marketplace) remain hypotheses.

## Quality goals

Manual logging must work when AI or wearables fail. Aim for typical WOD entry under 30 seconds. Parser goals are 90% exact match on a defined evaluation set and p95 response times of 3 seconds for text / 8 seconds for photos. These are targets, not measured results. Once users exist, assess retention; a local demo cannot establish it.

The repository also serves as an engineering portfolio. Working software, clear boundaries, meaningful tests, and a few maintained documents are the evidence. Hosting, provider terms, operational costs, and privacy obligations must be resolved before real-user launch.
