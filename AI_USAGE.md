# AI Usage

## Tools

- **Claude Code** (Opus), in the terminal. Everything below happened there.
- **Prisma**, a small spaced repetition app I built earlier, also with AI help. It lives in
  a separate repo of mine. I called Prisma because it allows me to see concepts from 
distinct aspects or perspectives. I used it to study this problem before writing about it. 
There is more about that in `TASKS.md`. 

## How I actually used it

Mostly as an editor and as somebody to argue with. Not as a code generator.

The pattern that repeated all day: I write something, if I do not like it, I pass it to the AI and argue with it. Or the opposite, I ask for a draft and then spend four rounds cutting it down or improving it. A good part of this README exists because I rejected and accepted many versions of the same paragraph. 

The other pattern: when a decision felt slippery, I asked for the argument on both (or more) sides before choosing. The clock decision (section 2, group B) came out of that. So did the ordering of the ingest branches, which I only noticed was a problem because I was explaining it out loud whyle studying on Prisma.


## Representative prompts

Not a full log, just the shapes that came back over and over. I asked for these logs to the AI, do not laugh hahaha.

- "You have my tags and comments from Prisma, give me a draft"
- "Can you confirm what you understand of my request BEFORE starting? Confirm it back to me as a question."
- "Group these questions, the document got long and it is tedious to read."
- "Is this the only reason? It feels too specific."
- "Simpler profe."

## Where AI was wrong, and I caught it

Worth listing, because it is the part that says I was reading and arguing, and not only pasting.

- It proposed `408 Request Timeout` for an event arriving with an unexpected name. That status code is about the HTTP connection, not about business meaning. It is a `409`.
- It wrote that we would not know WHEN a trace expired. We do know, the deadline is stored. 
- It justified the design choice partly because it made testing easier. That is backwards and I cut it.
- It said that an event triggered the transition into TTL_EXPIRED. Nothing triggers that transition. Time does. That one mattered, because it is the whole idea.

## What I did not use it for

- Deciding. Every decision in section 2 of the README is mine, including the ones where I went against the suggestion.
- Writting justification for technical decisions and "Trade-offs and what we left out" is mine. Except for the things where I asked for a simpler version of what I've written.
- The story in section 1. That one is mine, typos and all.
