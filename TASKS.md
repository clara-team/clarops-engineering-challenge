# Tasks

How I split the problem before writing code.

The keys in brackets (`[T1]`, `[H3]`, `[I2]`) are the tests that prove each task is done. They are the same keys marked inline in the `README.md`, next to the behaviour they cover. `T` is a unit test of the status calculation, `H` is a Hurl end to end test, `I` is an integration test that hits the database.

The order is not arbitrary. Everything below the schema depends on the schema, and everything about reading depends on something being written first.

---

## Before the tasks

The list below is the second half of the work. This is the first half, and I am writing it down because how I got to those tasks explains why they are those tasks and not others.

**Understanding it.** I read the challenge README. Then I got the app running before writing anything, so that "it does not compile" would never be a surprise. I also asked AI to write a short essay mapping the problem, to see the whole thing at once before touching code. And I drew the state machine, which is where I caught something important: the transition into TTL_EXPIRED is the only one no event causes, time does. 

**Studying it.** I have a small terminal app I built with AI help, called Prisma. I point it at a document and it generates:
- A document with a list of quick questions which I have to read and answering out loud, even if I do not understand them. This is to create a void in my mind for everything I still do not understand. 
- Then it writes a theory document from the same source, which is a synthesis of the original source. 
- And finally it gives me a deck of questions I study with spaced repetition, each concept approached from seven different angles. It sounds like a lot for a four hour exercise, and it is, but the point is not memorising. The point is that answering badly is what shows you which corners of a problem you had not looked at. 

**Cross referencing it.** While studying I kept leaving comments and tags on the questions that felt unresolved. At the end I pulled all of them out and turned them into a single checklist, cross referenced against the challenge requirements. That document is the reason I am fairly sure no business rule, no edge case and no decision got lost between studying and delivering. It is the step I would repeat on any project.

**Writing it.** Then I wrote my own understanding of the problem. I invented a story for it, partly because a story forces you to justify every field you are asking for, and partly just for fun. After that I wrote every technical decision and everything I left out of scope.

**Organising it.** The decisions document got long and repetitive, so I asked the AI to group questions that were really the same decision seen from a different angle. That is where groups A, B, C and D came from.

**Describing tests.** Then I described the test cases I wanted and why, gave each one an identifier, and asked AI to tag the README next to the behaviour each test proves; years ago, it was the only way to catch inconsistencies in 50 pages use cases written by Multipack analysts hahaha, so I use that technique. The tags are there so that the AI and I can point at the same thing without describing it every time. The AI also found one more gap: a test key with nowhere to go turned out to be a decision I had never made.

**Improving tests descriptions.** Now that the tests I wanted have an indentifier and that the AI has found the corresponding rules in the README.md and tagged them, I asked it to improve the description of my tests and among other things it gave me these awesome tables. 

**Requesting a skill that implements from pseudocode.** Since the time was very little I created a skill that implemented java code from pseudocode in GWT form. More about this in AI_USAGE.md 

---

## 1. Schema

- [x] **1.1** Write the `events` table. One row per event received. `event_id` as primary key, which is what makes deduplication a constraint and not application logic. Put it in `docker/init-scripts/db/01-init-schema.sql`.
- [x] **1.2** Write the `trace_state` table. One row per `trace_id`. It will hold the facts of the last event that moved the flow, plus `next_expected_before` already calculated. Put it in `docker/init-scripts/db/01-init-schema.sql`.
- [x] **1.3** Add `received_at` to `events`, separate from `occurred_at`. We will not be using it to calculate the status, only to see the gap between their clock and ours.
- [x] **1.4** Put both tables in `docker/init-scripts/db/01-init-schema.sql`, next to the existing `health` table. Remember the script only runs on a fresh volume.

## 2. Receiving an event, happy path; breaking the ice

- [ ] **2.1** Request DTO with the nine fields, validating the five required ones and that `result` is `SUCCESS` or `ERROR`.
- [x] **2.2** `POST /api/events` that inserts the event row and returns 201.
- [ ] **2.3** In the same transaction, upsert `trace_state`. Both writes or neither.
- [x] **2.4** Calculate `next_expected_before` as `occurred_at + next_event_ttl_seconds` when both fields are present, and store it. This is the only place that math happens.

## 3. Calculating the status

This is the core. We test all of it through the endpoints, not against the class, so that a green test means the whole chain works and not just the calculation. The clock is the only thing we control from the test.

- [x] **3.1** A `statusAt(Instant)` on `TraceState` that returns one of the four statuses. It ended up living on the JPA entity instead of on a separate pure class, which is the first thing a refactor would split.
- [x] **3.2** Compare in this order: COMPLETED, then TTL, then WAITING, then STARTED. Checking TTL first would report a closed flow as expired. `[T5]`
- [x] **3.3** Promise alive and the clock is before the deadline. `[T1]`
- [x] **3.4** Same snapshot, clock moved past the deadline. Nothing was written and the answer changed. `[T2]`
- [x] **3.5** Exactly at the deadline the trace is still waiting. Expired only after. `[T3]`
- [x] **3.6** First event carries `finalEvent`. `[T4]`
- [x] **3.7** `result` never decides the status. `[T7]`
- [ ] **3.10** `[T6]` is only half proven: the COMPLETED test builds a trace whose last result is ERROR, but it never asserts that the response carries that ERROR back.
- [x] **3.8** A promise without a TTL is not a promise. `[T8]`
- [x] **3.9** The late event arrives after expiry and the flow moves on. `[T9]`

## 4. Reading the status

- [x] **4.1** `GET /api/traces/{traceId}/status` reading one row and calling the class from task 3.
- [x] **4.2** Response DTO with the seven fields documented in the README.
- [ ] **4.3** 404 with a body naming the `traceId` that was asked for. `[H5]` **← NEXT (1)** the endpoint answers 404 already, but with an empty body, which contradicts section 2 question 10.

## 5. The events that do not behave

Each one is a branch in the ingest path, in this order.

- [ ] **5.1** An `eventId` we already have. 200, nothing stored, snapshot untouched. `[I1]` **← NEXT (2)** today a repeated `eventId` does a silent update, which is the opposite of what we decided.
- [ ] **5.2** The trace is already COMPLETED. Stored, snapshot untouched. `[I2]`
- [ ] **5.3** The event is older than the last one stored. Stored, snapshot untouched. `[I3]`
- [ ] **5.4** An `eventName` we were not expecting. 409, stored, snapshot untouched. `[I4]`
- [ ] **5.5** `SELECT FOR UPDATE` on the trace row so two events of the same trace do not overwrite each other.

## 6. Hurl

- [x] **6.1** `started-flow.hurl` `[H1]`
- [x] **6.2** `waiting-other-event-flow.hurl` `[H2]`
- [x] **6.3** `ttl-expired-flow.hurl`, using an `occurredAt` far in the past instead of sleeping the thread. `[H3]`
- [x] **6.4** `completed-flow.hurl` `[H4]`
- [x] **6.5** `trace-not-found.hurl` `[H5]`
- [x] **6.6** Run all of them together and make sure the traceIds do not collide. `hurl --test hurl/*.hurl`, 5 of 5.
- [ ] **6.7** The files use a fixed `traceId` and expect 201. Once 5.1 and 5.2 land, a second run answers 200 and they turn red. Fix: have the `POST` return the `traceId` so the files can chain fresh ids.

## 7. Documentation

- [x] **7.1** How to run the project, including the Java 21 requirement and the `/api` prefix.
- [x] **7.2** How to run the Hurl tests.
- [x] **7.3** Assumptions.
- [x] **7.4** `AI_USAGE.md`.
- [ ] **7.5** A pass for typos over the whole README.
