# Contributing to Wildercord

Thanks for wanting to help! This page covers everything from setting up to getting a pull
request merged. If anything here is unclear, open an issue and ask.

## Setting up

You need:

- **JDK 25** (Temurin works well)
- **Python 3.11+** with **Pillow** (`pip install pillow`) for the asset generator
- Git, and an IDE with Gradle support (IntelliJ IDEA recommended)

```bash
git clone https://github.com/defnotean/wildercord.git
cd wildercord
./gradlew build          # compiles, runs unit tests, builds the jar
./gradlew runClient      # a dev client with the mod loaded
```

In IntelliJ, open the folder as a Gradle project; Loom sets up run configurations for the client
and server.

## Where things live

Read [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) first: it explains the layers and follows a spell
from the rune roster to the screen. To add runes, follow [docs/ADDING_RUNES.md](docs/ADDING_RUNES.md).
The game design (what each rune does and why) is in [docs/DESIGN.md](docs/DESIGN.md).

## The workflow

1. **Open an issue first** for anything bigger than a small fix, so we can agree on the approach.
2. **Branch** from `main` (`feature/fusion-altar`, `fix/stasis-arrows`...).
3. **Make the change**, keeping to the style below.
4. **Regenerate assets** if you touched runes, recipes, loot, language strings or art:
   ```bash
   python tools/generate_assets.py
   ```
   Commit the generated files together with your code. CI fails if they're out of date.
5. **Test** (see below).
6. **Open a pull request** and fill in the template.

## Testing

| Command | When |
|---|---|
| `./gradlew test` | Always. Fast, headless unit tests for the spell engine. |
| `./gradlew runClientGameTest` | Before any PR that changes runtime behaviour or UI. Starts a real client, checks mechanics and saves screenshots to `build/run/clientGameTest/screenshots/`. Set `WILDERCORD_TOUR_ONLY=1` to run only the feature tour (`tour_*.png`), which is quicker when you changed a world feature. |

Add tests with your change:

- New reading rules, numbers or costs go in `src/test/java/dev/wildercord/spell/`.
- New runes with runtime behaviour get added to the smoke list in `castEverything`
  (`src/gametest/.../WildercordScreenshots.java`), and a real assertion in `mechanicsChecks` if
  the behaviour is subtle (like Stasis holding damage).
- World features (Runebound, the Archive, ley lines, secret spells...) belong in the tour in
  `WildercordFeatureTour`: assert what can be asserted, and screenshot the rest.
- UI changes: look at the screenshots at GUI scale 1-4 and all three window sizes.

**Don't build while a dev client is running.** Loom's client loads classes from `build/`, and
rebuilding under it crashes the game. Use `./gradlew -PaltBuild compileJava` to compile-check into
`build-alt/` instead, then restart the client.

## Code style

- **Java 25**, tabs for indentation, braces on the same line, one class per file.
- **Match the surrounding code**: its naming, its comment density and its idioms.
- **Javadoc on classes and non-obvious methods**, written as plain sentences that say what it's
  for and why. Comments explain *why*, not *what*.
- **Keep `spell/` free of Minecraft imports.** It's what makes the readout and the tests trustworthy.
- **Server decides, client draws.** Never trust a packet; validate in `SpellCaster`.
- **A caster isn't always a player.** Monsters cast spells too: use `Casters` for anything only a
  player has (messages, building rights, creative mode, reach).
- **Send particles through `Fx`/`Vfx`**, damage through `Effects.hurt`, and delays through
  `Scheduler` (checking `cast.alive()`).
- **Numbers modifiers change live in `SpellNumbers`**, so the readout can show them.
- Python tools: 4 spaces, standard library plus Pillow only.

## Writing for players

Rune descriptions, tooltips and messages are read in-game, often in a small box:

- Say what it does in plain words, with numbers: "5 damage, then 5 more half a second later."
- Keep descriptions to one or two short sentences.
- No references to other games, shows or books; describe the mechanic itself.

## Commits and pull requests

- Commit messages: a short imperative summary line ("Add Gust rune", "Fix Domain budget"), then a
  body explaining why if it isn't obvious.
- One topic per PR. Split unrelated changes.
- Fill in the PR template: what changed, how you tested it, and screenshots for anything visual.
- Update `CHANGELOG.md` under "Unreleased", and `docs/DESIGN.md` if you changed a rune or a rule.

## Balance changes

Balance is a design decision: open an issue with the numbers you'd change and why (a clip or a
spell that shows the problem helps a lot). Keep spell tuning in `SpellNumbers`, `Circles`,
`Passives`, `Leaning`, `Feats`, `Secrets` and the rune definitions; runtime systems keep theirs
as named constants at the top of the class (`Charging.POWER`, `Rhythm.POWER_PER_STACK`,
`Overcast.MEND_TICKS`, `Unison.BONUS`...). Never scatter numbers through runtime code.

## License

By contributing, you agree that your contributions are licensed under the [MIT License](LICENSE).
