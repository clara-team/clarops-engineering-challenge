# Distributed Event Watchdog — Solución

> I moved the challenge content here: [CHALLENGE.md](./CHALLENGE.md)

## 1. Problem understanding

# A distibuted event watchdog

I could call it "Event noter watchdog" instead haha. But it is ok.

We have many systems in our company. All of them have useful information for us, and many of them are just part of a bigger flow. We have been asking to all the leaders of each team to provide an endpoint so we can ask for the status to each service and figure out what is the status of a single traceId (the id of a flow or process). We have been trying to acomplish this for 2 and a half years. We have dealth with:

1. Delays in deadlines of certain teams because they have another priorities.
2. Some of the business rules of certain teams change a lot and the json schemas that we share with have to constantly be updated. Thank god WE ARE THEIR ONLY CONSUMER haha. And we keep fighting about who should make a PACT implementation to know when an API contract is broken. (we already agreed its us, but we just do not have the time).
3. On the other hand, we have to make more than 150 calls (one for each service) per minute to comply with "fresh results all the time" and some of the services are taking even 5 seconds to complete. They say that this will improve, they do not know when, though.

So we started to thing about what WE could do. We made a survey with all of the teams to know what data could they provide to us; they were not very friendly, though, they were very bussy as always. Even though, it was better for us because we got just a small number of fields. In this case tha say was true: less was better. All the teams agreed to give us this data:

```
  event_id
  trace_id
  event_name
  result
  occurred_at
```

And they could send it almost for free. They had this big and complicated infrastructure with kafka topics and consumers that they could use to send us the data, and it will be ready this Monday.

U
