# Product: Syla

Syla (Ukrainian: Сила, "strength") is a social training platform for CrossFit and functional athletes. Full vision: [docs/vision.md](../vision.md).

## MVP scope

- Log workouts: strength sets and WOD formats (AMRAP, EMOM, For Time, Tabata), RX or scaled.
- AI-assisted logging from a whiteboard photo or free text. The athlete always reviews before saving.
- Progress: automatic PR detection, estimated 1RM, benchmark WOD history.
- Athlete profile with progress charts.
- Social: follow, feed of logged workouts, likes, comments.
- Gyms: public or private, gym classes published from a parsed board, "I was there" co-attendance, class results with optional hidden scores.
- Oura integration: sleep, readiness and HRV shown next to training.
- Privacy: explicit consent for health data, data export, account deletion.

## Not in the MVP

Native mobile apps, Apple Health, Health Connect, voice input, GPS tracking, coach features, verified gym owners, payments, other wearables, notifications. Do not build these unless a spec asks for them.

## Product principles

1. **Logging speed comes first.** A typical WOD must be logged in under 30 seconds.
2. **AI assists, humans decide.** AI output is a proposal until a person confirms it.
3. **Health data is sensitive by default.** GDPR special-category data.
4. **No medical advice.** Insights are about training, never diagnosis.

## Domain glossary

| Term | Meaning |
|---|---|
| WOD | Workout of the Day |
| AMRAP | As Many Rounds/Reps As Possible within a time cap; score = rounds + reps |
| EMOM | Every Minute On the Minute |
| For Time | Complete the work as fast as possible; score = time, or reps if time-capped |
| RX / Scaled | Prescribed loads and movements / modified version |
| Benchmark WOD | A named, repeatable workout (for example Fran, Murph, Grace) |
| PR | Personal record |
| 1RM / e1RM | One-rep max / estimated one-rep max |
| Board | The gym whiteboard with the day's training |
| Gym class | A gym's published workout for a date; a shared template |
| Attendance | An athlete's "I was there" link to a gym class, with their own workout record |
