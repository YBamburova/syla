# Agreed workout examples

Status: validated as representative by the user on 2026-11-04. All athlete results below are fictional test data. Approval covers the examples; it does not settle the complete domain schema, ranking policy, or first-milestone scope.

These examples address R1 in the [architecture review](architecture/review-2026-11-04.md). Use them when specifying manual logging, parser output, class templates, and progress. Source workouts are linked; synthetic variations are explicitly identified.

See the [workout model](workout-model.md) for how these examples map to sessions, sections, prescriptions, and results. The user endorsed the conceptual model; explicitly unresolved specification choices remain open.

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

EMOM is still MVP scope but has no validated example yet. These five examples are a starting fixture set, not the parser's full evaluation dataset. Validation errors, date/time behavior, kg/lb conversion, benchmark equivalence, ties, and interval-level data still need feature specifications. First-milestone format selection remains to be agreed during that specification.
