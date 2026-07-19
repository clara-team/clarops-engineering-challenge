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

Done! We've got it! Right? (spoiler alert, no). To finalize this short story, the questions we did NOT answer that day:

1. Was **event_id** enough for deduplication? (they do retry...)
2. What if a service sends an event we were not expecting?
3. What if the expected event arrives **after** we already called it expired?
4. What is the most important clock? theirs (**occurredAt**) or ours (when the event arrived to us)?
5. Can a finished flow keep receiving events?
6. What if the very first event is also the last one?
7. If a step failed (**ERROR**) but still promises a next event, do we keep waiting?
8. What do we do with the extra data they may attach?
9. How do we keep our own snapshot from lying to us?
10. What do we answer when someone asks for a **trace_id** we have never seen?

Whad did we ended up doing? This is a story for another paragraph.

**== end of story**

**So, how did we decouple?**

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
