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

Three of these questions were not on the list. Nobody asked them in that meeting, we found them later, on our own they are marked.

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
- The MAIN one: our status can change. Today we say expired. Tomorrow the late event arrives and we say waiting. So if you ask twice, you get two answers. We take that. Saying a flow is dead when it was only slow is worse.
- We drop duplicates. So if somebody reuses an eventId for a different event, that event is lost. The weekly log is our only safety net.
- Kafka sends events out of order. And we answer 409 when the name is not the one we expected. So a 409 will not always mean somebody did something wrong.
- If a team marks finalEvent by mistake, that flow is closed forever. And it looks the same as a flow that really ended.


### B. Whose clock we trust

4. What is the most important clock? theirs (**occurredAt**) or ours (when the event arrived to us)?
13. Do we trust their clock even when it is clearly wrong? (we found this one later)

**[What could be going on?]** 

**[Approach for this MVP]** 

**[The trade-off]** 

### C. How we read what an event promises

6. What if the very first event is also the last one?
7. If a step failed (**ERROR**) but still promises a next event, do we keep waiting?
11. What if they promise a next event but they do not say for how long? (we found this one later)

**[What could be going on?]** 

**[Approach for this MVP]** 

**[The trade-off]** 

### D. The loose ones

8. What do we do with the extra data they may attach?

**[Approach for this MVP]** 

**[The trade-off]**

9. How do we keep our own snapshot from lying to us?

**[Approach for this MVP]** 

**[The trade-off]** 

10. What do we answer when someone asks for a **trace_id** we have never seen?

**[Approach for this MVP]** 

**[The trade-off]** 

