# The aura overhaul

Aura in 0.9 works: a breathing method, five stages earned through trials (Glow, Flow, Edge, Form, Sovereign), one Aura
key (tap to slash, sneak to guard, double-tap to step, hold for Dominion), the spellblade, aura marks, duelists, fallen
knights and aura-forged gear. But every swordsman plays alike. The ten methods differ only in colour and one small
passive, the techniques are the same for everyone, progress is one experience bar, and nothing gives a swordsman the
"I made this" feeling a mage gets from the Cord.

This overhaul fixes that in twelve steps, each built by one dedicated helper, one after another, each starting from the
finished step before it. The goal for every step is the highest quality we can reach: complete, tuned, tested, pictured
and documented, not a first draft.

## What aura should feel like

- **Every method is its own sword art.** Two Ember swordsmen fight alike; an Ember and a Thunder swordsman don't.
- **Skill shows.** Timing, rhythm and reading a foe matter, through ordinary swings rather than a wall of keys.
- **A fight builds.** Clean play builds momentum, momentum opens foes, and an opened foe can be finished.
- **It's yours.** A way you chose, techniques you wrote, a blade that grew with you.
- **It's a spectacle for everyone else, and clear for you.** Trails, auras and impacts read from third person. Your own
  first-person view stays readable: small, low and out of the middle of the screen (meditation taught us this).
- **Swordsmen and mages need each other.** Things only a blade can cut, reactions only the two together can set off.

## Ground rules for every step

**Design**
- Original names only. Never name or hint at any show, comic, game or book anywhere: code, assets, docs, commits.
- Server-authoritative. The client may detect input and draw, but the server decides what happens and validates
  every request (stage, method, cost, cooldown, range).
- Build on what exists: `aura/` (`Aura`, `AuraRules`, `AuraCombat`, `AuraGuard`, `AuraStep`, `AuraSlash`, `Crescents`,
  `AuraDominion`, `AuraVfx`, `Spellblade`, `AuraMarks`), `api/AuraApi` (techniques by stage and trigger, trials),
  the client's `AuraClient`, `AuraHud`, `AuraScreen` and `WildercordKeys`. Extend `AuraApi` so add-ons can hook each
  new thing too.
- Numbers live in `AuraRules` (or a sibling rules class) with JUnit tests, and server-tunable ones in the config's
  `aura` section (`config/WildercordConfig.java`, `AuraSettings`; `WildercordConfigTest` must cover new fields, and old
  config files must load with the new fields at their defaults).
- PvP stays fair: respect `Player.canHarmPlayer`, the existing spell-defence ideas (cap anything that scales damage
  against players), and never let a technique kill through a totem or ignore armour unless it's a deliberate, stated rule.
- Respect the performance settings (`Config`/visual settings: reduced particles, etc.) and shaders: under Iris, custom
  blends fall back to vanilla translucent layers (see `client/compat/IrisBridge`), so test with `-Pshaders` when you add rendering.

**Assets**
- Language strings are generated: add them to the `lang` dict in `tools/generate_assets.py`, never edit `en_us.json`
  by hand. Run `python tools/generate_assets.py` then `python tools/check_generated_assets.py`.
- Sounds come from the feel kit: `tools/feel/aura.py` (or a new part added to `PARTS` in `tools/feel/core.py`), then
  `python tools/feel/build.py --only <part>`, `--merge`, `--check`. No sound may reuse another's name.
- Textures and art come from the `tools/*_art.py` scripts, never hand-placed binaries without a generator.

**Tests**
- JUnit for every rule (`./gradlew build` runs about 480 tests; all must pass).
- A client game test for the step, with screenshots you look at yourself (first person and third person), registered
  in `src/gametest/resources/fabric.mod.json`. To run only some tests, back up that file, set the list (check it with
  `json.load` and stop if it fails), run `./gradlew runClientGameTest`, then restore the backup.
- Run the existing aura tests too (`WildercordAuraTest`, and any test your step touches) so nothing regresses.
- Never touch the player's own game: only build and run tests inside your worktree.

**Docs**
- The player guide: `wiki/progression/aura.md` (and new pages beside it if the step deserves one), then
  `python tools/player_docs.py` and `python tools/player_docs.py --check`.
- `CHANGELOG.md` under `[Unreleased]`, in the existing voice: what a player notices, in plain words.
- Update this file: mark your step done in the table, and add anything later steps must know to "Notes for later steps".

**Commits**
- Commit in your worktree with descriptive messages in the repo's style. The repository's configured author is the
  author. **Never add AI attribution of any kind: no Co-Authored-By lines, no "generated with" notes.**

## The steps

| # | Step | Status |
|---|---|---|
| 1 | Sword strings: the input language | done |
| 2 | Feel and spectacle: the shared visual and sound language | done |
| 3 | Arts I: the framework, and Ember, Rime, Thunder, Gale, Stone | done |
| 4 | Arts II: Verdant, Hollow, Starlit, Hourglass, Crimson | done |
| 5 | Momentum and openings | done |
| 6 | Awakening | done |
| 7 | Ways | done |
| 8 | Your own techniques | done |
| 9 | The bonded blade | done |
| 10 | Masters, disciples, sparring and the clash | done; merged and tested |
| 11 | The world of the sword | in progress: terrain training; other features pending |
| 12 | Mage and swordsman together | planned |

### 1. Sword strings: the input language

Ordinary swings become a language. The client watches the attack and movement inputs of a player holding a blade and
recognises short **strings**: a sequence of tokens with timing windows. A recognised string goes to the server as a
request; the server validates it and performs whatever is registered for it.

- **Tokens** (the step may refine these): a swing; a full-strength swing (the attack cooldown nearly full); a
  crouching swing; a jumping (airborne) swing; a sprinting swing; a swing straight after a perfect Aura Guard; a swing
  straight after an Aura Step. Each token after the first must come within a window of the previous one (roughly a
  quarter to most of a second; tune it so it feels deliberate, not frantic). Missing the window resets the string.
- **The five art strings**, one per stage, the same grammar for every method so a player learns it once. The step
  finalises them; a starting proposal:
  - Art I (from Glow): swing, swing, crouching swing.
  - Art II (from Flow): jumping swing, then swing (an aerial opener).
  - Art III (from Edge): perfect guard, then swing (the counter).
  - Art IV (from Form): Aura Step, then swing (the rush).
  - Art V, the Final Art (from Sovereign): three full-strength swings, then a crouching swing, and only at peak momentum
    or while awakened (steps 5 and 6 supply those; until then, gate it on a full aura pool).
- **Feedback**: a small, tasteful string indicator near the crosshair or hotbar (not in the middle of the screen) that
  shows the tokens landing and lights when a string completes; a soft sound per token and a distinct one on completion;
  a clear "fumble" when a string breaks. It must never get in the way.
- **API**: `AuraApi` gains string registration (a pattern, a stage requirement, a handler), so steps 3, 4 and 8 and
  add-ons register arts and techniques without touching the detector. Until step 3, register a placeholder art per
  stage that does something simple and visible (for example a stronger coated blow), clearly marked as a placeholder.
- **Guards**: strings never fire from swings at nothing while a screen is open, never in spectator, and the server
  rate-limits requests. Strings must not interfere with vanilla combat, the spellblade, the Aura key's existing four
  uses, or Cord casting.
- **Done means**: a detector with unit-tested pattern matching and windows, the server path with validation, the HUD
  indicator and sounds, the API, config (`string_window`, an on/off switch for the indicator), tests and docs.

### 2. Feel and spectacle: the shared visual and sound language

The look and sound every later step uses, applied first to what aura already has.

- **Blade trails**: a ribbon following the blade's arc in the aura's colour on coated swings, slashes and (later) arts,
  brighter and wider by stage. Third person shows the full trail; your own first-person view shows a thin, short one.
- **Body aura by stage**: Glow a faint shimmer; Flow wisps rising from the shoulders and blade; Edge a steady haze;
  Form a flowing mantle; Sovereign a blazing corona with glowing eyes. Calm when idle, flaring in a fight. Never more
  than a whisper in your own first-person view.
- **Impact**: a brief hit-stop (a few frames, client-side only, for the attacker and the struck player), a tiny camera
  nudge, and an impact flash on heavy blows; all scaled down or off by the performance settings and an accessibility
  switch (screen shake can make people sick).
- **Banners**: a named-technique banner (an art's name, a finisher's name) shown briefly and stylishly, not blocking
  the view, in the method's colour.
- **A sound palette per method**: each of the ten methods gets its own family of swing, impact and technique sounds
  from the feel kit, so a blind listener could tell Ember from Rime.
- **API**: a small client and server toolkit (`AuraFx` or similar) the later steps call: `trail`, `impact`, `banner`,
  `burst`, `bodyAuraFlare`, with network payloads where the server must tell clients.
- **Done means**: all of the above on the existing slash, guard, step, Dominion and coated blows; first- and
  third-person screenshots at every stage; checked under shaders; performance-setting fallbacks; tests and docs.

### 3. Arts I: the framework, and Ember, Rime, Thunder, Gale, Stone

Each breathing method gets five **Arts**, one per stage, performed with the step 1 strings (Art I counts as the
method's "First Art", and so on; Art V is its "Final Art"). They cost aura, have short cooldowns, and use step 2's
trails, banners, impacts and sounds. This step builds the framework (registry, per-method sets, costs and cooldowns
in `AuraRules`, the Aura page showing each method's arts with their strings and what unlocks them, a Grimoire entry
the first time each art is used) and the first five methods' arts. A starting design to refine, keeping the spirit:

- **Ember**: I *Kindling Draw*, a fast draw-cut that leaves a short line of fire ahead. II *Rising Cinders*, a
  leaping slash that lifts the foe in a shower of embers. III *Backdraft*, a counter that throws the caught blow back as
  flame. IV *Wildfire Rush*, a dash through foes that leaves a burning trail. V *Sunfall*, a leap that brings down a
  blazing arc and a ring of fire.
- **Rime**: I *Frostbite*, a cut that crusts and slows. II *Hailfall*, an aerial slash that rains ice. III *Glacier
  Mirror*, a counter that freezes the attacker briefly. IV *Skate*, a dash along an ice path you lay, shattering frozen
  foes. V *Winter's Hush*, a cone that freezes everything, then shatters it.
- **Thunder**: I *Crackle*, a three-cut flurry faster than the eye. II *Skyfall*, an aerial strike that calls a bolt.
  III *Static Riposte*, a counter that chains lightning through nearby foes. IV *Bolt Step*, blinking between up to
  four nearby foes with a cut on each. V *Heaven's Spear*, a charged thrust that drives a lance of lightning down a line.
- **Gale**: I *Cutting Breeze*, a long-reaching wind blade that pushes. II *Updraft*, launching the foe (and you) for an
  air juggle. III *Eye of the Storm*, a spinning counter that also turns arrows and projectiles aside. IV *Tailwind*, a
  long dash that speeds nearby allies. V *Hundred Winds*, a whirlwind of cuts that draws foes in and lifts them.
- **Stone**: I *Rockbreaker*, a heavy cut that shakes a foe's footing. II *Avalanche*, an aerial slam with a shockwave.
  III *Unmoved*, a counter that takes the blow, throws its knockback back and hardens you. IV *Landslide*, a charge that
  bulldozes foes. V *Mountain Splitter*, an overhead strike that splits the ground in a line of rising stone.

Arts must respect terrain rules (no griefing: anything that changes blocks is temporary or cosmetic, and never in
claimed or warded areas, as the terrain spells already handle). **Done means**: the framework, 25 arts each distinct
in mechanics, look and sound, balanced against each other and against the existing techniques, with screenshots of
every art, tests and docs (a guide page listing every method's arts).

### 4. Arts II: Verdant, Hollow, Starlit, Hourglass, Crimson

The other five methods, on step 3's framework, to the same standard. A starting design:

- **Verdant**: I *Thorn Lash*, vines from the cut root the foe. II *Blossom Fall*, an aerial strike whose petals mend
  allies where it lands. III *Rooted Parry*, a counter that mends you by what it caught. IV *Wild Growth*, a dash that
  leaves brambles to slow foes and mend allies. V *Grove's Heart*, the blade planted to raise a grove that mends allies
  and binds foes.
- **Hollow**: I *Void Cut*, drawing the struck foe to you. II *Collapse*, an aerial slam that pulls everything near into
  one point. III *Null Parry*, a counter that swallows the blow and silences the attacker's abilities briefly. IV *Rift
  Step*, stepping through to behind the foe and cutting. V *Event Horizon*, a black sphere cut that pulls in and crushes.
- **Starlit**: I *Star Needle*, a thrust that throws three darts of starlight. II *Meteor Shower*, an aerial strike
  that brings stars down. III *Constellation Guard*, a counter that marks the attacker with stars that burst. IV *Comet
  Dash*, a dash whose starry trail detonates. V *Nova*, a cut that bursts into a ring of starlight and gives aura back.
- **Hourglass**: I *Echo Cut*, a swing that repeats as an afterimage a moment later. II *Rewind Leap*, an aerial strike
  that snaps you back to where you leapt. III *Stopped Moment*, a counter that holds the attacker still in time. IV
  *Blur*, a dash that slows everything around you briefly. V *Thousand Moments*, nearby foes held still while your
  cuts gather, landing all at once when time moves again.
- **Crimson**: I *Bloodletting*, a cut that makes the foe bleed. II *Red Rain*, an aerial strike that drinks from foes
  near where it lands. III *Sanguine Parry*, a counter that drinks the blow. IV *Frenzy*, a dash whose every hit stacks
  attack speed. V *Crimson Moon*, a great arc that drinks deeply but costs some of your own health: a gamble.

**Done means**: as step 3, for these 25 arts, then a balance pass across all fifty.

### 5. Momentum and openings

- **Momentum**, a per-player meter (0 to 100): clean hits, arts that land, perfect guards and steps through an attack
  build it; taking a hit knocks a share off; it ebbs out of combat. Tiers (for example 25, 50, 75, peak) strengthen
  arts and make them cheaper; peak momentum unlocks the Final Art (replacing step 1's placeholder gate). Shown subtly
  on the aura HUD.
- **Stance**, on foes: every creature has a stance from its health and kind (bosses much more, and recovering).
  Blade and aura hits wear it down (arts more, Stone the most), and when it breaks the foe is **opened** for a few
  seconds: staggered, marked clearly, and the next blow is a **finisher** (a big, named, cinematic strike with step 2's
  hit-stop and banner, a share of the foe's missing health as damage, aura back). Players have a stance too, so PvP
  rewards pressure and guarding (capped, never a one-shot).
- **Done means**: rules with tests, both meters synced and shown, finishers for every method (each method's finisher
  looks its own), mob and boss behaviour, PvP caps, screenshots, docs.

### 6. Awakening

- With a full aura pool (and enough momentum, tuned), a deliberate input (designed to sit with the Aura key's existing
  four uses) **awakens**: the body aura blazes, the eyes glow, arts cost nothing or little, momentum holds high, and
  the swordsman is a little faster and harder-hitting, for a duration that grows by stage (from Edge upward).
- When it ends the swordsman is **spent**: slowed and unable to gather aura for a while, with a cooldown before the next
  awakening. At Sovereign, Dominion raised while awakened is stronger and shaped by the method.
- **Done means**: the state, its input, rules and config, a striking transformation (third person) that stays clear in
  first person, the spent state, interactions with momentum, arts and Dominion, tests, docs.

### 7. Ways

At the Edge breakthrough a swordsman chooses a **Way**, which branches the path at Edge, Form and Sovereign (a node at
each: a passive and a change to an existing technique or art):

- **Way of the Blade**, offence: slashes pierce, finishers hit harder, momentum builds faster from hits.
- **Way of the Bulwark**, defence: the guard grows wider and reflects, aura armour is sturdier, stance is hard to break.
- **Way of the Shadowstep**, movement: steps leave a striking afterimage, blows from behind open foes faster.
- **Way of the Banner**, together: aura and momentum are shared with nearby allies, a rallying cry, Dominion shelters
  the party.

Changing Way is possible but costly (designed in the step: an item, a place, or a trial). The Aura page shows the Way
tree clearly. **Done means**: the four Ways with three nodes each, the choice moment (a real moment, not a menu click),
the re-choice, the page, tests, docs.

### 8. Your own techniques

The swordsman's answer to the Cord. A swordsman writes **techniques** from three parts:

- a **stroke**: thrust, rising cut, falling cut, sweep, spin or draw;
- a **release**: on the blade, thrown as a wave, a burst around you, or an afterimage that strikes after you;
- an **intent**: pierce, sunder (stance damage), bind, echo (repeats), or others the step designs.

Each part is learned (from technique scrolls found in the world, from duelists, from Ways), the method's element flavours
every technique, cost and cooldown come from the parts, and each technique is named by its writer, given a string from
the step 1 grammar (slots unlock by stage: one at Edge, two at Form, three at Sovereign) and ranks up through use, as
spells do with mastery. The writing happens on a new page of the Aura screen. **Done means**: parts, writing UI, naming,
strings, ranks, the drops and teachers, balance against arts, tests, docs. It must feel related to, not a copy of, the
Cord.

### 9. The bonded blade

- A swordsman from Edge upward can **bond** one blade (a ceremony: the breathing stance with the blade at a place of
  power). The bonded blade gathers **resonance** from kills, arts, finishers and breakthroughs, takes on the aura's
  colour, and grows through tiers (for example Bonded, Named, Awakened, Soulforged). At Named it takes a name (the
  player's or one it suggests); at Awakened it gains a trait drawn from how its wielder actually fought (most-used arts,
  Way, method), like mastery traits.
- It's kept on death, only one can be bonded at a time, it works with aura-forged weapons, and it can be passed to a
  disciple (step 10) in a ceremony.
- **Done means**: bonding, resonance, tiers, naming, traits, the look (a glow and an inspect tooltip that tells its
  story), death and transfer rules, tests, docs.

### 10. Masters, disciples, sparring and the clash

- **Sparring**: a challenge between two players (a deliberate gesture with blades, accepted by the other) opens a ring;
  the spar ends at one heart or by leaving the ring, nobody dies or drops anything, and both earn aura (limited per day
  per pair).
- **Master and disciple**: a swordsman of Form or above can take a disciple (two stages or more below) in a ceremony.
  The disciple can learn the master's method, earns faster near the master, and can make a breakthrough by besting the
  master in a spar (a new trial); the master earns a share when the disciple breaks through.
- **The clash**: when two slashes or arts meet, they lock for a moment into a struggle won on timing (a short,
  readable rhythm), and the winner's strike carries on. Duelists clash too.
- **Done means**: all three, synced and readable for both players, griefing-proof (no forced spars, no trapping),
  tests (two-player where possible), docs.

### 11. The world of the sword

- **Training grounds**: standing in the breathing stance under a waterfall or on a high, open summit trains faster
  and counts toward stillness trials.
- **Old battlefields**: places where sword intent lingers, aura's counterpart to magic's residues, with something to
  find.
- **Sword tombs**: a dungeon of buried blades, intent gates that open only to a high enough stage, technique scrolls
  (step 8), and a guardian.
- **The sleeping blade**: a rare place with a legendary blade fixed in rock that only great enough intent can draw.
- **Aura beasts**: two or three creatures that spells barely touch but a blade cuts, so mages and swordsmen need each
  other.
- **Tournaments**: now and then a village holds one; duelists gather, and there are prizes.
- **Done means**: these placed in the world sensibly (biomes, rarity, structure tags, config), art, loot, tests, docs.
  This step may be split in two if it's too big to do well at once.

### 12. Mage and swordsman together

- **Resonant strikes**: a spell and a slash landing on the same foe within a moment, from one player or two, set off a
  reaction, in the spirit of the elemental reactions (their own names and effects).
- **Rune-etched blades**: a blade can carry one effect rune that wakes on arts or finishers.
- **Unity**: someone deep in both paths (circles and stages) can, briefly, let mana and aura feed each other.
- **Done means**: the three, balanced, tests, docs, and a final integration pass over the whole overhaul.

## Notes for later steps

(Each step adds what the next ones need to know: APIs, names, gotchas.)

### From step 1: sword strings

The full rules are in `docs/DESIGN.md` ("Sword strings"), the code map in `docs/ARCHITECTURE.md`, the player's view in
`wiki/progression/aura.md#sword-strings`.

**The language (settled).** Seven tokens, written as plain ids (`SwordString.parse("swing swing low")`, `text()`):

| Token | Id | A swing that... | Weight |
|---|---|---|---|
| a swing | `swing` | any swing | 0 |
| a full swing | `full` | attack strength ≥ `AuraRules.FULL_SWING` (0.9) | 1 |
| a low swing | `low` | sneak held (`isShiftKeyDown`) | 2 |
| a leaping swing | `leap` | off the ground, not in water or lava, climbing, riding, flying or gliding | 2 |
| a running swing | `run` | sprinting (read before vanilla's attack ends the sprint) | 2 |
| a counter | `counter` | the first swing within `StringRules.COUNTER_TICKS` (16) of a perfect Aura Guard | 4 |
| a step cut | `step` | the first swing within `StringRules.STEP_CUT_TICKS` (14) of an Aura Step | 4 |

A swing carries every mark it earns (bits, `Token.bit()`); a token fits a swing that has its bit, and `swing` fits all. At
most 6 tokens (`SwordString.MAX_LENGTH`).

**Windows.** Each swing within `base + recover` ticks of the one before: `base` is the server's
`aura.string_window_seconds` (default 0.5 s = `StringRules.WINDOW` 10 ticks, 0.1 to 2, synced in `Config.Sync`), `recover`
the weapon's ticks to a full swing (`StringRules.recover(delay)`: sword 11, iron axe 20, mace 30, fist 4, capped 60). A
perfect guard or step (a `StringReader.Cue`) inside a string keeps it open at least its cue window. A swing that would have
completed a 2+ swing string with a deliberate release (last token weight > 0) up to `LATE_GRACE` (10) ticks after it lapsed
is a fumble.

**Which art wins.** Every art whose string the last swings fit completes; `StringReader.best` ranks by the last token's
weight, then total weight, then length, then stage, then registration order. Of those the client thinks can go now (rested,
aura ≥ cost, condition met) the best goes; otherwise it **falls through** to the next art the same swings fit; with none,
the best is refused (reason above the hotbar). A completed or refused string uses its swings up.

**What counts as a swing** (client, `SwordStringsClient.reading`): aura weapon in hand, stage ≥ Glow, aura and
`aura.strings` on, no screen, not a spectator, not using an item, no `CHARGE` (casting) attachment. At a living entity under
the crosshair, or at nothing within `ENGAGED_TICKS` (80) of a blow given or taken, a perfect guard or a step. A block never.

**The five strings and the placeholder arts.** Ids in `PlaceholderArts.IDS`: `first_art` (Glow, `swing swing low`),
`second_art` (Flow, `leap low`), `third_art` (Edge, `counter`), `fourth_art` (Form, `step`), `final_art` (Sovereign,
`full full full low`). None of the five is played on the way to another (`SwordString.cutBy`, unit-tested). They're
placeholders every method shares: projected aura bursts (`AuraCombat.projected`, so element, marks, experience, PvP
defences) shaped as an arc, a rising arc, a staggering counter, a line, a ring (`AuraVfx.artArc/artCounter/artLine/artRing`),
priced in `StringRules` (`FIRST_COST`... `FINAL_COOLDOWN`). Steps 3 and 4: `AuraApi.unregisterString(id)` for each and
register each method's own on the **same five strings and stages** with `.forMethod(methodId)`; lang keys
`aura.wildercord.art.<id>` and `.desc` (':' as '.'). The Final Art waits on `PlaceholderArts.FULL_POOL` (aura ≥ 90% of
capacity: the string's own coated swings spend a little); steps 5 and 6 replace that condition (peak momentum or awakened)
through `StringArt.when(ArtCondition.of(test, hintKey))`.

**The API** (`api.AuraApi`, both sides unless noted):
- `StringArt(id, SwordString string, stage, cost, cooldownTicks, Predicate<Player> available, ArtCondition condition,
  ArtPerformer performer)`, or `StringArt.of(id, "swing swing low", stage, cost, cooldown, performer)` then `.forMethod(id)`,
  `.onlyFor(pred)`, `.when(condition)`. `available` = has it at all (method); `condition` = can it go now (both sides, so read
  only synced data; `hintKey()` is the refusal line, given the art's name as `%s`).
- `ArtPerformer.perform(ServerPlayer, StringContext)` returns whether it went off; **the framework** then spends `cost`
  (never past empty, so no backlash), sets the rest (`SwordStrings.COOLDOWNS`, attachment `aura_arts`, saved, synced to the
  owner, kept through death) and calls the hooks. `StringContext`: `marks()` (the swings as read), `released(Token)`,
  `struck()` (the server's `getLastHurtMob` if within 2 ticks, may be dead), `at()`.
- `registerString`, `unregisterString`, `strings()`, `string(id)`, `stringsOf(player)` (stage + available: the reader's
  candidates), `allStringsOf(player)` (available only: the Aura page tab), `artOf(player, id)`, `artReadyAt(player, id)`.
- `conflicts(string, exceptId)`: arts with the same swings or that cut/are cut by it. Step 8's writing screen should call it.
- `addStringSource(player -> List<StringArt>)`: per-player arts (step 8's techniques); asked on both sides on every swing
  read and every request checked, so keep it cheap and read synced attachments.
- `onString((player, art, context) -> ...)`: every art performed, after payment (step 5's momentum, step 9's resonance).

**Server checks** (`SwordStrings.check`): closed (aura/strings off, dead, spectator, stage, not available, marks don't fit
the string), no weapon, not ready, condition, no aura, unseen. "Unseen": the server keeps each player's last 8 swings
(`mixin.SwordStringsSeenMixin` on `handlePunch`, `mixin.PiercingWeaponStringsMixin` on `PiercingWeapon.attack`) and needs
at least the string's length within its longest span + `SEEN_SLACK`, the last within `LAST_SWING_SLACK`, and a recent perfect
guard / step for `counter` / `step` tokens (`SwordStrings.cue`, called from `AuraGuard.feedback` and `AuraStep.go`, which
also tells the client). Full/low/leap/run marks are trusted (network timing blurs them). Requests: `PacketThrottle(4, 5)`.

**Feedback.** `client.StringHud` draws the row of marks (7 px glyphs, `BELOW_CROSSHAIR` 19 px under the crosshair, or
above the aura strip with `MagicQuality.stringIndicator` HOTBAR, or hidden), its phases (live with the window's shrinking
line, done gold, fumble red shake, refused grey, lapse fade) and a cue's waiting mark; `StringHud.glyph` and `string` draw
marks anywhere (the Aura page uses them). Sounds `aura_string_tick` (played at `Feels.step(chain size - 1)`),
`aura_string_complete`, `aura_string_fumble`, for the player alone. Step 2's banners and trails should hang off
`SwordStringsClient.handle` (client) and `SwordStrings.perform` / `onString` (server).

**Gotchas.**
- 26.3 sends a `ServerboundPunchPacket` for every swing (after the attack packet for a hit) but none for a spear thrust
  (a `STAB` action): hence the two server hooks. `Player.attack` resets attack strength itself (`onAttack`) and ends a
  sprint, so the client samples both at the head of `Minecraft.startAttack`.
- The guard holds while sneaking, so a counter is also a low swing: ranking by the release's weight is what makes the Third
  Art beat the First. Keep counters and step cuts weightier than any plain kind.
- Playing low swings while standing still settles the breathing stance after a second (aura sense outlines appear): harmless.
- `Sigils.send` never sends shaped light centred within 1.25 blocks of a player's eyes to that player (first-person comfort),
  so an art's own crescents must be centred ahead of the body; a ground ring at the feet doesn't show to a crouching player.
- Additive shaped light washes out by day: lay a `Light.DARK` rim under it when `AuraVfx.brightBehind` (the slash does too).
- The perfect guard's own flash fills the first-person view in gold (`AuraVfx.perfect`): step 2 may want to tone it down. (Step 2:
  it was mostly the blade's own glow swinging across the view in the guard's swing; see its notes.)
- Game test `WildercordSwordStringsTest` plays every string with the real keys (`swing`, `lowSwing`, jump, the Aura key) and
  checks the server through an `onString` hook and `AFTER_DAMAGE` on `Aura.DAMAGE`; husks there have no AI, so lifts and
  knockback don't show.

### From step 2: feel and spectacle

The player's view is in `wiki/progression/aura.md#how-aura-looks-and-sounds`. Everything here is only how things look and
sound: call it once what happens has been decided.

**The shape of it.** The server says what happened (`aura.AuraFx`, five clientbound payloads); each client draws it as *it*
sees it (`client.AuraFxClient`), because only the client knows its camera: your own effects in first person are thin, short
and low, third person and everyone else get the whole spectacle. The server-sent shaped light of `AuraVfx` (crescents in
flight, Dominion's circle, the placeholder arts' arcs) stays for the big world shapes; `Sigils.send` still leaves those out
within 1.25 blocks of their own player's eyes.

**The toolkit** (`aura.AuraFx`, server side; numbers in `aura.AuraFxRules`, unit-tested in `AuraFxRulesTest`):

| Call | What everyone sees | Payload |
|---|---|---|
| `trail(player, Stroke)`, `trail(player, stroke, mirror, power)`, `trail(livingEntity, stroke, mirror, color, stage, power)` | a ribbon of light along the blade's arc, laid round the entity as it faces and carried with it | `Trail(entity, stroke, mirror, color, stage, power, flags)` to trackers and self |
| `impact(player, target, Weight)`, `impact(attacker, target, color, stage, weight)` | a flash (and by weight sparks, a glint, a ring, an echo ring) where the blow met the foe (`struckAt`); on the striker's and a struck player's screens a hit-stop and a nudge | `Impact(attacker, target, at, color, stage, weight)` to the target's trackers and the target |
| `banner(player, StringArt)`, `banner(livingEntity, name, kicker, color, BannerKind)` | the name at the left edge of the swordsman's own screen, over their head for others | `Banner(entity, name, kicker, color, kind)` (Components, translated on the client) |
| `burst(level, owner, at, facing, color, size, Burst bits)` | a burst of light: `FLASH`, `RING` (in the plane facing `facing`, or toward the viewer when zero), `SPARKS`, `STAR`, `ECHO`; `GUARD` is the first four | `BurstCue(owner, at, facing, color, size, style)` to everyone within 64 |
| `bodyAuraFlare(player, ticks, strength)` | the body's aura surging (0 to 1) and dying away | `Flare(entity, ticks, strength)` |
| `sound(player, Sound.SWING / IMPACT / ART, volume, pitch)` | the method's own sound, for everyone near | (a kit sound) |

`AuraFx.art(player)` returns an `Art` that remembers the player's colour and stage, so an art's look reads in one line:
`AuraFx.art(player).trail(Stroke.DRAW).impact(foe).burst(at, 1.2F, Burst.RING)`; `.color(gold)` recolours what follows,
`.flare(ticks, strength)`, `.sound(...)` and `.banner(name, kicker, kind)` are there too.

**What every art gets for free.** `AuraFx.performed`, an `AuraApi.onString` hook: the art's banner (its name; the kicker is
"<method> · <ordinal>" for a registered art, "<method>" for the placeholders, "<method> · Technique" for an art from a string
source, so step 8's techniques need nothing; the Final Art's banner is `GRAND`), a body flare (30 ticks at 0.7, the Final Art 50
at 1.0) and the method's `ART` sound. **So an art adds only its own trail, impacts, bursts and element light and sounds.**

**Giving an art its look** (steps 3 and 4):
- *Trail*: pick the `Stroke` that matches the art's motion: `CUT` (down across; `mirror` cuts back), `RISING`, `FALLING`
  (overhead), `SWEEP` (wide and level), `LOW` (across the legs), `DRAW` (a fast level draw), `THRUST` (a straight lance that
  follows the pitch), `SPIN` (a whole turn), `CROSS` (an X: two trails two ticks apart). `power` scales the width (arts use
  1.25 to 1.6). The trail grows with the stage by itself (`trailWidth`, `trailTail`, `trailAlpha`; motes from Flow, an edge from
  Edge, an echo from Form, sparks at Sovereign). A new stroke is one enum value (tilt, from, to, height, ahead, radius, sweep,
  life, ownStart) and gets its own view for free (`OWN_*`; `ownStart` is where your own view's part of the arc begins, 0 for
  the start, later when the start would cross the middle of the view, as `FALLING`'s 0.5); `THRUST` is the only special case.
- *Impacts*: one `impact(foe, Weight)` per foe really hurt (`AuraCombat.projected` returned > 0). `LIGHT` (no hit-stop),
  `FULL` (45 ms), `HEAVY` (70 ms, a ring), `GRAND` (110 ms, a ring and an echo). Stops within 150 ms are one, so a sweep through
  many foes holds once. `Art.color(...)` for a special colour (the counter's gold).
- *Bursts*: for the art's own moment (a landing slam, a detonation). Give the owner: a burst near their eyes in their first
  person becomes a "whisper" (a third the size, faint, moved to the bottom of the view; a ring shows only its lower arc).
- *Banners*: automatic for arts. Step 5's finishers: `AuraFx.banner(player, name, kicker, BannerKind.FINISHER)` (56 ticks).
  Step 6's awakening: `BannerKind.GRAND` and a long `bodyAuraFlare`.
- *Sounds*: the method's `ART` plays with the banner; add element sounds through `Feels.sound` as usual, and the method's
  `IMPACT` on a big landing (`AuraFx.sound(player, Sound.IMPACT, ...)`).

**The method sound families** (`tools/feel/aura_methods.py`, added to the `aura` part's events; `AuraFx.family(methodId)`):
`aura_<method>_swing` (3 variants, role effect), `aura_<method>_impact` (3 variants, role impact) and `aura_<method>_art` (1,
role cast, heard to 24 blocks) for `ember`, `rime`, `thunder`, `gale`, `stone`, `verdant`, `hollow`, `starlit`, `hourglass` and
`crimson`, and the neutral `aura_steel_*` for a method without its own. Add-ons: `AuraApi.registerSounds(methodId, family)`.
Where they play now: a coated blow plays `IMPACT` for everyone and `SWING` for everyone but the swinger (whose own client plays
`SWING` the moment they swing, in `AuraFxClient.swung`); a Flow sweep `SWING` at 0.78 pitch; the slash `ART` under `aura_slash`;
the step `SWING` at 1.35; Dominion `ART` at 0.7; every art `ART` with its banner.

**Ordinary swings' trails.** Your own client draws yours (`AuraFxClient.attackBegins` and `swung`, from
`mixin.MinecraftStringsMixin`) the moment you swing: only a blade with aura enough to coat (`Aura.coated`), never at a block
(digging). The stroke comes from the swing's sword string marks (`AuraFxRules.stroke(marks, thrust)`), alternating in a run
(`mirrored`, within 24 ticks), 1.3 times wider when the swing completed a string. Everyone else hears of it from the server:
`AuraFx.swung` from the punch (`mixin.SwordStringsSeenMixin`) or a spear's thrust (`mixin.PiercingWeaponStringsMixin`), with
the marks noted at the attack's head (`AuraFx.swingBegins` from `AuraCombat.swing`, before vanilla ends the sprint), flagged
`ORDINARY` (onlookers on "subtle", or with others' effects on minimal, skip those, never a technique's). A Flow sweep replaces
the ordinary trail: the swinger's client predicts it (`AuraFxRules.sweeps`, the same test as `PlayerAuraMixin`'s) and draws a
`SWEEP`; the server's `AuraFx.swept` sends a `SWEEP` to everyone and the swinger's client ignores it within `SWEEP_ECHO` ticks of
its own; vanilla's grey sweep particle is left out under a Flow sweep.

**The body's aura** (`client.render.AuraBodyLayer`, a layer on every `AvatarRenderer`, and `AuraFxClient.motes`): the stage
looks are in the guide; its strength is `AuraFxRules.intensity(lit, fighting, surge)`: `IDLE` 0.35, `FIGHTING` 0.8 (synced as
`AuraPresence.Look.fightUntil`, set by `AuraFx.fighting` from `Aura.fighting` and renewed only when under 60 of its 100 ticks
are left), plus up to 0.6 of a surge (`Flare`), times 0.4 while too low to coat. Drawn in two inks without shaders (a deeper
shade laid over the world, for day, and added light, for night: `Ink`); under Iris the laid-over one alone (`IrisBridge`
assigns `AuraBodyLayer.GLOW_PIPELINE`). Its sheet is `textures/entity/aura/body.png` from `tools/aura_art.py` `body()` (a haze,
four flame frames, a ribbon band, an eye, a ground pool). Never drawn for your own body in first person; instead
`AuraFxClient.whisper`, a faint band at the bottom edge from Edge while it flares (at most a seventh opaque). **Step 6
(awakening):** a long `bodyAuraFlare` at 1.0 already lifts the corona over the head; for a lasting state add a synced flag
beside `fightUntil` and fold it into `bodyIntensity` (the eyes are drawn from Sovereign only: open them for awakening there).

**First person, everywhere.** Trails: `OWN_WIDTH` 0.34, `OWN_ALPHA` 0.55, `OWN_SPAN` 0.55 of the arc (from `Stroke.ownStart`),
`OWN_DROP` 0.42 lower, `OWN_ASIDE` 0.22 toward the blade hand, `OWN_LIFE` 0.7, no extras and no shade rim; laid about the eyes
in the look's own frame as it was when the swing began (a world-level arc came up through the crosshair when looking down at a
foe), and faded out within `OWN_CLEAR` 12 to `OWN_CLEAR_FULL` 22 degrees of the middle of the view (`ownClear`), whatever a
stroke does; decided every frame, so F5 mid-swing switches. An impact seen by its striker in first person is a flash only, at half size; a struck player sees none of
their own. Bursts near their owner's eyes are whispers (`AuraBurst.nearOwnEyes`, 2.6 blocks). The blade's own glow
(`AuraBlade`) in first person: its soft halo at 45% and held close, no guard brightening, the crystal and the rim at 80%. **That
was step 1's "perfect guard fills the view in gold":** the guard's swing carried the blade's wide soft halo across the view (in
Thunder's yellow); the guard's own burst now draws as a thin gold arc low in the view.

**Hit-stop** (`client.fx.HitStop`, `mixin.EntityRenderDispatcherHitStopMixin` at the return of `extractEntity`): a held entity
keeps the pose it had when the hold began (place, turn, walk, swing: the `Pose` record), laid over each fresh render state;
nothing else is frozen. Never hand a stale render state back: its item states are reset each frame (it drew a held weapon
blank). Real-time milliseconds, so it doesn't depend on the frame rate.

**Settings** (`client.fx.MagicQuality`, `wildercord-visuals.json`): `blade_trails` FULL / SUBTLE / OFF, `body_aura` FULL / CALM
/ OFF, `impact` FULL / SOFT / OFF (`hitStop` 1 / 0.5 / 0, `flash` 1 / 0.75 / 0.45), `banners` ALL / OWN / OFF; `camera_shake` also
gates the nudge (`ScreenEffects.nudge`); `reduced_flash` halves flashes; `others` MINIMAL drops others' ordinary trails and
body motes. The performance profile sets SUBTLE / CALM / SOFT / OWN. New effects should read `MagicQuality` the same way.

**Gotchas.**
- Particles built of square quads (`QuadParticleRenderState`) bead when the pieces only meet or overlap by chance: walk a
  ribbon in steps of half a piece and give each piece a little over half the alpha (`AuraTrail.pass`). `LightStrokes` paints
  the ribbons, cores and glows of all of aura's particles.
- Additive light washes out by day: trails and wisps lay a `GlowLayers.DARK` rim under themselves when
  `level.isBrightOutside() && canSeeSky` (not your own first-person trail).
- The client's `ParticleEngine.add` only queues, so a particle may spawn others from its own `tick`.
- A render layer's pose is the model's (flipped, scaled 0.9375): `AuraBodyLayer` backs out to the feet, level with the world,
  as `AuraShellLayer`'s afterimages do; the eyes use `getParentModel().head.translateAndRotate` (the face is toward -z, y down).
- Game test `WildercordAuraFxTest` checks and films all of it (counts in `AuraFxClient.counts()`: trails, own trails, impacts,
  bursts, flares, body motes, whispers; `HitStop.stops()`, `ScreenEffects.nudging()`, `AuraBanners.ownShowing()`). A Dominion's
  circle lingers its full time on each client: wait it out before the next pictures. `WildercordShaderTest` films a Sovereign
  body and an art under its test pack (`shader_aura_*`).

### From step 3: Arts I

The player's view is `wiki/progression/sword-arts.md` (every art) and `wiki/progression/aura.md#the-arts`; the rules and every
art's numbers are DESIGN.md's "The methods' arts"; the code map is ARCHITECTURE.md's "The methods' arts".

**Giving a method its arts (step 4, for Verdant, Hollow, Starlit, Hourglass, Crimson).** Copy the shape of `EmberArts`:
1. `aura.arts.<Method>Arts`: `METHOD` (the method's id), the five art ids as constants, and
   `arts()` returning `List.of(MethodArts.art(AuraApi.ArtSlot.FIRST, ID, <Method>Arts::performer), ...)` (one a slot, First to
   Final). A performer is `static boolean x(ServerPlayer player, AuraApi.StringContext context)`: it decides what happens,
   draws it, and returns whether it went off (false: nothing is paid, nothing rests; call `MethodArts.blocked(player, id)` to
   say why). The framework already gives every art its banner, body flare, the method's `ART` sound, its price and rest and its
   Grimoire entry; the art adds only its own trail, strikes, light and voice.
2. `aura.ArtRules`: the art's numbers as constants under a `// ===== <Method>` heading, and one `Art` record a slot in `ARTS`
   (id, method, slot, the slot's cost and rest, and the balance model's `primary`, `area`, `control`, `reach`; see below).
   `MethodArts.art` takes the price and rest from there by id and throws if the slot disagrees.
3. `MethodArts.init`: `AuraApi.registerArts(<Method>Arts.METHOD, <Method>Arts.arts())`; add the method to `MethodArts.METHODS`
   and its voices to `MethodArts.SOUNDS`; `ArtRulesTest.METHODS` lists the methods too.
4. Lang in `tools/aura_art.py` `LANG`: `aura.wildercord.art.<id>` and `.desc` (what it does, in the guide's voice, numbers in
   words); then `python tools/generate_assets.py` and `python tools/check_generated_assets.py` (on Windows it may rewrite a few
   unrelated JSON files with only their line endings changed: `git checkout --` those).
5. A voice per art in `tools/feel/aura_arts.py` (`aura_art_<id>`; see the existing recipes and the `_whoosh`, `_crackle`,
   `_zap` and `_rubble` helpers), then `python tools/feel/build.py --only aura`, `--merge`, `--check`.
6. A scene per art in `WildercordArtsTest.scenes()` (where the foes stand, the method and stage, how to play it, what to check
   on the server), and the guide's tables.
7. **Tests that assume five methods without arts**: `WildercordSwordStringsTest` plays the common arts as Verdant, Hollow,
   Starlit, Hourglass and Crimson (and `SwordStringsTest`'s registry tests use "verdant"). Once every built-in method has arts,
   play the common arts with a method registered by the test itself (`AuraApi.registerMethod`), or a player with no method
   arts at all, and keep one test of a method's own art beside a common one. The Aura page's dimmed swatch and the Grimoire's
   "Sword arts (n of m)" count follow `hasArts` by themselves.

**The shared kit** (`aura.arts`; use these rather than reaching for `cast` directly, they carry the PvP and boss rules):
- `ArtKit.hits(player, fx)` then `hits.strike(foe, factor[, Weight])` for every blow: projected aura at the weapon's damage
  × factor × `damage_scale` × `art_damage`, its impact drawn, each foe answering once, a player capped at `PVP_ART_CAP` over
  the whole art. `hits.raw` for a computed amount (Backdraft's caught blow), `hurt(foe)`/`count()` for "each foe once".
- Who: `arc(player, first, reach, degrees, max)` (a cone in front, the foe struck first), `around`, `line`, `beam`, `primary`,
  `attacker` (the one a counter answers: `AuraGuard.caught`'s, else the nearest in front), `nearest`. All filter through
  `harmable` (targeting, teams, `canHarmPlayer`).
- What to them: `lift`, `knock`, `shove`, `pull`, `draw` (a pull that overrides the velocity: see the gotchas), `ignite`,
  `chill`, `slow`, `freeze` (the mod's freeze, Shatter-able), `hold` (stunned), `holdLater` (stunned once it lands), `shock`
  (interrupt, a short hold, ionised). Each is already held for players (`PVP_HOLD_TICKS` 15, then 80 ticks before another art's
  hold, `PVP_THROW` 0.6, `PVP_IGNITE_TICKS` 60) and bosses (only slowed).
- Moving the swordsman: `path` (never through walls or wards, `AuraStep.path`), `dash(player, path, ticks, stretch)` (a
  Fourth Art's rush, a callback per stretch), `beside`, `fits`, `blink`, `launch`; `MethodArts.whenLanded` for a leap's
  landing (the fall forgotten). `AuraStep.afterimages` for the streak.
- Light: `ArtLight.world(player)` (everyone, the owner too) and `ArtLight.spectacle(player)` (everyone else, and the owner only
  in third person: `AuraFx.Shown`). Both lay a thin dark rim under additive light by day; `.bare()` for a white core over a
  coloured stroke. Shapes: `ring`, `groundRing`, `ray`, `slash`, `tongues` (flames), `whirl`, `swirl`, `shards`, `orb`, `arc`
  (lightning), `sigil`, `ground`, `flash`.
- Ground: `ArtFields.open(owner, kind, shape, ticks, period, pulse)` with `strip`/`disc`/`ring` (Ember's fire line and ring,
  Skate's path, Glacier Mirror's pane); `ArtBlocks.spire/slab/sheet` (block displays that rise and sink, cleaned up after a
  restart); `ArtWards` for what stays on a body (mirror, eye, juggle, harden, crusts).
- Real blocks: only through `WorldMagic` (Skate's `frostWater`), gated by `aura.art_terrain`, `Casters.mayEdit`, wards and
  `world_changing_magic`. Verdant's brambles and Hollow's collapse should be `ArtBlocks` shapes or `ArtFields`, not blocks.

**Balance numbers and why.** Every method's art in a slot costs and rests the same (`SLOT_COST` 6/8/8/10/40,
`SLOT_COOLDOWN` 60/80/80/100/600 ticks; an art may rest a little less if it's lighter, as Crackle's 50) and is worth the same
within `POWER_SPREAD` 12% of `SLOT_POWER` 1.3/1.8/1.9/2.15/4.35 W by `ArtRules.power`: primary + 0.6 × area + 0.25 × control
seconds + 0.07 × blocks of reach past 3. `ArtRulesTest` enforces it (and that no method's five together outweigh another's by
more than a tenth, and that each method leads in its own thing). Method identities so far: Ember the most damage with fire after and
the least control; Rime the least damage and the most hold (only Stone's standing firm comes near); Thunder many foes a little each, and interrupts; Gale reach and the
air under them; Stone the heaviest single blows and standing firm. For step 4, keep each method a different answer: count
healing allies (Verdant, Crimson's drinking) as `area` at about its amount in W, aura given back (Starlit's Nova) as `primary`
at about a W per 10 aura, and Crimson Moon's own health cost as negative `control` (or a lower primary), and add a test like
`eachMethodHasItsOwnStrength` for the new identities. After step 4, the balance pass across all fifty is the same test with ten
methods.

**First person.** The swordsman's own view gets the thin trail (step 2), marks on foes, light on the ground ahead and anything
out at a distance; whatever sits round the body or lies across the line of sight (a lance, a near crescent, a mirror, a gout of
flame, a whirlwind, rising stone right in front) goes through `ArtLight.spectacle`. `Sigils.send` already drops shaped light
centred within 1.25 blocks of the owner's eyes, which isn't enough for a crescent 1 to 2 blocks ahead: it still crosses the
middle. Look at every art's `_fp` shot.

**Gotchas.**
- **Vanilla knockback rides every art strike** (projected aura goes through `hurt`, 0.4 back from the swordsman), so every hit
  pushes foes away. A pull (Hundred Winds) must set the velocity after the strike (`ArtKit.draw`), not add to it; a carry
  (Landslide) teleports mobs along; a throw adds on top of it.
- **A hold is `Spirits.hold`/`freeze`: NoAI for a mob**, which freezes its motion too, so a foe held in the air hangs there.
  Lift first, hold once it lands (`ArtKit.holdLater` waits up to 40 ticks for the ground).
- **Additive light washes out by day**; the rim `ArtLight` lays is narrow on purpose (ring ×1.15 its width, ray +0.06, slash
  ×1.15): wider rims read as black outlines. Night shots (`_night`) for the Final Arts check the other side.
- **Block and item particles** (`ParticleTypes.BLOCK`, `ITEM`) of ice draw as small dull cubes that read as clods of earth:
  Rime uses bright motes and snowflakes (`RimeArts.chips`). Earth chips are fine as cubes, but `ElementFx.crack` throws them
  straight up from all over the seal, through the swordsman's view when it's at their feet: `StoneArts.crackUnder` throws them
  low and outward.
- **The game test**: `WildercordArtsTest` husks keep their AI (so lifts and throws carry them) with no speed and no follow
  range; knockback scatters them, so `regroup()` puts them back between the first-person and third-person plays; the platform
  is refilled with air before each scene (Skate's ice, a Sunfall ring). `WILDERCORD_ARTS=a,b` plays only those (the Aura page
  always). Back up `src/gametest/resources/fabric.mod.json` before running a subset and restore it after.

### From step 4: Arts II

All ten methods have their five arts now (fifty); the player's view is `wiki/progression/sword-arts.md`, the rules and every
art's numbers DESIGN.md's "The methods' arts", the code map ARCHITECTURE.md's. The common arts (`PlaceholderArts`) are only for
an add-on's method with none of its own; the game tests play them with `gametest.TestMethods.plain()` ("Plain Breath").

**What each method is now** (an identity a later step should keep, and `ArtRulesTest` holds):
- **Verdant**: the least damage, the most mending, and roots (a root is a hold that leaves the foe turning: Slowness VII, under
  the player hold cap). Its fields (blossom, brambles, grove) mend allies and slow foes.
- **Hollow**: pulls (a single draw is a throw; a steady drag is held to `PVP_DRAG` on a player) and the only silence.
- **Starlit**: the only aura given back, and stars: a star an art sets on a foe (`ArtWards.star`, 5 s) bursts for more under
  the next Starlit art. The least control of any method.
- **Hourglass**: echoes (an art that repeats), a rewind, moments held still (Stopped Moment, Thousand Moments: holds under the
  player cap) and drag (time slowed round you, projectiles too). Second in control to Rime.
- **Crimson**: wounds that bleed on (`ArtKit.wound`, marked `BLEEDING`, half again while their bearer moves), drinking a share
  of what it deals, a frenzy (attack speed) and the only price in health. Second in damage to Ember, second in mending to
  Verdant.

**The decisions on the hard cases.**
- **Healing never outpaces danger.** Every art's mending and every drink goes through one bucket a body (`ArtKit.mend`): 10
  health at once at most, draining a health a second (`MEND_CAP`, `MEND_WINDOW`), so all arts together mend a body a health a
  second in a long fight. Mending is in health, not W: a better blade or a higher
  `art_damage` never mends more. A foe's art never mends a player its swordsman can't help (`helpable`).
- **Lifesteal never makes a swordsman unkillable.** Crimson's drinks are shares (a quarter to a half) held to a cap an art (3 to
  10 health), all in the same bucket as Verdant's mending: a Crimson swordsman lasts longer, and still dies to a pack.
- **Crimson Moon's floor**: the toll is a quarter of the greatest health, taken with `setHealth` (not damage: no armour, no
  totem, no death message) and never past a heart (`MOON_FLOOR` 2): at a heart or less it costs nothing more, so it can never
  kill. It's a gamble because it's paid before it lands: on a miss you're a quarter down.
- **Hourglass holds** go through `ArtKit.hold`: a player at most `PVP_HOLD_TICKS` 15 and not again for 80 (the same budget a
  freeze, a stun or a root spends). Thousand Moments still stores a player's blows taken while held, up to a weapon.
- **Silence** (`ArtWards.silence`): a creature 3 s, a player 1.5 s and not again for 5 s, a boss only interrupted. A silenced
  creature can't cast (`Statuses.silence`), a creeper's fuse goes out, a drawn bow is lowered. A silenced player is refused
  arts (`SwordStrings.Refusal.SILENCED`, with a line) and the Aura key but the guard (`Aura.press`), so they can still defend.
- **First person**: the spectacle shapes (a sphere, a moon, a column of light, a stopped clock behind a foe) are
  `ArtLight.spectacle`; the swordsman's own view keeps low, flat shapes (ground rings, faces on the ground, rays from 2.5 out).

**Final balance** (all fifty, `ArtRules.power`; the slot's worth 1.3 / 1.8 / 1.9 / 2.15 / 4.35, each art within 12%, each
method's five within 10% of every other's: 5.8% apart). The sums show who leads in what.

| Method | I | II | III | IV | V | Five | Damage | To others | Control (s) | Reach | Mends | Aura back |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Ember | 1.38 | 1.85 | 2.03 | 2.25 | 4.42 | 11.93 | **7.83** | 3.90 | 3.0 | 29.5 | 0 | 0 |
| Rime | 1.26 | 1.88 | 1.90 | 2.20 | 4.40 | 11.63 | 6.06 | 3.10 | **10.7** | 29.8 | 0 | 0 |
| Thunder | 1.26 | 1.74 | 1.96 | 2.07 | 4.26 | 11.28 | 5.31 | **5.10** | 3.8 | 43.0 | 0 | 0 |
| Gale | 1.42 | 1.78 | 1.82 | 2.25 | 4.30 | 11.57 | 5.22 | 3.40 | 9.1 | **44.0** | 0 | 0 |
| Stone | 1.30 | 1.77 | 1.90 | 2.16 | 4.49 | 11.63 | 5.90 | 3.70 | 8.0 | 36.5 | 0 | 0 |
| Verdant | 1.35 | 1.72 | 1.92 | 2.10 | 4.42 | 11.51 | 5.06 | 2.95 | 7.7 | 27.0 | **3.20** | 0 |
| Hollow | 1.29 | 1.78 | 1.95 | 2.17 | 4.54 | 11.73 | 5.76 | 3.55 | 8.9 | 38.0 | 0 | 0 |
| Starlit | 1.35 | 1.88 | 1.85 | 2.23 | 4.47 | 11.78 | 6.56 | 3.35 | 0.5 | 35.5 | 0 | **16.5** |
| Hourglass | 1.29 | 1.77 | 1.94 | 2.09 | 4.37 | 11.45 | 6.20 | 3.10 | 10.3 | 26.6 | 0 | 0 |
| Crimson | 1.27 | 1.86 | 1.88 | 2.09 | 4.24 | 11.34 | 7.75 | 3.25 | 0.4 | 27.8 | 2.26 | 0 |

(Damage and to-others in W; mending in W, a W about seven health; Crimson Moon's toll, 5 health on 20, is taken off its
worth.) How the new terms are weighed: mending at three fifths, as damage to the others (it doesn't always find a wound); aura
given back at a tenth of a W a point (Nova's up to 20 is 2 W at most, and it only comes back if it lands); a wound at what it
bleeds; a toll whole.

**The balance pass** moved eight step-3 arts: five that sat 5% to 7% under their slot once the fifty set the standard (Glacier
Mirror's freeze 2 to 2.5 s, Unmoved 0.9 to 1.0, Eye of the Storm 3.2 to 3.5 round, Skyfall's arcs 0.35 to 0.4, Hundred Winds 5
to 5.5 round and 0.22 to 0.24 a beat), and Gale's reach lead restored (Cutting Breeze 10 to 12 out and 0.5 to 0.45, Tailwind
11 to 13 blocks; Heaven's Spear 20 to 18), and Winter's Hush's shards now cut a foe once an art (two frozen foes side by side
were cutting each other's neighbours twice, beyond its worth). Cutting Breeze sits highest of the fifty in its slot (+9%): it
buys Gale's reach, and its damage is the slot's least.

**Hooks for step 5 (momentum, stance, finishers).**
- Every art blow goes through `ArtKit.Hits` (`strike`, `raw`; each returns what it took, `hurt(foe)` and `count()`): the one
  place to feed momentum ("arts that land") and wear down stance. Give `Hits` a hook rather than touching fifty performers.
  `AuraApi.onString` fires once an art is performed (with `StringContext.struck`), for momentum that counts arts, not blows.
- Stance by kind: `ArtRules.Art.kinds` says what an art does (`QUAKE` for Stone's heavy blows, `HOLD`, `ROOT`, `STILL`, `FREEZE`
  for holds); a stance weight could be derived from `primary` and the kinds instead of a new number per art.
- States a finisher can read: `ArtKit.rooted`, `ArtWards.stopped`, `silenced`, `starred`, `frenzyStacks`, `crusts`,
  `juggled`, `Reactions.has(foe, BLEEDING)` and `SHADOWED`. "Opened" should be another `ArtWards` state, cleared by `forget`.
- The Final Art's gate is `AuraApi.FINAL_GATE` (a full pool now): `gateFinalArts` swaps it for peak momentum in one place.
- Aura back for momentum or a finisher: `ArtKit.giveBack` (a tick later, after the art's own price; no multipliers or hooks,
  through `Aura.giveBack`). Healing from a finisher: `ArtKit.mend`, so the bucket holds it too.
- Each method's finisher should look its own: the new methods' looks are in `HourglassArts.clockFace`/`resume`/`gold`,
  `CrimsonArts.gash`/`drops`/`splash`, `StarlitArts.burst`/`starLook`, `HollowArts`' sphere (an orb over `ArtLight.shade`),
  `VerdantArts.rootsOn`/`bloom`.

**Gotchas from step 4.**
- **Vanilla particles draw with the wrong picture in the game test client** (a probe of each, side by side, in the arts
  test's world): block, item and crit particles (`ParticleTypes.BLOCK`, `ITEM`, `CRIT`, whatever block or item) all draw as
  the same small tan cube, and tinted leaves, the composter's specks, spore blossom and enchanted hits as white or pink rune
  glyphs; cherry leaves and happy-villager sparks draw as pale crystals. It looks like a particle atlas mix-up rather than how a
  player's game draws them, but it's unconfirmed outside the tests. The arts don't depend on it: they use the mod's own motes,
  dust and shaped light (`CrimsonArts.drops` for blood, `HourglassArts.resume` for time breaking, `VerdantArts.petals`, `leaves`
  and `impact`, `ArtBlocks.spire(..., false)` for a root or a trunk), which always draw right. `ElementFx.drip`, `bloodImpact`,
  `crack`, `petals`' leaves, `TimeFx.stasisRelease` and a spire's dust are best left out of new arts. The tan cubes round husks
  struck by a life method are the affinity cue's crits (`Affinities.notice`, "Weak!"), not the art.
- **No `ElementFx` (or anything that touches the particle registry) in a static field** of an arts class: `ArtRulesTest` loads
  the class without a bootstrapped game and fails with `ExceptionInInitializerError`. A dark colour is `0xRRGGBB | ArtLight.DARK`.
- **The strike's knockback carries foes out of a field or an echo's reach**: call `ArtKit.steady(foe)` after the strike when an
  art means to keep them where they are (Red Rain, Collapse, Echo Cut, Meteor Shower's small stars).
- **A root drawn at `foe.position()` hangs in the air** if a strike lifted the foe: draw it on `ArtKit.floor` below.
- **`SigilOption.CIRCLE` reads as a generic white rune circle by day**; give a method its own ground shape (Verdant's bloom
  and rings, Hollow's dark circle, Hourglass's clock face).
- **`SigilOption.GLOW` is always a billboard**, even through `ArtLight.ground`: a big one stands up facing the camera as a
  jagged glare, never a pool of light on the ground. Keep glows small (`flash`, `shade`); lay a field's bounds as rings.
- **A pale aura washes out** (Hourglass burns near white at high stages): draw its arts in `HourglassArts.gold(player)`.
- **Aura given back must land after the art's price**: `SwordStrings.perform` pays after the performer, so a refund in the
  performer lands in a full pool and is lost. `ArtKit.giveBack` waits a tick.
- **Long-lived light lingers into the next test scene** (a grove stands 8 s): `WildercordArtsTest.lingerTicks` waits it out,
  and `reset` kills block displays.
- **`ArtBlocks.live` leaked** when a display was removed early; `discard` now always counts it down.
- **Lambdas in loops** (a delayed tree, a staggered strike) need effectively final copies of the loop's values.
- **The arts game test now runs fifty scenes** (about ten minutes): use `WILDERCORD_ARTS=a,b` while working, and run the whole
  thing once before merging. Scenes that mend need a hurt ally (a tamed wolf, `wolf()`) and a hurt swordsman (`HURT`).

### From step 5: momentum and openings

The player's view is `wiki/progression/aura.md#momentum-and-openings`; the rules and every number are DESIGN.md's "Momentum and
openings"; the code map is ARCHITECTURE.md's. Pure rules: `aura.MomentumRules` and `aura.StanceRules` (unit-tested by
`MomentumRulesTest` and `StanceRulesTest`); runtime: `aura.Momentum`, `aura.Stance`, `aura.arts.Finishers`; client:
`client.StanceHud` and `AuraHud.momentum`; game test `WildercordMomentumTest`.

**Momentum, in numbers** (0 to 100). Builds: a clean hit 3.5 (critical 5), an art landing 6/8/10/10 by slot for its first foe and
1.5 for each of up to three more (the Final Art builds nothing), a perfect guard 12 (a shot or a spell 6), an Aura Step through an
attack 10 (once a step), a stance broken 8, a finisher 14, a worthy kill 3; all times the method's temper and `momentum_gain`, and
through `AuraApi.onMomentum` hooks (source "hit", "art", "guard", "step", "break", "finisher", "bloodied"). A hit taken knocks
off 12% to 40% (by its share of max health) plus 2, half through a held guard. It holds `GRACE` 80 ticks (a temper's own: Ember
60, Hourglass 160) after the last blow given or taken, then ebbs 8 a second (times the temper and `momentum_ebb`). Tiers 25, 50,
75, peak 95: price ×0.9/0.85/0.8/0.75, art strength ×1.05/1.1/1.15/1.2, stance worn ×1.1/1.2/1.3/1.45. The Final Art's release
spends 40.

**The momentum API** (`api.AuraApi`; reads are both sides, from the owner-only synced `Momentum.MOMENTUM`):
- `momentum(player)`, `momentumTier(player)`, `peakMomentum(player)`, `artPrice(player, art)`.
- `addMomentum(serverPlayer, amount, source)` (through the hooks, never past 100), `onMomentum((player, amount, source) -> amount)`.
- `holdMomentum(serverPlayer, floor, ticks)`: never below `floor` and no ebb until `ticks` run out; what's there is lifted to the
  floor at once; gains build above it; a hit still knocks some off but never below the floor while it holds. `ticks` 0 lets go.
- `openFinalArt(player -> bool)`: something besides the peak that opens every Final Art (both sides: read a synced flag).

**How step 6 (awakening) should use it.** On awakening, `AuraApi.holdMomentum(player, MomentumRules.PEAK, duration)` (momentum
stays at the peak, unebbing, for the whole awakening: arts at their cheapest and strongest, stance worn fastest) and
`AuraApi.openFinalArt(p -> <your synced awakened flag>)` once at init (the Final Art opens while awakened even if a hit took the
meter down, as it can't take it below the floor anyway). The Final Art's release still spends 40, but the floor keeps it at the
peak, so an awakened swordsman can play it again once its 30 s rest is over. When the awakening ends and the swordsman is spent,
call `holdMomentum(player, 0, 0)` and then either `Momentum.reset(player)` (spent: nothing left) or a big `Momentum.lose`; I'd
reset, since spent means "unable to gather aura for a while", and the meter emptying says so. "Arts cost nothing or little" is
not momentum's job: the one price function is `SwordStrings.price(player, art)` (both sides: the client's reader and the
server's check and payment all call it, and the Aura page shows it); multiply in an awakening factor there from a synced flag.
The body's glow already burns brighter at the peak (`AuraPresence.Look.momentum`, `AuraFxRules.momentumGlow`, +0.3 at the peak):
awakening's own state should add to `AuraFxClient.bodyIntensity` beside it.

**Stance, in numbers** (`Stance.STANCE` on any living entity, synced to everyone near, removed once whole again). Pools: a
creature 0.8 of its health, 20 to 100; sturdy (Runebound, `aura.world.AuraFighter`) ×1.25; a boss 0.3 of its health, 60 to 150,
×1.25 after each break up to ×2; a player 30; a training dummy 40. Wear: a full coated swing what it was dealt at (critical ×1.35,
glance or sweep ×0.35, no aura ×0.6), an art's strike ×2.0 (and ×1.6 for `QUAKE`, ×1.2 for `HOLD`/`FREEZE`/`ROOT`/`STILL`/
`SHOCK`, from `ArtRules.Art.kinds`), the slash or a spark ×0.5, a perfect guard 35% of the attacker's pool at once (a boss 20%),
Stone's blows ×1.25, times the striker's momentum and `stance_damage`; a boss takes ×0.7. Recovery after 60 ticks (a boss 30, a
player 40) at 20% of the pool a second (a boss 10%, a player 25%). Opened: a creature 60 ticks (held: `Spirits.hold`), a boss 40
(slowness III), a player 30 (slowness I, attack strength reset, shield cooldown, Aura Guard dropped and refused by `Aura.press`).
Steady after (nothing wears it): a creature 60 ticks, a player 100, a boss 200. Finisher: a full swing on an opened foe; extra =
a creature 35% of missing health (≤ 4 W), a boss 12% (≤ 2.5 W), a player 25% (≤ 8 × pvp_scale and ≤ 25% of max health), times
`finisher_damage`, added to the melee blow itself (`AuraCombat.blow`: armour, a totem, every defence apply); aura back 6 + 2 × stage
(Starlit ×1.5, practice ×0.25) through `ArtKit.giveBack`; momentum 14.

**The stance and finisher hooks** (for steps 7, 9, 10, 12):
- `AuraApi.onStance((attacker, target, wear, source) -> wear)`: every wear before it lands. Step 7's Way of the Bulwark ("stance is
  hard to break") answers a Bulwark *target* with less; the Shadowstep ("blows from behind open foes faster") answers an attacker
  behind its target (compare `target.getYRot()` with the direction to the attacker) with more. `StanceRules.Source` tells a blow
  from an art, a perfect guard (`GUARD`) or a held guard's catch (`GUARDED`).
- `AuraApi.onStanceBroken((attacker, target) -> ...)`: a stance broken (the Banner's rallying cry could hang off it).
- `AuraApi.onFinisher(new FinisherHook() { extra(...); landed(...); })`: `extra` changes what a finisher adds before it lands (the
  Way of the Blade: "finishers hit harder"; keep the player cap: `StanceRules.finisher` already held it, so scale and re-cap for a
  `Player` target), `landed` hears of every finisher landed with what it dealt (step 9's resonance, step 12's rune-etched blades
  waking on finishers).
- `AuraApi.registerFinisher(methodId, new AuraApi.Finisher(id, look))` gives an add-on's method its own; `commonFinisher(...)` the
  default; `finisher(methodId)`, `finishers()`. A finisher's name is `aura.wildercord.finisher.<id>` (and `.desc`).
- `AuraApi.wearStance(attacker, target, wear)`, `stanceLeft(entity)`, `opened(entity)` for an add-on's technique (step 8's
  "sunder" intent should call `wearStance` with a multiple of what its strike dealt, through `ArtKit.Hits` if it's an art: `Hits`
  already wears stance by the art's weight, so add only the intent's bonus).
- The momentum budget per foe and the "helpless" test (`Momentum.helpless`: no AI and not held, or riding a boat or a cart) are
  where step 11's aura beasts or tournaments might need exceptions.

**Gotchas.**
- **`Aura.java` has its own private class `Stance`** (the breathing stance's memory). Inside `Aura.java`, the openings' class is
  `dev.wildercord.aura.Stance`, written out in full.
- **Hits know their art by `SwordStrings.performing()`**, set only while a performer runs. A `Hits` made later (in a delayed task)
  has no art: its strikes still wear stance (as an art) but build no art momentum. All fifty arts make theirs at the top of their
  performer, and `PlaceholderArts` now strike through `Hits` too. Strength is the tier as the art began.
- **The finisher rides the blow.** `Stance.finisher` is asked in `AuraCombat.blow` before the blow lands (it adds the extra) and
  `Stance.finished` after (if it hurt; turned aside, the opening stands). A finisher skips the blow's ordinary `felt` impact (its
  own grand one plays) and the blow's stance wear.
- **A held foe is thawed by its finisher** (`FROZEN_UNTIL` at or before the opening's end is removed and NoAI lifted), or a lift or
  a throw in the finisher would leave it frozen in place till the opening's time ran out.
- **Ebb and recovery are worked out on both sides**: the server writes `Momentum.State` (with its own ebb a tick, so the server's
  `momentum_ebb` reaches the client) and `Stance.State` only on a change; `engaged` rewrites the hold only when it moves on by half
  a second. Don't tick them.
- **`Config.Sync` is full** (fourteen fields, `StreamCodec.composite`'s most). Step 5 added `combat`, a bitfield (`MOMENTUM` 1,
  `STANCE` 2): later client-side flags go in its free bits.
- **The stance bars are a HUD element** (`StanceHud`, projected over heads like others' banners): hidden with the HUD, not hidden
  by blocks. Only the nearest eight (and the crosshair's) are drawn. A mark rides higher over a player or a named creature (over
  its name tag), and is held at the top edge of the screen when a foe close in front has its head above it.
- **Other game tests' foes stand steady**: `WildercordArtsTest` gives every husk a stance steady for the whole scene (so no art
  check meets an opened, held foe); do the same (`Stance.STANCE` with `steadyUntil` far off) for a test about something else.
  Final Arts in tests need the peak: set `Momentum.MOMENTUM` to `new Momentum.State(100, now + 100000, 0, 0, 0)`.
- **Fabric's `FakePlayer` can't be hurt** (`isInvulnerableTo` is always true) and has no team: `WildercordMomentumTest.Rival`
  overrides both for a duel. To show it on the client, send `ClientboundPlayerInfoUpdatePacket.createPlayerInitializing` before
  `level.addNewPlayer` (and the remove packet after). It isn't ticked, so reset its invulnerability by hand between blows
  (`Effects.readyToHurt`).
- **In the test client** the Crimson finisher's drops (the mod's motes) draw as rune glyphs, as other tinted particles do (see the
  notes from step 4).

### From step 6: awakening

The player's view is `wiki/progression/aura.md#awakening`; the rules and every number are DESIGN.md's "Awakening"; the code map is
ARCHITECTURE.md's. Pure rules: `aura.AwakeningRules` (unit-tested by `AwakeningRulesTest`); runtime: `aura.Awakening`; looks:
`aura.AwakeningFx` (the frame) and `aura.arts.Awakenings` (each method's flourish and the awakened Dominion's `Ground`); client:
`client.AwakeningHud`, `AuraHud` (the mark), `AuraFxClient.awakenedForm`, `render.AuraBodyLayer` (the awakened form); game test
`WildercordAwakeningTest` (`WILDERCORD_AWAKENING=input,gives,moment,forms,methods,fed,final,spent,death,duel,dominions,pages` plays only those).

**The input (settled).** `AuraApi.Trigger.TAP_HOLD`, the Aura key's fifth way: a tap, then a second press within the double tap's 8
ticks held `AwakeningRules.HOLD_TICKS` (14). In `WildercordKeys.auraKey`: a second press after a tap (for a player with a TAP_HOLD
technique, so from Edge) is marked `auraSecond`; the tap held back for it (if it waited) goes with the pair; let go before `HOLD` (5)
it's a double tap (plus the tap it was where there's no step), held to 14 it's the TAP_HOLD (never a HOLD as well), let go between it
sends nothing. `tapsWait` is now "has a double-tap technique, or has a TAP_HOLD technique and `Awakening.ready`": an Edge swordsman's
slash waits 8 ticks only while an awakening is ready. Below Edge the key behaves exactly as before. **Changed behaviour**: a Sovereign's
"tap, then hold within 8 ticks" used to raise Dominion and then slash; now it's the awakening (or its refusal). The charge shows
(`WildercordKeys.awakeningCharge`, `AwakeningHud.charge`) from the second press's 4th tick (`CHARGE_SHOWS`), so a double tap never
flashes it. Step 10's challenge gesture should stay clear of all five: tap, sneak+press, double tap, hold, tap-then-hold.

**The state** (`Awakening.AWAKENING`, saved, synced to everyone near, copied on death): `State(phase, since, until, spentUntil, readyAt,
price, extended)`, phases `NONE`, `AWAKENED`, `SPENT`, `RESTING` (`AwakeningRules.Phase`); `awakened(now)` = phase AWAKENED and now
before `until`; `spent(now)` = phase SPENT and before `spentUntil`; `resting(now)` = before `readyAt`. `price` is the art price share
written as it began (both sides read it: don't read the config for it on the client). `extended` is what finishers have fed it.
`Awakening.tick` (from `Aura.tick`) ends it (pool emptied, momentum `hold(0,0)` then `reset`, Slowness I, the hooks), puts the spent
slow back if washed off, and moves SPENT to RESTING. A death sets RESTING (the rest runs from the death if it was burning). Momentum is
held again on a change of world and on joining (`holdAgain`).

**The API** (`api.AuraApi`): `awakened(player)`, `spent(player)`, `awakeningLeft(player)`, `awakeningRefusal(player)` (an
`AwakeningRules.Refusal` or null; both sides), `awaken(serverPlayer)` (every check, says why), `endAwakening(serverPlayer)` (ends it on the
next tick, spent after), `onAwakening(new AwakeningHook() { awakened(player, ticks); ended(player); })`. Reading only: `Awakening.state`,
`priceShare`, `damage(player, target)`.

**The numbers** (`AwakeningRules`, server-tunable in the `aura` section):

| What | Default | Config |
|---|---|---|
| Opens at | Edge | |
| Asks | a pool 95% full, momentum 50 (none if the server's momentum is off) | `awakening_momentum` |
| Lasts | 240 / 320 / 400 ticks at Edge / Form / Sovereign | `awakening_duration` (×) |
| Fed | 20 ticks a finisher, 80 at most | |
| Arts cost | 0 × their price (after momentum's discount) | `awakening_art_price` |
| Momentum | held at 95 (the peak) for the whole of it | |
| Coated blows | ×1.15 (×1.075 against a player, then the player cap and `pvp_scale`) | `awakening_damage` |
| Speed | +10% movement (base), +10% attack speed (total) | `awakening_speed` |
| Spent | 600 ticks: pool emptied at the end, Slowness I, `Aura.gain` and `Aura.giveBack` refuse everything | `spent_seconds` |
| Rest | 3600 ticks from its end (the spent time inside it) | `awakening_cooldown_seconds` |
| Awakened Dominion | radius ×1.5, time ×1.5, weaken +0.1, chain 2, flow ×2.5 (Starlit 3) | |

**Interactions decided.** The Final Art opens while awakened (`openFinalArt(Awakening::awakened)`) and is free; its release spends 40
but the hold lifts momentum straight back to the peak; its 600-tick rest outlasts a fully fed Sovereign awakening (480), so it comes at
most once per awakening (`AwakeningRulesTest.theFinalArtComesAtMostOncePerAwakening` holds it: keep it true if you change either).
Finishers come faster (peak stance wear) and each feeds the awakening (an `onFinisher` hook). Nothing else multiplies: a finisher's
extra, art strength and every PvP cap are as step 5 left them.

**For the later steps.**
- **Step 7 (Ways)**: natural hooks. The Blade: `onFinisher` already feeds the awakening; a Blade node could raise `AwakeningRules.extend`'s
  share (add a hook rather than editing the constant). The Bulwark: the spent state is the weak point, a node could soften the slow
  (`Awakening.tick` puts Slowness back: make it ask a hook). The Banner: `onAwakening.awakened` is where a rallying awakening could
  `holdMomentum` nearby allies for part of it. Any Way that gives aura must remember `Aura.gain` refuses it while spent (by design).
- **Step 8 (techniques)**: a technique from a string source is priced through `SwordStrings.price`, so it's free while awakened too.
  If techniques should cost something awakened, give `StringArt` a flag and check it in `SwordStrings.price`.
- **Step 9 (bonded blade)**: `onAwakening` for resonance; the awakened body look is `AuraBodyLayer`'s, a bonded blade's glow could
  read `Awakening.awakened(player)` on the client to blaze too.
- **Step 10 (sparring, the clash)**: a spar should probably end any awakening (`endAwakening`) or forbid it; decide there. Two
  awakened swordsmen clashing is a natural spectacle moment.
- **Step 12 (Unity)**: letting mana and aura feed each other must respect the spent state (no aura in while spent).

**Gotchas.**
- **`Config.Sync.combat` now carries more than switches**: bit 4 `AWAKENING`, and bits 8 to 15 the momentum an awakening asks for
  (`Sync.awakeningMomentum()`). Later client-side flags go in bits 3 to 7 (8, 16, 32, 64, 128); don't use bits 8 and up.
- **Rings racing out from a swordsman's own feet sweep across their first-person view**: everything the burst throws out over the
  ground is `ArtLight.spectacle`; only still marks on the ground (a cracked seal, a clock face) are `ArtLight.world`. The first run of
  the game test caught it (`awakening_burst_fp`).
- **The body haze no longer grows past a full flare** (`kh`, 1.15 in `AuraBodyLayer.body`), the head's haze is softer where eyes burn,
  and corona tongues in front of the face burn at 45%: a brighter body (a surge, momentum, awakening) blazes in its flames, never as a
  glare over the face. Add later looks the same way.
- **An awakened Dominion draws its own ground, not the rune circle** (`AuraVfx.dominionRise(..., circle)`): the generic circle drowned
  every method's shape. Its `Ground` keeps one `ArtKit.Hits` made at the raise, so its strikes have no art (no art momentum, stance
  worn as an art's, one PvP cap for the whole Dominion).
- **The tap-and-hold needs the tap to wait at Edge**: if `Awakening.ready` disagrees between client and server (a server with odd
  settings mid-sync), the tap goes as a slash and the awakening is refused for the pool. That's why momentum is refused before the pool:
  the line names what was really missing.
- **Fabric copies copy-on-death attachments in its own `AFTER_RESPAWN` listener, after ours**: a respawn handler that changes such an
  attachment on the new body must register in `Aura.AFTER_COPY` (a phase ordered after the default), or the copy undoes it. The
  awakening's death rule and the old "a new body starts with its aura empty" rule both do now; the latter had been silently undone
  since 0.9 (the death scene of `WildercordAwakeningTest` caught it).
- **In the game test**: `AuraApi.awaken` goes through every check (give a full pool and `Momentum.State` at 60+); clear with
  `calm()` (`endAwakening`, two ticks, then remove the attachment and the effects, refill the pool); an awakening burns the pool to 0
  when it ends. FakePlayer rivals aren't in the player list, so `Awakening.tick` never ends theirs and their `AuraAttachments.LOOK` must
  be set by hand to be drawn.

### From step 7: Ways

The player's view is `wiki/progression/ways.md` (a page of its own beside Sword Arts); the rules and every number are DESIGN.md's
"Ways"; the code map is ARCHITECTURE.md's. Pure rules: `aura.WayRules` (unit-tested by `WayRulesTest`); runtime: `aura.Ways` (the
state), `aura.Crossroads` (the choosing), `aura.WayEffects` (the Blade's, the Bulwark's and the Shadowstep's nodes), `aura.WayBanner`
(the Banner's, allies, and the steadying in the damage path), `aura.CrossroadsIncense` (changing); client: `client.WayHud`, the
`AuraScreen` Way tab; game test `WildercordWaysTest` (`WILDERCORD_WAYS=crossroads,called,moments,blade,bulwark,shadowstep,banner,incense,death,pages`),
and `WildercordShaderTest` films the standards under its pack (`*_aura_crossroads`).

**What was decided, and why.**
- **The choice is the crossroads**: standards of light rise round the swordsman 50 ticks after the Edge breakthrough and they **strike**
  one (lean), then strike it again (walk it). Strikes are swings the server saw (the punch), tested against each standard's axis along
  the look (`WayRules.strike`). Not a menu: the blade chooses, in the world, seen by everyone near, and two strikes keep a swing at a mob
  from choosing for you. The Aura page's Way tab can be read while it stands.
- **Swordsmen already at Edge or above** call the crossroads by holding the **breathing stance** 60 ticks after it settles (never during
  a stillness trial); their **first choice is free** and gives every node they've reached at once. They're told on joining.
- **Changing** is a **Crossroads Incense** (two Aura Shards, an amethyst shard, blaze powder) burned at a **place of power**
  (`way_change_at_power`), then **settling**: the new Way's Form node waits on half of `way_settle_xp` (240) experience earned walking
  it, its Sovereign node on all of it (counted before the stage's cap). The old Way is gone at once.
- **The twelve nodes** (DESIGN.md has the table): two Ways change **awakening** at Sovereign (Blade: free, quick slashes; Shadowstep:
  free, quick steps, double afterimage) and two change **Dominion** (Bulwark: a bastion that turns shots at its edge; Banner: a shelter
  for the party). At Edge two change the **guard** (Bulwark: every side, longer perfect, throws back, turns shots; Shadowstep: a perfect
  guard slips behind the striker), one the **slash** (Blade: pierces) and one **finishers** (Banner: a rallying cry). At Form one changes
  **finishers** (Blade: Cascade), one the **step** (Shadowstep: a striking afterimage) and two **Intent** (Bulwark: challenges foes off
  allies; Banner: steadies allies while it presses).
- **Allies** are `WayBanner.ally` (the rule chorus casting keeps: never duellists against each other; teammates; or two who couldn't harm
  each other either way, `canHarmPlayer` both ways and the mod's `Targets.canHarm`). Pets share in steadying (`Targets.canHelp`).
- **Steadying** (cries, presence, shelter, an unbroken Bulwark) is one store per body (`WayBanner.steady`, the strongest holding) plus
  the shelter and the Bulwark, combined in `WayBanner.harm` (`WayRules.steadied`, capped at 0.3; against a player's harm times the PvP
  scale), applied in `mixin.LivingEntityAuraMixin` after Dominion's weakening and before the guard. Only harm a foe deals (a source
  entity): never falls, the void, hunger.
- **Balance** is `WayRules.WORTH` (each node in shares of a swordsman's strength, the reasoning beside each) held by `WayRulesTest`:
  alone, Blade, Bulwark and Shadowstep within 15%; the Banner the quietest alone (about 57% of their mean) but within a fifth of them
  with one ally and leading with two; each Way leading its own term (offence, defence, mobility, support). If you change a node's
  numbers, change its `Worth` and its reasoning with it.

**The Way API** (`api.AuraApi`; reads are both sides, from the synced `Ways.WAY`):
- `registerWay(new Way(id, color, List.of(new WayNode(nodeId, stage), ...)))`, `ways()`, `way(id)`: an add-on's Way gets a standard at
  the crossroads (up to six; a plain pillar in its colour) and a column on the page (the plain emblem `aura/way_unknown`: `AuraScreen.emblem`
  only knows the built-ins' sprites, so an add-on's own emblem needs a lookup there). Lang: `aura.wildercord.way.<id>`, `.creed`,
  `.short`; `aura.wildercord.way_node.<node>`, `.passive`, `.change`.
- `wayOf(player)`, `hasWayNode(player, nodeId)` (walked, the node's stage reached, awake after a change), `wayNodeState(player, way, node)`.
- `chooseWay(serverPlayer, wayId)` (an add-on's own rite; owes the settling after a change), `unbindWay(serverPlayer)`,
  `openCrossroads(serverPlayer)`, `onWay(new WayHook() { chosen(player, way, first); unbound(player, way); })`.
- **A node's effect is its Way's own code.** The built-in nodes ask `Ways.has(player, WayRules.X)` where the technique lives (see
  ARCHITECTURE.md's list), or go through the hooks (`onMomentum`, `onStance`, `onFinisher`, `onAwakening`, `onGain`). An add-on's node
  does the same through the public hooks and `hasWayNode`.

**For the later steps.**
- **Step 8 (your own techniques)**: Ways are a natural **source of technique parts** ("from Ways" is already in the plan). Give each
  built-in Way a part (a stroke, release or intent) learned once the Way's Edge node is theirs (check `hasWayNode`, not the Way, so a
  change of Way and its settling apply), and decide whether a part is kept after a change of Way (say so in the guide). Parts that fit:
  Blade, an intent *pierce* (the slash's own); Bulwark, an intent *ward* or a release *on the guard*; Shadowstep, a release *afterimage*
  (reuse `AuraStep.Stepped.linger` and `WayEffects.afterimageStrikes`); Banner, an intent *rally* (`WayBanner.steady`, `company`). A
  technique's strikes through `ArtKit.Hits` already meet the Shadowstep's from-behind stance (an `onStance` hook); the Blade's momentum
  boost answers only "hit", not "art". The Way tab could list the parts a Way gives.
- **Step 9 (bonded blade)**: a trait "drawn from how its wielder fought (most-used arts, Way, method)": read `Ways.state(player).way()`
  (and `former`, `changes` if the blade remembers a past Way). `onWay` hears of choices and unbindings.
- **Step 10 (masters, sparring, the clash)**: **the clash** meets the Blade's pierce: today a piercing crescent wins a crescent clash
  outright (`Crescents.clash`: the winner flies on at `BLADE_CLASH_CARRY`). When the clash becomes a timed struggle, decide whether the
  pierce becomes an edge in it (a wider window, a head start) rather than an automatic win, and move that rule out of `Crescents.clash`.
  **Sparring**: keep Ways in a spar (they're how people fight), but if spar partners aren't `Duels` duellists, add the spar to
  `WayBanner.ally` so partners never shelter or steady each other. The Bulwark's challenge and stagger never touch players. A Way is
  chosen, not taught; a master could raise the crossroads for a disciple at a ceremony (`AuraApi.openCrossroads`).
- **Step 11 (world)**: sword tombs' intent gates or a tournament could ask for a Way; a shrine of the four Ways could be a place to change
  Way once without an incense (`Ways.unbind` then `Crossroads.open`). Aura beasts: the Shadowstep's "behind" reads a creature's `yBodyRot`;
  a beast that doesn't turn its body to its target would count every blow as from behind.
- **Step 12 (mage and swordsman)**: the Banner's allies are chorus casting's allies, so a Banner beside a mage is the same "together"; a
  resonant strike "from one player or two" can ask `WayBanner.ally`. Unity must respect the spent state (the Banner's aura share goes
  through `Aura.giveBack`, which refuses a spent ally).

**Gotchas.**
- **`Crossroads` and `Ways` hold attachments**: a unit test that loads them fails (`AttachmentRegistry` needs the game). The numbers, the
  voices (`WayRules.voice`, `WayRules.SOUNDS`) and the balance live in `WayRules` for that reason.
- **A perfect guard's slip skips the stagger's knockback** (`AuraGuard.stagger(player, attacker, knock)`): thrown back after the swordsman
  slipped behind it, the striker landed on top of them. The slip's spot is found before the stagger (`WayEffects.slipSpot`).
- **"The perfect moment answers once"** moves the guard's raised time back past the window: with the Bulwark's longer window that's
  `now - WayEffects.perfectWindow(player) - 1` (all three places in `AuraGuard`).
- **`Momentum.add` multiplies by `momentum_gain`**, so a share of what was built is divided by it before it's added to an ally (or the ally
  gets the gain twice). Shares never share again (source "banner" is skipped). The aura share uses `Aura.giveBack` for the same reason.
- **The standards are server-sent shaped light**, redrawn every 4 ticks with a 9-tick life; what was sent lingers up to 9 ticks after a
  crossroads closes. In first person all four fit a 16:9 view (24 degrees apart, 4.2 blocks out); the Banner's pennant flies toward the
  middle of the arc so it never leaves the view. A lean's big flash is spectacle; the swordsman's own view gets a small one.
- **In the game test the third-person camera turns the player**: set the player's yaw to face the foes before they act (a swing, a
  slash, a step), and turn the camera only after (a slash's flight is fixed as it's loosed). FakePlayer allies and rivals need scoreboard
  teams for the ally rule (`board.addPlayerToTeam`); the test's `reset` drops them and takes the swordsman off any team.
- **The Way tab hides the method's passive footer** to make room for a node in full; keep node texts to about three lines at the page's
  width.

### From step 8: techniques of your own

The player's view is `wiki/progression/techniques.md` (a page of its own beside Sword Arts and Ways); the rules and every number are
DESIGN.md's "Techniques of your own"; the code map is ARCHITECTURE.md's. Pure rules: `aura.TechniqueRules` (unit-tested by
`TechniqueRulesTest`); runtime: `aura.Techniques` (the book, the string source, writing, ranks, payloads), `aura.arts.TechniqueArts`
(the performance), `aura.ScrollSources` and `aura.world.TechniqueScrollItem` (scrolls); client: `client.TechniquePage` inside
`AuraScreen` (the Writing tab); game test `WildercordTechniquesTest`
(`WILDERCORD_TECHNIQUES=page,played,releases,intents,elements,ranks,scrolls,slots,awakened,fair,lent`).

**What was decided, and why.**
- **A technique is a string art of the player's own** (`AuraApi.addStringSource`, ids `technique_1` to `technique_3`), so the reader,
  momentum, stance, finishers, awakening (free while awakened), the banner, the string HUD and `onString` all take it with no special
  case. Its name reaches every place an art is named through `AuraApi.artName` (a namer asked before the lang key).
- **Parts**: six strokes (thrust, rising cut, falling cut, sweep, spin, draw), four releases (on the blade, wave, burst, afterimage),
  seven intents (pierce, sunder, bind, echo, ward, rally, infuse). **Innate from Edge**: draw, on the blade, infuse, so a swordsman can
  write their first technique the moment they reach Edge. **Infuse** is the "or others" intent that makes the element the point; **ward**
  and **rally** are the Bulwark's and the Banner's and only a Way lends them.
- **Balance is the arts' own model**: `TechniqueRules.model` builds an `ArtRules.Art` and `ArtRules.power` weighs it; price and rest are
  worth × 4.32 aura and × 43.2 ticks (the arts' mean rate, a shade under), at Tempered with its temper and edge, held to 3..13 aura and
  30..130 ticks, nudged at most 2% by the string's effort. `TechniqueRulesTest` checks every combination at every rank, temper, edge,
  method and string sits inside the arts' own worth-per-aura and worth-per-tick band. **If you change a part's numbers, rerun it**: the
  band is tight (0.2065..0.2536 W an aura) and the test names the combination that leaves it.
- **The element is in every technique** (`Flavour`, by the method's passive), each worth about a seventh of a W, so methods sit within 8%.
- **Ranks are their own**, not spell mastery: a technique has no mana or cast to measure. Same shape as mastery (real foes, the moment,
  repetition, a capped practice share). Raw 0, Honed 60 (a temper), Tempered 200, Keen 520 (an edge), Peerless 1300 (inscribe).
  Strength ×0.965..1.04. Records (up to 12) survive erasing.
- **Ways lend, never give**: a Way's part is known while `Ways.has(player, its Edge node)` (so a change of Way and its settling apply);
  leave the Way and the part goes, and a technique written with it rests (its swings play the arts). Pierce and afterimage can also be
  found on scrolls; ward and rally can't.
- **Names** are cleaned on both sides (`TechniqueRules.cleanName`) and always shown as `Component.literal`: never parsed, so nothing a
  player types can format, click or translate on another player's screen. The page filters typing to `nameCharacter`.
- **Strings**: 2 to 5 swings, at least one mark, weight at least 3; never `clash` with an art or another technique
  (`AuraApi.conflicts` and `TechniqueRules.clash`); an `overlap` is allowed and the page says which goes first, except one an art's
  counter or step would only meet in passing (`incidentalCue`).

**The technique API** (`api.AuraApi`; reads are both sides, from the synced book):
- `techniqueParts(family)`, `knowsPart(player, part)`, `partsOf(player)`, `writtenTechniques(player)`, `techniqueRank(player, slot)`.
- `teachPart(serverPlayer, part, sourceId)`: a part for good, with the line, the sound, the Grimoire (`aura:technique_part`) and the
  hooks; false if known. `techniqueScroll(part)` makes the item.
- **Scrolls for step 11's sword tombs**: `registerScrollSource(new ScrollSources.Source(id, lootTable, chance, weights))` adds a loot
  table (the `wildercord:random_technique_part` loot function, its chance a percent times `technique_scroll_chance`), or a source in code
  with `lootTable` "" drawn with `drawScrollPart(sourceId, random)`. **`sword_tomb` is already registered** (spin, burst, afterimage, sunder
  and echo favoured): a tomb's chest can be a loot table with `{"function": "wildercord:random_technique_part", "source": "sword_tomb"}`
  in a pool, or code calling `drawScrollPart("sword_tomb", random)` then `techniqueScroll(part)`. A guardian could `teachPart` outright.
- `onTechnique(new TechniqueHook() { written(player, slot, technique); ranked(player, slot, technique, rank); learned(player, part, source); })`.
- `registerTechniqueIntent(TechniqueRules.Intent.own(id, factor, control, area), effect)`, `intentEffect(id)`: an add-on's intent,
  priced by what it says it's worth; learned only through `teachPart` (never on a scroll).
- `nameArts(namer)`, `artName(player, art)`: any per-player art can be named the same way.

**For the later steps.**
- **Step 9 (bonded blade)**: resonance from techniques comes through `onString` (the art id starts with `technique_`; `Techniques.written(
  player, art)` gives the `Written`: its parts and name) and from `onTechnique.ranked` (a Peerless technique is a story worth telling in
  the blade's tooltip: "Ember Fang reached Peerless on this blade"). A trait "drawn from how its wielder fought" can read the most-used
  technique's parts (a blade that thrust and pierced its whole life). Keep the bonded blade's damage bonus out of `TechniqueRules`: the
  technique's blow is a share of the blade (`ArtKit.Hits`), so a stronger blade already carries every technique.
- **Step 10 (masters and disciples)**: a master can **teach a part** (`teachPart(disciple, part, "master")`) at the ceremony; a
  **Peerless** technique's parts already set down on scrolls (`Techniques.inscribe`), the in-world way to pass one on, and a master's
  could cost nothing. A spar is the natural place for technique experience to count less (it's not danger): `Techniques.Use` reads
  `AuraCombat.moment` and the foe's worth; a spar partner should be worth less, as a training dummy is (`TechniqueRules.practice`).
  **The clash**: a technique's wave is not a `Crescents` flight (it strikes in bands from `TechniqueArts.wave`), so it doesn't clash today;
  if the timed clash should take waves too, move the wave onto `Crescents` or give the clash a hook.
- **Step 11 (world)**: see the scroll API above; the parts hardest to find elsewhere are the tomb's favourites. An intent gate can ask
  `techniqueRank(player, slot) >= TechniqueRules.KEEN` as well as a stage. Tournaments could award a scroll of the winner's choosing.
- **Step 12 (mage and swordsman)**: a technique is an art (aura off the blade, `ArtKit.Hits`, its element), so a resonant strike that
  answers "an art" answers techniques too; `onString` tells them apart. A rune-etched blade's rune waking "on arts" wakes on techniques
  unless it checks the id.

**Gotchas.**
- **`Techniques` and `TechniqueArts` hold attachments and the scheduler**: a unit test that loads them fails; the numbers, names, strings
  and balance live in `TechniqueRules` for that reason (and `TechniqueRules.registerIntent` leaks between tests: the tests skip `:` ids).
- **The client's string source reads only synced data** (the book, the method, the stage, the Way's node through the synced `WAY`): the
  client predicts which technique a string plays and draws its rest from `SwordStringsClient.PREDICTED`; anything the server knows and the client doesn't
  would make the HUD lie.
- **An unknown part rests the technique, it doesn't break it**: `Techniques.usable` is the string art's `available`, so its swings fall
  through to the next art that fits (`StringReader` already does that). A part an add-on removed shows "write it again" on the page.
- **The momentum a technique builds** is the art slot nearest its worth (`TechniqueRules.momentumSlot`, `Momentum.artLanded`).
- **Thunder's spark waits** until the stroke has struck all it will (`TechniqueArts.Cast.spark`; a wave's at its last band), or it leaps
  to a foe the stroke strikes anyway.
- **In the game test**: husks need knockback resistance 1 (a draw's knockback pushes them out of reach and the next swing misses), the
  swordsman must wait for full attack strength after a weapon change before the first full swing, the third-person camera at yaw 0 and
  pitch 12 keeps the crosshair on the husks, and an afterimage directly ahead is hidden behind the swordsman in third person (step back
  and aside before it strikes). `§` can't be typed through the client (it filters it), so the forged-name check writes server-side.
- **The writing page's readout** shows five lines at most; a longer one ends in "..." and shows in full on hover. Keep new element and
  intent lines short.

### From step 9: the bonded blade

The player's view is `wiki/progression/bonded-blade.md` (beside Aura, Ways and Techniques); the rules and every number are DESIGN.md's
"The bonded blade"; the code map is ARCHITECTURE.md's. Pure rules: `aura.BladeRules` (unit-tested by `BladeRulesTest`); the bond on the
blade: `aura.BladeBond` (the `wildercord:bonded_blade` component); the world's record of live bonds: `aura.BladeRegistry` (SavedData
`bonded_blades`); runtime: `aura.BondedBlades`, `aura.BladeCeremony`, `aura.BladeTraits`; client: `client.fx.BondGlow`,
`client.BladeTooltip`, `client.BladePage` (the Aura page's Blade tab); game test `WildercordBondedBladeTest`
(`WILDERCORD_BLADE=ceremony,glow,resonance,page,traits,dropped,death,theft,pass,tooltip`).

**What was decided, and why.**
- **The bond lives on the blade** (a data component: tier, resonance, name, trait, offer, origin, history, lineage), so the glow, the
  tooltip and the page read it on any client with no extra sync, and the blade is the same blade in any chest or hand. **The world's
  registry** (bond id to owner, live or not) is what makes it safe: a copy whose bond isn't live, or isn't its holder's, is ended or sent
  home on the next scan. The swordsman's own `blade_bond` attachment holds a `Shown` copy of its look for the page while it's away.
- **Weapons**: `wildercord:bondable_blades` = swords, axes, spears and the mace (aura-forged gear included, all of it swords, axes or
  spears); never the trident (thrown and left lying). One of it, and it must wear.
- **Ceremony at a ley crossing** (a place of power; `bond_at_power` off allows anywhere): the breathing stance with the blade in the main
  hand, ten seconds, three parts (kindling, joining along the two lines' real bearings from `LeyLines.directions`, sealing) and the
  seal. It runs **before** the Way crossroads in the stance (`Crossroads.breathing` waits while `BladeCeremony.busy`), so at a crossing
  with a bondable blade in hand the bond comes first; a crossroads already raised stops a bond starting.
- **Resonance only with the blade in the main hand, only on worthy foes**, everything but the kill capped per foe (`foeCap`), nothing in
  practice, a player's fall once a day. Tiers wait on the stage (Named Edge, Awakened Form, Soulforged Sovereign and a boss felled by the
  blade): resonance keeps gathering while one waits.
- **Kept through death** whatever the gamerule or curse; **never breaks** (stops at its last point); **only its swordsman's** on the
  ground (pickup, hoppers, mobs, despawn, fire, lava, blasts, the void), in a chest (`Slot.mayPickup`) and in anyone else's hands
  (slips home at once; offline, the registry holds it until they join). A stranger holding it has no aura weapon and no trait. **No
  recall**: a blade is only ever moved, never copied or summoned, so there's nothing to race or dupe. Anvils, grindstones and the repair
  recipe can't consume it, no crafting recipe takes it as an ingredient (the rune recipes ask for axes and swords) and no furnace or
  stonecutter takes it; a smithing upgrade or forging carries every component, so the bond goes with it. A new recipe type of the
  mod's own that consumes weapons must refuse bonded blades too (`BondedBlades.bonded`).
- **Names** suggested from the mod's own word banks by the blade's story (element, Way, place, foes, favourite art), cleaned exactly as a
  technique's name and shown as a literal; an anvil can't rename it.
- **Traits** (13): drawn from the blade's history by habit (each with a floor: no habit, no trait), leaned by method and Way, three
  offered, first choice free, changes 10 levels, one free change at Soulforged; small and even (2.5 to 4.5% of a swordsman's strength),
  fair to players (at most 3%, never more than alone; Mountainfeller, Gravewarden and Rallying Steel never touch a player).

**The bond API** (`api.AuraApi`; reads are both sides, from the component):
- `onBlade(BladeHook)`: `resonance(player, amount, source)` (return the amount: change or veto it), `bonded`, `tiered`, `named`,
  `traited`, `released(player, bondId)`, `passed(from, to, blade)`.
- `bondBlade(player, hand, how)`: bond at once for a rite of your own (every check but the ceremony: Edge, a bondable blade, no standing
  bond); `how` is the story's word ("ceremony", "drawn from the rock").
- `releaseBlade(player)`, `addResonance(player, amount, source)` (only while they carry their own blade, through `resonance_gain` and
  the hooks), `registerBladeTrait(BladeRules.Trait)` (a namespaced id; what it does is your code asking `bladeTrait(player)`).
- Reading: `canBondBlade(stack)`, `bladeBond(stack)`, `bondedBlade(player)`, `bladeTier(player)`, `bladeTrait(player)`.
- Passing: `allowBladePassing(rule)`, `mayPassBlade(master, disciple)`, `bladePassingRefusal(master, disciple)`, `passBlade(master, disciple)`.

**The passing contract for step 10 (what step 10 must provide).**
- **Register who may receive a blade, once at start-up**: `AuraApi.allowBladePassing((master, disciple) -> <disciple is this master's>)`.
  That's all the built-in ceremony needs. Nothing passes until a rule says yes (an operator can: `/wildercord aura blade pass <player>`).
- **The built-in ceremony** (`BladeCeremony`, 8 seconds): the master holds the breathing stance with their own bonded blade in the main
  hand; the disciple sneaks within 2.75 blocks; the two face each other (flat look dot ≥ 0.5 both ways). Moving, standing up, the
  disciple leaving or turning away, or a rule changing its mind breaks it. Both see the countdown on the HUD (the `blade_rite`
  attachment is synced to everyone).
- **Or step 10's own ceremony**: check `bladePassingRefusal(master, disciple)` (a language key or null; refusals: nobody, bonds off, no
  blade with the master, the disciple already bonded, a disciple with no aura, a rule's no) and call `passBlade(master, disciple)` at its
  end (server thread; the rules still apply). It moves the blade from the master's inventory into the disciple's (their hand if empty,
  else any slot, else at their feet, still theirs), rebinds the registry, writes the master into the lineage and a "passed" deed, plays
  the moment and calls `BladeHook.passed`.
- **A passed blade sleeps** below its tier's stage (`BladeRules.effective`): a Flow disciple's Soulforged blade gives nothing (a faint
  vein) until they reach Edge (Bonded and Named), Form (Awakened) and Sovereign (Soulforged). Step 10 can say so in its own UI.
- **Spars must not grow blades**: a spar partner is a player (worth `MasteryRules.PLAYER`) and their arts, guards and stances broken would
  feed resonance (capped per foe, but still). Either make `AuraCombat.worth` 0 for a spar partner (which also keeps technique ranks and
  mastery honest) or return 0 from `BladeHook.resonance` while a spar is on. No spar kills, so the once-a-day kill rule never comes in.
- A master teaching a part at the ceremony (step 8's notes) and passing the blade are separate calls; do both if you like.

**For the later steps.**
- **Step 11 (the sleeping blade)**: when it's drawn, put the blade in the hand and call `AuraApi.bondBlade(player, hand, "drawn from the
  rock")` (the blade must be in `bondable_blades`: a custom sword in `#minecraft:swords` already is). A swordsman already bonded must
  release first (`bladeBond`/`bondedBlade` to check; say so in the world). To give it a head start, `addResonance(player, n,
  "sleeping_blade")` after bonding (the tiers still wait on the stage). A fixed name: `BondedBlades.rename(player, name)` (public; cleaned).
  **Old battlefields** can feed a carried blade through `addResonance` (keep it small and once per place). **A tomb's guardian** counts
  toward Soulforged only if `Spirits.isBoss` says it's a boss (a `DungeonBoss` does); its kill then gives the boss bonus (30) and a deed.
- **Step 12 (rune-etched blades)**: a bonded blade is an ordinary stack with components, so a rune component rides along. The anvil mixin
  refuses a bonded blade as the **right** input and reverts a rename, nothing else: etching with the blade on the left keeps its bond.
  A rune waking "on finishers" can read `bladeTrait` for a combination; keep the trait numbers in `BladeRules` and rerun
  `BladeRulesTest`'s balance test if a rune changes what a trait touches.

**Gotchas.**
- **`Config.Sync.combat` bits 32 and 64 are taken** (bonds, blade traits); 128 is the last free one in that byte.
- **Fake players aren't scanned** (`BondedBlades.tick` walks the server's player list) and aren't ticked: a fake player holding someone's
  blade keeps it until `BondedBlades.scan(fake, registry, true)` is called by hand (the game test does); what it holds reaches the
  client only if sent by hand (`ClientboundSetEquipmentPacket`), and its sneak never shows there at all (neither an entity-data packet
  nor setting the client entity's pose made it kneel), so the passing pictures show the disciple standing. Step 10's two-player
  tests with real clients won't have this problem.
- **`Slot.mayPickup` runs on the client too**: `BondedBlades.mayTake` says no on the client for anything not the viewer's own, so the
  client never predicts a take the server refuses.
- **`ignoreSwapAnimation` on the component**: without it every once-a-second resonance write replays the raise in first person.
- **The kept blade comes back in `Aura.AFTER_COPY`**, after Fabric's copy-on-death attachments: anything that clears a new body's
  inventory must run before that phase.
- **An art's later strikes** don't answer through `AuraCombat.landed` (only its first on each foe does), so a kill by an art's second
  blow is counted by `ArtKit.Hits.raw` through `BondedBlades.artFelled`.
- **In the game test**: a draw's knockback can throw a husk out of reach even with full resistance; set a foe squarely in front before
  the felling blow. The ceremony's front view needs a negative pitch to look down on the ring. A tier moment's spectacle is filtered by
  the camera when it arrives, so switch to third person before the tier is reached.

### Step 10, completed: masters, sparring and timed clashes (2026-10-02)

The two commits preserved on `aura-step10-wip` were rebased onto main in `aura-step10-complete`. The rules/server work is now joined by the client controls, authored sounds, player text, physical visuals, guide and interaction suite.

- **Clashes:** `AuraSocialClient` receives Begin/Mark/End, turns the real Attack key into a Press during a lock, and shows three converging tick pairs, authoritative grades/scores and the outcome clear of the crosshair and vanilla action bar. Shorter viewports place the compact panel above the aiming area. Extra or out-of-window presses fumble. The server applies the same bounded connection allowance when judging a press and closing its window, then waits for the last allowed delayed packet before resolving. It retains authority over participants, timing, scores and carried damage.
- **Spars:** reciprocal blade-use salutes, count-in, one-heart knockouts, free concession by leaving, timeout, outside-harm cancellation, restoration and per-pair experience limits use `Duels.Watcher`. Four cloth standards and fracture/gouge impressions mark the bounds; the new spar visuals do not draw ritual circles. The small standards remain visible to their owner in first person; only the overhead Rallying Cry standard is suppressed there.
- **Lineage:** cancellable master/disciple and lesson ceremonies, nearby ordinary-experience bonus, saved offline shares, master spar trials, graduation and release are wired into the stage/experience/blade systems. Releasing a bond cancels its active lesson. Interrupted and disabled ceremonies clear both progress attachments.
- **The Aura page:** a sixth Lineage tab, bounded names, nearby/online markers, master and next lesson, spar records, paginated disciples/honoured graduates, and a five-second two-click release confirmation. Existing tabs retain their controls.
- **Sound/text/guide:** 21 authored social sound events, human-readable HUD/command/refusal messages, and `wiki/progression/lineage.md`, exported into GitBook. The Aura guide points to the new clash rules.
- **Cleanup:** expired clash pair cooldowns and art-answer records are pruned even while other clashes remain active; salute cooldowns expire after offers disappear. Client cues expire after resolution and clear on disconnect.

Verification uses `dev.wildercord.aura.AuraSocialTest` with a real integrated server/client and a fake second swordsman. It exercises breathing/sneak consent and cancellation, an actual taught technique part, the real release packet and Attack-key clash presses, forbidden spell/projectile damage, safe knockout and restoration, outside-creature interruption, experience and records after meaningful fighting, all three clash kinds, losing art payment, one-time winning art release, answering-art damage restoration, offline shares, saved-data round-trip, graduation, and offline release. The unit rules test timing windows/fumbles, connection allowances, daily spar caps, mentor eligibility and rewards. Targeted Ways and bonded-blade regressions accompany the suite.

The fake player does not receive ordinary player ticks: its damage cooldown is reset explicitly between bouts. This is not a two-client network-latency measurement; the allowance race is covered by deterministic timing tests. Physical ground marks fade without excavating terrain. Steps 11 and 12 remain planned.


## Visual revision: cloth standards and rune-free Aura (2026-10-02)

The crossroads and Rallying Cry use `AuraFx.Standard` and the client `AuraStandard` renderer. Each built-in Way has its own
woven cloth texture and crest, drawn on a double-sided animated cloth grid with bronze poles. Refresh cues update the existing
standard rather than piling up light-outline particles. The live cache is capped at 48 and cleared on disconnect. The rightmost
crossroads standard flies inward; the owner cannot see the small rally standard in first person. Cloth reduces its mesh detail on calm/off settings and at distance; the choice stays visible. It uses vanilla translucent
blending so it remains legible in daylight and follows the shader-compatible particle path.

Technique-name banners use a stitched nameplate, a Way emblem and bounded names. The generator is `tools/aura_standard_art.py`,
called by `tools/aura_art.py`. Add-on Ways use the neutral crest tinted in the supplied Way colour.

Aura's rune-bearing CIRCLE and RING requests become partial slash strokes, and STAR motifs become crossed pressure glints.
Dominion's default boundary uses fractured ground instead of `Sigils.ground` spell seals. `ArtLight` routes ground marks to physical
scars to keep future arts consistent. Ordinary rune magic retains its casting circles. No damage, costs, hit volumes, Way selection
rules or stage progression changed.


### Physical ground effects (2026-10-02 revision)

The owner clarified that Aura should break and cut the floor instead of painting magical light. `AuraFx.GroundScar` now carries
fracture (0), crater impression (1) and blade gouge (2) cues. `client.fx.AuraGroundScar` projects authored non-emissive textures onto
nearby solid block tops, samples world lighting, skips changed blocks and unsupported air, emits fragments of the actual floor,
and fades the marks without deleting terrain. Its cache is capped at 24, with an eight-block radius and 600-tick lifetime cap;
refreshes do not emit debris again. Calm/off settings reduce or omit fragments. The textures come from `aura_standard_art.py`.

`ArtLight.ground` and `groundRing` now use physical scars, and airborne ring requests become sharp partial slash strokes. The default
Dominion's light column and ground rings are removed; its boundary is fractured ground. AuraBurst's luminous RING/ECHO rendering is
removed; those cues now produce floor scars. Heavy impacts add blade gouges. Ground-facing Hourglass motifs use gouges instead of
a glowing clock face. Duelist arrival and duel ground cues use the same physical effects. This supersedes the pressure-band ground
rendering used during the initial visual revision. Combat mechanics and actual terrain-changing arts retain their existing rules.

The Dominion screenshot pass also found indirect spell seals inside shared elemental helpers. Aura now calls `arts.AuraPhysicalFx`
for elemental impacts, frost creep, clock strokes and pressure rings. Each method keeps distinct cuts and timing while using floor
fractures, gouges or crater impressions. The shared helpers remain available to actual spells. Floor block and lighting samples are
cached and refreshed every five ticks rather than sampled on every rendered frame.

### Step 11 progress: terrain training (2026-10-02)

`aura.world.TrainingGrounds` samples a dry waterfall bank or exposed mountain summit while breathing. Falling columns need six blocks within three blocks; summits require the mountain biome tag, sky, altitude sea+64 and eight relief samples. The weak-keyed cache lives at most as long as its player, refreshes in 40 ticks, invalidates on stance break/movement/dimension change, and never requests unloaded chunks. Server settings are in aura_world; older callers/configs retain defaults.

Recovery is multiplied 1.25 by default and each full breath earns 0.5 practice inside the existing lifetime 40-point cap. No new repeatable idle progression loop. The early STILLNESS trial accepts these terrain grounds; TEMPEST still requires the ley crossing. TrainingRulesTest covers habitat boundaries and practice caps; TrainingGroundsTest covers real fluids, mountain biome/relief, actual sneak input, rewards, interruption and breakthrough.

At this terrain-training checkpoint, battlefields, sword tombs/guardian, sleeping blade, Aura beasts and tournaments were pending. Battlefield progress is recorded below. Step 12 remains pending. The broader goal is tracked in LIVING_WORLD_ROADMAP.md.

### Step 11 progress: the Marchkeeper battlefields

OldBattlefieldPiece adds three rotated outdoor layouts on gentle, dry terrain; worldgen spacing is 64 chunks, separation 24, with open-country biome tags. They are not in the dungeon monster/ward tags. Generated memorial block entities persist their provenance; an ordinary placed marker cannot award a memory. Server setting aura_world.battlefields gates new placement and interactions without removing existing ruins.

Battlefields listens only to requested player rites, validates breathing/blade/range/dimension/view/marker throughout eight seconds, and cancels on interruption. The discovery key is per player and tradition, using the saved Grimoire. Three traditions teach Sunder, Bind and Echo and give three different portable written books (nine short pages). Repeated visits retell all pages without rewards and are rate-limited. Four memory sounds and generated memorial/book models share each tradition's crest. Rites clear on disconnect/stop without ticking every memorial or loading remote chunks.

BattlefieldsTest covers all three layouts in four orientations, generated provenance save/load, actual client use packets, cancellation, teaching, finite books, new-marker rejection, the real reading screen and normal-terrain /place generation. BattlefieldRulesTest and configuration tests cover terrain, stance bounds and migration. Sword tombs, the sleeping blade, Aura beasts and tournaments remain pending.
