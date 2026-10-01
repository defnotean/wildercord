# WilderCord: gameplay, fixes, performance and expansion audit

Reviewed September 30, 2026. This is an audit of the current repository, including its uncommitted Cinnamon, casting presentation, Fusion Altar and test changes. Findings below identify work to do; they do not claim those fixes have already been implemented.

**Historical audit:** this records the repository before the authorized implementation. See [implementation progress](implementation-progress.md), [the implementation changelog](2026-09-30-implementation-changelog.md) and [the later physical-magic expansion](2026-09-30-physical-magic-expansion.md) for current changes and verification. The suggested items below have since been implemented as gameplay systems; external multiplayer, broad shader and launcher validation limits are reported in the current ledger.

## Evidence and limits

The review inspected production casting, visual effects, companion management, fusion rules, defensive gear, targeting, scheduling, networking, temporary blocks, dungeon pieces, build configuration, CI, and focused tests. It also inspected the saved beam formation and first person release images.

Existing JUnit XML reports contain 452 tests with zero failures or errors in both build directories. The latest complete client log ends in `BUILD SUCCESSFUL in 35m 59s`. There are 48 client test entrypoints. An entrypoint count is not an executed-test count: Cinnamon and the authored-animation gallery return immediately without their environment flags; shader checks return immediately without Iris. These conditions must be reported as skips in future results.

No new performance benchmark or multiplayer play session was run for this audit. Performance risks below come from code structure, rather than measured FPS or server tick regressions. All timing goals are proposed acceptance criteria.

The repository builds a Fabric mod for Minecraft 26.3 and Java 25. A distributable modpack manifest was not found in the reviewed file list. A modpack profile and its dependency compatibility therefore remain separate work.

## What is already valuable

- The plain Java compiler is shared by the server and spell readouts. This is a strong foundation for honest mana costs and descriptions.
- Casting has creature, block, segment and link-depth limits. Repeating shapes deliberately receive fresh per-strike hit allowances.
- Scheduler already groups tasks by due tick. Replacing it with another global queue is not the first performance priority.
- Fx and Sigils build a particle packet once for its recipients rather than allocating one per player. Particle range and eye clearance checks already exist.
- Network requests are throttled; backpack state is written through and the open bag is protected against movement.
- Spell defence already includes armour contribution, Warding, Warded, Resolve, a bonus cap, and spellguard. Resolve has an outgoing-power tradeoff.
- All 288 registered shape/effect runes already have explicitly authored three-beat sequences and illustrated emblems. RuneChoreographyTest checks complete-sequence uniqueness. The remaining presentation gap is integration into the complete firing sequence, rather than a complete absence of individual rune artwork.
- Fusion supports 55 element recipes, 16 named signature recipes, and exact effect pairs woven with an amethyst block. Existing tests iterate eligible woven pairs.
- The original dungeon bosses use interactions rather than just extra health. That design is worth extending.

## Prioritized fixes

### F1 — First person release still obscures aim

**Priority: high. Evidence: screenshot and code.** The saved `magic_beam_fire_wind_first_person_release.png` shows bright rings, flames and pale particles across the central aim area. Moving the main circle behind the player did not remove every foreground visual. Vfx still creates beam-origin circle/ring layers, and some foreground formation objects are intentional.

**Fix:** distinguish the caster's formation circle, the outgoing spell object, and the impact display. Put formation glyphs behind the shoulders; draw the outgoing object far enough ahead and with a controlled silhouette. Cull decorative geometry against the local camera rather than only checking its anchor distance. Preserve other players' view of the effect.

**Acceptance:** first person clips of every shape at normal and wide FOV, aiming level/up/down, moving and turning; target silhouettes remain readable during release. Use screenshots to check framing and video to assess smooth motion.

**Sources:** `cast/Vfx.java:216`, `cast/Vfx.java:373`, `cast/Sigils.java`, the saved beam release image.

### F2 — Formation still shares eight generic motion branches

**Priority: high. Evidence: code.** Vfx.castCircle switches on Motion, which groups many shapes into eight families. Individual rune panels already have authored gestures, but those panels do not give every complete spell a unique gathering, release and travel silhouette.

**Fix:** author a release specification for each of the 39 registered shapes. Reuse drawing primitives while giving each shape its own timing, path and geometry: for example, Arc should curve before release, Ricochet should segment, Cluster should visibly separate, and Glaive should form a returning blade. Apply effect identity to those objects instead of placing unrelated emblems beside a generic shot.

**Acceptance:** a 39-shape gallery showing formation, release, travel and impact; checks for distinct complete specifications; representative element and modifier combinations for every shape.

**Sources:** `cast/feel/Motion.java`, `cast/Vfx.java:273`, `spell/RuneChoreography.java`, `spell/RuneChoreographyTest.java`.

### F3 — Formation and actual aim can disagree while turning

**Priority: high. Evidence: code path; dynamic reproduction still needed.** Formation captures look and focus when the cast starts. The engine obtains the caster's current position and look when the scheduled release runs three ticks later. Some delayed visuals mix the old focus with the current eye position. Moving or turning during that interval can disconnect formation from the shot.

**Fix:** define a release pose containing position, direction and dimension, and consistently use it for visuals and delivery. Choose either tracked aim until release or a committed aim at the initial press, then expose that behaviour consistently.

**Acceptance:** sprint, jump and turn rapidly during casts; the formed object and the actual shot share one origin and direction. Check scrolls as well as Cord casts.

**Sources:** `cast/Vfx.java:216`, `cast/SpellCaster.java:245`, `cast/CastEngine.java:48`, `content/SpellScrollItem.java:134`.

### F4 — Release callbacks need the full cast lifecycle guard

**Priority: high. Evidence: code path; portal regression still needed.** New release callbacks check removal and life but not the original dimension. Cast.alive already checks dimension and cancellation. Ordinary engine segments use it; SecretSpells.cast has no equivalent entry guard, and WildSurge can directly act on the current player level. Decorative formation tasks also retain captured world positions without that guard.

**Fix:** use cast.alive at every delayed entry, including secrets, echoes and wild surges. Bind formation cancellation to the same lifecycle. Define the mana and scroll-consumption policy when a paid formation is interrupted; do not introduce implicit refunds that can be farmed.

**Acceptance:** death, disconnect, portal use and explicit cancellation between formation and release leave no gameplay effect in either dimension. Include secret spells and forced wild surges.

**Sources:** `cast/Cast.java:310`, `cast/SpellCaster.java:246`, `cast/SecretSpells.java:84`, `cast/WildSurge.java`, `content/SpellScrollItem.java:134`.

### F5 — Generic supporting particles do not resolve fused ingredients

**Priority: medium. Evidence: code.** Vfx.theme(Group) collects each effect's single element tag. A named fused rune carries one such tag even when its recipe has two elements. Its specialized impact visuals may already express the fusion, but the new generic formation/travel support path does not recover the second ingredient from Fusions.recipeFor.

**Fix:** resolve visual ingredients through the fusion recipe and retain order/roles. A fire/wind fusion should consistently show flame as material and wind as motion across formation, travel and impact. Expand dynamic woven contents where needed and keep modifier behaviour visible.

**Acceptance:** compare named fusion, exact woven pair, and two separate effects for the same elements. All preserve both identities without accidentally duplicating damage or particles.

**Sources:** `cast/Vfx.java:126`, `spell/Fusions.java`, `spell/WovenRunes.java`, `cast/feel/Feels.java`.

### F6 — Cinnamon forgets sitting after recreation

**Priority: medium. Evidence: code.** CinnamonDog.shouldBeSaved returns false; the companion manager discards her on logout or dimension replacement and creates a fresh body. Her sit state is not stored independently.

**Fix:** persist companion state separately from her replaceable entity. Store ordered sitting, an anchor dimension/position and any future cosmetics. Decide whether a sitting dog stays at her anchor or follows across dimensions; avoid quietly resetting player choices.

**Acceptance:** sitting survives logout/rejoin and server restart. Cross-dimension behaviour follows the chosen rule and never creates duplicates.

**Sources:** `pet/CinnamonDog.java:92`, `pet/CinnamonCompanion.java:65`.

### F7 — Cinnamon needs safe spawning and rescue behaviour

**Priority: medium. Evidence: code.** Automatic spawning uses player position plus one block on X/Z without checking floor, collision or hazards. Damage immunity does not prevent falling below the world or being trapped. The current test covers generic damage and direct interaction, rather than these travel hazards.

**Fix:** find a safe nearby position and defer spawning when none exists. Rescue her from the void or prolonged entrapment. Keep recovery compatible with sitting. Add clear feedback when an unconfigured summoned Cinnamon has no owner.

**Acceptance:** cliff edges, boats, narrow rooms, lava, underwater travel, unloaded areas and the End void; exactly one companion, no stuck body and no unwanted owner commands.

**Sources:** `pet/CinnamonCompanion.java:61`, `pet/CinnamonDog.java`, `gametest/WildercordCinnamonTest.java`.

### F8 — Cinnamon's localization is missing from the asset generator

**Priority: high for release. Evidence: source and CI.** en_us.json has a hand-added Cinnamon entry. write_lang builds a fresh dictionary and rewrites that file without a Cinnamon entry. CI regenerates assets and fails on changed JSON. Committing the current tree without addressing this creates a reproducibility problem.

**Fix:** make the generator own Cinnamon localization and document how her texture is regenerated. Update player-facing mod metadata, whose description still says 186 runes and one dungeon. Reconcile the outdated dungeon guide with the added ruins.

**Acceptance:** regeneration leaves committed generated output unchanged; Cinnamon stays localized; metadata and feature guides match the actual roster.

**Sources:** `tools/generate_assets.py:430`, `tools/generate_assets.py:727`, `.github/workflows/build.yml`, `src/main/resources/fabric.mod.json:6`.

### F9 — Test runs need explicit pass/skip coverage

**Priority: high. Evidence: code and existing reports.** Cinnamon requires WILDERCORD_CINNAMON_SUITE; the rune animation gallery requires WILDERCORD_ANIMATION_GALLERY; the shader test needs Iris. The all-rune compilation test proves basic usability in representative roles, not all interactions, fairness or appearance. CI currently builds and runs JUnit but has no real-client job.

**Fix:** publish a suite manifest of executed, passed, failed and skipped scenarios. Separate required mechanical checks from optional long screenshot runs. Add release gates for multiplayer, dedicated server startup, save/reload, regeneration and shader compatibility. Record visual review decisions separately from assertions.

**Acceptance:** a normal release report cannot call a skipped companion/gallery/shader check a pass. Keep the full local client run, but use smaller reliable CI gates where graphics automation is practical.

**Sources:** `gametest/WildercordCinnamonTest.java:26`, `cast/RunicAnimationGalleryTest.java:32`, `gametest/WildercordShaderTest.java:47`, `spell/EveryRuneCompilationTest.java`, `.github/workflows/build.yml`.

### F10 — Spellguard alone cannot establish fair combat

**Priority: high for balance validation; no exploit reproduced in this audit.** Defence has substantial protection, including a lethal-hit guard. It does not by itself answer multi-hit kill windows, simultaneous enemies, repeated silence/freeze, displacement into hazards, or large defensive stacks. Interrupt already has an eight-second gap; extending that to a coherent control policy deserves testing rather than assuming it is absent everywhere.

**Fix:** build a matchup matrix for control, burst, sustain and escape. Measure effective damage and time without player control. Apply diminishing duration or brief resistance to repeated hard control if testing demonstrates lockouts. Make protective triggers and their recharge readable in the HUD.

**Acceptance:** two players of equal progression can recognize danger and make a defensive decision after the first hit. Test a fully warded defender too, so stacking does not create near-invulnerable stalemates.

**Sources:** `cast/SpellDefence.java`, `cast/SpellDefenceRules.java`, `cast/Statuses.java`, `gear/GearDef.java`.

### F11 — Sound limit can reset when dimensions alternate

**Priority: low/medium. Evidence: code.** Fx stores one current voiceLevel and voiceTick. Alternating sound calls between dimensions during the same tick clears that single map each time, so the documented per-level limit is not consistently maintained.

**Fix:** keep a per-level counter for the current tick and clear it on level/server teardown. Profile the small bookkeeping cost rather than retaining levels indefinitely.

**Acceptance:** interleave calls from two dimensions and verify each still enforces three copies of a sound per tick.

**Source:** `cast/Fx.java`, voiceFree.

## Performance work to add

### Establish measurements before tuning

Use a fixed seed, fixed spell loadouts and consistent camera positions. Measure server tick time, frame time, allocations, scheduled task peaks, entity queries, particles and bytes sent per recipient. Capture idle exploration, a single cast, a dungeon boss, repeated area spells, and cooperative combat with 2/4/8 casters. Compare the same scene with vanilla rendering, Sodium, and the chosen shader profile.

Suggested goals are stable 20 TPS with p95 ticks under 50 ms in the declared supported load, and a chosen client frame target on documented hardware. Report p95/p99 and spikes, not only average FPS. Do not advertise eight-player performance until it has been measured.

spark documents CPU and allocation profiling and local profile saving. A profiling build should use a verified compatible Fabric version; exact 26.3 availability was not established in this audit. For local capture, start profiling, reproduce the scenario, then stop with --save-to-file. This avoids automatically publishing a profile.

### P1 — Send visual events and reconstruct detail on clients

Today many visual primitives still generate separate server particle sends and scan level.players for recipients. Packet reuse helps allocation, but does not remove this work.

Send a deterministic visual event with spell/shape id, pose, element roles, seed and timestamps; reconstruct decorative strands and motes on each client. Keep hits, target selection, block changes and damage entirely on the server. Batch compatible events and use tracked/range-limited recipients.

Acceptance: identical server combat results; lower packet counts and server visual CPU in the same stress scene; no random divergence of important telegraphs.

### P2 — Add client visual quality settings

Offer Full, Balanced and Minimal with separate settings for own spells and other players' spells. Reduce decorative motes, trail length and circle detail with distance. Preserve collision-relevant projectiles, boss warnings, hostile area boundaries and defensive signals in every profile. Provide reduced flashing and camera shake settings independently.

Acceptance: Minimal materially reduces frame spikes in crowded fights while players still understand incoming attacks. Quality changes must not silently change gameplay timing.

### P3 — Bound decorative tasks and work per source

Scheduler already handles due ticks efficiently. The remaining risk is too much work becoming due at once, especially with many elements, chained effects or area hits. Add counters and per-caster/world budgets for decoration first. Consider separate limits for persistent spell objects and scheduled gameplay work only after profiling; apply deterministic game rules rather than dropping damage at random.

Acceptance: repeat-heavy stress spells have documented maximum object/task counts; no cross-player starvation; decoration reduction leaves effects intact.

### P4 — Reuse compiled plans with correct invalidation

SpellHud already caches compiled readouts. Server casting reads/compiles active runes for each press. Profile compilation before adding a bounded server cache keyed by active rune identities/ranks and registry version. Keep live gear, affinity, status, charge and mana checks outside a stale cached decision. Invalidate on edit, loadout, unlock or add-on changes as needed.

Acceptance: edit/cast, Cord swaps, missing add-ons and dynamic woven runes remain correct; reduced allocation or CPU is measured.

### P5 — Reduce repeated spatial scans

Cinnamon performs a 48-block nearby entity scan every second for her configured owner. Replace most of that polling with spawn/load/remove registration, retain an occasional repair scan, and avoid parsing the owner UUID repeatedly. Profile area spells' overlapping queries before caching same-tick candidates. Preserve hit budgets and target movement correctness.

Acceptance: no duplicate companions and no missed new summons; fewer entity queries in the same scenario.

### P6 — Build three tested distribution profiles

- Performance: Fabric, WilderCord and a verified Sodium dependency; no shader pack; conservative render/simulation distance.
- Balanced: the same base plus the tested visual settings, ordinary draw distance and optional reduced shaders.
- Cinematic: a tested Iris/Sodium pair and a specific shader configuration; retain Balanced as fallback.

Start comparisons around 10-12 render chunks and 6-8 simulation chunks, then tune against measured hardware and server size. These are proposed starting points, not existing installed profiles. The Gradle -Xmx2G setting applies to Gradle and is not the game's runtime heap setting. Size the game heap from actual live memory and GC behaviour.

Sodium's 26.3 release exists, but its release note warns Iris was not yet compatible at publication. The repository pins an Iris version; treat the actual pair as a dedicated compatibility gate rather than assuming its validity from the version strings alone.

## Additions that would make the game more fun

| Addition | Concrete play experience | Guardrail / reason to prioritize |
|---|---|---|
| Spell practice room | Preview a spell against moving dummies, read damage/control, save and compare builds | High value immediately; also provides a repeatable performance scene |
| Fusion recipe preview and discovery book | Show shard versus block outcomes, exact input pair, XP and missing requirements before Fuse | Makes existing deep fusion mechanics easier to discover |
| Focus of Reprieve | Convert part of one large spell hit into delayed, visible damage | Delayed damage remains real; cooldown, power tradeoff and strict damage accounting |
| Grounding focus | Spend stored charge to resist a launch or pull, then release a short escape gust | Creates an answer to control; competes with other focus choices |
| Mirror-thread mantle | A timed defensive action softens a spell and redirects a weak fragment | Skill-based response with a telegraph and cooldown; no automatic full reflection |
| Emberweave / Rimebound / Stonebound armour | Fire rewards movement, frost rewards timing, earth trades mobility for stability | Sidegrades with unique silhouettes; cap stacking and test full sets |
| Rootbound Maze guardian | Prune roots to open routes, restore plants for cover, expose a guardian's heart | Develops an existing ruin into a distinctive encounter |
| Storm Spire conductor encounter | Ground rods, redirect arcs, choose between safe platforms and risky chain attacks | Uses verticality and storm/wind mechanics already present |
| Clockwork crypt | Pause mechanisms and decide where stored attacks land when time resumes | Keep preview clear; never require a single rare rune to progress |
| Living greenhouse expedition | Heal corrupted plants, grow paths, then decide whether to cleanse or harvest the heart | Gives support/world spells objectives beyond damage |
| Moving sky ruin | Wind movement, shifting platforms and a visible destination | Performance and motion tests first; falls have a recovery route |
| Dungeon room variants and optional routes | Different patrols, puzzles and treasures on repeat expeditions | Increase replayability before adding many fixed copies |
| Dungeon relic sidegrades | Unlock new spell handling, such as one ricochet or a directional ward | Avoid flat damage escalation and keep ordinary loot valuable |
| Cooperative rituals | One player holds a seal, another shapes the effect, a third interrupts threats | Solo alternative required; visible contribution and shared rewards |
| Rune research board | Set small experiments and earn hints or cosmetics for trying new combinations | Complements current Grimoire/contracts instead of duplicating grind |
| Spell build library | Compare revisions, tag utility/combat/travel, share previews and practice safely | Build on existing loadouts and spell codes |
| Magical home projects | Enchanted lamps, gardens, temporary bridges and weather instruments | Extend existing world magic with reliable ownership and cleanup |
| Familiar specializations | Choose scouting, control or support behaviour with visible actions | Offer roles rather than universal damage upgrades |
| Cinnamon personality | Petting, curl-up sleeping, occasional head tilts, owner greeting and a favourite toy | Cosmetic and companion-focused; keep immortality and ownership reliable |
| World event aftermath | A mana storm leaves temporary discoveries, altered enemies or research clues | Gives existing events consequences and a reason to return |
| Optional spell trials | Weekly dungeon rules or challenge arenas with cosmetic rewards | Clear opt-in rules; preserve normal progression |

Art should be reviewed as a roster. New equipment needs consistent palette, material shading, ornament density, icon readability and worn proportions against the original items. Add neutral-light turntables and inventory comparisons to the review suite, not only spell screenshots. Cosmetic spell skins may vary presentation but must retain their recognizable hit and area silhouettes.

## Suggested implementation order

1. Release reliability: generated localization, explicit test skips, cast lifecycle, safe companion spawning/state.
2. Casting readability: camera-safe outgoing geometry, one pose policy, per-shape release specifications, fused ingredients.
3. Measurement: practice/stress scene, counters, CPU/allocation captures and compatibility profiles.
4. Targeted performance: client-reconstructed decoration, visual quality settings and measured query/cache changes.
5. Combat depth: validate matchups, add a small defensive sidegrade set, and compare their artwork together.
6. Replayable content: deepen Rootbound Maze and Storm Spire, add room variants and unique relics, then expand into new expeditions.

## External references checked for performance recommendations

- [spark command documentation](https://spark.lucko.me/docs/Command-Usage): profiling, allocation mode and local save option.
- [Sodium 0.9.2 for Minecraft 26.3](https://github.com/CaffeineMC/sodium/releases/tag/mc26.3-0.9.2): release availability and its Iris compatibility warning at publication.
- [Iris release listing](https://github.com/IrisShaders/Iris/releases): consulted for shader dependency review; the pinned 1.11.6 version was not confirmed by the fetched listing.
