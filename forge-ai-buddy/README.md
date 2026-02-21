# AI Buddy (Forge 1.20.1 prototype)

This is a starter Forge mod prototype for your idea:

- Spawn an AI buddy in survival context.
- Talk to it in chat using `@buddy ...`.
- Ask it to **mine** blocks and **build** simple cobblestone placements.

## What it does now

- `/buddy spawn` creates/teleports a fake AI player near you.
- `/buddy mine` tells the buddy to mine a nearby block and keep the drop.
- `/buddy build` tells the buddy to place cobblestone near you (if it has any).
- Chat command parser:
  - `@buddy mine some stone`
  - `@buddy build a wall`
  - `@buddy follow me`

## Run locally

1. Install Java 17.
2. From `forge-ai-buddy` run:

```bash
./gradlew runServer
```

or

```bash
./gradlew runClient
```

## Notes

This is intentionally a prototype to prove the loop (chat -> intent -> action).
For a smarter AI next, you can add:

- Pathfinding to specific blocks.
- Tool selection and durability handling.
- Structured build plans (e.g., 5x5 hut blueprint).
- Optional LLM bridge (local or API) to convert chat into tasks.
