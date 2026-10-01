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
| 1 | Sword strings: the input language | planned |
| 2 | Feel and spectacle: the shared visual and sound language | planned |
| 3 | Arts I: the framework, and Ember, Rime, Thunder, Gale, Stone | planned |
| 4 | Arts II: Verdant, Hollow, Starlit, Hourglass, Crimson | planned |
| 5 | Momentum and openings | planned |
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
