# September 30 audit implementation

This records the gameplay, reliability, performance and content work requested after the audit. Changes remain uncommitted. The original audit describes the findings before these changes; `implementation-progress.md` records current verification status.

The later expansion is detailed in [physical magic and custom presentation](2026-09-30-physical-magic-expansion.md): nine new runes, original material sprites/motion, two-to-eight-effect weaves, innate imprinting/ownership and multi-hit spellguard protection. Its roster of 338 named runes and 77 named fusions supersedes the initial pass's counts below.

## Casting presentation and reliability

- Formation circles assemble behind the caster. A smaller focus forms ahead of the caster, below the aiming line, before the shape releases.
- The rear circle is emitted on its first client render tick even when the packet's initial age is one, preventing packet/tick timing from skipping that stage.
- It follows the caster's position and facing until it fades, so a rapid turn does not leave the old rear anchor in front of the camera. Opposite-facing anchors are not interpolated through the player's view.
- All 39 shapes have explicit formation specifications. Self, projectiles, rings, beams, rain and world effects keep their own release geometry rather than all becoming a continuous beam.
- The client follows the caster's aim during the assembly stage. Outgoing rays avoid a wide ring directly over the first-person camera; their start geometry is cropped and narrowed for the local first-person view.
- Named fusions, exact woven pairs and Knots expose their actual ingredient materials. Fire and wind formations therefore combine their material details rather than merely picking one colour.
- A bounded formation packet replaces repeated server sends for the formation detail. Payloads validate list sizes, string lengths and finite scales. Large Knots are flattened for visual ingredients without changing their gameplay sequence.
- Delayed release, echo, secret, wild and scroll callbacks verify that their caster remains alive and in the original dimension. World changes clear client formations.
- Sound duplicate counters are kept separately per level, preventing alternating dimensions from resetting the sound limit.
- Generated descriptions and rune metadata were corrected; the README now distinguishes 329 named runes from dynamic woven pairs and Knots.
- A Minecraft 26.3 mouse-button migration error was found while testing the native notebook. Fusion Altar clicks and the Cord's right-click search action now use Minecraft's named mouse constants. Interaction tests also use those constants, so they exercise actual left/right clicks rather than reproducing the obsolete numeric values from the screen implementation.

## Combat choices

### Focus of Reprieve

The existing Focus slot, or an otherwise available off-hand fallback, can hold this focus. A sufficiently large spell hit defers 35% of its damage into four installments, beginning after one second. The initial hit still lands. Deferred damage can kill, persists when the item is removed or the player logs out, and does not receive Wildercord's defence contribution twice. The original damage type and available attacker attribution are retained, so applicable vanilla armour, enchantments, Resistance and absorption continue to work. An eight-second recharge and 10% outgoing power penalty make this a handling choice rather than free protection.

### Focus of Grounding

The first substantial spell impulse is reduced by 70% and grants a two-second escape gust. It recharges for eight seconds, reduces outgoing spell power by 10%, and cannot have its recharge reset by removing and re-equipping it. Gravity-well pulls now use the common impulse helper too.

### Elemental armour and mantle

Three complete sets add twelve normal armour pieces, with their own item icons and worn textures:

| Set | Response | Cost |
|---|---|---|
| Emberweave | Protection while sprinting and actually moving | No stationary bonus |
| Rimebound | A brief guard when beginning a crouch | Holding crouch cannot renew the window |
| Stonebound | Passive spell and impulse resistance | Movement penalty per piece |

Mirror-thread Mantle occupies the chest slot. A timed crouch softens one hit and returns a small fragment; the fragment has a strict cap and cannot recursively reflect. Ordinary set protection is capped and combined with the existing defensive rules. The equipment remains enchantable through vanilla armour tags.

### Repeated silence

Player casting locks have a two-second maximum duration. Another seal cannot refresh that lock. A two-second recovery window follows, allowing the player to cast despite another silence hit. This applies to the shared Silence/Manaburn casting-lock helper; it is not a blanket immunity to independent movement hazards.

## Practice, research and build tools

- A separate practice dimension supports normal player entry and return without changing inventory, game mode, mana rules or learned runes.
- Operators can reset targets, select moving targets, create up to 24 targets and run visual diagnostics. Dummy damage readouts remain actual damage measurements.
- Twenty-four named builds can be saved independently of the five equipped spell slots. Names are cleaned and overwrite matching names case insensitively.
- Loading validates every rune, learned state, available slot, Cord tier and socket limit before changing the spell or its name. Missing add-on runes remain in the saved build.
- A native research and library screen opens through `/runelab notebook` or a Book used on a Runic Hearth. It has progress readouts, a slot selector, named build buttons, save/load/delete and a fusion hint. The server validates changes and sends a fresh small snapshot afterward.
- Three permanent experiments track five spell shapes, three distinct fusions and a cleansed greenhouse. Each experiment grants its reward once. Creative and spectator casts do not earn research progress.
- Fusion Altar hover previews show the result's description, base mana and formation ingredients. The Grimoire lists learned exact woven pairs and both ingredient effects.
- Precision, variety and fusion trials are optional practice-room challenges. They require paid casts and actual magic hits on a dummy, enforce a mana budget and deadline, and award a first-completion page once. Leaving practice, dying, changing to Creative or exceeding the deadline ends the attempt.

## Boss encounter upgrades

### Root Guardian

The Rootbound Maze gains a custom bark-and-branch guardian with three protective root bindings. Shears or fire prune the bindings, exposing the heart for a damage window and interrupting the encounter rhythm. Its root, mire, tremor and venom patterns change with its phases. Bindings and phase state are saved, and the encounter has loot and advancement hooks.

### Storm Conductor

The Storm Spire gains a custom copper cage and gyroscope creature. It cycles through real lightning rods; standing at the sparking rod grounds it and opens a vulnerable window. Removed rods are detected and an empty rod cache is retried. The boss has persistent encounter state, loot and advancement hooks.

Both bosses use dedicated models, hand-painted textures and emissive details.

## Three new expeditions

| Expedition | Central interaction | Accessible alternative |
|---|---|---|
| Clockwork Crypt | Store an incoming harmful spell, pause the clock trap, then choose when to release the stored strike | Physical clock control and ordinary stairs |
| Living Greenhouse | Life spells cleanse the heart and grow a moss crossing | Bonemeal and the physical rim route; shears offer the alternative harvest |
| Moving Sky Ruin | Shift real collision platforms with wind or its anchor | Physical anchor control, scaffolding and a fall recovery terrace |

- All three are registered world structures with biome placement, hall/vault loot, guards and directional mechanism blocks.
- Each has three deterministic layouts. Optional galleries, rear entrances, work areas and outer routes change with the variant.
- Mechanism orientation follows the rotated piece's world direction rather than applying rotation twice.
- Greenhouse cleansing and harvesting are mutually exclusive rewards; the moss path covers the full gap.
- Sky movement preflights the entire platform group and landing clearance. One obstruction preserves every platform and its offset. Players on the moving stones are carried, and falls within the recovery area return to the anchor.
- Sky fall recovery checks every tick and clears fall distance and downward momentum. The structure has weathered copper borders, carved quartz columns, broken high arches and an amethyst crown.
- Three off-hand relics are dungeon sidegrades: Keeper's Hourglass extends duration while reducing power; Living Seedpod improves efficiency while reducing power and increasing cooldown; Sky Feather improves cooldown while increasing mana cost.

## Home magic and cooperative rituals

The craftable Runic Hearth has four owner-configured projects:

| Project | Fuel | Result |
|---|---|---|
| Reading Lantern | Fire or arcane | Nearby eligible readers receive night vision |
| Bloom Planter | Life | Advances an immature crop without harvesting or replacing terrain |
| Ward Chime | Wind | Detects a nearby fall and grants short slow falling |
| Threefold Ritual | Three distinct elements | Regeneration for contributors and a page for the owner |

Named and woven fusions contribute their real materials: Firestorm supplies fire and wind. Utility projects recognize matching ingredients inside a fusion too.

Projects save ownership, selected project, charge and ritual state. Teammates can contribute, while configuration remains with the owner. Production needs loaded chunks, an online owner in that dimension and eligible players nearby. It does not force-load chunks or produce offline rewards.

The ritual consumes a Mana Crystal in Survival. A solo contributor completes after twenty seconds; two active contributing teammates complete after ten. A saved sixty-second cooldown survives reconfiguration. Breaking the workstation clears its block entity and stored progress.

## Familiar roles and event discoveries

- Companion retains the familiar's original elemental assistance.
- Scout marks a nearby threat and points toward it.
- Guardian offers a small emergency absorption shield after an attack.
- Gardener marks ripe crops without automatically harvesting the farm.
- Changing role replaces the assistance choice rather than stacking every role.
- Completed fallen-star, rift and mana-storm events leave bounded, saved discovery echoes for ten minutes. A player can crouch near an echo to claim its temporary benefit once.
- Echoes expire, cap their saved claims, use loaded chunks and avoid permanent terrain changes or chunk tickets.

## Cinnamon

Cinnamon remains a dedicated small, round dog based on the supplied photos, with a darker black coat, tan face and paws, long ears and a custom creature model. She is immortal and pre-tamed to the configurable owner.

- Sitting is saved on her owner and restored after direct summoning, body replacement, travel, rejoining and death.
- Spawn and rescue select safe loaded surfaces, excluding collision, fluid and hazards. No unsafe forced spawn is used when a suitable surface is unavailable.
- Load/unload registration tracks her body, with smaller occasional repair scans and cached owner resolution.
- Owner clicks toggle sit/follow; sneak-click pets without changing that choice.
- Quiet sitting leads to closed-eye resting. Greeting, petting and her reusable Red Bone toy produce distinct small animations.
- The default owner remains blank. Set an exact username/UUID, or `@singleplayer` for the integrated dev owner, in `config/wildercord-cinnamon.json`.

## Performance and release checks

- Own and other casters' formations have separate Full, Balanced and Minimal quality settings. Reduced flashing and camera motion are independent choices.
- Performance, Balanced and Cinematic config folders are included, with a reproducible `.mrpack` builder for fresh launcher instances. All include the built WilderCord jar and exact verified Fabric API/Sodium files; Cinematic also pins the locally tested Iris version. Dependency URLs, sizes and SHA-1/SHA-512 hashes are locked. Render/simulation defaults are included, and shader pack selection remains optional. Archive contents and dependency consistency are checked; fresh launcher import still needs verification.
- Decorative work has bounded per-source budgets and counters. Gameplay damage and warning telegraphs are preserved.
- Compiled spell plans use a bounded 256-entry cache with ranks in the key and copied mutable graphs. Reload and shutdown clear it.
- Packet construction/delivery counts, formation limits, scheduled work, cache activity and bounded server tick timing are inspectable through `/wildercord visualstats`.
- A thirty-second local frame sampler records median, p95 and p99 presentation intervals. The corrected benchmark disables Minecraft AFK throttling and records whether throttling occurred.
- `tools/capture_profile.ps1` starts a local Java Flight Recorder CPU/allocation capture without uploading it.
- Shader compatibility was exercised with the pinned Iris/Sodium pair and the supplied shader test pack.
- CI now checks generated resources, builds/unit tests and runs the client suites under Xvfb, retaining logs and screenshots. Remote CI has not been executed from this uncommitted working tree.
- Generation was repeated and compared across 3,517 files with no differences. The latest build passed 460 unit tests in 54 suites.
- Game screenshots are archived before later launches clear their output. `tools/review_gallery.py` creates a filterable roster and equipment comparison, and the animation gallery labels release/impact timing frames.

See `2026-09-30-performance-measurements.md` for measured results and limits. An uncapped low-end hardware comparison, arbitrary shader packs, natural structure placement across many seeds, and a real multi-client dedicated-server balance/performance session are not established by the local tests.

## Useful commands

```text
/runelab notebook
/runelab research
/runelab builds save "Warm breeze" 1
/runelab builds load "Warm breeze" 2
/runelab practice enter
/runelab practice leave
/runelab familiar scout
/runelab trial precision
/wildercord practice stress 24
/wildercord visualstats
```

First audit pass evidence: the client gate passed in 44m 40s with 55 passed and three explicitly excluded optional suites. Expedition, 39-shape/turning and enabled-shader checks followed, then a release build with 460 passing unit tests. Its review index had 656 captures. This historical pass predates the later expansion below. No commit, push or installation into an existing play instance was performed.

## Latest expansion and release verification

- **Strata Rise, Tidal Lift and Wind Steps** add real temporary terrain, conserved source-water attacks and collision platforms. **Cinder Bulwark, Root Bulwark, Boiling Surge, Thunder Tide, Rime Causeway and Thunder Walk** add six signature variants with distinct geometry, status effects and terrain reactions.
- All nine have individually authored icons, emblems, bands and three-stage choreography. The roster is now **338 named runes**, including **297 shapes/effects**, and **77 named fusions** (55 elemental and 22 signature).
- Original ember, frost, storm, wind, stone, petal, void, arcane, time, blood, water and vapour sprites replace legacy spell decoration. Their particle motion differs by material. Spell-only vanilla lightning entities and Span/Rampart block-break decoration were replaced with custom visuals.
- Rear casting strokes follow the caster's current pose. The foreground shape still determines the actual projectile, ring, beam, rain or self effect.
- Exact weaves combine two through eight elemental effects, retain every registered leaf, use the full combined mana cost and scale their tier/XP requirement. An existing weave cannot be silently reduced through the shard recipe. Innates have a Tier IV soul-weaving route: crouch-use a Blank Rune at an altar to imprint your own awakened innate for three levels; foreign innates are refused before learning or payment.
- Spellguard now recognizes starting health for a same-cast, same-tick multi-effect burst. Cleanup verifies full block states and construct identity, preventing older callbacks from removing later terrain replacements.
- **464 unit tests in 55 suites pass.** The final **59-entrypoint client gate passed in 45m 6s**: 56 passed, three explicit optional skips. Enabled Iris/Sodium checks passed separately. The final release build passed and all three launcher profiles were rebuilt and validated.
- **3,623 generated resource hashes match** over two final generator runs. The full run archived **840 screenshots**, including **186 animation timing frames**. The merged review index contains **666 searchable captures**, 20 equipment comparisons, nine physical rune icons and twelve original material silhouettes.

See [the physical-magic expansion](2026-09-30-physical-magic-expansion.md), [player instructions](../features/physical-magic.md), [performance measurements](2026-09-30-performance-measurements.md) and [the complete uncommitted inventory](2026-09-30-uncommitted-file-inventory.md). Real multi-client balance/performance sessions, broad shader-pack compatibility, fresh launcher import, uncapped low-end comparison and natural placement over many seeds remain unverified. All repository changes remain uncommitted.

## Later casting circle expansion

Added twelve craftable circle disciplines and twelve animated mechanisms, shape-selected plain circles, material-coloured circuits, shared world/editor geometry and linked impact circles. Coverage, flying speed, effect duration, mana, support strength, fusion ingredients, movement, crouching, water/rain and night now provide different build choices with explicit tradeoffs. Extra disciplines in a group are ignored without payment, and conditional bonuses stay with the release snapshot and the group they belong to. The Cord readout uses short lines so the full tradeoffs can be read.

The current roster is **350 named runes**, including **36 modifiers** and **199 craftable runes**. The circle addition has ten unit tests covering all 468 shape/discipline pairs and twelve distinct bounded mechanisms, a real client mechanics/presentation suite, 52 circle captures and a 190-capture shape regression run. Full details and final verification are in [the circle expansion report](2026-09-30-circle-expansion.md); recipes and usage are in [the player guide](../features/circle-disciplines.md).
