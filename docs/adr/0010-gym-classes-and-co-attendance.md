# ADR-0010: Gym classes and co-attendance in the MVP

- Status: Accepted
- Date: 2026-11-04

## Context

A whiteboard holds the training for everyone in the gym that day (ADR-0009). If every athlete photographs and parses the same board, they all repeat the same work and Syla pays for the same LLM call many times. Gyms are also the best way to bring in users (see the vision): one active gym brings a group of athletes who already know each other.

The idea: one member publishes the parsed board as the gym's class for that day, and others tap **"I was there"** to log it with their own score, instead of taking their own photo.

This brings a light version of the "gym" concept into the MVP, earlier than planned. Verified gym owner accounts, programming and billing stay in a later phase.

## Decision

### Concepts

| Concept | Meaning |
|---|---|
| **Gym** | A place where athletes train. In the MVP any athlete can create one; it is not verified. Each gym is **public** or **private**. |
| **Membership** | An athlete belongs to zero or more gyms. Role is `member` or `admin`. |
| **Gym admin** | The athlete who created the gym, plus anyone they promote. Admins manage settings, invites and join requests. This is a member role, not a verified owner. |
| **Join request** | A request by an athlete to join a private gym, approved or rejected by an admin. |
| **Invite link** | A link created by an admin for a private gym. It can be revoked and has an expiry date. |
| **Gym class** | The published workout for a gym on a date (optionally with a time slot). It holds the structured workout only, never the photo. |
| **Attendance** | An athlete's link to a gym class. Created when they tap "I was there" and log their own result. |

### Gym visibility

| | Public gym | Private gym |
|---|---|---|
| Found in search | Yes | Yes, name and city only, so athletes can request to join |
| Joining | Anyone, instantly | Invite link, or a join request approved by an admin |
| Classes and results | Visible to any signed-in user | Members only |
| Member list | Members only | Members only |

- The creator chooses the visibility. Admins can change it later.
- Switching public to private takes effect at once; existing members stay.
- Switching private to public requires admin confirmation, because past classes and results become visible to any signed-in user.
- Even in public gyms the member list stays members-only, because it shows where someone trains regularly.

### Rules

1. **Publishing.** After parsing a board, the athlete can publish it as a gym class for one of their gyms. Before publishing they review it, as with any parse.
2. **Co-attendance.** Members see the gym's classes for the day and tap "I was there". This creates their own workout from the class template; they fill in their score, load and RX/scaled. Every athlete owns their own workout record. The class is only a shared template.
3. **Duplicates.** If a class already exists for the same gym and date, the publisher is offered the existing class first ("Already posted by Anna — use it?"). Separate time slots may have separate classes.
4. **Corrections.** The publisher or an admin can edit the class. Athletes who already logged keep their own copy unchanged.
5. **Class results.** A class shows attendees' results, ranked within RX and scaled. This is the seed of the later gym leaderboard.
6. **Hidden scores.** Scores are visible by default. An athlete can hide their score per class, or set "hide my scores in classes" as a default. A hidden score still counts as attendance ("Anna — score hidden") but is left out of the ranking. The workout stays in their own history and follows their normal feed visibility.
7. **Admin continuity.** If the last admin leaves or deletes their account, admin passes to the longest-standing member.
8. **Module placement.** A new `gyms` module in the core API owns gyms, memberships, invites, join requests, classes and attendance, in its own schema (ADR-0002). It refers to workouts by ID and communicates through events, for example `GymClassPublished`, `ClassAttended` and `MemberJoined`.

## Alternatives Considered

- **Every athlete parses their own photo.** Rejected: repeated work and cost, and no shared class view.
- **Only verified gym owners can publish classes.** Deferred: needs an owner onboarding flow, and most gyms will not join before there are users. Verified owners come later and can "claim" a gym created by members, taking over the admin role.
- **Shared workout record for all attendees.** Rejected: one record with many owners breaks personal data ownership, export and deletion (GDPR), and makes editing unsafe.
- **Only open gyms, or only invite-only gyms.** Rejected: open gyms help growth; private gyms are needed for small boxes and closed groups.

## Consequences

- Logging a gym class becomes the fastest path: no photo, no parsing, just "I was there" and a score.
- One LLM call per board instead of one per athlete.
- The gym becomes part of the MVP data model, which supports the multi-tenancy plan in the vision.
- Gym visibility, membership role and score hiding are authorization rules. They are enforced in the core API and covered by tests, not just in the UI.
- Unverified gyms can be duplicated or misused (for example "CrossFit Kyiv" created twice). Accepted for the MVP; handled later by merging and claiming.
- Public gym results are visible to any signed-in user. The hide option and a clear notice when joining a public gym limit the privacy impact.
- Account deletion removes the athlete's memberships, join requests, attendance and results. Classes they published stay for other attendees, without the publisher's identity.
