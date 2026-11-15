# Manual activity logging

Current slice: running (`RUN`), swimming (`SWIM`), cycling (`RIDE`), and general training (`WORKOUT`). Requested by the user on 2026-11-11; CrossFit formats below are deferred. All activities start private and are manually entered, with no GPS recording, maps, imports, photos, public feed, or automatic device estimates.

## Fields and calculations

| Area | Data |
|---|---|
| Every activity | Sport; optional title (120 characters, otherwise sport name); today/past training date; optional local start time; explicit IANA time zone; total duration (1 second–7 days); optional notes (5,000 characters) and perceived effort (1–10) |
| Running | Outdoor/treadmill setting; optional distance in km/mi/m, moving time, outdoor elevation, cadence in steps/min, equipment |
| Cycling | Outdoor/indoor setting; optional distance, moving time, outdoor elevation, cadence in rpm, average power in watts, equipment |
| Swimming | Pool/open-water setting; optional distance in m/yd/km/mi, moving time, stroke; pool-only length in m/yd and count of one-way lengths |
| Optional metrics | Entered average/max heart rate in bpm, energy in kcal, equipment text; no inferred health values |
| Derived | Canonical distance in meters; run pace per km/mi; cycling speed in km/h or mph; swim pace per 100 m/yd. Use entered moving time when present; otherwise total duration. No distance means no pace/speed. |

Preserve entered amounts/units. One mile is 1609.344 m; one yard is 0.9144 m. Pool distance can be supplied directly or calculated from length × lengths; if both are supplied they must match within 0.01 m. A length is one trip across the pool, not a round trip. Moving time cannot exceed total duration, max heart rate cannot be below average, and sport-incompatible fields are rejected. Date/time remain civil values in their stated zone; they are not shifted through a viewer's UTC offset. Nonexistent daylight-saving local times are rejected; an overlapping local time is retained as entered, without claiming a unique UTC start instant.

## Behavior and verification

- Sign in via Keycloak OIDC. Record purpose/version/timestamp consent before storing training data. Owner identity comes from the session; requests cannot choose another athlete.
- Create uses a UUID `Idempotency-Key`. Same key/content returns the same activity; changed content conflicts. A retry after deletion returns 410 and cannot recreate the deleted record. Submission hashes (no raw request) remain until account deletion.
- List is owner-scoped, paginated, and optionally sport-filtered. Details, export, edit, and deletion enforce the same ownership. Unknown and non-owned IDs both return 404.
- Audience changes require a quoted revision (the athlete profile is initialized from registration). PRIVATE hides the activity from everyone else; FOLLOWERS exposes only an allowlisted summary to currently approved followers. Content edits preserve the audience. Unfollow, follower removal, blocking, making an activity private and account deletion revoke subsequent shared reads. Shared summaries use canonical metric units; original entered units remain in the private record/export.
- Replace/delete require quoted revision `If-Match`; stale revisions return 409. An open edit keeps its original revision and unsaved values when background queries refresh. Concurrent account deletion blocks new changes before cleanup. Internal change events carry IDs/revision only; there are no speculative downstream consumers.
- Connection/server failures retain unsaved input in memory, with retry and duplicate-submit protection. Session expiry opens sign-in in a separate tab so the form can remain open. Unsaved navigation/reload is guarded. No browser draft persistence: closing the page still loses the entry.
- Export includes the stored account/consent, profile, relationships, outgoing blocks and activities. Account deletion withdraws consent, blocks mutations, deletes workouts/submission hashes and social/profile data, deletes the provider account, and invalidates the current session. Provider cleanup failures stay blocked and retryable. A hash-based identity barrier contains no workout/name and expires 24–25 hours after completion, outliving the 12-hour absolute app session.

Acceptance: all four sports save/reload with original units and civil date; pool/pace calculations are deterministic; invalid combinations fail with field errors; two athletes cannot read/change/export each other's full records; retries do not duplicate; edits/deletes handle stale revisions; export/deletion work. Verification uses domain tests, real PostgreSQL integration tests, and desktop/mobile browser flows through real Keycloak. No logging-speed or user-usability target has been measured yet.

## UX research used

[Strava manual activities](https://support.strava.com/en-us/articles/15402188-how-do-i-upload-a-manual-activity-to-strava) supports a simple manual sport/time/distance entry path without GPS. [Strava activity privacy](https://support.strava.com/en-us/articles/15401987-how-do-my-activity-privacy-controls-work-on-strava) treats audience as activity-level control; the implementation defaults to private and requires an explicit follower-summary sharing action. [Garmin pool-swim modeling](https://developer.garmin.com/fit/articles/cookbook/decoding_activity_files.html) distinguishes pool lengths and total swim distance. [Nielsen Norman Group form guidance](https://www.nngroup.com/articles/4-principles-reduce-cognitive-load/) supports grouping, clear optional labels, and progressive disclosure. Our design applies these patterns; it is not a copy of Strava or evidence from user testing.

---

# Workout domain model

Status: Conceptual model endorsed by the user on 2026-11-04, based on the five [validated examples](#agreed-workout-examples). Explicitly unresolved choices in section 9 remain open for the logging specification. This document defines concepts and intended invariants, not a database schema, accepted ADR, or final API contract.

## 1. Central distinction

Keep what was prescribed separate from what the athlete actually did. A workout session belongs to one athlete and can contain multiple independently scored sections. A gym class is a shared prescription; it does not own athletes' results.

```mermaid
flowchart TD
    template["Optional source: benchmark or gym class revision"] --> snapshot["Prescription snapshot"]
    session["Athlete's workout session"] --> section["One or more performed sections"]
    section --> snapshot
    section --> performance["Actual movements, loads and scaling"]
    section --> result["Typed result or explicitly unscored"]
    snapshot --> items["Ordered movements, sets, rounds or interval blocks"]
```

This is a conceptual relationship diagram. The shared class remains in the `gyms` module and is referenced by ID under ADR-0010; the snapshot and athlete result are owned by `workouts`.

## 2. Concepts

| Concept | Meaning and minimum information |
|---|---|
| Workout session | One athlete's logged training occasion: ID, owner ID, training date, time zone, optional title/notes, ordered sections, creation/update timestamps and revision |
| Performed section | One part of the session, such as strength or metcon: stable section ID, order, title, format, prescription snapshot, actual performance, score state and result |
| Prescription snapshot | The reviewed instructions associated with the attempt: ordered work, timing, rep scheme, applicable load option, and optional benchmark/class source revision |
| Movement | A stable movement identity and readable name, with aliases for input such as “T2B.” An unresolved name can remain in a draft but must be reviewed before a scored canonical comparison |
| Movement occurrence | A particular position within a prescription: the same movement can occur in several rounds with different reps or loads. Give occurrences stable local identities so substitutions are unambiguous |
| Actual performance | Completed sets or the performed movement/load configuration and any deviations from the prescription. Do not copy a prescribed load into an actual result without athlete confirmation |
| Section result | A format-specific value, such as strength sets, rounds plus reps, elapsed time, capped reps, or interval measurements |
| Source reference | Optional benchmark ID/version or class ID/revision plus source section identity. References provide provenance; editing a source never silently changes an existing attempt |

The user-facing term can remain “workout.” Internally, distinguish the session, its instructions, and the athlete's attempt. Do not introduce separate deployables or modules for these concepts.

## 3. Prescription structure

Use a closed set of supported formats with explicit structures, rather than a free-form expression language:

| Format | Prescription structure | Proposed result structure |
|---|---|---|
| Strength | Ordered sets, target reps, movement, optional load target or instruction to build | Ordered actual sets: completed reps and load |
| AMRAP | Duration and ordered repeatable round | Whole rounds plus additional work in a partial round |
| For Time | Ordered finite rounds/work; optional cap | Completed with elapsed duration, or capped with completed work |
| Tabata | Ordered blocks, movement/unit per block, work/rest durations, interval count, rest between blocks | Interval values and/or an explicitly reported block summary |
| EMOM | Reserved MVP format; exact work scheduling and result structure need a validated example | Not defined yet; do not treat it as AMRAP or Tabata |

For Fran, represent three rounds with rep targets 21, 15, and 9 for both movements. Do not store “21-15-9” as the only machine-readable prescription. For Cindy, store one ordered 5/10/15-rep round with a 20-minute duration.

Prescribed load alternatives, such as Fran's 65 lb and 95 lb options, are explicit options. The attempt selects an option; eligibility and comparisons are not inferred from a person's profile or from the display name “Fran.”

## 4. Values and validation

- Repetitions and whole rounds are nonnegative integers. Prescription set/round/interval counts and required durations are positive. Missing values are distinct from zero.
- A load is a decimal amount plus an explicit unit (`kg` or `lb` initially). Preserve the entered value/unit. Comparison conversion and rounding policy belong in the progress specification. Record load basis where relevant, such as total barbell load versus load per dumbbell; do not compare those as identical.
- Duration is stored as a numeric time value with defined precision in the eventual API; display `7:42` is not the source of truth. For the examples, completed Fran is 462 seconds; the synthetic cap is 600 seconds.
- Counts, calories, and distances retain explicit units. Initially compute a flat AMRAP rep total only when the round consists of countable reps. Mixed meters/calories/reps need an explicit scoring rule before support; do not add unlike units.
- For a fixed 30-rep Cindy round, additional reps range from 0 to 29. Normalize a full extra round into the whole-round count. Thus `12 + 8` means 368 reps, not 12.8 rounds.
- A completed For Time result has elapsed duration. A capped result has the cap and completed work; it has no completed-workout time. Reject a capped result claiming all required work was completed. Early stopping before the cap needs a separate rule; do not silently call it capped.
- No overall numeric score is proposed for a mixed session. Each section retains its own result and units.
- Store the athlete's training date and the relevant IANA time zone explicitly, with UTC timestamps for record creation/update. Do not derive historical training dates from the viewer's current zone. An exact start instant is optional when an athlete logs only a date.

## 5. Scoring state and scaling

Proposed scoring state: `scored` with a valid typed result, or `unscored` with no result. A performed section can be unscored when the athlete trained but did not record a score. A section not performed is omitted from the athlete's session; preserve source-section references rather than inserting zero results. An unsaved form is not automatically a stored workout.

Proposed prescription status: `rx`, `scaled`, or `unspecified`. RX is relative to the selected prescription option, not a universal claim. Use `unspecified` when it is not asserted, including strength work without an RX prescription. A known deviation cannot be marked RX. Keep actual substitutions, loads, rep changes, and duration changes as structured information where supported; optional notes add context but do not replace those fields.

For these examples, use one performed configuration throughout an AMRAP/For Time section. A mid-workout change such as switching from pull-ups to ring rows after round five needs additional modeling; do not silently apply the last configuration to every round. Recording such a session unscored with notes can be considered in the feature specification.

### Tabata detail versus reported summaries

The validated W4 contains minimum scores without the eight underlying values. Proposed support: distinguish `interval_detail` from `reported_summary` so an athlete can log the known minimum without inventing interval data. This is a proposed convenience, not previously approved behavior.

When all eight values are supplied, derive the minimum and reject an inconsistent accompanying summary. When only a reported summary exists, display it as reported and do not claim interval completeness. Partial interval data must not be treated as a verified minimum across all eight intervals. Rowing remains calories; other W4 blocks remain reps. Overall benchmark aggregation is still open.

## 6. Mapping the approved examples

| Example | Session/section representation | Result and distinctions |
|---|---|---|
| W1 | One session, one strength section with five target sets | Five actual sets with loads 50/55/60/62.5/65 kg. The final-set variation stores 3 actual reps against 5 target reps |
| W2 | One session, one 20-minute AMRAP section with a 30-rep round | RX: 12 rounds + 8 reps. Scaled variation records ring rows and 14 + 3 (423 reps), preserving the original pull-up prescription |
| W3 | One session, one For Time section; selected 65 lb load option | Completed: 462 seconds. Separate capped variation: 600-second cap and 81 of 90 reps, including all final thrusters and none of the final pull-ups |
| W4 | One session, one Tabata section with five ordered blocks | Reported block minima 5 calories / 14 / 4 / 6 / 10 reps, with no fabricated interval arrays. Interval-detail mode is a separate fixture to add |
| W5 | One session, strength section plus 10-minute AMRAP section | Strength as W1; metcon 6 + 12 (192 reps) with ring rows. Log Part A alone by omitting Part B, not by creating a zero score |

The W5 shortened prescription is a variant, not a new standard 20-minute Cindy record. A display name or source link alone cannot establish benchmark equivalence.

## 7. Ownership and lifecycle

- Derive owner ID from authenticated identity, never trust a submitted owner ID. Workout operations and exports check ownership in the core API. Wider audience policies remain the separate authorization specification.
- Save one session and its included sections atomically inside `workouts`. Use an explicit request idempotency mechanism for retries; similar content on the same day is not proof of duplication because an athlete can repeat a workout.
- On edit, validate the complete affected section, increment the record revision, and signal changes for derived progress. Use revision checks to prevent one browser overwriting another's edits. Event envelopes and publication guarantees need the runtime design before code.
- On delete, remove/invalidate derived progress and references through module APIs/events. Account deletion must also handle pending events; this model alone does not define that orchestration.
- A class edit preserves old snapshots. Two attempts against different revisions are not automatically comparable. Saving personal results never edits the shared class.
- Parser output is a proposed prescription with unresolved fields flagged. It is not an athlete result or saved session until the athlete selects sections, reviews values, and confirms. No raw photo belongs in this model.

## 8. Persistence and contracts

Follow ADR-0004: relational fields for stable identities, ownership, dates, format, revision, and fields used in queries; validated JSONB can hold format-specific structures. Final tables, indexes, JSON schema/versioning, payload limits, and mapping of set/result details must be designed with the first feature. No cross-module foreign keys or SQL joins.

Core domain types should make invalid result combinations impossible. OpenAPI owns the API contract; generated clients and Python boundary schemas must align with it. This conceptual model is not a reason to hand-copy service contracts or create all future tables now.

## 9. Earlier CrossFit slice proposal (deferred)

Approved M1 format boundary: strength, AMRAP, and For Time, initially one section per saved session. W1–W3 supply the examples; detailed completed/capped validation remains in the draft specification. Keep the conceptual section boundary so W5 can extend it in M2. Tabata and mixed sessions join M2 as already planned; EMOM requires its own validated example. The full MVP scope is unchanged.

Product choices to resolve in the logging specification:

1. Resolved on 2026-11-04: M1 formats and one-section boundary approved; performed-but-unscored logging is included. Private history and today/past training dates were also approved.
2. Decide whether Tabata allows reported summaries without interval detail; recommendation: yes, explicitly labeled.
3. Define early-stopped For Time attempts and mid-workout scaling changes before supporting those scored cases.

Benchmark equivalence, ranking, estimation formulas, and kg/lb comparison tolerance belong to later progress/gym specifications. They must not be silently inferred from this draft. Authentication and initial privacy workflows remain prerequisites to implementing M1.


# Agreed workout examples

Status: validated as representative by the user on 2026-11-04. All athlete results below are fictional test data. Approval covers the examples; it does not settle the complete domain schema, ranking policy, or first-milestone scope.

These examples are development fixtures. Use them when specifying manual logging, parser output, class templates, and progress. Source workouts are linked; synthetic variations are explicitly identified.

See the [workout model](#workout-domain-model) for how these examples map to sessions, sections, prescriptions, and results. The user endorsed the conceptual model; explicitly unresolved specification choices remain open.

## W1 — Strength: back squat

Source: [CrossFit workout 260619](https://www.crossfit.com/260619).

Prescription: five sets of five back squats.

| Set | Actual reps | Load |
|---|---|---|
| 1 | 5 | 50 kg |
| 2 | 5 | 55 kg |
| 3 | 5 | 60 kg |
| 4 | 5 | 62.5 kg |
| 5 | 5 | 65 kg |

Variation: only three reps completed in the final set. Preserve the prescribed five reps separately from the actual three; do not count it as a completed five-rep set.

Expected capture: ordered sets, actual reps, decimal load, explicit units, and optional notes. Do not infer a universal RX load for this strength workout.

## W2 — AMRAP: Cindy

Source: [CrossFit Cindy](https://www.crossfit.com/benchmark/cindy).

Prescription: repeat 5 pull-ups, 10 push-ups, and 15 air squats for 20 minutes.

Example result: 12 complete rounds plus 8 additional reps, RX. The additional reps mean 5 pull-ups and 3 push-ups; total completed reps are 368.

Synthetic scaled variation: same duration and rep scheme, replacing pull-ups with ring rows; result is 14 rounds plus 3 reps. Record the performed substitution and scaled status, not just a free-text label. Keep it distinguishable from RX for later comparisons.

Expected capture: time limit, ordered movements and reps, complete rounds, additional reps, and performed scaling.

## W3 — For Time: Fran

Source: [CrossFit Fran](https://www.crossfit.com/benchmark/fran).

Prescription: 21, then 15, then 9 reps of thrusters and pull-ups. The source lists prescribed thruster loads of 65 lb and 95 lb; the example uses the 65 lb prescription.

Example result: completed in 7 minutes 42 seconds at 65 lb, RX for that prescription.

Synthetic gym variation: add a 10-minute time cap. Athlete completes 81 of 90 total reps: the 21 and 15 rounds, then 9 thrusters, with the final pull-ups unfinished.

Expected capture: completed duration versus capped/incomplete status, completed reps for the capped attempt, actual load/unit, and prescription identity. The added cap is a gym variation, not part of the cited official prescription. Do not display an incomplete attempt as a completed 10-minute Fran.

## W4 — Intervals: Tabata This!

Source: [CrossFit Tabata This!](https://www.crossfit.com/benchmark/tabata-this).

Prescription: rowing, squats, pull-ups, push-ups, and sit-ups in separate blocks, with one minute of rest between blocks. Each block has eight intervals of 20 seconds of work and 10 seconds of rest. Each movement's score is its lowest interval result; rowing is measured in calories.

Fictional minimum scores:

| Block | Minimum interval result |
|---|---|
| Row | 5 calories |
| Squat | 14 reps |
| Pull-up | 4 reps |
| Push-up | 6 reps |
| Sit-up | 10 reps |

Expected capture: per-interval values, explicit units, work/rest timing, and calculated minimum for each block. The minimum scores alone are illustrative summaries, not a complete interval-level fixture. Exact eight-interval arrays and any overall aggregation rule remain to be specified; do not fabricate missing intervals from the minima.

## W5 — Mixed strength and conditioning session

Constructed test session combining W1 with a shortened W2; this combination is not an official benchmark.

- Part A: five sets of five back squats; results as in W1.
- Part B: 10-minute Cindy variation; result 6 rounds plus 12 reps, using ring rows in place of pull-ups.

Expected capture: one session with independently scored sections. Part B retains its changed duration and substitution and must not be treated as an RX 20-minute Cindy benchmark. The athlete can select and log only the section performed; an unperformed section must not be recorded as a zero score.

## Coverage and remaining specification work

| Concern | Examples |
|---|---|
| Decimal loads and per-set performance | W1 |
| Rounds plus partial round | W2, W5 |
| Movement substitutions | W2, W5 |
| Completed versus capped score | W3 |
| Units and interval aggregation | W4 |
| Multiple sections and partial participation | W5 |

EMOM is still MVP scope but has no validated example yet. These five examples are a starting fixture set, not the parser's full evaluation dataset. Validation errors, date/time behavior, kg/lb conversion, benchmark equivalence, ties, and interval-level data still need feature specifications. The earlier CrossFit proposal supported strength, AMRAP, and For Time; the current first slice is the manual multi-sport flow above.
