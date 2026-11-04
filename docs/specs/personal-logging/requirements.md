# Personal logging — requirements

Status: Five initial product defaults approved by the user on 2026-11-04; remaining detailed requirements are a draft. No application behavior is implemented and no new ADR is accepted by this approval.

Approved defaults: strength/AMRAP/For Time; one section per workout initially; private personal history; optional “Score not recorded”; today or past training dates, with future planning deferred.

Links: [design](design.md), [tasks](tasks.md), [workout model](../../workout-model.md), [examples W1–W3](../../workout-examples.md), [milestone M1](../../milestones.md). Addresses architecture review R1–R4 and prepares for R7.

## Scope and proposed defaults

An authenticated athlete can manually create, view, edit, and delete private workouts. Each session contains one section in M1: strength, rep-only AMRAP, or rep-only For Time, including capped results. Multiple sessions on the same day are valid. kg and lb are supported without automatic conversion.

Personal history is private and an athlete may explicitly log a performed-but-unscored session, following the approved defaults. Additional proposed behavior: no implicit RX classification; unsupported early-stop and mid-workout scaling cases can be saved unscored with notes. Those detailed rules remain part of the draft. Tabata summaries remain an M2 question and do not block M1.

Excluded: AI parsing, multi-section UI, EMOM/Tabata, class attendance, social sharing, wearable data, exercise programming, PR calculations, public profiles, offline sync, and persistent form drafts. These remain in their planned milestones where applicable.

## User flow

1. Sign in through the authentication approach selected in a separate ADR. If required consent is absent, complete the approved consent flow before personal training data is processed.
2. Open History. An empty state offers “Log workout.” Existing entries show training date, format, title/movement summary, result, and RX/scaled status when asserted.
3. Select a format. Enter training date, time zone, prescription, actual performance, and result. Optional title and notes are collapsed initially. A clear unit selector accompanies loaded movements.
4. Review the same form and save. On validation failure, retain input and focus the error summary; field messages identify corrections. On uncertain network outcome, retry the same save rather than start another record.
5. Open the saved detail. Edit returns to a populated form; saving returns to detail. Cancel preserves the saved record and warns before discarding unsaved changes.
6. Delete from detail using a confirmation that identifies the workout. Confirmed deletion returns to history; failure leaves the record visible with a retry action.

Export and account deletion are available in account settings. They require separate confirmation and apply to the account's complete implemented data inventory, not only the currently viewed workout.

## Requirements and acceptance criteria

### PL-01 — Ownership and access

- Unauthenticated requests receive an authentication error. Authenticated athlete B cannot read, edit, delete, export, or list athlete A's private records by guessing identifiers or modifying payloads.
- The server derives ownership from identity; a client-supplied owner cannot reassign a workout. Nonowned and nonexistent workout identifiers produce the same resource response.
- No training data is sent to Python, Oura, or an LLM during manual logging. Logging works when those services are unavailable.

### PL-02 — Session metadata

- Save the selected calendar training date and valid IANA time zone independently of creation/update timestamps. Viewing from another zone does not shift the training day.
- Proposed default date is today in the selected zone. Past dates are valid. Reject future training dates in that zone for this completed-training slice; planned workouts are excluded.
- Optional title and notes do not substitute for required structured fields. They render as text, not executable markup. Exact length/payload limits must be finalized in the contract before coding.

### PL-03 — Strength

- Capture one movement, ordered prescribed sets and target reps, and actual reps/load for each reported set. Each set carries explicit units and load basis; M1 loaded examples use total barbell load.
- W1 round-trips 5 reps at 50/55/60/62.5/65 kg. Its variant stores 3 actual reps in the last set against a target of 5.
- Blank actual values mean unreported, not zero; a reported zero-rep attempt is distinct from an omitted set. At least one reported actual set is required for a scored strength session. Reported external loads must be nonnegative decimals; bodyweight is an explicit load kind, not an assumed zero-kg load.
- RX is not inferred from a heavy load. A strength session without an applicable RX prescription uses unspecified status.

### PL-04 — AMRAP

- Capture positive duration, an ordered repeatable round with positive integer rep targets, and actual movement/load substitutions. M1 supports fixed rep-count rounds only.
- A scored result has nonnegative whole rounds and additional reps. W2 stores 12 + 8 and derives 368 reps; its scaled variation stores ring rows and 14 + 3, deriving 423 reps.
- Normalize additional reps spanning complete rounds before saving and display the normalized result. Reject negative or fractional rounds/reps; do not interpret decimal “12.8” as rounds plus reps.

### PL-05 — For Time

- Capture explicit finite rounds and ordered work, with an optional positive cap. W3 stores 21/15/9 thrusters and pull-ups and a selected 65 lb prescription.
- Completed result: positive elapsed whole seconds, no capped score. W3 saves 462 seconds and displays 7:42. If a cap exists, completion may equal but not exceed it.
- Capped result: cap required, completed reps nonnegative and strictly below the performed work total, no completed-workout duration. W3's gym variation stores a 600-second cap and 81/90 reps, displayed as capped rather than “finished in 10:00.”
- Early stopping before a cap is not a capped result. In M1, use unscored with notes for that case; do not fabricate a time or silently change the cap.

### PL-06 — Scaling and unscored training

- Preserve original prescription and the actual movement/load/rep configuration separately. Totals for a scaled attempt use its actual rep scheme. Do not silently edit the source prescription to erase scaling.
- RX means the athlete asserts they followed the selected prescription; known structured deviations prohibit RX. Unspecified is allowed. No automatic ranking or benchmark qualification follows from this assertion.
- An unscored entry has no result payload but retains the reviewed prescription, known actual configuration, and optional notes. It displays “Score not recorded,” not zero.
- Changing an existing scored entry to unscored explicitly clears its result on confirmation. Switching format similarly requires confirmation before clearing incompatible fields. Mid-workout changes are not represented as one constant scored configuration; use unscored with explanatory notes in M1.

### PL-07 — Save and history

- One successful save creates one complete session and section atomically. Failure cannot leave a partial session in history. The record survives application restart.
- Double-click or retry after a lost response creates only one record for the same save operation. A deliberate new save of identical content can create another session.
- History uses stable newest-training-date ordering with a deterministic tie-breaker and paginates without repeating or skipping unchanged records. Detail preserves all entered values and original units.

### PL-08 — Edit and delete

- Edit preserves stable session/section identity, validates the full replacement, and increases revision. An outdated editor cannot silently overwrite newer data; it receives a conflict and retains unsaved input for comparison.
- Delete requires the current revision and explicit confirmation. A repeat of an already successful owned deletion is safe and reveals no other athlete's information. A stale deletion cannot remove a newly edited workout without a fresh confirmation.
- Successful delete removes the record from detail/history/export. Edits/deletes publish the internal change signals required for future derived-data consumers through the accepted event mechanism; no speculative progress consumer is required now.

### PL-09 — Failure and usability

- Field errors, expired authentication, validation, revision conflicts, and uncertain network outcomes have distinct understandable messages. Retry does not silently lose entered values.
- Forms work on a narrow mobile viewport and with keyboard-only input, labels, visible focus, and accessible errors. Unit and RX/scaled controls remain explicit.
- No persistent browser storage of workout drafts in M1. Page/session loss may lose unsaved input; disclose that behavior. Do not cache authenticated responses in shared caches.
- Measure W1–W3 manual logging time under documented conditions. The product goal remains under 30 seconds; report measured results and limitations rather than assume the target is met.

### PL-10 — Privacy and verification gate

- Record applicable consent version, purpose, and timestamp before processing where required; purpose wording and withdrawal behavior must be settled with the privacy design.
- Export includes implemented identity/profile data, consent records, sessions, prescriptions, actuals, results, dates/zones, and units in a documented machine-readable format. It cannot include other accounts' data.
- Account deletion blocks new mutations before cleanup, removes the slice's personal records, and is safe to retry after partial failure. Pending work must not restore deleted data. Any retained operational records require a defined reason, minimization, and retention period before implementation.
- Logs/traces contain no workout payloads, notes, tokens, or health values. The slice's data inventory, authorization tests, consent/export/deletion checks, and threat scenarios must be complete before M1 is declared done.

## Decisions before implementation

The five initial defaults are approved; detailed validation and fallback rules remain draft. Technical gates: authentication ADR; web tooling and CI decisions; exact schemas and input limits; idempotency lifecycle; runtime transactions/events; privacy/consent/deletion and retention design. Product-default approval does not resolve or accept those technical decisions.
