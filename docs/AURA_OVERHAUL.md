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
| 6 | Awakening | planned |
| 7 | Ways | planned |
| 8 | Your own techniques | planned |
| 9 | The bonded blade | planned |
| 10 | Masters, disciples, sparring and the clash | planned |
| 11 | The world of the sword | planned |
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
  by blocks. Only the nearest eight (and the crosshair's) are drawn.
- **Other game tests' foes stand steady**: `WildercordArtsTest` gives every husk a stance steady for the whole scene (so no art
  check meets an opened, held foe); do the same (`Stance.STANCE` with `steadyUntil` far off) for a test about something else.
  Final Arts in tests need the peak: set `Momentum.MOMENTUM` to `new Momentum.State(100, now + 100000, 0, 0, 0)`.
- **Fabric's `FakePlayer` can't be hurt** (`isInvulnerableTo` is always true) and has no team: `WildercordMomentumTest.Rival`
  overrides both for a duel. To show it on the client, send `ClientboundPlayerInfoUpdatePacket.createPlayerInitializing` before
  `level.addNewPlayer` (and the remove packet after). It isn't ticked, so reset its invulnerability by hand between blows
  (`Effects.readyToHurt`).
- **In the test client** the Crimson finisher's drops (the mod's motes) draw as rune glyphs, as other tinted particles do (see the
  notes from step 4).
