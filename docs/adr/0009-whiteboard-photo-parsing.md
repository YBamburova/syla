# ADR-0009: Whiteboard photo parsing in the MVP, voice input deferred

- Status: Accepted
- Date: 2026-11-04

## Context

Most CrossFit and functional gyms write the day's training on a whiteboard. Athletes already take photos of it. Turning that photo into a structured workout removes most of the logging effort, which is quality goal 1 (logging speed).

The original vision listed voice input and photo parsing as later features. Voice is less useful in a gym: it is loud, athletes are out of breath, and they would be reading the board aloud anyway. Photo parsing matches what athletes already do.

Whiteboard photos bring new concerns:

- **Content:** a board usually holds several parts (warm-up, strength, metcon, scaling options), handwritten with gym-specific abbreviations ("T2B", "DU", "@ 60/40kg").
- **Latency:** image requests to an LLM are slower than text.
- **Privacy:** photos can include people, and phone photos carry EXIF metadata such as GPS location.
- **Cost:** image input uses more tokens than text.

## Decision

1. **MVP input methods:** manual form, free text, and whiteboard photo. Voice moves to a later phase.
2. **One parsing pipeline.** Text and photo produce the same structured workout schema. A board can produce several workout sections; the athlete picks the ones they did and adds their own score, load and scaling.
3. **Multimodal model through the existing provider interface** (ADR-0005). The Anthropic models support image input, so no new provider is needed. This extends ADR-0005's data rule: preprocessed whiteboard images may be sent to the LLM in addition to workout text.
4. **Client-side preprocessing in the web app** before upload: resize the long edge to about 1600 px, compress to JPEG, and strip EXIF metadata (including GPS location).
5. **Synchronous request with its own budget:** text parsing p95 ≤ 3 s, photo parsing p95 ≤ 8 s, with a progress indicator in the UI. The manual form is always the fallback.
6. **Process and discard.** The image is sent to the agent service, parsed in memory, and never written to disk, database or logs (traces record only size and format). Only the confirmed structured workout is saved.
7. **Eval dataset** of labeled whiteboard photos, collected only with the gym's permission and with people removed or blurred. It lives outside the production system.
8. **A parsed board can be published as a shared gym class** so other members log from it without their own photo. See [ADR-0010](0010-gym-classes-and-co-attendance.md).

## Alternatives Considered

- **Voice first.** Rejected for the MVP: worse fit for the gym environment, and it needs a separate speech-to-text step.
- **Dedicated OCR (for example Amazon Textract or Tesseract) followed by text parsing.** Rejected: OCR handles handwriting and whiteboard layouts poorly and loses the structure of the board. A multimodal LLM reads layout and meaning in one step. OCR could be revisited as a cost optimization.
- **Store every photo.** Rejected for the MVP: requires object storage, a retention policy, consent for images that may show people, and more GDPR work. Can be reconsidered with a new ADR.

## Consequences

- Strongest single feature for logging speed, and a clear differentiator from existing CrossFit trackers.
- Quality goal 1 has two latency targets (text and photo).
- Image cost per parse is higher than text; it is tracked per request (ADR-0005), and preprocessing keeps it bounded.
- No image storage, so no object storage container in the MVP.
- Workouts cannot show the original photo, and the eval set cannot grow from real use. Both are accepted.
- The parser eval suite needs a photo dataset as well as a text dataset.
