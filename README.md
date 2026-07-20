# Distributed Event Watchdog

> I moved the challenge content here: [CHALLENGE.md](./CHALLENGE.md)

## 1. Problem understanding

Let me explain. I'll invent a small story to demonstrate that I understood the problem and also to start mentioning some of the desicions I made.

**== start of invented story**
We have many systems in our company. All of them have useful information for us, and many of them are just part of a bigger flow. We have been asking to all the leaders of each team to provide an endpoint so we can ask for the status to each service and figure out what is the status of a single traceId (the id of a flow or process). We have been trying to acomplish this for 2 and a half years. We have dealth with:

1. Delays in deadlines of certain teams because they have another priorities.
2. Some of the business rules of certain teams change a lot and the json schemas that we share with have to constantly be updated. Thank god WE ARE THEIR ONLY CONSUMER haha. And we keep fighting about who should make a PACT implementation to know when an API contract is broken. (we already agreed its us, but we just do not have the time).
3. On the other hand, we have to make more than 150 calls (one for each service) per minute to comply with "fresh results all the time" and some of the services are taking even 5 seconds to complete. They say that this will improve, they do not know when, though.

So we started to thing about what WE could do. We made a survey with all of the teams to know what data could they provide to us; they were not very friendly, they were very bussy as always. But we explained our goal and they got kinder. We basically needed to provide these statuses to X, our single ONLY consumer: `STARTED`, `WAITING_OTHER_EVENT`, `TTL_EXPIRED_FOR_EVENT`, `COMPLETED`.

We knew what we wanted. We scheduled a meeting with the 150 leaders and we requested:

1. **event_id** to identify the event of their process/flow; this would also work for deduplication.
2. **trace_id** to know about which artifact we were taling about.
3. **occurred_at** to know when did the event happen.
4. We needed to know when a flow was expired. They gave us different reasons explaining why they could not give this to us,
     - so we thought: "man, it does not matter, time is who decides if something has expired",
     - and pivoted to request **nextEventTtlSeconds** and **nextExpectedEvent**.
     - We agreed that **if** a following event was expected they would send **nextEventTtlSeconds** and **nextExpectedEvent**.
     - And also told os that if it the next event did not arrive in the threshold of **nextEventTtlSeconds**, we could consider it expired.
5. We wanted to know if a step was successful, because we needed to provide the status of the steps of the artifact. So this ask was totally fair. And they gave us **eventName** (which is the step name) and **result** (which only could be either "SUCCESS" or "ERROR").
   - This let me thinking haha, because I said, "maaaan, am I going back to have all the statuses of all the service in my system, again?"
   - **How to we decouple?**
6. We also needed to know, what was the final event. They agreed to send **finalEvent** (boolean)
7. Knowing that "this was not over" and that probably more requirements could be added in the future, they actually proposed to send **metadata** as well.

To make the story short, all teams agreed to give us these 9 fields: **event_id**, **trace_id**, **event_name**, **result**, **occurred_at**, **next_expecte_before**, **next_event_ttl_seconds**, **final_event** and **metadata**. And they could send it almost for free. They had this big and complicated infrastructure with kafka topics, consumers and http-senders, that they could use to send us the data, and it will be ready this Monday.

Done! We've got it! Right? (spoiler alert, no).

Whad did we ended up doing? This is a story for another paragraph.

**== end of story**

### **So, how did we decouple?**

We do not read **eventName**. It is just a string for us. We never map a name to a status. If we would've done that, we would be copying their business rules again.

So what do we ask? 3 things:

1. Did the event promise another one? (**nextExpectedEvent** and **nextEventTtlSeconds**)
2. Did the event say it was the last one? (**finalEvent**)
3. What time is it?

And can we infer the four statuses with only that? Yes:

1. **STARTED**. First event. It promised nothing and it did not close the flow.
2. **WAITING_OTHER_EVENT**. Something was promised and the deadline has not passed.
3. **TTL_EXPIRED_FOR_EVENT**. The deadline passed and the event never arrived.
4. **COMPLETED**. An event said it was the last one.

And the main decision: we do not save the status. We calculate it when someone asks. Like we said in the meeting, time decides.

## 2. Technical decisions

These are the questions we did NOT answer that day. They are not 13 separate decisions: once we started answering them we saw that many of them were the same decision looked at from a different angle, so we grouped them. For each group: what could be going on, what we do for this MVP, and the trade-off.

Three of these questions were not on the list. Nobody asked them in that meeting, we found them later, on our own. They are marked.

### A. When a strange event arrives

1. Was **event_id** enough for deduplication? (they do retry...)
2. What if a service sends an event we were not expecting?
3. What if the expected event arrives **after** we already called it expired?
5. Can a finished flow keep receiving events?
12. What if an event arrives with an **occurredAt** older than the last one we already stored? (we found this one later)

**[What could be going on?]** What does it mean that we receive unexpected events?

- Do the originator has a bug?
- Was the event sent from a place where internet was not available or intermintent, and thus, several intents arrived?
- Are we a target of a hacker? Do the duplicates have the same content? Are there arriving close to each other of after hours, days months?
- Maybe the client is sending some event in lowercase and some events in upper case.
- Maybe they are just using kafka with different partitions and thus, some events are sent in the wrong order?

**[Approach for this MVP]** 

- We will not introduce a sorting feature for the moment, which we might actually need in the future.
- Even if they are using some weird configuration in kafka, we should let that team know about this.
- The duplicated events are rare and we will try to prove it by logging every event arriving more than once, and scheduling a manual check every week. If we see weird things in the logs, then we can take action.
- We'll consider that there's no bad intention when unexpected events arrive. For the moment, we will be interested in saving facts. We want feedback and facts are facts.
- Also our devops team is logging every request going into our infrastructure and our service is behind a VPN.

Now almost everything gets stored because they are facts, we'll deal with specific exceptions in other iterations, when we HAVE MORE INFO. But we need to know when we allow to update the snapshot (the trace state).

A normal event is persisted and updates the snapshot, returning 201. Dealing with special cases has to have an order because these cases overlap. The same event can be old, late and unexpected at the same time, so what we look at first is what decides the answer. We already learned this when we defined the four statuses: checking TTL before completion would report a closed flow as expired.

So we will:
1. Since for the moment, looks that the risk is low, we'll return OK (200) to duplicates, but we will not be saving duplicates, and also we'll not let duplicates update our databae.
2. If the trace is already COMPLETED, we keep the event and answer 200. But the snapshot does not move. A closed flow stays closed.
3. If the event is older than the last one we stored, we keep it and answer 200. But it does not move the snapshot. It happened, it is just not the newest thing that happened.
4. If he TTL was already over, we keep it and we DO let it move the snapshot. The flow comes back to life. We are not going to chase every employee working where the internet signal is bad. And the log will tell us who they are anyway.
5. If the `eventName` is not the one we were told to expect, we keep it and answer 409. They told us to wait for X and Y arrived. So officially, this IS a conflict. We want to see those 409 and find out what the sender was attempting. And the snapshot does not move.

**[The trade-off]** 
- The MAIN one: our status can change. Today we say expired. Tomorrow the late event arrives and we say waiting, we'll bring the flow back. So if you ask twice, you could get two answers. We take that. Saying a flow is dead when it was only slow is worse (unless we receive NEW INFORMATION that makes us change our opinion).
- We drop duplicates. So if somebody reuses an eventId for a different event, that event is lost. The weekly log is our only safety net.
- Kafka sends events out of order. And we answer 409 when the name is not the one we expected. So a 409 will not always mean somebody did something wrong.
- If a team marks finalEvent by mistake, that flow is closed forever. And it looks the same as a flow that really ended.


### B. Whose clock we trust

4. What is the most important clock? theirs (**occurredAt**) or ours (when the event arrived to us)?
13. Do we trust their clock even when it is clearly wrong? (we found this one later)

**[What could be going on?]** Two clocks are involved here, and they will not agree.

- Their clock says when the event happened. Our clock says when we found out.
- The network is in the middle. A slow sender, a retry, a queue, a mountain, a dead battery, and the two times will disagree.
- And somebody's server can simply have the wrong hour. Nobody notices a wrong clock until something depends on it. We are the ones who depend on it.

**[Approach for this MVP]** We count the TTL from `occurredAt`. Sendr's clock wins.

- Expired is a fact for everyone. The deadline is `occurredAt + nextEventTtlSeconds`. Anybody looking at it gets the same answer. If we judged it with our own arrival time, the deadline would be local to us, and we would be comparing apples to oranges.
- And if our service goes down for an hour, everything arrives at once when it comes back. If we use our clock, all events arriving would look fresh, and we would be extending everybody's deadline because WE were down. With `occurredAt` the ones that are expired are shown as expired, which is the truth.And it holds up group A. A late event brings a flow back, so if the deadline came from our arrival time, the same events would give different answers on different days. With occurredAt the past does not move.
- And the answers in group A depend on this. When a flow comes back, it does not come back alone. It comes back with a new promise. And that new promise needs a deadline. If we started counting when it arrived to us, the deadline would be ours. Not theirs.
- We store `received_at` too. We will not use it for the verdict, but we want to see the gap. If `occurredAt` lands in the future, or the gap is absurd, we log it. Same idea as the weekly duplicate check.
- Somebody could put something like `signal_recovered_at` inside `metadata`. That is fine, it is a field that we could use in the future to try to infer more about timing between events.

**[The trade-off]**

- The worst one, and it is real. Our snapshot moves forward by occurredAt. So a sender whose clock is an hour ahead FREEZES that trace for an hour. Nothing moves it, from anybody. A possible fix: ignore an occurredAt that is minutes ahead of our now(). It only catches the gross ones. We will not build it for this MVP.
- And a clock running behind does the opposite. Its deadline already passed when the event arrives, so we call the flow expired, but it just started.
- The `received_at` column is what would let us catch all of this. It is not a fix. It is a way to find out.

### C. How we read what an event promises

6. What if the very first event is also the last one?
7. If a step failed (**ERROR**) but still promises a next event, do we keep waiting?
11. What if they promise a next event but they do not say for how long? (we found this one later)

**[What could be going on?]** Flows are not all the same shape.

- Some flows have twenty steps. Some have one. And a flow with one step is still a flow.
- A step can fail and the flow can still continue. They retry, or somebody fixes it by hand.
- An event promise that is not correctly filled, is not a typo. A template fills these fields, so probably if 1 event is wrong all of them are, until somebody notices.

**[Approach for this MVP]** We only read the structural flags. That is `finalEvent`, and the pair `nextExpectedEvent` + `nextEventTtlSeconds`. Nothing else decides the status.

- If the very first event says `finalEvent`, the flow is COMPLETED. We do not need a second one.
- `result` decides nothing. It talks about the step. The status talks about the flow. So an ERROR that promises a next event keeps waiting, and a flow can be COMPLETED with `result` ERROR. Two different things.
- A promise that is not correctly filled is not a promise. Without `nextEventTtlSeconds` there is no deadline, and without a deadline there is nothing to expire. So the trace status will be STARTED, not WAITING. We are not going to say we are waiting for something that can never expire, and we are not going to call it COMPLETED either, because nobody told us it was the last one. If another event arrives with an actual promise, the flow starts waiting again. Rejecting it would mean rejecting that whole team.

We did not decide anything new here. We only read the flags they send us, which is what we agreed in that meeting, and these three questions answer themselves.

**[The trade-off]**

- A step fails (result=ERROR) and the team gives up. But that event promised another one, so we keep saying WAITING until the TTL runs out. And we cannot do better, because an ERROR looks the same whether they are retrying or they gave up. We still find out, but only when the TTL expires.
- A promise that is not correclty filled just disappears. They meant to promise something, but we do not know how to wait for it, so we do not wait at all. And we never find out.

### D. The loose ones

8. What do we do with the extra data they may attach?

**[Approach for this MVP]**

- We store `metadata` as JSONB.
- We never look inside it. Nothing in there decides a status.
- They proposed this field (I mean, from the story I wrote at the begining haha) for requirements we do not know about yet. So we keep it and we do not touch it.

**[The trade-off]**

- Nobody can query by what is inside.
- And we validate nothing. If a team sends us garbage in there, we keep the garbage.

9. How do we keep our own snapshot from lying to us?

**[Approach for this MVP]** Three things.

- One transaction per event. The event row and the snapshot are written together, or neither one is written. If we saved the event and the snapshot write failed, we would be keeping a fact that our own summary does not know about, and nobody would ever tell us.
- A lock on the trace row (SELECT FOR UPDATE). Two events of the same trace can arrive at the same time. Without the lock both would read the same snapshot, both would write on top of it, and one of the two would just disappear. With it, the second one waits for the first to finish.
- And no `status` column. Sounds like irony but it is not: we do not store the status, so it cannot go out of date.

**[The trade-off]**

- Events of the same trace are handled one by one, so if many events arrive for the same artifact (trace_id) they take longer; only for that trace id, though. One trace never blocks another.

10. What do we answer when someone asks for a **trace_id** we have never seen?

**[Approach for this MVP]** Simple, 404.

- We are not going to invent a status for a flow nobody ever told us about.
- STARTED would be a lie. 
- And 200 with an empty body? That is confusing, at least.

**[The trade-off]**

- Maybe that trace will exist in a second, and we just have not received its first event yet. Both cases are 404. 


## 3. Trade-offs and what we left out

We'll build only what the problem needs now. Everything here is something we could have added and decided not to.

1. **A scheduler or a background job.** We left it out because a scheduler is for things that must happen even when nobody asked. Three of our four statuses only change when an event arrives; I could use that moment to send a notification, no need to schedule it. The fourth one is expiration, no event triggers that one. A clock is the one who does, and we only find out when somebody asks. We would change our mind if somebody had to be NOTIFIED when a flow expires, because then the status has to exist even when nobody is asking, and that is a different system.

2. **Kafka, SQS, Pub/Sub.** We left it out because the events arrive over HTTP and we do not have a transport problem. They already have that infrastructure on their side (again, according to my story in the top of the document haha). We would change our mind if the volume forced us to separate receiving from processing, or if we needed to replay history.

3. **A `status` column.** We left it out because, like we said in question 9, a status we do not store cannot go out of date. We would change our mind if we had to filter or count by status when there are millions of them. Or if somebody asked which traces expired yesterday, or how long this one was expired, maybe even requesting for a list of expirations. We know when a trace expires, the deadline is stored. What we do not have is memory that it WAS expired.

4. **Distributed locks.** We left it out because there is one service and one database. Like we said in question 9, a lock on the trace row is enough to keep two events of the same trace from overwriting each other. We would change our mind if the state lived somewhere without transactions.

5. **Flyway or Liquibase.** We left it out because a plain init script is enough to CREATE a schema. Those tools solve the EVOLUTION of a schema over time, maybe for next iteration. Also I would change my mind if suddenly we need to deploy this in an existing database. 

6. **A real alerting system.** We left it out because for us the expired status IS the alert. We would change our mind if somebody wanted an email, a webhook or a ticket, and then see point 1, because those two come together.

7. **Retry mechanisms.** We never call anybody, we only receive. So there is nothing to retry. Senders retry though, this is why we have event_id on our side to detect duplicates. 

8. **Ordering.** We left it out because we decided that an older event is kept but does not touch the snapshot, so we do not need to reorder anything. We would change our mind if somebody needed the full history of a trace in the order it really happened, and not just where the flow is now.

9. **Authentication.** We left it out because we are behind a VPN (according to my story) and there's a single consumer requesting our endpoints, we should be fine for the moment. What we DO have is validation of the required fields and of the allowed values of `result`. We would change our mind the moment a second consumer shows up.

10. **A catalog of flow definitions.** We left it out because it is the main trap of this whole problem, you almost can "smell" that you "could" need thosw haha. But if we stored flow types and their expected sequences, we would be coupled to every team again, and we would be copying their business rules, which is the thing we were running away from. The flow is defined by them.



11. **Checking our own clock at boot.** Since we depend a lot on our watch, this one is tempting. And the simple version is easy: our database has a clock too, so we could compare ours against `now()` from Postgres. We left it out because doing it once at boot proves little, a clock drifts while the service is running. We would change our mind the day a wrong verdict turns out to be our own fault.

12. **More than one promise at a time.** One more thing we did not build, but we know where it would go. What if a step promises two events at the same time? For example, a payment that has to be approved by risk AND by compliance, and we have to wait for both. Today our snapshot only accept "serial" promises per trace, so we could not do it. We would need a table with all the promises that are still open, and the status would look at all of them together. 

13. **A dashboard of expired traces.** Sooner or later somebody will want to see all of them on one screen. Today they would have to ask trace by trace, because there is no `status` column to filter on. It is doable: a partial index (`WHERE completed_at IS NULL`) over the traces that are still open, and the deadline comparison on top of that. We would change our mind the day somebody has to WATCH the flows, and not just ask about one.
