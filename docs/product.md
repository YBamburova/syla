# Product scope

Syla (Сила, “strength”) is a social training network for CrossFit athletes and gym people: a Strava-like place where strength and functional training have meaningful workout data. Hybrid and endurance training also fit. Social connection is a core part of the product, alongside useful logging and progress tracking.

## Product direction

- Log strength sets, WODs, loads, scaling, and scores quickly.
- Follow athletes, share permitted workouts, like and comment, and train with a gym community.
- Track PRs, estimated 1RM, and comparable benchmark results.
- Turn text or a whiteboard photo into an editable workout proposal; the athlete confirms before saving.
- Publish a reviewed gym class once so members can log their own result with “I was there.”
- Show optional wearable recovery data next to training, starting with Oura.

The current first slice, requested on 2026-11-11, is private manual logging for **running, swimming, cycling, and general workouts**. An athlete signs in, consents, records an activity, reloads history, edits/deletes it, exports data, and can delete the account. It captures sport-relevant metrics without inventing GPS tracks or device data. CrossFit-specific strength/AMRAP/For Time logging is deferred until this foundation is reviewed; the earlier conceptual examples remain in [workouts](workouts.md).

The interface starts with sport, date, optional local start time, duration, and optional distance. Extra metrics appear under More details. Running uses pace, cycling speed, and swimming pace per 100 meters/yards. Pool swimming can use pool length × lengths swum. Missing data stays missing; generic workouts do not require distance.

After this slice, revisit CrossFit logging, reviewed AI input, shared gym training, progress, social interactions, and Oura. Agree the next small slice in an issue or PR; no milestone documents or feature-spec folders are required.

## Later and outside the first release

Coaching dashboards, coach-approved AI programming, verified gym owners, payments, challenges, MCP access, other wearables, and native mobile apps come later. Start with a responsive web app. GPS recording/maps and medical advice are outside the initial scope. Revenue ideas (coach/gym subscriptions and a marketplace) remain hypotheses.

## Quality goals

Manual logging must work when AI or wearables fail. Aim for typical WOD entry under 30 seconds. Parser goals are 90% exact match on a defined evaluation set and p95 response times of 3 seconds for text / 8 seconds for photos. These are targets, not measured results. Once users exist, assess retention; a local demo cannot establish it.

The repository also serves as an engineering portfolio. Working software, clear boundaries, meaningful tests, and a few maintained documents are the evidence. Hosting, provider terms, operational costs, and privacy obligations must be resolved before real-user launch.
