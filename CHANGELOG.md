# Changelog

All notable changes to Wildercord. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

- Add the Archive’s Relay Circle lesson at active Heart Circle VIII: one paid remote focus, a fresh second cast-key press, a warned ray, and persistent three-page lore/practice guidance. The initial shape accepts only Relay with Harm, Frost or Shock.

### Masters of Tomorrow, development foundation

- Added a committed release and original Classic/articulated body and first-person motion to the existing Crimson Moon Final. Its ten-tick warning uses accepted facing and release-time feet with visible-cone admission; the existing price, rest, release-only health toll, outward arcs, wounds and healing caps remain. Native Final/lifetime and stronger visual verification remain pending.

- Added Ember Kiln Ring: a stationary warned annulus with an inner pocket, outer escape, cover and timed-jump counters, original body/weapon motion, finite Aura and full recovery. Its three dedicated native mechanics, presentation and opponent-view suites pass on `160ff125`; final-candidate and continuous gameplay acceptance remain open.

- Let Glimmerwings sense the lantern light already around them and follow a bounded brighter-air gradient, preserving their existing natural flight and exploratory searches. The strengthened native check requires actual light acquisition and lured flight before arrival; candidate runtime acceptance remains pending.

- Added the isolated Wall Turn lesson: a Sovereign with a recorded Gale clear can learn from a wandering teacher, retain its original three-page story, and equip one separate Master form. Fresh C inputs brace and kick through a bounded, paid server movement with real collision, fall risk and safe-landing recovery. Classic body/hand presentation and native acceptance suites accompany the source; final combined native acceptance is pending.

- Prevent an extra rimehare bound when its last grounded step has already reached the final native waypoint.

- Let rimehares shed excess horizontal momentum during fast grounded recovery so bounds can follow native waypoints and settle at the end of a route. Native escape, terrain and arrival checks remain release gates.

- Extended the default-off original articulated player preview to Rising Break and Driving Cut, with separate low-to-high and point/recoil body forms and HUD-safe hand compositions. Existing gameplay windows, inputs and full unsupported-state fallback remain; accepted-input native visual review is pending.

- Added Stone Fracture: a fixed-facing paid brace with rear/axe/stance counters, a separate narrow reply warning, original body/weapon motion and a guaranteed two-second recovery. This isolated slice still requires its native gameplay and visual gate.

- Retuned cast pursuit to 8 warning ticks, 6 native movement steps and 8 final-warning ticks, allowing an early free opening to reach a normal 30-tick charge. Both visible tells, committed aim, all Aura/cooldowns and the 30-tick exposed recovery remain; fast casts and early release can still win.

- Defined pursuit spell interruption by resolved health or absorption damage before Mana Skin restoration; full guards, dodges and rejected hits preserve the cast. Damage amounts and the shared eight-second interruption protection remain unchanged.
- Applied the same landed-damage rule to close-range Master spellbreaks and Driving Cut. Driving Cut now respects shared eight-second interruption protection for held spells, while retaining its one-second idle-player seal; hit callbacks cannot transfer interruption to a replacement charge.

- Added a school-tuned, server-owned cast-punish dash for all three Masters: visibly committed native movement, a separate short strike warning, actual-damage-only interruption, finite Aura/cooldowns and a guaranteed exposed recovery. One-tick feints, bystanders, cover, unsafe terrain and interruption cannot create free or homing dash chains; native counterplay fixtures accompany the new fallback body motion.
- Authored a separately default-off articulated netherite armor preview: primary-body-only ownership, runtime vanilla slot geometry with welded joint bands, stock enchantment/trim materials and optional chestplate arms in first person. This remains an isolated, native-unaccepted slice; unsupported equipment uses the complete fallback.
- Added a default-off, operator-reviewed existing-world pavilion adapter with exact-region previews, fail-closed claim-provider checks, durable recovery manifests and conflict-aware rollback. The first template is limited to 123 inert blocks in one already loaded, uninhabited chunk; it does not retrofit dungeons or spawn bosses. Native recovery validation remains a release gate. See [Safe world upgrades](docs/SAFE_WORLD_UPGRADES.md).

- Added a default-off original segmented Spellcut/Master SWEEP renderer proof, with visible elbow/knee/wrist joints, skin-sliced limbs, independently composed first-person arms and stable-camera preview. Existing rig remains the whole-body fallback for armor and unsupported states; native validation and enchanted-netherite support are still required.

- Raised the permanent Mana Crystal absorption cap from 10 to 100. Each crystal still adds 10 mana; existing absorbed crystals are preserved.
- Extended Heart Circles from 8 to 20 with twelve additional, solo-achievable breakthrough gates, titles, advancements, bounded visuals and saved-progress validation. The original eight milestones and their perks retain their meaning.
- Added voluntary parties of up to eight players, invitations and leader controls. Shared spell and Aura targeting, direct impacts, owned summons, delayed harmful effects, and status/fire/movement mutations now check current membership. Explicit agreed duels retain their existing exception.
- Added opt-in Sword Master trials in the Ember, Gale and Stone disciplines, separate from progression-teaching duelists. A staged roster locks health, stance and attack coverage; telegraphed attacks, counter windows, finite Aura, directional spell cutting, simple-bolt redirection and pressure responses provide counterplay.
- Gave the Ember Master a distinctive Cinder Wake: a fully committed broad cut, a separately warned delayed narrow afterburn, original two-beat body motion, school-specific guidance, finite Aura cost and a guaranteed counter window. Group lanes add coverage without repeated hits or bystander damage.
- Added Spellcut, Rising Break and Driving Cut on rebindable U/Y/J controls, with server-owned payment, windup/active/recovery timelines, interruption, bounded target queries, and original first- and third-person animations.
- Extended the bounded native screenshot artifact to include requested Rising Break/Driving Cut captures with explicit omissions and unknown rendered phases; full evidence and native verdicts remain separate.
- Added paid, server-timed body and first-person releases to the existing Rising Cinders and Blossom Fall second forms, preserving their independently released rain and field effects.
- Gave all ten existing first-form style arts committed windups and original body/weapon motion, preserving prices and inputs; physical multi-cuts are interruptible while launched afterimages and fields retain their own lifetimes.
- Added Survival introductions through travelling Duelists and permanent first-clear technique lessons (Echo, Afterimage and Sunder) without repeatable item/XP farming.
- Improved Normal/Hard Gloomstalker and Bog Witch-Frog pursuit and attack cadence while preserving Easy, damage, health, tells and guaranteed miss recovery.
- Fixed delayed effect source attribution and impact-time alliance checks, preserving helpful and self effects. Added regression suites for party mutations, projected attacks, twenty-circle progression and native combat presentation.

This is a development build, not a measured balance release. The larger style roster, ordinary-encounter retuning, and complete animation review remain in progress. See [Masters of Tomorrow](docs/features/masters-of-tomorrow.md).

## [0.10.0-alpha] — 2026-10-04

Install the same build on the server and every client. New blocks, items, entities, particles and packets mean 0.9.x clients must update before joining.

### Release fixes

- Fixed read-only and in-place checks running Wildercord's own block-break handlers. The Bank Surveyor's Line, Mossveil Cowl home check, Watchweft and Manabraid floor checks, Cinder Fern and Moonreed harvests and fern spell interactions now ask claim mods as before, but no longer erase a dungeon room's "player placed" record (which made a player's own block unbreakable afterwards) or set off another player's glyph trap.
- Fixed a Nullcatch or Quietus cast on a target that already had the other screen silently replacing that paid screen; the later cast is now refused before its target allowance or rest is spent.
- Fixed the frame benchmark started from the Grimoire's visual-settings link reopening the Grimoire over the scene; it now always measures the plain game view.
- Repaired garbled dashes and multiplication signs in the Life outcomes, Mossveil and Fusion Altar guide pages, and missing spaces before numbers in the changelog and guide.
- Split the native client test job into four ordered shards with a longer limit. The full roster takes several hours and never completed within one 90-minute CI job.

### Art

- Redrew all art added since 0.9.1 in the established Wildercord style: 16×16 item icons with a dark outline and top-left light (previously 32×32), crisp vanilla-resolution block textures, and repainted Cinder Bailiff, Rootmolt Strider, Siltcrest Bittern, Mossveil Dormouse and Sporeback Snail skins on their unchanged models. Spawn eggs share the standard egg silhouette.
- The 21 new runes (18 signatures, Root Carry, Watchweft and Manabraid) now use the standard element-coloured rune stones with their own pictograms; the signatures animate like the established ones.
- The Reed Rattle, Cave Breather and Mara's Empty Bell are flat inventory icons like every other item; Rook's Rainshield and the Bank Surveyor's Line keep their 3D held models with repainted materials. Cinder Ferns are 16×16 crossed plants.
- Earth, Life, Void and Camp Concord material particles are greyscale so each spell's colour tints them cleanly; Watchweft, Manabraid and Root Carry circle tiles follow the standard rune-circle pattern.
- Updated the guide's rune icons and recipe pictures for the new art, and fixed the Cinder Fern recipe picture, which showed a missing-texture square.

### Living-world additions

- Added **Mara's Drainhouses**: three-room underground waterkeeping stations generated in newly explored lush and dripstone caves, with an optional investigation (snail dew, a witnessed Rootmolt meal and a counter), three ledger lore books, a once-per-player **Mara's Empty Bell** that interrupts one Rootmolt's warning or hold, and **Rootbound Greaves** that let a still, crouched wearer deny one restraint at a wear and rest cost.
- Added **Cinder Bailiffs** and **Cinder Ferns**: a broad woodland creature that tends cooled ferns and answers threats with a warned three-vent heat fan (no terrain fire), and a three-stage fern that can be crafted, planted, harvested without losing its root, and cooled, warmed or grown by real paid spells.
- Added **Root Carry**, a rank-II Life rune that moves a young Cinder Fern, with its growth and cooling state, onto nearby empty soil over two separately paid casts.
- Added the **Tideward crossing kit**: **Reedwater Waders** for shallow supported banks, **Dewglass Spectacles** that read a nearby Reedback Crab's raised claws, and the **Bank Surveyor's Line**, which shows a short physical route over actual footing. Each has wear, saved rests and its own sounds.
- Added the **Siltcrest Bittern**, a wetland bird with its own rig and voices that coils and strikes at wild cod and salmon from dry banks at night, rests under cover by day, protects named/bucket/persistent fish, and can be offered a fish by a crouching player.
- Added **Mossveil Dormice**, small cave companions tamed with three Glowcap Gill feedings, and the craftable **Mossveil Cowl**, a one-armour head filter: crouch still for two seconds beside your curled dormouse and a Fungal Nursery to shorten current Poison by up to three seconds, at the cost of wear, brief Slowness and a saved rest.
- Added **Camp Concord**: **Watchweft**, a 45-second private camp watch that warns once when a hostile targeting you crosses in, and **Manabraid**, a consenting, lossy mana gift to an allied caster (at most 24 taken, 16 given).
- Added **Rook's Rainshield**, a crafted reed-fan relic that catches one incoming arrow for yourself or for a teammate who accepts your offer by crouching; no mana, saved rest.
- Fixed repeated paid wards (Zone, Pulse or Echo of the same payment) refilling spent Reflect and Foresight charges, and Reflect returning a hit whose damage was not actually received.
- Live wildlife spawn and population queries now run only on the owning server thread and refuse during chunk generation; Aura beast and wetland queries are bounded, and a Reedback Crab's follow-up sweep keeps its original owner.
- Guarded the blade tooltip's client state during the Codex background search.

### Magic, fungal fieldcraft and earlier development changes

- Added Rootmolt Striders: original articulated cave creatures that compete for mature Glowcaps, defend their garden with a readable single physical rake, and apply a short source-owned restraint with practical melee, cleanse and sidestep counters. Added finite saved resource/threat clocks, Sporeback warning reactions, bounded population/alarm queries, compatible configuration, five authored voices and illustrated guide coverage.
- Fixed the Rootmolt texture path, natural-spawn dimension admission and stale equipment contacts after callback movement/target replacement. Verified once-only consumption, counterplay, restart, habitat and dense-query behavior in focused native tests.
- Ground Sporebloom's actual fruit on the recipient's real block/slab support; use separate hanging spores for airborne recipients. Keep finite Full/Minimal materials above the floor and resolve support after event admission. Updated all 30-rune surface regressions and preserved genuine gameplay/screenshots.
- Tightened synchronous counter-test damage provenance and retained strict actual health-loss, cancellation and no-resurrection gates. Focused tests pass; full Linux CI remains independently tracked.

- Corrected 34 proven malformed translation literals in the generator and regenerated language data: Codex mana/cooldown separators and multiplication signs, Cord/tooltips, passive/readout and research/loadout text now use their intended punctuation.

- Fixed visual settings overflowing short/narrow windows: adaptive columns and spacing keep every control reachable at Minecraft's minimum 320x240 GUI, with full preset-label tooltips. Native tests verify 18 non-overlapping controls, journal state preservation and learned-only guidance.

- Added individually authored actual-outcome material responses for 30 existing Life identities: tissue, sap, seed coats, thorns, leaf shutters, roots, hanging berries, spores and equipment stitches follow the body, slot or changed cell that really acted. Refused/no-change actions remain silent; admitted wards, real triggers and endings stay distinct.
- Added actual-caster source-aware Life visual delivery and smaller authored Minimal recipes, finite queue/decoration budgets, loaded-world/range/freshness checks and camera/eye clearance. Orient material to real body/cell surfaces and retain ingredients in mixed fusions.
- Removed duplicate generic SELF/impact and old Venom/Fortune success overlays only where authored outcome coverage owns the response. Added 56 distinct Life cue/outcome sound identities with 112 generated variants; Ashen Mercy and Pulse Ferry keep their original sole voices.
- Fixed spectator interception in projectile/line/Touch/Chorus/imbued aim; fixed Fortune success feedback occurring before final damage admission and replaced its old kill flourish with actual experience-reward provenance. Preserve real damage/payment rules and finite renewal/expiry cleanup.
- Added a Life field chapter to wiki/GitBook, a labelled illustrated plate, learned-only native Grimoire material notes, and direct Magic visuals navigation returning to the journal. Updated Haven's description to its actual leaf-shutter defense.
- Added focused actual gameplay/source/quality/surface/lifecycle/Fortune/spectator regression coverage and native screenshot galleries. Corrected the strict Void alternate-flight camera without relaxing pixel assertions. Full Linux/dedicated multiplayer and all maintained visual lifecycles remain open.

- Added twelve expedition signatures: Nullcatch, Second Bell, Red Ledger, Quietus, Blood Escrow, Frost Molt, Pulse Ferry, Last Lantern, Pocket Current, Wayline, Night Seam and Shard Compass. Each has an exact Fusion Altar pairing, carved icon, original circle motif, material preparation, moving body, successful-action presentation and original cue/impact voices. The runtime now contains 40 signatures, 95 named fused recipes and 369 runes; eighteen signatures are additions under the living-world goal.
- Added finite shared-payment counter and support resources, post-bonus damage caps, permanent reflected-projectile provenance, voluntary return/tow controls, persistent chest-local ownership and atomic intake, permission-rechecked wall inspection and sample-paid mineral clues. Refused actions preserve their extra resources while ordinary casting mana is still paid.
- Fixed Red Ledger cancellation during actual synchronous damage callbacks and silenced Pulse Ferry success presentation when real healing is prevented. Minimal Pocket Current retains its authored folding motion.
- Bounded counter scans to complete pools of 64 projectiles, with early abort at 65 and explicit crowded refusal; Quietus snapshots all old projectiles separately from its nearest-twelve handling. These constraints are verified with actual projectile fixtures and are not a blanket performance benchmark.
- Added the illustrated Expedition Signatures field-journal chapter and refreshed the rune references/GitBook. Verified all twelve actual client Fuse packets and paid Full/Minimal preparations and flights; scoped persistence, counterplay, support and exploration checks pass. The full Linux descriptor and real remote multiplayer remain separate gates.
- Fixed Void's diagnostic screenshot framing to aim at retained production particles after projectile motion, preserving strict pixel-visibility checks. Linux native tests select OpenAL Soft's real null backend when no explicit driver is configured; general game launches retain their audio configuration.

- Fixed Grow's direct adjacent placement checks for vanilla short grass, fern, seagrass and pale moss carpet. Both plant halves or the actual topper destination now pass loaded-world, border, eligibility and edit checks before native placement; shared block budgets reserve exactly two or one cells atomically. General registry-feature tree/flower adjacency remains under review.
- Fixed the nursery's earned-drop test to track the actual newly emitted dew and walk to its randomized landing using native client input. Inventory and item lookups now use an atomic success check so legitimate pickup between callbacks cannot fail the fixture. No item insertion, relocation, ownership changes or shortened rests are used.

- Corrected the nursery harvest regression fixture to approach through actual supported, collision-free footing and line of sight, use native crouch interactions, and verify real emitted dew reaches Survival inventory. The complete focused earned-resource and restart chain passes; the full Linux CI rerun remains a separate gate.
- Added individually authored preparations and velocity-oriented moving bodies for all thirty-six Void effect runes. Eleven original material styles include torn folds, shells, jaws, pressure and running hounds for Shades; mixed Fire, Wind, Time, Blood and Arcane ingredients remain visible in Full and Minimal quality. Rear casting circles and actual delivery remain authoritative.
- Verified paid Void Self wards, safe Blink/Warp movement, Warp Step's return/crouch choice and Zipper's protected-wall refusal in focused native tests. Full impact, sound, beam, rain and summon presentation remains tracked separately.
- Added an illustrated Shadow Materials guide chapter and stage-specific Void acceptance audit. Canonical moving-body coverage now includes 206 distinct identities across seven families; this count does not imply complete spell lifecycles.

- Added six authored signature fusions with specific ingredient pairs, carved rune icons, distinct material preparations/projectile bodies, successful-action effects and twelve original cue/impact sounds:
  - **Springbed** (Basinfill + Grow): fills one safe shallow basin, then feeds at most eight existing adjacent plants without creating new plants or resources.
  - **Cinder Sieve** (Ember + Collect): spends one carried coal or charcoal to process up to sixteen eligible dropped furnace inputs. Output capacity is simulated before fuel or inputs change; ownership, pickup delay, reach and editable ground are respected.
  - **Ashen Mercy** (Fireward + Cleanse): consumes harmful conditions and extinguishes fire, healing at most four health and granting brief heat protection. Repeated parts of one payment cannot renew the same target.
  - **Clockroot** (Root + Foresight): records safe grounded footing and returns a fleeing eligible enemy once within a short window. Bosses, claims, unsafe destinations and blocked return paths refuse the movement.
  - **Skylatch** (Levitate + Anchor): lifts an ally into a short vertical hover while retaining lateral movement. Crouching releases it into brief slow descent; external Levitation supersedes the latch without being removed, and no flight flags are granted.
  - **Thresherwind** (Harvest + Windcut): advances three bounded sickle rows, collects actual crop drops and replants only when the native drops provide a seed.
- Authored twenty-nine Life preparations and moving projectile bodies with distinct leaves, seeds, petals, spores, vines, sap, living tissue and thorns. Supporting fused materials remain visible in Full and Minimal. Existing effect gameplay remains authoritative; the broader impact, aftermath, sound and non-projectile delivery audit remains open.
- Added **Glowcaps**, **Fungal Nursery** canopies and the thirty-two-use offhand **Cave Breather**. Damp covered cave growth requires an actual rested Sporeback browse to mature; Life only prepares a bud. The Breather clears current Poison with filter wear, three seconds of Slowness and a persistent shared ten-second rest, without granting future immunity or cancelling independent magical damage budgets.
- Added Mara's optional **Three Breathmarks** investigation: living observation, two authentic generated clues, personal mature-cap harvest, nursery placement, native filter crafting and a contextual return. Three original field books and a once-only reward use persistent discovery state. Natural-generation and complete acquisition acceptance are recorded in the milestone audit before release credit.
- Fixed sparse Glowcap decoration with twelve distinct columns inside the current chunk while preserving moisture, cover and actual cave-biome admission. Fixed Sporeback visit movement and separate finite search/journey admission so a late valid path can finish without retrying forever. Nursery supports now reach the walking floor and leave a tested passage for the actual visitor.
- Fixed **Collect** moving drops reserved for other players or still awaiting pickup. Foreign target/thrower UUIDs and pickup delays now preserve those items in place, alongside existing ground protection and radius limits; ready own/unowned items and experience remain supported.
- Fixed generic formation effects overlapping fully covered field-fusion projectile preparations. Mixed groups containing an uncovered effect retain their fallback presentation. Corrected the six carved rune texture paths so actual inventory models use the authored artwork.
- Extended the generic front-orb/line suppression to fully authored Fire, Frost and Storm projectile preparations. Rear glyph strokes and Jolt's own conducting arc remain; actual mixed Ember/Umbra retains its fallback. Focused native Full/Minimal paid-cast checks passed.
- Added an illustrated **Living Materials** guide chapter using original Life preparation and travel captures, plus documented mixed-material benchmark measurements and their practical limits. The player guide now validates and exports 120 pages.
- Added a sustained mixed-material native benchmark and complete restoration of visual, frame-limit, VSync and HUD settings. Retained scene measurements document their local scope and 120 FPS-cap limitations rather than claiming a profile speedup.
- Corrected the Runic Hearth lantern test to allow its actual charge cooldown and scheduled update interval. The focused native planter, lantern, falling chime and cooperative ritual sequence passes; the full Linux gameplay gate remains separately tracked.
- Added the **Reed Rattle**, a crafted physical instrument that answers a Reedback Crab's warning claws. Crouching within three visible blocks gives six seconds to pass; committed sweeps remain dangerous. Forty-eight uses, a saved twenty-second player rest and the crab's shared response rest prevent replacement instruments and repeated interactions from bypassing the tradeoff. Original clay/reed/floss model and pebble-shake sound accompany it.
- Added the **Sporeback Snail**, custom animated cave wildlife that approaches and browses actual mushrooms without destroying them. A finite saved dew reserve rewards quiet gathering; Life elicits a brief spore response without producing resources, while Fire and injury cause temporary retraction. Cave habitat, local population limits and bounded food/shelter searches keep its behavior sustainable.
- Added **Mycelial Dew**, a craftable **Fungal Poultice** with thirty seconds of night vision and six initial seconds of slowness, and the first-observation journal **The Patient Spiral**. Resource rests, observations and the creature survive save/reload. Added a configurable snail spawn switch, including old-configuration upgrades.
- Authored thirty-two Earth preparations and projectile bodies with world-lit rock, stratified slabs, soil, fractures, roots, bone and crystals. Each rune has its own material arrangement and motion, including supporting fire, water, storm, Life, time, blood, arcane and void ingredients. Full and Minimal rendering preserve the defining materials; release/impact/sound work beyond this scope remains on the living-world roadmap.
- Added **Basinfill**, a craftable rank-I utility rune that fills an enclosed shallow hole with up to sixteen permanent source-water blocks. Complete-vessel validation, protection checks, finite per-payment use and atomic block-budget reservation prevent partial flooding. Includes original pouring formation, cupped projectile, rune artwork and a dedicated synthesized liquid cue.
- Added player guidance for Aura progression: real-combat XP, the shared forty-point practice limit, and breakthrough trials. Stillness instructions now mention valid waterfall banks and mountain summits alongside ley crossings.
- Added a conservative remaining-work audit for the living-world goal, correcting lore-book item credit and separating baseline signatures from new fusion additions.

## [0.9.1-alpha.1] — 2026-10-03

- Regenerated five stale modifier descriptions so in-game tooltips show their stacking limits and the asset reproducibility gate passes. Supersedes the initial 0.9.1-alpha upload.


## [0.9.1-alpha] — 2026-10-03

### Administrator fusion discovery

- `/wildercord learnall` reveals all 55 elemental and 22 signature fusion recipes as well as learning their rune results. One attachment sync preserves existing discoveries and saves, without discovery rewards or innate ownership changes. Actual command, repeat, client sync and saved-world restart tests pass.
- Published a matching 0.9.1 client/server build, updated installation/version information, the player guide and CurseForge description.


### Spell damage exploits

- Reproduced a paid Touch/nine-Vows/Harm cast dealing all 1,000 dummy health for under 200 mana.
- Vow, Execute and Trial Key apply once per target rune; Focus twice; Extend three times. Extra copies
  are ignored with a compiler warning, including inside Knots. Useful copies keep their existing cost.
- Player-owned spell Casts admit at most 96 raw damage per hit and 12 + twice the effective mana price
  per target for the entire payment, up to 512. Copies, echoes, pulses, repeats, linked hits and
  reflections share the same allowance. Separate targets/payments keep separate allowances.
- Real scheduled Venom stops after its allowance is exhausted. Nonfinite damage is refused and per-cast
  target bookkeeping is bounded. Armor, resistances and spellguard still apply to admitted hits.
- Verified the fixed paid Harm cast at 18.2 and paid Bleed at 26 total, plus a production Venom stress
  case held to 212 total for a 100-mana allowance. Updated modifier tooltips, wiki and GitBook.


### Wind as air: preparations and projectile bodies

- Added twenty-five explicit wind recipes with their own shear, lift, compression, return, snatch, slipstream, vortex and feather motion. A dedicated airflow renderer uses original fine filaments, translucent world-lit curves and traveling crests instead of shared glowing beam primitives.
- Fully authored wind projectiles prepare their air body without an overlapping luminous orb/arc. Field delivery cues and the rear casting glyph remain. Supporting stone, leaves, blood, arcane, time and vapor retain their identities; covered wind-only fusion layers avoid duplicate generic material presentation.
- Extended flight coverage to wind while preserving unknown/omitted/foreign fallback. Added owner-selected curve quality and a finite orientation for camera-aligned vertical tangents.
- Added native full preparation/paid flight/variant suites and matched close flight captures. Complete release/impact/aftermath, all delivery combinations, dark-location readability, shaders, multiplayer and measured performance remain open. New particle registration requires the same client/server build.

### Authored storm projectile bodies

- Added twenty explicit moving storm recipes with individually timed contact switching, traveling leaders, expanding pressure, pulsing lobes, sequential copper terminals, inward-pulling filings, falling rain, circulating wind and charged feathers. Supporting materials remain in Full/Minimal; Ripple carries sunlight without electrical fragments.
- Extended shared flight coverage to storm. Covered Frost/Shock now keeps both authored schools; unsupported mixed groups preserve fallback. Existing gameplay and travel voices remain.
- Added paid Full/Minimal roster coverage, three-age evolving-motion checks, actual flight captures and six Arc/modifier/mixed-group examples. Complete release/impact, other deliveries, close visual review, shaders and remote multiplayer remain open.

### Authored frost and water projectile bodies

- Added thirty individually authored moving bodies: needles, forks, plates, biting jaws, panes, cold fronts, snow/stone, living ice petals, water membranes, channels, hooks, wave crests and sediment bores. Water effects retain water; fused ingredients remain in Full and Minimal quality.
- Extended the shared client/server coverage contract to frost. Covered groups replace duplicate travel particles and the generic comet while preserving travel voices; unsupported mixed groups retain fallback. Fire dispatch remains restricted to fire IDs.
- Added paid Full/Minimal roster coverage, particle-clear recovery, actual screenshots and focused Arc, Pierce, Frugal and covered/uncovered mixed-group checks. Existing costs, collision, speed, reflection and impact mechanics remain unchanged. Complete release/impact, every delivery/modifier combination, shaders and multiplayer remain open.

### Complete fire projectile body roster

- Added twenty further individually authored flight bodies, covering all twenty-eight runtime fire effects. New forms include heat shutters, pressure ribs, crowns, fuses, wood splinters, solar lenses, brands, ash curtains, hearts, bevels, shields, grates, jaws, time forks, heated water and stone mortar.
- Authored emission now follows live entities independently of comet particles. Removed entity IDs retire independently, omitted effect metadata preserves fallback, and Pierce narrows and elongates authored bodies. Starfire retains its arcane material in Minimal settings.
- Fully authored fire groups replace duplicate server travel particles as well as the generic comet; travel voices remain. Unsupported mixed groups retain their previous travel layer. A shared coverage contract keeps server and client decisions aligned.
- Expanded paid native coverage to the full fire roster and particle-clear recovery. Complete release/impact choreography, other deliveries, modifier combinations, shaders and remote multiplayer remain open.

### Authored fire projectile bodies

- Ember, Fire, Firestorm, Steam, Meteor, Soulfire, Starfire and Phoenix Pyre now carry distinct moving bodies on actual Bolt/Arc entities. Wind/flame, water/vapor, stone/heat, void/flame, stars/fire and living feathers participate in flight.
- Added bounded effect identity synchronization and client reconstruction. Fully covered groups replace the shared comet; mixed unsupported groups retain fallback presentation. Server collision, payment, reflection, speed and effects are preserved.
- Added paid native Full/Minimal flight coverage and early/later game captures. The remaining effects, complete release/impact choreography, modifiers, other deliveries and real multiplayer remain open.

### Authored storm preparations

- Added twenty individually authored preparations: conductor forks, interruption clamps, meeting leaders, magnetic filings, charged birds, sunlight fans, copper terminals, living stems, rifts, threaded nodes and timed charge.
- Fused preparations retain fire, wind, life, stone, void, arcane, time, frost and water motion/materials. Ripple uses warm sunlight rather than electrical particles.
- Added native Full/Minimal packet fixtures, recipe and ingredient checks, rendered evidence and paid Bolt/Shock plus Self/Surge casts. This changes preparation only; complete launch/travel/impact, shaders and real multiplayer review remain open.

### Authored frost and water preparations

- Added thirty individually authored preparation recipes: branching rime, latching plates, forward needles, mirrors, cages, ice petals, teeth, cold fronts, water membranes, lung lobes, hooks, flow lanes and moving water courses. Frost's water effects use water motion rather than generic ice recolors.
- Fused preparations combine their materials: storm-charged hail, stone-supported glacier, wind/snow, life/ice petals, void/black ice, arcane lattice, time/ice capsule and blood/frost teeth. Existing gameplay, cue sounds and impacts remain unchanged.
- Added native Full/Minimal packet fixtures, recipe evolution/uniqueness/material checks, actual framebuffer evidence and paid Bolt/Frost plus Self/Tidebreath casts. Full all-delivery launch/travel/fusion/Aura/shader/multiplayer review remains open.

### Linked formation origins

- Linked stages now emit their own isolated formation when the casting engine admits the segment. Delays and successful conditions form at execution; rejected conditions and cancelled casts emit nothing. Continuations do not open another rear glyph.
- Impact-centered preparations preserve the trigger position and direction. Ground fields use the native ground resolver, Rain prepares twelve blocks above it, and Self/Orbit/Trail remain on the caster as their gameplay requires. No gameplay delay, damage or extra payment is introduced.
- Added native paid-Delay coverage, successful/refused condition coverage and decoded origin fixtures for nine linked deliveries. Full linked launch/travel choreography, all link variants, secret-specific staging, shaders and real multiplayer remain open.

### Group-aware initial formations

- Ordinary Cord and scroll preparations now follow compiled initial groups, with isolated effects/materials, one complete rear glyph and mastery-adjusted aim range. Delayed/conditional groups no longer appear in the initial assembly; empty initial segments show only the rear glyph.
- Caster-centered Burst, Nova, Ring and Wave join Self/Domain/Orbit/Trail staging. Aimed Zone, Rain, Wall, Pillar, Mine, Totem and Vortex follow the terrain aim resolver; Rain prepares twelve blocks overhead and marks its ground area. Gameplay release timing, payments and damage are unchanged.
- Added decoded production-packet tests against server delivery aim, delayed-effect exclusion, rear-glyph count and an actual paid mixed Bolt/Self cast. Linked continuation, secret-specific and remaining delivery staging remain open.

### Authored fire preparations

- Added distinct formation recipes for all 28 built-in fire effects, plus Bloodboil's heat preparation: cinders, tongues, fuse, grate, shield, falling fragments, ash curtains, solar rays, feathered wings and more. Firestorm, Steam, Seethe, Cinder Bulwark and Boiling Surge visibly combine their material ingredients.
- Preserved server deliveries, costs, mechanics and existing impact voices. Preparations use the existing bounded client canvas; Minimal quality retains authored outlines with fewer midpoint particles.
- Added native packet coverage for all 28 fire effects in Full/Minimal modes, finite nonduplicate/evolving recipe checks, exact-namespace dispatch checks, rendered-pixel evidence and a Survival Ember cast with mana payment. Full delivery/launch/travel/fusion/Aura/shader/multiplayer review remains open.
- Added illustrated casting-guide coverage, a formation matrix and actual-game review captures. This improves existing abilities; it adds no new ability or fusion count.

### Highland travel and interruption fixes

- Replaced random food/cover probing with finite nearest-first local sweeps. Eight columns are considered each second, with three footing heights, at most two search path attempts and loaded-cell checks. Ordinary wandering retains the sweep cursor so distant nearby resources are not repeatedly missed.
- Food and shelter goals now follow the exact reachable native path selected by the search. The convenience movement call previously changed the path's allowed destination distance and could stop a Stonehorn just short of its food. Interrupted movement can retry at most four times, one second apart, within the existing travel deadline.
- Natural crop grazing yields to timed feeding and recovery poses. Feeding an approaching Stonehorn keeps its complete authored meal animation and calm period. An unsafe player approach can interrupt crop navigation with the ordinary warning and charge counterplay.
- Chewing requires two consecutive seconds near a visible crop; a solid obstruction blocks feeding. Shelter settlement requires the animal to stand under cover. Neither behavior moves entities through obstacles, forces chunks, generates rewards or destroys terrain.
- Added a native travel suite for separate food and shelter, a wall detour, Rimehare travel, actual player feeding, proximity interruption, sealed-food refusal and a complete Galeclaw satiety server restart.

### Highland resources and field equipment

- Added three-stage Windreed, small biome-specific patches, daylight regrowth, two-tassel harvesting that retains roots and replanting on dirt. Life impacts advance existing roots with ordinary claim/build permissions; Wind rustles them without minting items or plants.
- Added bounded native Stonehorn/adult Rimehare forage goals. Grazing clips one crop stage, respects mob griefing and pauses between meals, producing no items or experience. Panic, courtship, predator avoidance, shelter and combat can preempt grazing.
- Added a reusable Draft Kite: one tassel fuels eight seconds of Slow Falling, with a twenty-second cooldown. Windreed, a Galeclaw plume, leather, string and a stick craft it.
- Added consumable Windreed Braids: thirty seconds of Downwind with ten percent slower movement. Calm approaches frighten fewer hares, avoid provoking Stonehorns and hide recent-casting attraction from unprovoked Galeclaws. Sprinting, very close hare approaches, acquired targets, committed attacks and retaliation still matter.
- Both tool rest periods persist as shared player deadlines; joining restores their native indicators, and swapping copies or clearing an indicator cannot bypass the server deadline.
- Authored three plant stages, detailed field-item textures, a native status icon and three short physical sound cues. Added recipes, recipe discoveries, bestiary hints and player-guide coverage.

### Highland shelter and predator–prey behavior

- Rimehares now use a real predator-avoidance goal for hungry Galeclaws, alongside their existing player fear, panic and breeding behavior.
- Galeclaws become sated for one minute after successful feeding, consuming one dropped raw rabbit/chicken, or finishing a Rimehare hunt. The saved deadline is not renewed by loading. Sated runners leave prey and further scraps alone; casting attraction and defensive retaliation remain dangerous.
- Stonehorns seek nearby dry cover at night, while Galeclaws rest through the bright middle of the day. Both seek storm shelter. Searches probe at most sixteen loaded positions per five seconds, never force chunks or alter terrain, and limit travel attempts. Threats interrupt resting. Existing Peaceful/config suppression remains in place.
- Added distinct folded-leg Stonehorn and tucked-wing Galeclaw resting poses, plus Galeclaw feeding motion, field-guide knowledge and player documentation. This advances the highland ecosystem foundation; it adds no new creature or item count and does not complete the region.

### Aura cooperation and lifecycle integration

- Fixed Unity resuming an active window after an integrated-server shutdown/reconnect. The old disconnect callback could run after the player had already been saved. Server stopping now closes active windows before saving, and joining closes any loaded active window while retaining exact rest and delivered allowances.
- Added an integrated client suite combining native elemental effects, projected art, resonance, a bonded fire inscription and Unity. It checks one reaction, exact combined payments, independent inscription rest and refusal of repeated bonuses.
- Added actual client-requested death/respawn checks with both inventory rules, plus closing and reopening the saved world in another integrated server. These verify the same owned inscribed blade, exactly one inventory copy, retained Unity budgets/rest, inscription cooldown, activation refusal and owner-client synchronization. Real two-client/dedicated-server sessions and broader balance/performance remain open.

### Unity: two paths, one breath

- Added a hybrid technique for Form and five working Heart Circles, activated from the Aura page. Twelve mana plus twelve Aura open twelve seconds of exchange, with a two-minute rest from activation.
- Actual paid Cord/inscription mana returns 25% Aura, at most twelve; actual Aura payment returns 50% mana, at most twenty-four. Rest and delivered allowances persist together. Activation, restoration, Blood Price, creative casting, unpaid echoes and synthetic cast events cannot feed it; backlash and spent Awakening cannot convert resources.
- Putting away the weapon, losing a prerequisite, silence, Aura lessons/sparring, disconnect and death end the window without clearing rest. Dimensions retain the same window and budgets. Fixed allowances and lossy conversion prevent resource circulation from becoming a free-energy loop.
- Added a woven insignia, compact Aura-page control/HUD marker, open rear/side strand formation and transfer strokes, and three authored sound cues. No magic circles or first-person veil. Visual transfers are throttled to one per four ticks and voices to one per twenty ticks.
- Added pure rule tests, a native GUI/packet/payment/lifecycle client suite and player documentation. Final Aura integration, real two-client/restart verification, broader matchups and the larger living-world roadmap remain open.

### Rune-etched blades

- Anvil inscription: an eligible weapon plus one rank I effect rune, consuming one rune and five experience levels. Replacing an inscription deliberately loses the old one; shapes, links, modifiers, innates, duplicate inscriptions and foreign bonded blades are refused.
- The first successful hit of an art or written technique, or a successful finisher, wakes the effect for the basic Touch spell mana price. Harmful effects touch the foe, helpful/movement effects touch the swordsman, and world effects use the contact and floor. Native effect presentation and defenses remain in charge.
- Player-persistent rest lasts at least five seconds or the compiled effect cooldown. Missing mana spends nothing; swapping weapons, inscriptions or dimensions cannot reset rest. A delayed art must still hold its starting blade. The primary target cannot earn an extra resonant answer from the inscription itself.
- Weapon name, wear and custom components survive etching and normal repairs; an etched weapon cannot be silently sacrificed in an anvil's right slot. Unknown inscriptions remain stored and dormant. Tooltips and a player guide explain prices, replacement and activation.
- Focused runtime covers native anvil result pickup/consumption, actual art damage and effect payment, self healing, shields, refused payment/repetition/swaps, full stack serialization, repair preservation and rendered first-person/anvil screenshots. Full restart, two-client behavior, the complete effect roster's runtime behavior and final Aura integration remain unverified. Unity and the larger living-world expansion are still open.

### Resonant strikes: blade and spell cooperation

- Elemental spell damage and a full coated swing, slash, art or written technique can meet on a living foe within 1.2 seconds, in either order. One player or allied partners can coordinate. The swordsman pays four Aura, and both foe and swordsman rest four seconds.
- Ten authored material responses, ten school incision geometries, twenty ordinary/complementary names and ten distinct sound voices. Target-local three-beat sequences use physical cuts, grit, splinters, flame, leaves, facets and displaced echoes without magic circles.
- Bonus damage uses the weaker actual hit, capped at three health against creatures and 0.75 against players before normal shield/armor/ward/boss/guard defenses. Utilities have player limits; pure support/control spells, dead targets, immune/blocked hits, protected pets, allies and spar partners cannot prime it.
- Shared-payment per-target tracking stops repeated beams, echoes and pulses from farming reactions. Bounded UUID ledgers, periodic expiry, cross-dimension striker rest, disconnect cleanup and a proc reentrancy guard accompany the feature.
- Training dummies retain actual hurt damage before restoring health, allowing resonance practice and correctly measuring projected hits. Dummy/Practice Room hits cannot provide Spell Thirst or Life-reaction healing.
- Player guide, developer design/architecture and a dedicated real-game suite added. Rune-etched blades, Unity, full ability presentation audit and the broader living-world expansion remain in progress.

### Village tournaments and the Three Bows

- Added inhabited-village gatherings with three stewards from different breathing methods. A bounded daytime visitor search finds flat, dry, open ground near a village bell with at least three residents. It places a timber registration stand and four embroidered cloth standards into empty space, preserving the underlying terrain and respecting mob spawning/mob griefing settings.
- Three consecutive knockout wins complete an Aura circuit. Hosted bouts use existing nonlethal duel protection and opponent-only health/effect restoration, scale to the challenger's Aura stage, have an eight-block radius, three-second countdown and two-minute limit, and interrupt on third-party hits. Return to the stand between bouts within one minute. A spell touching a steward disqualifies the run; losses, draws, missing opponents and interrupted runs award nothing.
- A saved prize ledger grants a chosen scroll, three Aura Shards and the three-page Three Bows history once per player per gathering. Sneak-use cycles the selected scroll; normal use claims it. Outstanding prizes persist across saves and later gatherings. The bracket itself does not resume after reload. Creative/Spectator and overlapping duel/spar entries are refused.
- Added distinct stand/standard/book art, four authored sound cues and subtitles, short entry messages, tournament discovery and a complete player-guide chapter. Stewards wait away from the registration stand. Hosted rosters do not consume the wandering-duelist spawn allowance.
- Added `aura_world.tournaments` with backward-compatible settings constructors. The server bounds terrain searches, cached venues and reward ledgers, and never forces terrain chunks to load. An ordinary grass block is accepted explicitly because Minecraft 26.3's dirt tag excludes it.
- Added dedicated integrated-game checks for actual registration/choice/claim packets, resident and terrain requirements, nonlethal restoration, stage scaling, three distinct opponents, finite saved prizes, boundary forfeits, spell disqualification, third-party interruption, lore reading and ordinary-duel behavior. Natural village frequency, real two-client play, a complete server restart, shaders and sustained performance remain separate verification work.

### Highland beasts and useful field tools

- Added Stonehorn, a custom rock-armoured grazer with layered shoulders, moss, cloven feet and curved horns. Quiet approaches and wheat avoid its territorial warning. A two-second stamp precedes a straight charge, followed by a vulnerable recovery; wall impacts leave a longer opening.
- Added Galeclaw, a custom feathered ridge runner with hooked beak, fan tail, talons and articulated wings. It follows recent casting, hunts Rimehares and scavenges raw rabbit/chicken. Its crouch marks a fixed landing before a leap. Highland habitats include shared snowy slopes/groves, suitable ground/open-sky checks, one-animal groups and a two-per-species local cap.
- Added posture-dependent spell resistance through the common damage pipeline. Physical blades/Aura retain normal damage, control magic remains useful, and recovery admits substantially more spell damage. Distinct warnings, physical dust/ground cuts, model poses, fourteen authored sound voices and individual subtitles communicate the encounters.
- Peaceful feeding gives Stonehorn Plates or Galeclaw Plumes on a saved two-minute cooldown. Craft a Bastion Poultice for ten seconds of Resistance I with Slowness I, or a reusable Ridge Whistle to distract stalking Galeclaws. Committed attacks and retaliation resist distraction; cooldowns and consumption are enforced.
- Added two bestiary entries, spawn eggs, item art, recipes/advancements, a player-guide chapter, configurable spawning/aggression and safe entity reload into recovery. The runtime suite verifies resistance, actual attack/evasion, feeding/save-load limits, recipes, tools, control recovery, ecology and habitat registration.

### The sleeping blade and Oathkeeper

- Added rare Marchkeeper resting places in dry highland/open-country habitats, with three surrounding ruin details and a custom cracked stone socket. Only generated sockets hold a blade. The first completed draw permanently claims that site's weapon.
- From Form, kneel with an empty main hand and no standing blade bond, then use the stone. Steel rises through three lift poses over six seconds. Movement, damage, interrupted kneeling, changed intent or an occupied hand cancels the draw; orphan lift poses settle after loading through scheduled checks.
- Added Oathkeeper, a custom shaped diamond-grade sword with wrapped grip, brass guard and an automatic ownership bond. Its origin and modest resonance head start use the existing bond system. While bonded and held, guard costs 15% less and perfect guards build 30% more momentum, while ordinary hits build 20% less.
- Added four distinct sounds with individual subtitles, physical stone-strain cuts, a three-page Last Oath history, player guide and configuration migration. Later visitors receive the empty stone's history once without another weapon. aura_world.sleeping_blades controls new generation and socket interactions; acquired blades retain their bond behavior.

### Sword tombs and the Buried Keeper

- Added rare grass-covered sword tombs with descending stairs, buried blade galleries, two intent thresholds, a physical duel chamber and alternate burial details. Flow opens the outer gate; Edge opens the inner gate and challenges the reliquary. The galleries and encounter rooms use the existing dungeon wards.
- Added the Buried Keeper, a custom blindfolded stone effigy with segmented arms, burial cloth and a long bound blade. Its sweep and thrust lock direction through distinct 1.8-second preparations; its forward guard can be flanked or broken with an axe. Recovery windows are vulnerable. Its effects use sword strokes, stone dust and ground gouges without magic circles.
- Added a saved one-keeper-per-site encounter and participant reward ledger. Eligible participants claim two different technique scrolls, three Aura Shards and a three-page illustrated Testament once per tomb. Reloading cannot duplicate the rewards or resume a dangerous stale attack.
- Added ten authored sounds with individual action subtitles, custom gate/reliquary art, a bestiary entry, player guide, configuration migration and integrated runtime tests. New generation and interactions can be disabled with aura_world.sword_tombs.
- Updated the older Aura-world regression checks for timed crescent clashes: both flights must lock, harm nobody during the contest, and resolve exactly once after the server's network-delay allowance.
- Registered the keeper with shared boss classification so boss control protections and bonded-blade boss progression apply to it.

### The Marchkeeper battlefields

- Fixed Windows line-ending drift in generated physical terrain block assets so regeneration remains byte-identical across platforms.
- Added three authored outdoor ruins: broken barricades, a shelter garden, and retreat cairns with a ruined watchtower. They appear rarely in open-country biomes and reject flooded or steep ground.
- Generated memorials retain a sword tradition. Hold the breathing stance and use one to recover its history and Sunder, Bind or Echo lesson. Each tradition rewards each player once; repeated visits retell all pages without minting more rewards.
- Added three distinct weathered memorial models, three illustrated field-book covers, nine short lore pages, four authored memory sounds, discovery messages and a player guide. The books connect the Marchkeepers' history to the landscape and technique lessons.
- Memories cancel when the stance, blade, range, view or memorial is lost. Only requested rites tick, with no scans of all memorial blocks or forced chunk loads. Added a server switch and old-configuration defaults.

### Training with the landscape

- Dry waterfall banks and exposed mountain summits now reward the breathing stance with faster passive Aura recovery and a little practice experience within the existing lifetime allowance.
- Both landscapes can satisfy the early stillness breakthroughs. Higher-stage tempest trials retain their ley-crossing and thunderstorm requirements.
- Terrain checks are bounded, run at most once every two seconds per breathing player, and do not load chunks. Added server switches and recovery/practice tuning with defaults for older configuration files.
- Began the living-world expansion roadmap covering creatures, ecosystems, magical items, locations, lore, progression and the remaining Aura overhaul. These broader additions are still pending.

### Sparring, masters and timed clashes

- A deliberate sneak-and-use blade salute now asks another swordsman for a spar. Their answering salute begins a three-second count-in on grounds marked by cloth standards and fractured floors. Blades and Aura reach a partner; spells, projectiles and pets do not.
- Spars finish at one heart without killing either participant. Walking out concedes, the timer ends evenly, and outside harm calls the spar off. Both players regain their partner's damage and spent Aura; awakenings stay paid and end, and momentum starts and ends at zero.
- Real two-sided spars teach Aura experience with a daily cap per partner. Quick or one-sided bouts grant nothing. Sparring does not grow bonded blades, mastery, technique ranks or ordinary combat trials.
- Form masters can take disciples at least two stages below them through a cancellable breathing-and-kneeling ceremony. New disciples learn their master's method; disciples with another method receive a manual they may choose to read.
- Daily lessons teach unknown technique parts or help eligible disciples find their crossroads. Nearby disciples learn ordinary Aura experience faster; their breakthroughs return a configurable share to their master, saved while the master is offline.
- Besting a master by knockout in a real spar can meet a waiting trial for a stage below the master's own. Reaching their stage graduates with honour. Either person can release a bond, including offline bonds, through two clicks in the new Lineage tab.
- Opposing crescents and eligible arts now enter timed clashes. Attack becomes a rhythm press during the lock; three server-judged beats award perfect/good grades, misses and fumbles. The Blade widens timing windows and wins equal scores rather than automatically winning. Winning crescents carry reduced damage.
- Added compact consent/progress/rhythm HUDs, paginated lineage records, 21 authored sounds, player text, operator commands, addon hooks and server settings.
- Fixed lag allowance closing clash windows too early, lessons continuing after release, interrupted ceremony attachments remaining on a disciple, outside creature/environmental damage failing to call off a spar, and stale clash/salute cooldown records accumulating.
- Added a client/server interaction suite and a player guide covering the controls, rewards, limitations and configuration.

### Feathered Soar wings

- Soar now grows a pair of articulated feathered wings with layered shoulder feathers and long flight feathers. They fold on the ground, spread in flight, beat faster while moving, and fold and fade as flight ends.
- Wings follow the player's interpolated position and body rotation, remain visible from either side, and stay outside the owner's first-person view. One persistent model replaces repeated light-stroke particles; feather detail reduces with distance and calmer settings.

### Aura standards and pressure effects

- The crossroads now raises four animated cloth standards with stitched edging, swallowtail hems, bronze poles and distinct sword, shield, crescent and rallying crests. The selected standard gains pressure streamers; the others dim.
- Rallying Cry raises a small version of the Banner standard above the swordsman. It stays out of the owner's first-person view.
- Technique announcements use a woven nameplate and Way crest, with smaller type and bounded long names to leave the aiming area clear.
- Aura awakenings, Dominions, finishers, blade bonding and duelist boundaries use fractured floors, crater impressions, blade gouges, floor debris and sharp slash strokes. Rune-bearing circles and ritual stars are reserved for spell casting.
- Ground effects use world lighting, skip open air and changed floor blocks, and fade without removing terrain. Ground scars are capped at 24 live effects.
- Elemental sword impacts use dedicated Aura effects, removing the spell seals hidden inside shared frost and impact helpers. Each method keeps its own slash pattern and timing.
- Standard refreshes update an existing cloth effect, with a bounded cache and cleanup when leaving the world. Add-on Ways receive a neutral crest tinted in their registered colour.

### The bonded blade
- **Bond a blade of your own from Edge.** Kneel in the breathing stance at a **ley crossing** with a sword, an axe, a spear or the
  mace in hand (aura-forged ones too) and hold still: the ley lines light across the ground and run into you, the blade kindles from
  the hilt up, a ring closes in, and ten seconds later a column of light seals the bond. One blade at a time.
- **It grows with every real fight.** Foes felled, arts and finishers landed, stances broken, perfect guards, awakenings, duelists
  beaten, techniques reaching Peerless and your breakthroughs all give it **resonance** (never a training dummy, and never one patient
  foe pounded for ever). It rises from Bonded to **Named**, **Awakened** and **Soulforged**, each waiting on your stage too (and
  Soulforged on a boss felled with it): Named in your first evening with it, Soulforged at the end of a long road.
- **At Named it takes a name**: one of its own, built from its story (your element, where it was bonded, what it fells, the art you
  play most: Cinderwake, Oath of the Dunes, Meteor Oath), or one you give it. Its name is kept to 24 characters and shown to everyone
  as plain text.
- **At Awakened it takes a trait drawn from how you fought**: it offers the three your habits show most strongly, out of thirteen
  (your favourite art cheaper, finishers giving back more aura, stances sundered sooner, a riposte after a perfect guard, a cheaper
  Aura Step or a longer, cheaper Aura Slash, techniques ranked faster, a quicker awakening, more against bosses or the undead, less
  harm at your last light, more aura by night, momentum for your allies). Each is small and even, and fair to other players. At
  **Soulforged** it's half again as strong, and you may choose again.
- **It glows in your aura's colour**, more by tier: a vein of light beating like a heart, then marks running up the steel as if its
  name were written there, then its aura licking off the edges, then a white-hot edge, a ring about the guard and a corona. Lying on
  the ground it lights a pool under itself (a Soulforged one a column you can see from afar); in anyone else's hand it's cold.
- **Its tooltip tells its story**: where and when it was bonded, whose it has been, its deeds counted, the art it played most and its
  latest great deeds (hold Shift for all of it). The Aura page's new **Blade** tab shows it all, lets you name it and choose its trait.
- **Only ever yours.** It's kept through death (keepInventory or not), never breaks (it only notches), and can't be lost: nobody else
  can pick it up or take it from a chest, hoppers and mobs leave it be, it never despawns, burns or melts, and from the void, or from
  anyone else's hands, it comes straight home to you (held for you if you're away). In a stranger's hands it carries no aura and no
  trait, so stealing it gives nothing.
- **Pass it on**: a master in the breathing stance with their blade, a disciple kneeling before them, and in eight seconds it's theirs,
  its tier, name, trait and story kept and its master written into its lineage. Who counts as a disciple comes with masters and
  disciples; until then only an operator can pass one. Or release it, and it's only steel again.
- **For server owners**: `aura.bonded_blades`, `resonance_gain`, `bond_at_power` and `blade_traits`, and `/wildercord aura blade`
  (`bond`, `resonance`, `tier`, `name`, `trait`, `release`, `pass`). **For add-ons**: `AuraApi.onBlade`, `allowBladePassing`,
  `passBlade`, `bondBlade`, `addResonance`, `registerBladeTrait` and the readers `bladeBond`, `bladeTier` and `bladeTrait`.

### Techniques of your own
- **Write your own techniques from Edge.** A technique is a **stroke** (thrust, rising cut, falling cut, sweep, spin or draw), a
  **release** (on the blade, a **wave** flying on past it, a **burst** all round you, or an **afterimage** left standing where you
  struck that strikes again a moment later) and an **intent** (pierce, sunder, bind, echo, ward, rally or infuse), with your method's
  element in every one: an Ember technique burns, a Rime one chills, a Thunder one throws a spark on to a foe it missed, a Verdant
  one mends you, and so on. Name it, give it a string of swings, and it goes off when you play that string, naming itself in your
  banner like an art.
- **The Aura page's new Writing tab**: three seals on a cord for the stroke, the release and the intent, a readout of what it does
  and costs beside a little diagram playing the shape it strikes, your name for it inked as you type, its string composed from the
  seven swings, and a line saying whether it's ready (or what's in its way, and which goes first when an art's swings fit it too).
  One slot at Edge, a second at Form, a third at Sovereign.
- **Find the parts in the world.** The draw, on the blade and infuse are every swordsman's from Edge; the rest come on **technique
  scrolls** in trial chambers, ancient cities, strongholds, bastions, temples, mansions, outposts and every expedition's vault (and
  now and then a fallen knight's), each place likeliest to hold certain parts. A **duelist** you beat shows you a part before it
  goes, and your **Way lends** you one while you walk it (the Bulwark's Ward and the Banner's Rally are found nowhere else).
- **Techniques rank up as they land** on real foes, from Raw to Peerless: **Honed** lets you temper one (swift or heavy), **Keen**
  gives it an edge (long or broad), and a **Peerless** technique rings its name out in gold and can have its parts set down on
  scrolls for another swordsman.
- **Never better or worse than an art**: every technique is weighed on the scale the fifty arts were, so the more it does, the more
  aura it costs and the longer it rests. Momentum, stance, finishers and awakening (free while awakened) all treat it as an art.
- **Fair and safe**: a technique deals a player at most 8, its holds are short, and its name is kept to 24 characters with
  formatting and invisible characters taken out, shown to everyone as plain text.
- **For server owners**: `aura.techniques`, `technique_damage`, `technique_xp_multiplier` and `technique_scroll_chance`, and
  `/wildercord aura technique learn|forget|xp|clear`. **For add-ons**: `AuraApi.teachPart`, `techniqueScroll`,
  `registerScrollSource`, `drawScrollPart`, `registerTechniqueIntent`, `onTechnique` and `nameArts`.

### Ways
- **Your path forks at Edge.** When you break through to Edge a **crossroads** rises round you: four standards of light, one for
  each **Way**. Strike the one you mean to walk with your blade (once to lean toward it, again to walk it) and its light pours into
  you while the other three break apart. Looking at a standard names its Way and its three nodes, low on your screen; you can open
  the Aura page and read them in full while it stands.
- **Four Ways, each a node at Edge, Form and Sovereign**, each node a passive and a change to something you already do:
  - **The Blade** (offence): clean hits build momentum faster and your **slash pierces** (through a guard, through ten foes, and
    through another crescent it meets); finishers hit harder and **cascade**, opening the next worn-down foe so a crowd falls one
    after another; awakened, your slash is free and ready every second, and finishers feed your awakening twice as long.
  - **The Bulwark** (defence): a stance that's hard to break and a **guard that covers every side**, stays perfect longer, throws a
    third of every blow back and turns shots; sturdier aura armour and an Intent that **draws foes off your friends** and staggers
    whatever hits your guard; awakened, nothing breaks you and blows land a fifth weaker, you're never slowed when spent, and your
    Dominion turns back every shot fired into it.
  - **The Shadowstep** (movement): blows from behind open foes faster and a **perfect guard slips you behind** whoever struck; after
    an Aura Step every blow counts as from behind and the **afterimage you leave strikes** where you were; a finisher from behind
    readies your step at once, and awakened your step is free, quick and its afterimage strikes twice.
  - **The Banner** (together): your **momentum and aura are shared** with your allied swordsmen near you, every finisher lets out
    a **rallying cry** that steadies you and your friends, your Intent steadies everyone near, your awakening holds your allies'
    momentum high, and your **Dominion shelters the party**. It only ever helps allies (your team, or players who couldn't fight you
    anyway), never anyone who could fight you.
- **Already past Edge?** Hold the breathing stance for a few seconds and the crossroads rises for you; your first choice is free
  and gives you every node you've reached at once.
- **Changing your Way** takes a **Crossroads Incense** (two Aura Shards, an amethyst shard and blaze powder) burned at a **ley
  crossing**: your Way is burned away, the crossroads rises again, and the new Way's Form and Sovereign nodes wake as you earn
  experience walking it. Your Way is kept through death.
- **The Aura page's new Way tab** shows the tree: every Way's nodes, yours in force framed in gold, waking ones filling, ones still
  to come outlined, other Ways' dark, and the one you pick in full.
- **Fair against players**: finishers and stance from behind stay inside the caps they always had, everything thrown back is aura
  off the blade and meets a player's defences, and the Banner's steadying is held to three tenths and scaled down against players.
- **For server owners**: `aura.ways`, `way_settle_xp`, `way_change_at_power`, `banner_range`, `banner_share` and `banner_aura_share`,
  and `/wildercord aura way <way|none|crossroads>`. **For add-ons**: `AuraApi.registerWay`, `wayOf`, `hasWayNode`, `chooseWay`,
  `unbindWay`, `openCrossroads` and `onWay`, to read a Way or add your own.

### Awakening
- **Awakening.** From Edge, with your aura full and your momentum at half or more, **tap the Aura key and then hold it** and you let
  everything go at once: for 12 seconds at Edge, 16 at Form and 20 at Sovereign your arts cost nothing, your momentum holds at its
  peak (and with it your Final Art opens), and you're a little faster and hit a little harder. Every finisher you land feeds it
  another second. A ring fills round your crosshair as you hold, so you can let go in time; a quick double tap is still the step
  and a lone hold still Dominion.
- **The moment is worth seeing.** Your aura draws in, then bursts: a column of light, a shockwave racing out over the ground, foes
  close by thrown back a step, and your method's own flourish and voice (a ring of flame, ice bursting, bolts from a clear sky, a
  rising wind, the ground cracking, a flower of light, a black point bursting, a star, a clock face, crescents of blood). Your
  body's aura climbs past your stage: your eyes burn, a mantle and a corona of fire, streamers of light racing up round you. In
  first person it stays out of your way: a banner, a faint glow at the edges of the screen as it bursts, and the time left by
  your aura bar.
- **Then you're spent.** When it ends whatever aura you had left burns away and for half a minute you're slowed and can't gather
  any aura at all: no guard, no slash, no arts until it passes. The next awakening waits three minutes.
- **The Sovereign's awakened Dominion.** Raised while you're awakened, Dominion is wider, longer and harder on foes, chains to two,
  and is your method's own: *Throne of Cinders* (Ember: foes inside burn), *Court of Winter* (Rime: they freeze, then chill),
  *Seat of Storms* (Thunder: a bolt each second), *Windward Ground* (Gale: updrafts throw them up, shots turn aside from you),
  *Unmoving Mountain* (Stone: you're hardened, their stance worn), *Wildwood Court* (Verdant: roots, and mending for you and your
  allies), *Sunken Hall* (Hollow: silenced and drawn in), *Field of Stars* (Starlit: falling stars, aura back faster), *Stilled
  Hour* (Hourglass: held still, then slowed, shots too) and *Crimson Court* (Crimson: they bleed, you drink).
- **Fair against players**: its harder blows count half against a player and inside the usual caps (a few percent in the end),
  and every hold and throw of an awakened Dominion is held to what an art may do.
- **On your aura bar**, a small flame after the stage diamonds: breathing gold when you could awaken, blazing while you are, ash
  while you're spent, filling back while it rests. The Aura page shows it too, and lists Awakening with its key.
- **For server owners**: `aura.awakening`, `awakening_momentum`, `awakening_duration`, `awakening_cooldown_seconds`, `spent_seconds`,
  `awakening_art_price`, `awakening_damage` and `awakening_speed`. **For add-ons**: `AuraApi.onAwakening`, `awakened`, `spent`,
  `awaken`, `endAwakening`, and the Aura key's new `Trigger.TAP_HOLD` for techniques of your own.

### Momentum and openings
- **Momentum.** A clean fight now builds momentum, a thin line under your aura bar: full swings that land, arts that land,
  perfect guards and Aura Steps taken through an attack all fill it. A hit you take knocks a share off, and out of a fight it
  ebbs away in a few seconds. At each quarter your arts cost less (up to a quarter off) and strike harder (up to a fifth), and
  wear your foes down faster. At the peak the line turns gold and your aura blazes for everyone to see.
- **The Final Art waits on peak momentum** now, not a full pool of aura, and playing it spends some of it. (A server with
  momentum switched off keeps the full pool.)
- **Each method's momentum moves its own way**: Ember burns hot and out, Rime and Verdant hold theirs longest, Stone barely gives
  ground to a blow, Thunder feeds on crowds, Gale on steps, Hourglass holds the moment, and Crimson feeds on blood, its own too.
  Every method builds fastest on foes in the state its own arts leave them in: burning, frozen, ionised, thrown up, cracked,
  rooted, silenced, starred, stopped in time or bleeding.
- **It can't be farmed.** Weak swings, the slash and anything from a bow or a spell build nothing; a foe that can't fight back
  (with no mind of its own, or stuck in a boat or a cart) gives nothing; each foe gives only so much until you break its stance;
  a training dummy teaches it only to the third tier (the practice arena to the peak, emptied when you leave); and it's gone a
  few seconds out of a fight.
- **Stance and openings.** Every foe has a stance, a thin bar over its head once you start wearing it down: blows wear it by what
  they deal, arts twice as fast, a perfect guard a third of it at once, and Stone's blades most of all. When it breaks the foe
  is **opened**: staggered for a few seconds, a cracked gold seal beating over its head, and two small brackets closing in round
  your crosshair. Your next full swing on it is your method's **finisher**: a grand, named strike that deals a third of what the
  foe has already lost on top of the blow, gives aura back and builds momentum. Then the foe stands steady a moment.
- **Ten finishers**, each its method's own look and voice: *Pyrebrand* (Ember: an X of fire and a pillar of flame), *Winterbreak*
  (Rime: frost bursting into shards and spires of ice), *Skysunder* (Thunder: a bolt out of the sky), *Windscour* (Gale: a spiral
  of wind that lifts its foe), *Faultline* (Stone: the ground cracking and stone bursting up), *Thornbloom* (Verdant: roots and a
  flower of light that mends you and your allies), *Nullfall* (Hollow: everything falling into a black point, then bursting),
  *Starbreak* (Starlit: a star bursting in rays, the most aura back), *Hour's End* (Hourglass: a clock face striking the hour and
  a second cut) and *Heartrend* (Crimson: a tearing crescent that bleeds its foe while you drink). A method without its own lands
  *Decisive Cut*.
- **Bosses** have far more stance, take less from each blow and recover quickly, are only slowed when opened (never held), take
  a smaller finisher, and stand steady long after, a little steadier each time: breakable, never trivial.
- **Players have a stance too**, so a duel rewards pressure and guarding: no blow wears more than a third of it, a held Aura
  Guard or a raised shield takes the wear itself (so a player who only blocks breaks in the end), and a perfect guard breaks into
  the attacker's. Opened, a player is slowed and their guard broken for a moment, never held still, and a finisher on them adds
  at most a few hearts, through their armour and their totem. Never a one-shot. Teams and the PvP rule are respected.
- **On the Aura page**: momentum and finishers among the techniques, your method's finisher under its arts, a line on what your
  momentum gives you, and your arts' prices at your momentum.
- **Server settings**: `aura.momentum`, `momentum_gain`, `momentum_ebb`, `stance`, `stance_damage`, `finisher_damage` and
  `pvp_stance`.
- **For add-ons** (`api.AuraApi`): read, add and hold a swordsman's momentum, open the Final Art some other way, change what
  wears a stance, hear of every stance broken and finisher landed (and change what a finisher deals), and give a method its own
  finisher.

### Sword arts
- **Every breathing method has five arts of its own**, fifty in all, on the same five sword strings, so you learn the strings
  once and your method decides what they do:
  - **Ember**, fire that spreads and lingers: *Kindling Draw* (a draw-cut and a line of fire racing on ahead), *Rising
    Cinders* (foes thrown up in flames, cinders raining off them), *Backdraft* (the blow you caught thrown back as a gout of
    flame), *Wildfire Rush* (a second rush through foes, the ground burning behind) and *Sunfall* (a leap, a sun on the blade,
    a blazing landing and a ring of fire).
  - **Rime**, slow, freeze, shatter: *Frostbite* (crusts that freeze at the third), *Hailfall* (a cloud of hail over the foe),
    *Glacier Mirror* (whoever struck frozen, and a mirror of ice that turns arrows back), *Skate* (a glide on a path of ice that
    shatters frozen foes, across water too) and *Winter's Hush* (a cone that freezes everything, then shatters it).
  - **Thunder**, faster than the eye: *Crackle* (three cuts in a blink, sparks leaping off), *Skyfall* (a bolt called down),
    *Static Riposte* (lightning leaping on through the foes near), *Bolt Step* (blinking from foe to foe) and *Heaven's Spear*
    (a charged lance of lightning eighteen blocks down your line of sight, the sky striking everything it ran through).
  - **Gale**, reach and the air under them: *Cutting Breeze* (a wind blade flying twelve blocks on), *Updraft* (foes thrown high
    and your blade biting harder while they're up), *Eye of the Storm* (a spinning counter, then arrows turned aside), *Tailwind*
    (a long dash that speeds you and your allies) and *Hundred Winds* (a whirlwind of cuts that draws foes in and throws them up).
  - **Stone**, weight and footing: *Rockbreaker* (a heavy cut that shakes a foe's footing), *Avalanche* (a slam and a rolling
    shockwave), *Unmoved* (whoever struck hurled back, and you hardened like stone), *Landslide* (a charge that carries foes
    along and crushes them against walls) and *Mountain Splitter* (the ground split in a line of rising stone).
  - **Verdant**, mending and binding: *Thorn Lash* (a thorned vine flicked past your sword's reach that roots its foe),
    *Blossom Fall* (a carpet of blossom where you land that mends you and your allies and slows foes), *Rooted Parry* (whoever
    struck seized by roots, and you mended by the blow you caught), *Wild Growth* (a rush that leaves brambles behind) and
    *Grove's Heart* (the blade planted, roots bursting under every foe near and a grove of trees standing round you a while).
  - **Hollow**, drawing in and silencing: *Void Cut* (a tear at the blade's end that draws foes to you), *Collapse* (a well
    that drags everything near into it, then collapses), *Null Parry* (the blow swallowed, whoever struck silenced), *Rift
    Step* (through a rift to behind the foe ahead, cutting its back) and *Event Horizon* (a black sphere that drags everything
    in and crushes it).
  - **Starlit**, stars that burst and aura given back: *Star Needle* (three seeking darts of starlight), *Meteor Shower* (stars
    brought down over a circle, a great one last), *Constellation Guard* (whoever struck set with stars that burst one by
    one), *Comet Dash* (a rush whose trail of stars bursts behind you) and *Nova* (starlight gathered on the blade and burst
    out in a ring). Their stars set on foes burst for more under the next Starlit art.
  - **Hourglass**, time bent: *Echo Cut* (your cut repeated by an afterimage), *Rewind Leap* (a falling cut, then time snaps
    you back to where you leapt from), *Stopped Moment* (whoever struck held still in time, then flung back as it moves
    again), *Blur* (a rush faster than any, and time dragging round you: foes and arrows slowed) and *Thousand Moments* (time
    stopped round you while your cuts gather, landing all at once).
  - **Crimson**, blood and its price: *Bloodletting* (a cut that leaves a bleeding wound you drink from), *Red Rain* (a red
    rain where you land that bleeds every foe under it), *Sanguine Parry* (the blow you caught bled back out of whoever
    struck), *Frenzy* (a rush whose every cut quickens your blade) and *Crimson Moon* (a great arc of blood-light that drinks
    deeply, paid for with a quarter of your health).
- **Every art looks and sounds like itself**: its own trail, light, impacts and voice over your method's. The big shapes are for
  everyone watching you and for you in third person; in first person you see a thin, low version that keeps the middle of your
  view clear.
- **Balanced slot by slot.** Every method's art in a slot costs and rests the same and is worth about the same, so a method is a
  different answer, never a better one, and each method leads in its own thing: Ember the most damage, Rime the most hold,
  Thunder the most foes, Gale the most reach, Verdant the most mending, Hollow its pulls and silence, Starlit its aura back,
  Hourglass its moments held still, Crimson its bleeding and drinking. Damage follows your blade.
- **Healing that never outpaces a fight.** Everything arts mend, Verdant's mending and Crimson's drinking together, gives one
  body at most five hearts at once and half a heart a second after, so a mending swordsman lasts longer but can still fall.
- **Crimson Moon can't kill you.** Its price comes straight off your health but never below a heart.
- **Fair to other players**: an art never deals one player more than 8 damage, meets armour and the spell defences, holds a
  player still for at most three quarters of a second (and not again for 4 s; a root or a stopped moment counts), drags a player
  slower than they can run, silences one for at most a second and a half (their guard still works), and respects teams and the
  PvP rule. Bosses are only ever slowed.
- **Nothing is griefed.** Fire, ice and stone left by an art are light and shapes. Skate's ice over water is real so you can glide
  across, only where you could build, and it thaws.
- **The Aura page's Sword strings tab** shows every method's arts: click a method's colour to read its five, with their strings,
  stages and prices. **Your Grimoire** records each art the first time you play it, with a toast.
- **A new guide page, Sword Arts**, lists every art with its numbers. Server owners can scale arts' damage
  (`aura.art_damage`) and stop Skate freezing water (`aura.art_terrain`).
- An add-on's breathing method with no arts of its own plays the common arts.

### How aura looks and sounds
- **Your blade leaves light where it cuts.** With aura enough to coat a blow, every swing trails a ribbon of your aura's
  colour along the blade's arc, cut the way you swung: a cut down across you (each swing of a quick run cutting back the other
  way), a low cut across the legs, a leaping cut from above, a straight thrust for a running swing or a spear, an X for a
  counter. Trails grow with each stage: motes from Flow, a bright edge from Edge, an echo from Form, sparks at Sovereign. By
  day a faint rim of shadow keeps them readable against the sky.
- **Flow's sweep is yours to see now.** A full swing at a foe sweeps wide and level round you in your aura's colour, the
  sweeper included, in place of the grey sweep crescent.
- **Your body shows your stage.** A faint shimmer at Glow, wisps rising from your shoulders and blade at Flow, a steady haze
  and a pool of light at your feet at Edge, a mantle of light streaming off your shoulders at Form, and at Sovereign a corona of
  your aura's fire and burning eyes. It's calm at rest, flares in a fight, and surges with every art, perfect guard, step and
  Dominion.
- **Blows land with weight.** A coated blow flashes where it bites; a full swing holds the moment for a few frames (a
  hit-stop, on your screen and the struck player's only) with a small nudge of the view; arts and the slash land heavier.
- **Techniques name themselves.** An art or a Dominion slides its name in at the left edge of your screen in your method's
  colour; other swordsmen's float over their heads.
- **Every breathing method has its own voice**: its own swing, blow and technique sounds, from Ember's crackling roar to
  Rime's ringing ice, Thunder's snap, Hollow's implosion and Crimson's heartbeat.
- **Your own view stays clear.** In first person your trail is thin, short and low, down the side of the view and never across
  its middle, even looking down at a foe; your body's aura is only a faint glow at the bottom edge while it flares; your impacts
  are small; a perfect guard is a thin gold glint low in the view instead of a flash across it; and your blade's own glow sits
  closer to the blade, so a swing or a guard no longer floods the screen.
- **New visual settings** in Magic visual settings: blade trails, body aura, impact (hit-stop and flash) and technique
  banners, each with a softer and an off choice. Camera motion off also stops the nudge, and the performance profile softens
  them all.

### Sword strings
- **Your swings are a language.** With an aura weapon in hand, play a short string of ordinary swings, each in time with
  the one before, and your blade answers with an art, one for each stage:
  - **First Art** (Glow): swing, swing, then a low (crouching) swing. An arc of aura in front of you.
  - **Second Art** (Flow): a leaping swing, then a low swing. A rising arc that throws the foes in front into the air.
  - **Third Art** (Edge): a counter, your first swing straight after a perfect Aura Guard. A cut that staggers that foe again.
  - **Fourth Art** (Form): a step cut, your first swing straight after an Aura Step. A line of aura five blocks ahead.
  - **Final Art** (Sovereign): three full swings, then a low swing, with your aura full. A whirl of cuts and a ring round
    you that throws foes back.

  These are the common arts; five methods now answer the strings with arts of their own (see Sword arts). Arts cost a little
  aura and rest a few seconds (the Final Art half a minute), never spend past empty, and carry your element like the slash.
- **Paced to your blade.** Each swing must come within half a second of your blade being ready again: about a swing a second
  with a sword, longer with an axe or a mace, and quick half swings always keep up. Pause and the string breaks. Swings at
  the air count only in a fight, and digging never does, so nothing goes off by accident.
- **When strings overlap,** the art that asks the most goes (three full swings and a low one are the Final Art, not the
  First). If it's resting or your aura is short, the next art the same swings spell goes instead; if none can, you're told why.
- **A small row of marks below your crosshair** shows each swing as it lands, with a thin line for the time left. It lights
  gold when an art goes, and shakes red for a fumble, a finishing swing that came too late. Soft ticks climb with each swing,
  an art chimes and a fumble clanks. Magic visual settings moves it above the aura bar or hides it.
- **The Aura page has a Sword strings tab** with your arts, their strings, prices and rests, and what every mark means.
- **The server checks every art:** your stage, its rest and price, a blade in hand, and that the swings, guard or step
  really happened. Server owners can switch strings off (`aura.strings`) or change how long each swing may wait
  (`aura.string_window_seconds`).

### Cinnamon
- **Truer to the real Cinnamon.** A rounder head with a small snout, plain black eyes under little angry brows, shorter
  ears, fluffy legs that fade to cream at the feet instead of skinny legs on big paws, a white belly that starts at her
  chest, and a little black nub of a tail.
- **Her collar bell** jingles every 20 to 40 seconds, loud enough to find her by ear. Put `"bell": false` in
  `config/wildercord-cinnamon.json` to quiet it.
- **Now and then she sticks the tip of her tongue out** for a few seconds, asleep or awake.
- **She defends you.** Anyone who hurts her owner gets bitten (not while she's sitting, and never creepers, your other
  pets, or players you can't fight).
- **Cinnamon's Bow**, her "armour": two string and pink dye. Use it on her and she wears it, even after she's
  re-summoned; shears take it off again.

### Fixed
- **A new body starts with its aura empty again.** After a death the aura you held was copied back onto your new body (the
  path is kept through death; the aura held never was meant to be). It now empties as it should.
- **The Aura badge no longer hides under the Cosmetics tab.** The Cord screen's header ran out of room once 0.9.0 added
  the Aura badge. Its page tabs now tighten to fit, and a long Cord name is cut short (hover it to read it whole). The
  Cosmetics page lays its header out the same way, so the tabs don't jump when you switch.

## [0.9.0-alpha] - 2026-10-01

The wilds come alive and the sword finds its path. Twelve new creatures (six monsters with tells and counterplay, six
enchanted animals) and a field guide in the Grimoire, and **Aura**: a swordsman's path beside the Cord, from a haze on
the blade to a domain of your own, with ten breathing methods, breakthroughs, duelists who teach them, fallen knights,
aura-forged gear and a spell that rides your slash. Players need 0.9.0 to join a 0.9.0 server (it adds creatures, items,
sounds and synced data). Back up your worlds before updating.
Full release notes: [0.9.0-alpha](docs/releases/0.9.0-alpha.md).

### Monsters of the wilds
Six new magical monsters make the wilds dangerous again, each with its own model, glow, voice and way of fighting, a
tell before its big attack and a way to beat it. They spawn on their own where they belong (a few in fifty of the
monsters where they live, inside the game's own monster cap), never on Peaceful.
- **Bramblewalker** (forests at night, most of all dark forests and the pale garden): a hunched, walking thicket of bark,
  moss and thorn. It rears back, its vines glowing, and lashes a vine along the ground at where you stood: caught, you're
  rooted where you stand for a breath (a second and a half on Normal). Step aside as it rears. It's dry wood: fire hurts
  it half again as much, and set alight it panics and runs. Drops **Living Bramble**: throw it and it roots what it hits
  for two seconds (it composts well, too).
- **Gloomstalker** (dark forests at night, and the deep caves below 0 anywhere): a shadow panther that's all but
  invisible in the dark, two violet eyes hanging in the gloom. It circles, crouches (its eyes flare: the tell), pounces,
  and slinks away into the dark to try again; a missed pounce leaves it sprawled. Light undoes it: a torch's glow, day,
  standing close, glowing, or a spell of fire, storm, arcane or life (which lays it bare for six seconds). Drops
  **Shadow Pelt**, which brews a Potion of Invisibility straight from an Awkward Potion.
- **Thunderwing Harpy** (the bare peaks and windswept hills, from 90 up, at night or in a storm): a storm-feathered hunter
  that circles out of reach, hangs a moment and shrieks with its wings flung wide, then dives. A dive that misses ploughs
  into the ground and leaves it stunned: dodge, then punish. In a thunderstorm it calls lightning on a ring of light
  where you stand. Any earth spell drags it out of the sky; frost stiffens its feathers. Drops **Storm Feathers**: use
  one and a gust lifts you a few blocks and lets you drift down.
- **Geode Crawler** (caves below 50, mostly near amethyst geodes): a crystal-backed beetle. Struck, it curls into a ball
  of crystal that only a fifth of a blow gets through, rattles, and rolls at you; a roll into a wall dazes it. A mace, a
  pickaxe, a blast or a shock (storm magic, lightning) cracks it open. Drops **Geode Grit** (the earth reagent) and
  amethyst shards.
- **Bog Witch-Frog** (swamps and mangrove swamps at night): a frog the size of a cow, a lily pad on its head for a hat.
  Its throat swells and glows before it lobs a slow bubble of bog poison that bursts where it lands; shoot or strike the
  bubble in the air and it pops harmlessly. Its tongue snatches chickens, rabbits, bats, silverfish and the smallest
  slimes, and it swallows them whole, healing, its next bubble fatter; up close it lashes its tongue at you to drag you
  in (a raised shield turns it). Drops **Bog Gland**: with an Awkward Potion it brews Water Breathing, with a Thick Potion
  Leaping.
- **Mana Ooze** (the deep caves below 0, or caves below 40 under a ley line): a slime of clear jelly that eats spells. A
  spell's harm sinks into it and only fills it; full, it grows a size, healed, and full at its biggest it bursts into
  two. Its touch drinks a little of a caster's mana. Blades, arrows and fire (spells or flames) beat it; it never splits
  when it dies. Drops **Mana Gel**: eat it for 15 mana at once.
- **Any of them can be Runebound**, carrying a spell that suits it (a Bramblewalker's roots, a harpy's lightning bolt, a
  Mana Ooze's silence), with the Runebound's nameplate, aura and drops.
- **Every monster has its affinities** (a Bramblewalker weak to fire, a Gloomstalker to arcane, a harpy to earth and
  frost, a Geode Crawler to storm, a frog to frost, a Mana Ooze to fire, and their resistances), which the Grimoire's
  Bestiary learns as you fight them.
- **Their magic lands like any spell**: a harpy's lightning, a frog's poison and an ooze's burst go through armour,
  Warding, the Potion of Warding and the spellguard.
- **A spawn egg for each**, in the Wildercord tab and the Spawn Eggs tab.
- **Server settings**: a new `monsters` section with a master switch (`enabled`), `spawn_rate` (0 to 4, read when a
  world loads) and a switch for each monster (at once, with `/wildercord reload`). An older config file gains it at its
  defaults.
- **The feel kit has a monster part** for the creatures' own voices, with subtitles that name them.
### Magical wildlife
Six new creatures to make the world feel enchanted between the fights. None of them is hostile; each belongs to an
element and a land, and each leaves something behind that's worth a little.
- **Glimmerwings.** Soft-glowing moths that flutter in little swarms at night through forests and flower fields,
  moonlit blue in most woods, rose in flower forests and cherry groves, amber in meadows. They circle lamps, torches and
  campfires, and anyone who has just cast a spell, leave a faint glittering dust behind them and fade one by one at
  daybreak. They drop **Glimmer Dust**, which brews Night Vision from an Awkward Potion and turns an ink sac into a glow
  ink sac.
- **The lumen stag.** A rare, shy deer of old forests, taigas and cherry groves, always alone, whose crystal antlers
  burn brighter as the moon fills. It bolts from anyone who walks up to it, and lets someone sneaking quietly come close:
  stay beside it, still, for a few seconds and it bows and sheds an antler at your feet, once a day. A **Lumen Antler**
  can take the diamond's place at the heart of a Mana Crystal. A stag never drops one: killing one leaves nothing, brings
  five minutes of Bad Luck, and frightens every stag nearby.
- **The mossback tortoise.** A huge, slow tortoise of swamps, mangroves and jungles with a little garden on its shell:
  blue orchids and a mushroom in swamp moss, a propagule and a lily pad in the mangroves, ferns and a dandelion in the
  jungle, growing over to match if it wanders into another of those lands for long enough. Struck, it pulls into its
  shell and takes far less. It loves melon: two fed melon raise a big-headed baby with a bare shell. Now and then it lets
  a **Mossback Scute** go, which brews the Turtle Master and mends a turtle shell.
- **The cinderfox.** A big-eared fox of deserts and badlands whose tail ends in a living ember that sparks at night.
  Tame one with rabbit (one try in three): it follows, sits when told and fights for you, and its bite sets anything
  weak to fire alight and hurts it half again. Brush a tame one for an **Ember Tuft** once a day: it brews Fire
  Resistance and burns long in a furnace.
- **The skyray.** A manta of the open sky, three and a half blocks from wingtip to wingtip, gliding in slow loops high
  over mountains, windswept hills and meadows, its back full of stars at night. Rare, and never more than two in one sky.
  Now and then a **Skyray Membrane** falls from it, which brews Slow Falling and mends an elytra.
- **The rimehare.** A quick snow hare that goes in bounds, leaving pairs of frost prints that fade as you watch. It bolts
  faster than you can run, unless you hold out sweet berries and don't run at it. It drops **Rime Fur**, which weaves
  Rimebound armour in place of packed ice, and four of which make a piece of leather.
- **Their own voices**, made for each: wing whirr, a stag's glassy call and the chime of a falling antler, a tortoise's
  grumble and plod, a fox's yip and the crackle of its embers, a skyray's long song carried down on the wind, a hare's
  squeak and the crunch of its landing.
- **Spawn eggs** for all six, in the creative tabs. Each creature has its own affinities: glimmerwings fear fire and
  wind, a stag's light is snuffed by void, a tortoise shrugs off earth and wind but feels the cold, a cinderfox resists
  fire and fears frost, a skyray rides out wind but not a storm, a rimehare resists frost and fears fire.
- **The Grimoire's field guide.** A creature seen up close for the first time goes into your Grimoire with a toast (and a
  little mana toward your next circle). The Grimoire lists every creature you've met with a short entry, and the rest as
  a hint of where to look.
- **Server settings**: a new `creatures` section in `wildercord.json` switches wildlife's natural spawns off
  (`wildlife`, on), scales them (`wildlife_spawn_multiplier`, 1.0) and switches each creature on its own
  (`glimmerwing`, `lumen_stag`, `mossback_tortoise`, `cinderfox`, `skyray`, `rimehare`). An older config file gains the
  section at its defaults. They spawn in vanilla's own pools, so the mob caps hold, and the rare ones stay rare and keep
  their distance from each other.
### Aura: the swordsman's path
The swordsman's path. Aura is mana drawn into the body and out along a blade: learn a breathing method, gather aura from
real blows and a still, steady breath, and climb in leaps from a haze on the weapon to a blade of solid light. It's open
to everyone alongside the Cord, needs no Cord at all, and the two paths combine. Players need this version to join a
server running it (it adds an item, sounds and synced data).

- **Breathing methods.** Ten Breathing Manuals, one per element: Ember, Rime, Thunder, Gale, Stone, Verdant, Hollow,
  Starlit, Hourglass and Crimson Breath. Read one to learn its method: your aura takes its element's colour and its
  element, so a blade meets a creature's weakness or resistance and the elemental climate as a spell of that element
  would, and sets off the reactions waiting on its marks (a frozen foe shatters under an Ember blade). Each method has a
  small passive that grows as you do: Ember sets foes alight from Flow (always from Edge), Rime slows, Thunder throws
  sparks to the next foe, Gale lends speed in a fight, Stone steadies you against knockback, Verdant mends a little with
  each blow, Hollow draws nearby foes together, Starlit gathers aura faster, Hourglass quickens your swings, Crimson
  drinks a little of what it takes. Switching methods keeps your stage but costs the road to your next breakthrough (and
  your aura), so the manual asks to be read twice.
- **Where manuals are found**: the Archive's library and vault, every expedition's vault (each favouring its element's
  method), trial chambers' vaults, stronghold libraries and ancient cities; master weaponsmiths and clerics sell one for
  emeralds and a book.
- **Aura** is a small pool that never fades: 20 at Glow, 40 at Flow, 70 at Edge. Full swings of a sword, axe, spear,
  trident or mace on real foes fill it (spam, farming one spot and training dummies much less), and so does the
  **breathing stance**: sneak and stand still with your blade in hand. A ring closes on the aura bar with each breath; let
  sneak up and press it again as it closes for a breath on the beat, worth a burst more. You meditate for mana at the same
  time.
- **Glow**: every blow of an aura weapon is coated, landing 10% harder in your element, and each breath of the stance
  senses hostile creatures within 16 blocks, outlined for you alone. The blade wears a haze of your aura.
- **Flow**: every aura weapon sweeps, wider and further than a sword ever did, and your blade can **guard**: sneak and
  press the Aura key. A held guard halves blows and arrows from in front, paying aura for what it takes. Raised just as
  the blow comes, it's a **perfect guard**, timed like a Shield's parry: the blow is turned aside and the attacker
  staggered, an arrow flies back at whoever loosed it, and a spell is parried and answered. The blade's aura flows, light
  running along it and ripples leaving it.
- **Edge**: a blade of solid, translucent crystal along your weapon, reaching past its point: a block more reach, and a
  quarter of each coated blow goes through armour. Tap the Aura key for an **Aura Slash**, a crescent of aura that cuts
  everything in its path for your weapon's damage in your element (12 aura, every 2 seconds).
- **Backlash**: spend past empty and you're slowed and weakened for a few seconds. It never hurts.
- **Breakthroughs.** Aura experience comes from meaningful fighting (more in danger, against bosses and stronger foes,
  nothing from spam), and at each stage's threshold a breakthrough waits for a trial: hold the breathing stance unbroken
  for half a minute where ley lines cross, or fell a foe stronger than you (a boss, a Runebound or anything with twice
  your health) by your blade alone. A breakthrough bursts out of you in your aura's colour, with its name on screen, a
  sound and a Grimoire entry. Flow takes about half an hour of real play, Edge about two hours (Form and Sovereign, below,
  about six and fifteen).
- **The Aura key** (Z, under Wildercord in the controls; V is still the spell key): tap for the slash, sneak and press
  for the guard, double-tap for the step and hold for Dominion (below).
- **The aura bar** sits on top of the spell panel (or alone, without a Cord): your stages, the next pulsing gold while a
  breakthrough waits, your aura in your method's colour with the slash's price marked, and the breath's beat ring. The
  **Aura page**, from the Cord screen's new Aura badge (it opens without a Cord too), shows your method, stage, aura, the
  road to your next breakthrough and its trials, and every technique.
- **Fair in PvP.** Aura blows are melee, so armour applies. Against players aura's bonuses sit under the spell-defence
  cap and a PvP scale, and the slash meets the spell defences in full, spellguard included.
- **Server settings**: a new `aura` section (`enabled`, `xp_multiplier`, `gain_multiplier`, `coat_bonus`, `damage_scale`,
  `slash_damage`, `slash_cost`, `slash_cooldown_seconds`, `pvp_scale`, `backlash_seconds`, `guard_share`). An older config
  file gains it at its defaults. Servers and add-ons can add weapons to the `wildercord:aura_weapons` item tag.
- **Hooks for what comes next** in `api.AuraApi`: a stage registry and trials, a technique registry for the Aura key, hooks
  on aura gained and spent, and breathing methods and the places manuals are found (and a way to teach one outright).

### Aura's last stages, and the blade that carries a spell
- **Form** (110 aura): aura leaves the body.
  - **Aura Step**: double-tap the Aura key and aura carries you about six blocks in an instant, the way you're moving (ahead
    when you stand still), leaving afterimages of you in your colour. For its first moments nothing can touch you. It never
    passes through anything solid (it climbs a slab or a stair on the way), never carries you into or out of a dungeon's
    warded room, and stops short of lava and fire (12 aura, every 2 seconds). Once you have it, a lone tap of the Aura key
    waits a moment in case it's the first of two, so a step never looses a slash as well.
  - **Aura armour**: while you hold 20 aura or more, a faint shell of aura round your body takes a quarter of the harm that
    reaches you (blows, arrows, spells, fire), half a point of aura for each point it takes, and flares where it's struck.
    It never spends you below 20, so it fades rather than breaks. Falling, drowning and starving get through.
  - **Intent**: with your blade in hand, weaker hostile creatures within 8 blocks (less health than you, or a lower aura
    stage) are slowed and now and then falter where they stand. Bosses never feel it.
  - **Aura sense** reaches 24 blocks, and pulses on its own every few seconds while you fight.
- **Sovereign** (160 aura): **Dominion**. Hold the Aura key and a circle of your aura six blocks across opens where you
  stand for 8 seconds: foes inside are slowed and hit 30% weaker, each of your blows on a foe inside chains once to another
  foe inside for half of it, and your aura flows back twice as fast while you stand in it. A big moment: a great circle on
  the ground, a column of light, the camera shaking, a deep chord. 40 aura, and a minute and a half's rest.
- **Harder breakthroughs** for the top stages: hold the breathing stance where ley lines cross **through a thunderstorm**,
  open to the sky (45 seconds for Form, a minute for Sovereign; lightning breaks it), or fell a **boss** by your blade
  alone within three minutes. A stronger foe or a calm sky no longer does.
- **The spellblade** (from Edge): cast a spell while sneaking with your blade in hand and it flows into the blade, which
  glows in the spell's colour with coils of light winding up it. Your next Aura Slash within 5 seconds carries it: the slash
  replaces the spell's shape and lands it on the first few foes it cuts (each a little weaker), or bursts it where the
  slash breaks. Both prices are paid, and the spell goes through the cast engine as any does, so Shields, the spell
  defences, the PvP cap and mastery all apply. Unused, it slips off and leaves as cast. Spells cast standing, Self spells,
  secret spells and overchannelled spells go out as usual.
- **Aura marks**: an elemental strike (a coated blow or the slash) may leave its element's reaction mark for a mage's
  spell to set off: a Rime blade leaves foes frozen (Shatter, Fracture), Gale windswept (Wildfire), Crimson bleeding
  (Rupture), Hollow shadowed (Blight), Starlit exposed (Unweave), Ember burning and Verdant a touch of poison (Overload,
  Elapse). Earth, storm and time leave none. A modest chance (15% at Glow, 5% more a stage), short marks, a rest before
  the same foe takes another from you, and never fire or poison on another player.
- **The aura bar** shows the step recharging on the Form diamond and a Dominion on the Sovereign one (with a thread filling
  back as it rests), and above the bar a spell's time on the blade and a Dominion's time left. The **Aura page** lists the
  new techniques and the top stages' trials.
- **Fair in PvP**: Intent presses on another player only if their stage (or, without aura, their health) is lower, as a
  dark vignette and a slight slow (5%); a player inside a Dominion is slowed less and hits only a little weaker (the
  weakening times the PvP scale); a Dominion's chain onto a player is aura off the blade, which meets their spell
  defences; a carried spell meets them as any spell does.
- **Sounds**: `aura_step`, `aura_armour`, `aura_intent`, `aura_dominion`, `aura_dominion_fade` and `aura_spellblade`.
- **Server settings**, more keys in the `aura` section: `step_cost`, `step_cooldown_seconds`, `step_distance`,
  `armour_share`, `intent_pvp`, `intent_pvp_slow`, `dominion_cost`, `dominion_seconds`, `dominion_cooldown_seconds`,
  `dominion_weaken`, `spellblade_seconds` and `mark_chance_multiplier`. An older file gains them at their defaults.
- **For add-ons**: Form and Sovereign are open in the stage registry, the step and Dominion are in the technique registry,
  the top stages' trials are allowed through `AuraApi.allowTrial`, and a duel trial (`AuraBreakthroughs.DUEL`) waits for
  whoever teaches aura duels to allow it.
### The world of aura
Swordsmen of the world to meet, fight and learn from, gear forged with aura, and slashes that meet in the air.
- **Wandering duelists.** Sword masters in travelling cloaks, a scabbard at the hip, one for each breathing method (its
  colour in the cloak's trim, the sash and the blade's aura). Now and then, by day, one wanders in near a village's bell,
  on a road, or makes a small camp in the open and sits by its campfire. Rare (about one an hour of daylight out in the
  world), never two near each other, and each moves on after twenty minutes; nothing harms it outside a duel.
- **Duels.** Use a duelist and it offers a duel; use it again to accept. A countdown in a circle of light, then the fight:
  you and the duelist, nobody else. It meets you **at your own stage** and fights as that stage fights: coated blows, from
  Flow a guard with a perfect moment that turns a rushed blow and staggers you, from Edge the slash (its blade raised high
  first), and from Form a dash, once Aura Step exists. Every move has a tell. Brought low, it yields on one knee; knocked
  out, you get back up on one health. Either way you're put back as you began: losing costs nothing but pride.
- **Winning teaches.** A duelist you beat teaches you its breathing method (or, if you already breathe another way, hands
  you its manual to choose), a little aura experience and a Grimoire entry, then bows and goes. A duel won without magic
  is a new breakthrough trial, kept for Form and Sovereign.
- **Fallen knights.** Old armour that a swordsman's aura still walks in, haunting strongholds, ancient cities, the
  expeditions and the spawner rooms of dungeons, its visor and the cracks in its plate glowing a dim, smoky version of the
  method it once breathed. It raises its blade before a slash, and a line of light marks the ground where the crescent
  will fly: step off it, clash with it, or turn it back with a perfect guard; after the swing it's open. Close in, it
  braces its guard: wait it out, strike from behind, or break it with an axe. Tougher in deeper places. Its slash is
  magic: armour, Warding and the spellguard meet it.
- **Manual pages and Aura Shards.** A knight always drops a page of its method's manual (four of one method and a book
  bind into its Breathing Manual) and sometimes an **Aura Shard**.
- **Aura-forged gear.** At a smithing table, an Aura Shard forges a diamond or netherite weapon with a reagent of the
  world, keeping everything it was: **Lumenedge** (a sword, with a Lumen Antler) gives half again the aura from every
  blow; **Skyrend Glaive** (a spear, with a Fulgurite Shard) slashes 60% harder, further and wider, through more foes;
  **Bulwark Maul** (an axe, with Geode Grit) guards for 40% less. Each has its own look and its aura in hand.
- **The Breath Sash.** Worn where the Tome of the Fifth Page goes, on the gear tray: a quarter more aura held, and a
  breathing stance that settles twice as fast and breathes in half again as much. A fifth spell, or a steadier breath.
- **Aura clash.** Two Aura Slashes meeting in the air break together in a burst that shoves creatures back and harms
  nobody, a duelist's and a knight's included.
- **Guard and slash.** A held guard facing a slash catches it (it cuts the guard, halved, and goes no further); a perfect
  guard sends it back at whoever loosed it, as your own. A perfect guard no longer staggers a slasher from across a field.
- **The slash by day.** Its crescent has a rim of shadow under its light and a brighter edge, so it reads against a bright
  sky as well as by night (and under shader packs).
- **The field guide** lists the duelist (under a new Wanderers heading) and the fallen knight.
- **Sounds**: a blade drawn and sheathed, a bow, a yield, the clash, and the knight's creaking plate, its tell, its steps
  and its fall (the feel kit's new duelist part).
- **Server settings**: a new `aura_world` section: `duelists`, `duelist_spawn_rate`, `max_duelists`, `duelist_camps`,
  `knights`, `knight_spawn_rate`, `max_knights_nearby`, `forged_gear`, `lumenedge_gain`, `skyrend_slash`,
  `bulwark_guard_cost` and `sash_capacity`. An older config file gains it at its defaults. The structures knights haunt
  are the `wildercord:knight_haunts` structure tag.

### Fixed
- **Blooms and fallen stars take grass again.** Minecraft 26.3 took grass blocks out of the dirt tag, so the blooms that
  life magic leaves and the ground fallen stars land on had quietly stopped counting grass as ground.
- **Meditation you can see through.** In your own first-person view the ritual circle sits smaller and further out, the
  rising light and the gathered-light orb stay out of your face, and formation rings show only to others while you get a
  quiet progress arc at your feet. Other players still see the whole ritual.
- **Tracing a sigil no longer counts as meditating.** Sneaking while you charge a spell kept starting meditation.

## [0.8.0-alpha] - 2026-10-01

Magic that feels like your own. Every world answers to harmonies of its own and bends a few runes its own way, runes
have to be read before they're understood, spells grow with the one who casts them, casting is a performance with a
risk in it, and the world remembers big magic where it lands. Players need 0.8.0 to join a 0.8.0 server (it adds
blocks, items, sounds and synced data). Back up your worlds before updating.
Full release notes: [0.8.0-alpha](docs/releases/0.8.0-alpha.md).

### Every world's magic is its own
- **Harmonies: spells only your world answers.** Each world draws about a dozen hidden harmonies from its seed: exact
  sequences of three or four craftable runes (Tier I to III) that are ordinary spells anywhere else, but here carry a
  twist of their own. There are 27 twists, each with its own look and its own synthesised sound: rain that lands as
  falling glass, a flame that mends your friends as it burns your foes, a bolt bursting into birds of light, flowers
  blooming out of frost, gravity turning over, a pale steed rising under you, the whole spell sounding again from where
  it landed, a slow hour for arrows, a storm of written pages, standing stones, a red moon, a great bell and more. A
  twist is a modest extra on the spell (a few points of damage, a little mending, a set piece around you), dealt through
  the spell defences and the cast's budget, never at you or your friends, never moving a boss. Casting one finds it:
  its name fills the screen ("the Glasswind Rite"), it goes into the Grimoire with its runes and riddle (400 mana toward
  your next circle), and the server tells everyone its name, but never its runes. Once found it costs 15% more mana
  and recharges a fifth slower, and the Cord screen names it and says what its twist does. No harmony is a secret
  spell or holds a signature fusion's pair, and the ten secret spells are unchanged.
- **Rune quirks.** A few runes bend a little differently in each world, in their own conditions ("Here, Shock cracks
  harder in the rain", "Here, Veil lasts longer at night"): a little stronger, longer, or striking again, faintly, a
  moment later. Nobody is told; each shows itself the first time it matters to one of your spells, with a glint, a line
  in chat and a Grimoire entry (100 mana).
- **Torn Pages carry this world's riddles too.** Half the time, while any are left, a page tells one of your world's
  harmonies, by name, and its riddle, instead of a secret's. A riddle speaks of every rune in order.
- **This world's harmonies in the Grimoire**: those you found (hover for the runes, the twist, the riddle and who
  found it first), the riddles you read, those other players found (by name and finder), and how many nobody has found;
  then the quirks you've met. A client is never sent a harmony its player hasn't found, read or heard announced.
- **Reading runes.** A rune you've just learned starts unread: the Codex shows its name, family, tier, element and cost,
  but only a hint of what it does, written by hand for the runes everyone meets first and made from each rune's own
  nature for the rest. The first cast glimpses it (its text, every number veiled in a shimmer); three casts that land,
  or five that don't, and you understand it. A small ? and a dot mark the runes still being read, the Grimoire lists
  them, and rune item tooltips follow the same rule. Every rune you knew before this version, and the starter runes,
  are understood, and creative players see everything. The wiki keeps the full reference.
- **The Codex's tooltips show each rune's cost.**
- **Server settings** for all of it: a new `harmonies` section (`enabled`, `count` of 12, `reroll_salt` to draw a
  world a fresh set, `announce`, and `quirks` of 4) and `features.unread_runes`. An older config file gains them at
  their defaults.
- **Hooks for other systems** in `cast.WorldResonances`: whether a player found a harmony, this world's harmonies,
  a harmony matching some runes, an extra condition a harmony must meet to wake, and a listener for each find.
- **A world's draw is saved with it**, so a later version adding runes never moves a world's harmonies under its
  players; it's drawn again only when the owner changes the count or the reroll salt.

### Spells that grow with you
- **Spell mastery.** A spell is its exact runes in order; each one you use keeps its own record (the 48 you used most
  recently, and never one threaded on your Cord). Change a spell and you're growing a new one, but the old record
  waits: change it back and its rank and traits return. Mastery is kept through death and travels with loadouts.
- **Five ranks: Kindled, Practised, Adept, Master and Mythic**, at 100, 350, 1,000 and 3,000 experience. A spell
  used as your main attack reaches Practised in under half an hour of real play, Master in about three to four
  hours, Mythic in about ten.
- **Experience from casts that matter, not from casting.** Striking a real foe (more for the share of its health the
  hit takes, more again for the kill; Runebound half again as much, bosses twice, players with PvP on half), healing
  someone who's hurt, warding an ally in a fight, a little for a spell that moves you or works the world. Danger
  (low health, a crowd, a boss nearby, a dungeon) multiplies it up to three times, a kind of foe the spell hasn't met
  lately is worth a quarter more, and doing the same thing in the same place is worth less each time until the place
  is forgotten (boss fights don't wear out). Training dummies and the practice arena teach at half the rate and only
  up to 60, a little over half the way to Practised. One cast earns at most 20. Scrolls, imbued items, glyphs and
  passives teach nothing, and nothing grows in creative mode.
- **Traits: one of three at each of ranks II to V**, from a catalogue of 53, filtered by what the spell is made of
  (its elements, its shapes, whether it harms or only helps) and by how it was used: each spell counts its casts in
  the rain, at night, underground, in the Nether or the End, at low health, beside allies, against the undead or a
  crowd, and more, and some traits are only offered to a spell used often enough in theirs. A fire spell cast a lot
  in the rain may be offered Undying Flame (its fire burns at full strength on the wet); one cast at night,
  Nightshade. Traits are modest: a little cheaper or quicker, a chance to leap to one more foe, a mark that sets up a
  reaction, a bell, starlight from the circle. Their damage together adds at most a fifth to a hit, half that against
  a player and always under the spell-defence cap; they take at most 15% off a price or a cooldown. Each rank allows
  one change of mind for 3 experience levels: re-roll the offer, or unbind the trait chosen.
- **A personal sigil** drawn from the spell's runes and its owner, the same every time, at the heart of the spell's
  circle (cast, charged and beside the Cord screen) and on inscribed scrolls. **Circles grow richer with rank:** a
  fine ring at Practised, a deeper colour and brighter sigil at Adept, a second ticked ring at Master and a slow
  shimmer at Mythic. Everyone nearby sees your sigil and rank, never your traits or numbers.
- **Spoken names.** A spell you've named shows its name to everyone within 32 blocks once it's Adept: a brief title
  over the caster that drifts up and fades (or a line low on the screen when they're out of view). Spell titles can
  be switched off in the magic visual settings.
- **Inscribing a mastered spell.** An Adept spell's scroll carries its earned traits and its sigil. Read it to cast
  it once with them; sneak and use it to study it: the spell is threaded into an empty row of your Cord and you
  start your own record at Kindled with the traits borrowed (they work at once; at each rank you may keep a borrowed
  trait or choose another). No experience travels, a spell you already have can't be studied again, and borrowed
  traits are never inscribed.
- **On the Cord screen:** a rank badge and growth bar on every spell row, the mastery panel (`Ctrl`+`M`, or click the
  badge) with the spell's circle and sigil, its four trait slots, the waiting offer as cards, re-roll and unbind and
  what it's been used in most, and rank and trait lines in the readout. Prices and cooldowns there, on the HUD and
  on the spell wheel include the traits. Reaching a rank pops a toast with the spell's sigil, and the next time the
  Cord screen opens, the choice opens with it.
- **Server settings** in a new `mastery` section: `enabled`, `xp_multiplier` (1.0), `traits`, `spoken_names` and
  `inscription`, all on at first. An older config file gains the section at its defaults.
- **For other parts of the mod and add-ons:** `api.SpellMasteryApi` adds circumstances a spell's casts are counted
  in, offers traits from outside the catalogue, and sets what a residue trait leaves where its spell lands.

### Casting as a performance
- **Overchannel.** Keep holding a charged spell past full and it climbs into overchannel stages, one every 1.2 seconds:
  +20%, +40% and +60% power on top of the full charge's +40%. Each stage drains a little of your spare mana (15% of
  the spell's price a second, never the price itself), cracks the circle behind your shoulders a little further, throws
  sparks off its rim, strains the hum higher and closes the edges of your screen in, and adds a 7% chance (14%, 21%)
  that the spell surges into wild magic when you let go. A young heart holds one stage; two Heart Circles hold two, and
  the 4th holds all three (a cracked circle holds nothing). Short of spare mana the channel simply stops climbing:
  overchannel never cracks a Heart Circle.
- **The beat.** The moment a charge fills, and the moment each stage lands, is a beat: let go just then (within about
  a third of a second after) for 10% more power and a bright chime. While you charge, the ring on your spell badge
  closes on the charge's own beats instead of the rhythm's.
- **Tearing loose.** Hold on past your last stage for another 3 seconds (the ring turns red, the circle reddens and
  shakes) and the channel tears loose: the circle bursts, the spell fizzles, a harmless wild surge goes off (butterflies,
  a blink, a lift with a slow fall, slowed time or healing for everyone near), you're dazed for a second and a half and
  lose 30% of your mana. It never costs health: on one heart, you walk away on one heart.
- **Sigil tracing.** While a spell charges, a faint glyph of three to five strokes (made from the spell's own runes,
  the same every time) sits round your crosshair. Hold sneak to steady your hands: the camera holds still and the mouse
  draws a point of light along the glyph. A good trace steadies an overchannel (up to 60% less surge chance) and adds
  up to 8% power; not tracing loses nothing. Your client scores the trace; the server only believes as much of it as
  the time you really spent steadying allows. The glyph is the same share of the screen at every GUI scale, the mouse
  works at any sensitivity, and two new options in the Magic visual settings screen turn tracing off or set how much it
  helps (none, light, strong).
- **Incantations.** Every rune has its own spoken syllable ("vo" for Bolt, "ign" for Fire, "sei" for Split...). While
  you charge, your spell's syllables rise one by one from behind your shoulders as each rune's roundel opens, and
  gather into a line of glowing script over them: the spell's incantation, readable by anyone near enough (it fades
  with distance), with a soft whisper under each rune's note. In a duel, a sharp-eyed opponent can read what's coming.
  A tap is cast without a word. A Magic visual settings option hides incantations: all of them, only others' or only your own.
- **Server settings**, in a new `channeling` section: `overchannel` (on), `power_per_stage` (0.2), `drain_per_second`
  (0.15), `surge_chance_per_stage` (0.07), `beat_bonus` (0.1), `backfire_stun_seconds` (1.5), `backfire_mana_burn`
  (0.3), `sigil_tracing` (on) and `trace_power` (0.08). With `features.wild_magic` off nothing surges. Against players,
  everything a performance adds counts inside `defence.max_bonus`, so it never stacks a spell past the cap. An older
  config file gains the section at its defaults.

### A world that remembers magic
- **Residues: big magic leaves a mark.** A spell whose mana price is 30 or more, any overcast, a boss's spell and now
  and then an element reaction leave a residue of their element where they land: **Smouldering Ash** that stays warm
  and glows (fire), **Everfrost** that holds about a day and is slick as ice (frost), **Fulgurite**, storm-glass that
  still crackles and jolts whatever brushes it with speed (storm), a **Lingering Eddy** that lifts you gently (wind),
  **Riven Stone** with glowing amber cracks (earth), the **Wildbloom**, a strange luminous flower that bees visit and
  that seeds a little (life), a **Void Scar** that hums, draws loose items in, keeps animals off and slowly closes
  (void), a **Star Glyph** that shows invisible creatures to whoever stands on it (arcane), **Stilled Sand** where time
  runs thick, slowing what walks it and keeping dropped items from ageing (time), and **Bloodmoss** that feeds nether
  wart beside it (blood). A stronger spell leaves more (one block at the threshold, up to five) for longer (up to half
  again). Each has its own art, glow, ambient particles and a quiet sound of its own.
- **Residues never grief.** They take only natural ground (grass, dirt, sand, stone, snow, netherrack, end stone...)
  or open air over it, never anything built or placed, never a block with contents and never a spell's passing block.
  They ask the same permission any spell's block change does (claims, spawn protection, `spells_edit_blocks`), keep
  out of dungeon wards, and fade on their own, giving back the ground they took. A chunk holds at most 12, a few blocks
  around one spot at most 6, a dimension 1024, and one caster's magic 64. Their records are saved, fade on time after a
  restart too, and one in unloaded ground waits for its chunk rather than loading it.
- **Reagents.** Harvesting a residue gives its reagent: Cinder Ash, an Everfrost Shard, a Fulgurite Shard, a Bottled
  Gale (fill a glass bottle at an eddy), Geode Grit, a Wildbloom Petal, Hollow Dust, Star Dust, Hourglass Sand and a
  Sanguine Bead. Each has a small use of its own: furnace fuel that burns hot, freezing still water or cooling lava,
  scraping weathered copper, 30 seconds of slow falling, showing the ore around you, growing a plant, drawing loose
  items to you, a mote of starlight that lights a spot for five minutes, jumping a furnace or a young animal ahead,
  and growing nether wart.
- **Reagents at the Fusion Altar.** Lay one in the free rune socket while two effects fuse or weave: Cinder Ash keeps
  the higher of the two ranks, an Everfrost Shard halves the XP, a Fulgurite Shard makes a rank I result rank II, a
  Bottled Gale unbinds a signature pair into its elements' fusion, Geode Grit keeps the amethyst, a Wildbloom Petal
  makes two, Hollow Dust keeps the lower-tier rune, Star Dust raises the rank for 3 more levels, Hourglass Sand frees a
  fusion you've made before, and a Sanguine Bead pays up to six levels in health instead. One that would change nothing
  is refused, and the altar says what each does.
- **Ley crossings.** Ley lines now run in two weaves that cross every few hundred blocks. Where they meet, every spell
  is 10% stronger and costs 10% less, the crossing shimmers with turning rings, a faint column of light and rising
  motes, and stepping onto one says so. The first time earns the **Crossroads** feat.
- **The moon, the hour and the weather.** Under the open sky a full moon strengthens arcane and void (+15%), a new moon
  blood (+15%) and void (+10%), the noon sun fire (+15%), dawn and dusk time (+15%), and rain now strengthens frost
  (+10%). These ride on the elemental climate, so the Nether still favours fire and the End void, and a thunderstorm
  storm. All of them go through the spell-defence cap against players.
- **The HUD says why.** For a few seconds after something new comes into force where you stand, lines over the spell
  panel say what's favoured and why ("Full moon: Arcane +15%, Void +15%"); the Grimoire's page keeps them.
- **New feats**: Crossroads (stand on a ley crossing) and Lasting Mark (harvest a residue), with their advancements.
- **Server settings**: a new `residues` section (`enabled`, `min_spell_cost` 30, `lifetime_multiplier` 1.0,
  `max_per_chunk` 12, `max_per_dimension` 1024) and a new `places_of_power` section (`ley_crossings`, `crossing_bonus`
  0.1, `celestial`, `celestial_multiplier` 1.0). An older config file gains both at their defaults. Switching
  `elemental_climate` off turns places and times of power off too.
- **More ley lines.** The second weave adds lines where there were none, so a little more of the Overworld lies on a
  ley line (faster mana, quicker Heart Circles, Wellstones that wake).
- Other spells leave residues alone (they count as a spell's passing block, so pistons can't move them either).

### Where they meet
- **Lingering Mark leaves a real residue.** The mastery trait now leaves a small residue of its spell's element where
  it lands, at most once every 8 seconds for each caster (a faint glimmer where none can be left).
- **World-Tuned**, a trait only your world offers: a harmful spell with one of your world's quirked runes, cast often
  where that quirk holds, may be offered it at a new rank, and then strikes 15% harder while the quirk holds.
- **Some harmonies are bound to a place of power.** About one in four of a world's harmonies wakes the first time only
  where ley lines cross. Cast one anywhere else and its runes stir and a line over your hotbar says *not here*; once
  found, it answers anywhere. A server with ley crossings switched off frees them.

## [0.7.1-alpha] - 2026-10-01

- New detailed project icon shared by the mod JAR, player guide and CurseForge: a braided Cord, luminous elemental rune beads and a mint-and-gold magic circle.
- Rewritten CurseForge description and summary, current feature counts, gameplay images, player guide link and updated gallery.
- All gameplay changes from 0.7.0-alpha are included. This patch changes branding and publication materials.

## [0.7.0-alpha] - 2026-10-01

Full release notes: [0.7.0-alpha](docs/releases/0.7.0-alpha.md). This release also adds physical terrain spells, twelve circle disciplines, multi-effect weaving, Cinnamon, defensive equipment, expeditions, practice, research, home projects, performance profiles and a reworked player guide. See the release notes for current counts and tradeoffs.


- **A distinct release and impact for every castable rune.** All 39 shapes and 258 effects now have an explicit three-beat animation instead of a hash-selected stroke pattern. Each rune keeps its own emblem and element colour, but its motion is selected by name from an authored sequence; no two built-in castable runes reuse a complete sequence. Secondary effects and both effects inside a woven rune animate too. The common emblem frame is smaller so it does not hide the rune's silhouette. A client gallery captures every release and effect impact at three moments.
- **More detailed Cord and item art.** The Twine, Copper, Amethyst and Echo Cord icons have fuller strands and distinct pendants, and their worn bands have separate material patterns. Backpacks have clearer seams, straps, stitching and hardware in hand and on the wearer. The Focus of Resolve and the Rootbound and Stormglass relics have larger, readable silhouettes beside the older gear.
- **A rune woven from any two elemental effects.** At the Fusion Altar, an amethyst block binds a specific pair into one socket at their full combined mana cost. The pair is order-independent and survives saving and trading. This is separate from the 55 element-pair fusions and 22 signature fusions; shapes, modifiers and links are not inputs for weaving.
- **Two Overworld relic dungeons.** Rootbound Mazes generate in swamps and mangrove swamps; Storm Spires generate on mountain peaks. Both have Runebound guards, side caches, two elemental seals and a vault that holds a reusable relic. The Rootbound Relic heals and slows monsters; the Stormglass Relic pushes monsters away and gives speed and slow falling. Both have a 30-second rest after use.
- **Animation coverage and client evidence.** Tests check that the built-in rune roster and authored sequences match, no complete sequence is repeated, and every castable rune has an emblem. Client screenshots cover release and impact across 180 frames. Linked spells skip the internal Target placeholder, and add-on runes retain a safe visual fallback.

- **Focus of Resolve.** Craft it from a Mana Crystal, two amethyst shards and two iron ingots, then wear it in the focus slot (or hold it in your off-hand while the slot is empty). Incoming spells deal 20% less damage; your spell effects are 15% weaker. It also joins the existing focus loot pools.
- **Shield's cast cue.** The Shield rune now announces itself with a sealing gesture, an amber accent and a clamping sound. Every built-in effect has a registered cast signature, checked by a roster-wide test.

Spells no longer kill players in one blow from full health: armour counts against every spell, a new armour
enchantment and potion ward spells off, and a spellguard catches the one spell that would have killed. Players need
this version to join a server running it (it adds a potion and an effect).

- **Warding**, an armour enchantment against spells, I to IV. Each level takes 8% off every spell that hurts you,
  adding up over every piece you wear, up to 80% (the most enchantments take off anything), and it works against sonic
  booms, which Protection doesn't. It's a protection against one kind of harm like Fire or Blast Protection, so it can't
  share a piece with Protection; the two share the one 80% limit against spells, so three pieces of Protection IV and
  one of Warding IV reach it and still keep 48% against everything else. From the enchanting table, anvil books,
  librarians and loot, about as often as Fire Protection. See
  [Defending Against Magic](https://defnotean.github.io/wildercord/progression/defence/#warding).
- **The Potion of Warding**, brewed from an Awkward Potion and Tinted Glass (glass that keeps light out, against magic
  that is light): its **Warded** effect makes every spell that hurts you 20% weaker for 3:00. Redstone makes it last
  8:00, Glowstone Dust makes it Warded II (40%) for 1:30, and it splashes, lingers and tips arrows like any potion.
- **The spellguard.** A single spell can no longer take you from 80% of your health or more straight to dead: it leaves
  you on one heart instead, with a ring of amber light, the sound of a ward breaking and a word on screen, and for the
  rest of that spell (its other effects land a moment later) nothing finishes you. Then it takes a minute to come back.
  It goes first, so no totem, Reversal, Second Wind, Rebirth or duel knockout is spent on a blow it caught, and it never
  stops /kill or the void.
- **A spell-defence badge on the Cord screen**, the amber shield beside the heart: how much less spells hurt you and
  from what (your armour, Warding and Protection, Warded, Resistance), whether your spellguard is ready or how long
  until it is (a dot on the badge while it recharges), and every way to stand up to spells.
- **Server settings for all of it**, in a new `defence` section: `spellguard` (on), `spellguard_health` (0.8),
  `spellguard_recharge_seconds` (60), `max_bonus` (2.5) and `armour_rate` (0.55). An older config file gains the
  section at its defaults, and the Cord screen shows the server's numbers.

### Changed
- **Armour counts against every spell.** Pure magic (Harm, most arcane and void spells), frost and sonic booms used to
  go straight through armour; now your armour and toughness count for a little over half their worth against them, so
  full netherite takes 35 to 40% off an everyday spell and about a third off a huge one. Fire, lightning and blasts,
  which armour already stopped, are unchanged. Players only: spells hit creatures as hard as ever.
- **A spell's bonuses are capped against players.** Execute, Trial Key, reactions, hexes, Decree, Drowse, Veil's ambush,
  Fortune and the rest still multiply together, but never past ×2.5 on a player (stacked, they could reach ×120).
  Against creatures they're uncapped, as before.

### Fixed
- **One-shots from full health.** On Hard, the Archivist's Sunfall (32.5 damage), the Tide Scribe's lightning on a wet
  player (29.7), the Archivist's beam of sonic boom (26.4, through any armour and every enchantment), the Cinder Warden's
  comet (25.7) and the Star-Eater's Domain (23.1, through any armour) each killed a 20-health player outright, and in PvP
  a strong caster's Hollow, Prismatic Burst or Sunfall dealt 43 to 55. Now the spellguard leaves an unarmoured player on
  one heart, full netherite takes those bosses' spells to 10 to 18, and netherite with Warding IV on every piece to 1 to 4.
- **Every spell's damage meets the same defences.** The Star-Eater's shards and eclipse, a wisp's spells, a Reflect
  ward's returned blow and Blood Thread's shared wounds used to reach players past them; they don't any more. A Stasis
  letting its held hits go and a Soulbond passing on a killing share now answer to the spellguard. A spell (an add-on's)
  that deals /kill damage lands as magic instead, so defences, Resistance, totems and the spellguard answer it; the real
  /kill and the void still kill.

### Added
- **Backpacks.** Three of them: the **Backpack** (2 rows, 18 slots: string over a chest in leather), the
  **Reinforced Backpack** (3 rows, 27 slots: a Backpack, a chest, 4 iron ingots and 2 leather) and the **Runewoven
  Backpack** (4 rows, 36 slots: a Reinforced Backpack, a Mana Crystal, an Echo Shard, 4 amethyst shards and 2 Blank
  Runes). Each bigger one is crafted from the one below it and keeps everything inside, its colour and its name. See
  [Backpacks](https://defnotean.github.io/wildercord/items/backpacks/).
- **What's inside stays inside.** A backpack carries its contents with it: drop it, put it in a chest, run it through
  a hopper or die with it, and it still holds everything. Its tooltip says how full it is and lists what's in it.
  Burnt up, it spills what it held; the Runewoven Backpack doesn't burn.
- **Open it anywhere.** Use a backpack in either hand to open it, or wear one and press `B` (a new key, under
  Wildercord in the controls), in the world or in your inventory. `B` or `E` closes it again. It opens and closes
  with a rustle of leather.
- **A Backpack slot.** The gear tray on top of your inventory has a fourth slot, for a backpack; in the creative
  inventory it's beside the gear slots. Shift-click a backpack to put it on. The worn backpack shows on your back for
  everyone: under a staff strapped across it, over a chestplate or a cape, and over folded elytra (it tucks in against
  your back between the wings while you glide). It isn't drawn while you're invisible.
- **No backpacks in backpacks.** Nothing that holds items goes inside one: no backpack, no shulker box, no bundle, no
  chest carrying its contents. A backpack doesn't go in a shulker box or a bundle either, and a hopper can't feed one
  into a shulker box.
- **Safe on servers.** An open backpack stays put: its slot is locked, and number keys, `F` and `Q` can't move it
  while it's open. Anything that moves it anyway (a death, a command) closes it, and everything it held stays in it.
- **Dye them.** Every backpack dyes like leather armour, keeping what's inside, and a cauldron washes the dye out.

### Added
- **Soar: flight.** A new Tier III wind rune (18 mana, an Amethyst Cord or better) gives real flight for 20 seconds,
  as in creative: double-tap jump to take off, then jump and sneak to rise and sink, a little slower than creative.
  Extend lengthens it (40 seconds at most), and cast while falling it catches you at once. It cannot be renewed mid-flight; after it ends, the wings rest for 30 seconds. On a shape that
  reaches your allies it lifts them too; a pet it reaches falls slowly instead. Craft it from a Phantom Membrane, a
  Feather and a Breeze Rod, or find it in trial vaults and End cities. See
  [Soar](https://defnotean.github.io/wildercord/runes/effects/wind/#soar).
- **Flight ends gently.** Three seconds before the end the wind starts to fade: a falling chime, wisps peeling off your
  wings and a line over the hotbar. Then it sets you down as Feather Fall does, drifting the way you look, and the
  landing never hurts, from any height.
- **Wings of wind.** A flier wears a pair of wings of wind that beat slowly (faster on the move) and fold small on the
  ground; on the move the wind streams off the wingtips and curls away behind. Taking off is a wingbeat and a whoosh,
  and soft gusts go by while you fly.
- **Flight has its counters.** A pull (Pull, Gravity Well, a vortex, a rift) or a grounding wind (Weigh, Downdraft) tears
  the flight away and rests the wings for 30 seconds; the fall is still a gentle one. The dungeons'
  warded arenas and vaults still the wind: nobody flies in there.
- **Flight is never left behind.** Soar never touches a player in creative or spectator, or anyone who can already fly
  some other way. Log out mid-flight and you're saved unable to fly, with what was left given back when you return
  (hovering, if you were up in the air); a restart or a crash is tidied the same way, death ends the flight and a trip
  to another world sets you down gently. A server with flying turned off doesn't kick a soaring player.


## [0.6.1-alpha] - 2026-09-29

The Cord stops glowing all the time, and magic looks right under shader packs. Works with 0.6.0 servers and clients.

### Fixed
- **The Cord no longer glows all the time.** It carried a white outline, drawn like the Glowing effect's and seen
  even through walls, and its beads showed grey instead of their runes' colours. Now the beads wear their runes'
  colours, glow only for a moment after the Cord goes on and while you charge or cast, and fade out smoothly.
- **Magic under shader packs.** With Iris and a shader pack on, spell light, void darkness and a wisp's glow were
  drawn with blends the pack doesn't expect, leaving squares, smears and wrongly lit patches wherever magic was.
  While a pack is on, they're now drawn the way vanilla draws its own glowing particles and eyes, which every pack
  is written for, and glows cast no shadows. Without a pack nothing changes. See
  [the FAQ](https://defnotean.github.io/wildercord/faq/#does-wildercord-work-with-shaders).

## [0.6.0-alpha] - 2026-09-29

Every spell now looks, sounds and plays like itself: a presentation and balance overhaul of all ten elements and
every shape, modifier and link (about 280 new sounds, visible reaction marks, spells that play a tune while you
charge), casting gear slots, two-rune passives, and many fixes.

Players need 0.6.0 to join a 0.6.0 server (its new sounds must match).

### Added
- **Every spell reads from how it's built.** Each shape leaves your hands with its own gesture and sound (a flick, a
  throw, a line of light, a slash, a blast, a seal, a call, an aura), and you hold each kind its own way while
  charging. The circle under your feet grows with the spell's cost, charge and tier, and the biggest spells leave
  with a shove. See [How a spell sounds and looks](https://defnotean.github.io/wildercord/spellcraft/casting/#how-a-spell-sounds-and-looks).
- **Spells have melodies.** While you charge, each rune plays its own note as its roundel opens (a knock for a shape,
  glass for an effect, a bell for a modifier, a clink for a link), and the hum is in your first element's key.
- **Modifiers show in gold, links in violet.** Amplify, Overcharge, Focus, Widen, Extend, Quicken, Split, Pierce, Vow,
  Blood Price and the rest each have their own cue as the spell leaves; bolts show Amplify, Overcharge, Frugal, Pierce and
  Homing in flight; Volley shots and Chain jumps climb the scale. Delay ticks down, Pulse keeps time, Echo ripples, On Hit
  rings at each creature it hands on to, On Kill tolls, conditions click open or shut, and reactive links show a seal at
  your feet while armed.
- **Reaction marks are visible.** Frozen, Windswept, Pulled, Wet, Soaked, Resonant, Cracked, Shadowed, Bleeding and Ionised
  creatures wear a small halo in the mark's colour, and a tick sounds when a mark is set.
- Fields keep time (a Zone beats, a Totem's bell climbs with each strike, a Domain tolls, each Orbit orb chimes its note),
  impacts leave a moment's trace of their element, a failed cast fizzles, and the HUD chimes and flashes when a long
  cooldown ends.
- **Casting gear slots.** Your inventory has three new slots for casting gear: **Staff** (any staff, a greater one
  too), **Focus** (any focus) and **Tome** (the Tome of the Fifth Page). A piece in its slot works with nothing in
  your hands, so you no longer have to hold your staff or a focus to cast. In the survival inventory the slots sit in
  a small tray on top of the panel, above your armour; in the creative inventory's Survival Inventory tab they are
  beside the Cord slot. Shift-click sends a piece to its slot and back, and a slot only takes what it is for, one
  piece each. Hover an empty slot to see what it takes.
- **Gear shows on your character**, to you in third person, to everyone around you and in the inventory's paper doll:
  a staff strapped across your back with its head over your shoulder, a focus hovering off your other shoulder
  (bobbing and turning, with a faint glimmer circling it), and the tome hanging from a belt at your hip. Each looks
  like its item, keeps clear of armour, capes and elytra, follows a sneak, and isn't drawn when you're invisible or
  already holding that piece.
- The **Cord screen's mana badge** lists the gear that counts, wherever it is; a gear tooltip now says which slot it
  goes in.
- For add-on makers: `api.gearSlots()`, `equippedGear`, `equipGear`, `unequipGear` and `gearSlotFor` (API 1.1).
- **Void and time look and sound like themselves.** Forty-eight new sounds (`void_*`, `time_*`: a hook, a corral, a snap, an
  unzip, a shriek, a chomp, a black flame's heartbeat, a clock's fuse, a run of hurried ticks, a stopped world and the crack
  of it starting again, a coin of borrowed time, a quill in the ledger...) replace the vanilla noises the runes used to
  borrow, and every void and time rune now announces itself the instant the hand moves (the element's own cast swell peaks
  nearly a second late). Each rune has its own shape as well: Pull is a taut tether, Gravity Well a wide disc of dust drawn
  in along the ground, Riftcall an upright slit, Hollow a sphere cut out of the world, Shadowstep a smear, Grapple a
  dotted rope that tightens, Warp two spirals that trade colours, Warp Step leaves a return sigil that counts down and snaps
  you back, Zipper's teeth close behind you, Eclipse is a filled dark disc with a corona, Wither climbs as black veins,
  Umbra rakes three claws in the dark, Anchor clanks when a push is refused. Time's faces now stand upright and turn to
  you (Countdown, Doomclock), Foresight is a halo with a pip for each blow it will sidestep, Time Skip flickers gold
  (never crimson), Rewind lights the way back in reverse, Stasis is a column of frozen sand with a shard for each blow
  held, Prolong an hourglass, Timesteal a thread of sand, and a Borrowed Time payment is a coin. Timed effects now end with
  a cue (Hex, Veil, Anchor, Accelerate, Foresight, Hush, Eclipse, Infinity), and a crowd can no longer flood the wire
  (Countdown and Stasis draw eight in full per tick; Entropy ticks once a cast).

### Changed
- **Life and arcane look and sound like themselves.** Arcane's star seal now means a lasting mark on someone (Exposed, a brand, a reflecting shell); instant strikes are needles, columns and comets (Harm is a thin needle of light, Smite a tall column after its ring, Starfall drops along a star of light threads with a different note per star), wards are glass, and Swap is two crossing arcs. Life's bloom means restoration; Bramble shows its four thorns, Regrowth winds a vine up in three stages, Remedy's grey motes turn green, Harvest sweeps a golden crescent, Grow ripples out, Reversal raises a sun-disc, and Fortune spins a coin. Every life and arcane rune has its own synthesised sound (about 75 new ones) and a role stinger under its cast sound, replacing the borrowed vanilla ones (Harm's melee crit, Venom's spider, Reversal's Totem of Undying).
- **Every storm and earth spell looks and sounds like itself.** Each has its own synthesised sound instead of borrowed
  vanilla thunder, anvils and chains: Shock's dry zap, Jolt's clamp, Lightning's crack with the thunder rolling in a moment
  after, Thunderclap's whine then boom, the Thunderbird's cry and the whistle of its dive, Stormweave's plucked wires,
  Stormclock's ticks and hour bell, Plasma's sizzle, Riftbolt's tear; Tremor's slam and grind, Weigh's groan, Root's creak,
  Mire's gloop, Stalactite's whistle and crunch, Geode's crystal pings, Fossilize's rising ticks and crack, the mining
  runes' pick bites. The tells the new mechanics need are drawn: a ring under the spot a Thunderbird will dive on, the
  patch of ground an Aftershock is about to strike again, a stalactite seen falling onto the spot it marked, Tempest's
  second bolt where its victim lands, Jolt's ring of arcs for as long as the stun holds, Plasma's violet haze while a
  target is ionised, Surge's crackle and Stormheart's charged ring for as long as they last, Stoneform's slate stance (no
  longer Stoneskin's cast), roots and bog that stay visible the whole hold. Plasma is a steady violet beam now, not orange
  lightning. Busy displays are trimmed: Lightning draws its full show for three strikes, Thunderclap two forks, a
  Thunderbird, Thunderhead, Stormclock and Magnetize about half their particles, Tempest one shake a cast, Basalt Surge one
  grind and one crack instead of a sound per column, a Rampart one crack instead of fifteen breaks; Prospect's ores light
  as its ring reaches them.
- **Casting sounds are cleaner.** The element's cast sound used to play twice on a dozen shapes (Bolt, Arc, Cone, Crescent, Spark,
  Comet, Ricochet, Stream, Blitz, Glaive, Imprint, Latch); now once. A sound can play at most three times in a tick, so a blast on a
  crowd no longer roars, and a tap cast has its own little snap of release.
- **Touch is a touch,** not a mini Beam: a thin thread, a glow at your palm and no beam sound.
- **Fields and streams are priced by their strikes.** Fields and streams were far too cheap for the hits they land, and Quicken, Linger and Extend multiplied
  them further (a Zone with Linger and two Quickens dealt 500 damage to one creature for 53 mana). Now: **Quicken** makes the same strikes come
  twice as fast, so a Zone, Wall, Totem, Domain, Latch, Stream or Barrage is over sooner instead of hitting more; **Linger's** later landings
  strike at 60%; and Zone (x2.75), Wall (x2.95), Totem (x3.15), Orbit (x3.05), Trail (x2.85), Vortex (x2.8), Domain (x3.9), Barrage (x1.85),
  Latch (x2.15) and Stream (x1.95) cost more in proportion to how often they strike.
- **Touch** lays the spell on 30% harder; **Cone** hits 35% harder in its near half and 15% softer at its edge; each of
  **Orbit's** orbs strikes a creature once a second on its own, so a Split Orbit's extra orbs count.
- **On Hit** fires its payload a little weaker at each further creature of one landing (85% each), still paid once.
- **Vow's** cooldown is 5x (was 4x); **Blood Price** costs 1 health per 4 mana (was 5); **Belated** is 25% stronger per rune (was 40%).
- **Weak runes lifted:** Amplify x1.5 cost (was x1.6), Overcharge x2.6 (was x3.0), Kindled +30% power (was +20%), **Rain**'s strikes
  seek enemies in the area and strike 2 blocks wide, **Orb** is steered by your aim and drifts 30 blocks (x2.0), **Cluster** costs x1.5.
- **Near clones set apart:** **Ring** is hollow (it spares the ground around you) and costs x1.7; **Arc** x1.25; **Ray** x1.15; **Sweep** x1.8;
  **Beam** costs 3; **Wall** is a real barrier (it turns projectiles aside and slows what touches it); **Snare** trips what it catches (Slowness II
  for a second).
- **Conditions give back what they didn't use:** Combo on its first two casts, and If Sneaking, If Airborne, If Wounded, If Outnumbered and If
  Wet when they don't hold, return the mana their branch cost.
- Casting poses now follow a spell's first *shape* (a spell that began with an effect always thrust before).
- **Passives hold two runes.** A passive used to hold up to five, so two of them could keep ten runes running at once,
  which was a lot of free power. Each passive now holds **two runes**, whatever your Cord, and you still have the two
  slots (1st and 5th Heart Circle): at most four runes are ever active. An effect before any shape is on Self already,
  so `Stoneskin · Empower` needs no Self rune, and a modifier takes one of the two sockets (`Swift · Amplify`). A
  passive or loadout saved with more keeps its first two runes (nothing else is lost); threading a third is refused
  ("Passives hold 2 runes"). See [Passive Spells](https://defnotean.github.io/wildercord/spellcraft/passives/).
- **A piece in its slot takes the place of held pieces of its kind:** a staff in the Staff slot and staffs in your
  hands, only the slotted one counts; the same for a focus and for the tome. A slot that is empty lets held gear work
  exactly as before (staffs in either hand, foci and the tome in the off-hand), so nothing stops working. The slots
  are separate, so the tome and a focus can now work together.
- Gear in a slot **drops when you die**, like the rest of your inventory (Curse of Vanishing destroys its own), and
  stays with you when `keepInventory` is on. It never duplicates through death, respawning or changing dimension.
- The tooltips and Cord screen texts about holding the tome or a Focus of the Deep Well now mention the slot.
- **Fire, remade rune by rune** so that each does something its neighbours don't:
  - **Ember** tops up a burn that's already going (3 more seconds, 10 at most) instead of restarting it.
  - **Flashfire** hits harder (5, burning 4 s) and thaws and dries the allies in the flash.
  - **Explode** throws what it hits and sets it up for **Wildfire**; **Meteor** now falls for 1.2 seconds (two at
    most), hits for 12 in 3.5 blocks and leaves a crater that burns on; **Primer**'s bomb goes off at once if its
    target is killed. The blasts of one cast no longer stack on one enemy, and blasts of enemies that stand together
    are merged: Burst + Explode on a bunched crowd used to deal about four times as much as the text said.
  - **Kindling** bursts for 14 and leaves everything the burst reached at two stacks; **Firestorm** deals 5 and the
    fire leaps in two waves, to neighbours and then to theirs; **Steam** hangs a cloud where it bursts that blinds and
    wets everything in it; **Sunscorch** makes its target glow and, by day, blinds it (bright light of its own counts
    as dusk); **Soulfire** is fire that water can't dull and the fire-proof can't shrug off; **Blazecall** hits for 3
    and each fireball staggers; **Cinderbrand** also stokes the burn; **Ashen Veil** blinds those it burns;
    **Everburn** burns two and a half times as fast; **Conflagration** flares more the more burning enemies are
    around; **Phoenix Pyre** flares once when an ally is nearly dead: 6 health and a burst of fire; **Cinderheart**
    lights one heart at a time and can't be lit again for 24 seconds after it goes out.
- **Blood, remade rune by rune:**
  - **Rend** also tears the natural resistances of what it hits (a blaze takes frost, a skeleton takes blood, at full
    strength); **Leech** turns overhealing into a shield of up to 4; **Bleed** tears wider while its bearer moves;
    **Gash** weeps 0.4 plus 1% of its bearer's health a second on top of the heal-lock; **Dismantle**'s last slash is
    twice as deep against something that isn't facing you; **Cleave** is 6 + 10% of max health (20 more at most) and
    the axe swings on to enemies beside the target for half.
  - **Overdrive** hits harder as it weakens you (Strength III under half health, IV under a quarter); **Warcry**
    heals its rallied allies for 1 on each kill; **Blood Moss** feeds the most wounded ally nearby, not only you;
    **Lifesteal** marks its target for 6 s: everything anyone does to it heals you for a quarter of it;
    **Heartstopper**'s heart skips harder each time (stumble, lurch, full stop); **Crimson Mist** bleeds foes for 2 a
    second, heals allies 1.5 and hides them from monsters more than 4 blocks away; **Hemomancy** is 4 + 1 per 1.5
    health missing (up to 8 more) and heals under half health; **Transfusion** heals three times what you give and
    cures one harmful effect; **Sanguine Rite** costs more health with Amplify, Overcharge and a crowd.
  - **Blood Thread** ties 4 creatures at most (a Burst used to thread every enemy it hit, multiplying all damage
    without limit) and shares 40% of each hurt, three times a second at most; the shares show as crimson pulses along
    the thread.
- **Fire and blood look and sound like themselves.** Every fire and blood rune now has its own shape, timing and
  synthesised sound (forty new sounds), and the displays that were the same show again and again are cut down:
  - Fire: **Flashfire** is a flat white-gold disc and one hard crack, gone in a quarter of a second, with no smoke;
    **Explode** is a hard shell out of a white heart with a deep boom (a cast with more than one blast shakes the ground once);
    **Meteor** shows a reticle that closes as the rock roars down, lands with one great blow and leaves a molten scar for
    three seconds; **Inferno** is a standing ring of low flames that breathes with a low roar and sighs when it goes out;
    **Primer**'s fuse ring closes tighter and turns pink to orange to white while its ticks climb, then goes quiet for a
    breath before the blast; **Kindling** circles one more gold ember over the head with each stack, a note higher each time;
    **Cinderbrand**'s glyph hangs over its target for as long as the brand holds; **Cinderheart** pulses a four-block ring,
    circles embers, and shows a grey ring of dead ash when it will not catch; **Ashen Veil** puffs ash into whoever strikes
    it; **Sunscorch**, **Soulfire** (whose mana comes back as a mote flying to you), **Blazecall** (three round
    fireballs), **Steam**, **Firestorm**, **Fireward**, **Hellmouth**, **Starfire**, **Everburn**, **Conflagration** and
    **Phoenix Pyre** each have a voice of their own, and the ones that burn or hang about end with a sigh.
  - Blood: **Leech** streams a thin thread to your hand and rings you when it shields you; **Bleed** drips (with a ring
    when the wound tears wider), **Gash** rips ragged and weeps, **Rend** cracks open across the chest and spreads,
    **Dismantle**'s three hairlines each tilt steeper (the backstab is longer and white-gold), **Cleave** is one heavy cut
    with a thin stroke to each enemy it swings on to; **Overdrive** surges with a racing heart, beats with a ring more for each
    tier of pain and flushes the screen edge red; **Warcry** is a horn and three rings; **Blood Moss** lays a patch of moss;
    **Heartstopper**'s beats slow, and each skip is a deeper stop; **Crimson Mist**, **Sanguine Rite**, **Hemomancy**,
    **Transfusion**, **Lifesteal** (each thing done to its target sends a mote of blood to you) and **Blood Thread**
    (taut threads, a pulse and a twang up the scale for each share) each have their own.
  - A crowd never multiplies the show: a crowd catching fire, bleeding or being rended shows a few of each, not one each.
- **Frost and wind runes each do their own thing.** Chill deepens if you Chill again (Slowness II, III, IV). Frost
  leaves a target brittle for Shatter. Icicle hits harder on a slowed target and melts after 2 seconds. Hail is five
  staggering stones. Hoarfrost creeps for 3 seconds, freezes solid, and blooms onto neighbours. Bubble lifts its
  target and holds by size (small 2.5 s, large 1.5 s). Absolute Zero counts a target's signs of cold: the more, the
  harder it hits and the longer it holds. Glacier's ice spreads to a few neighbours and cracks for damage. Blizzard
  walks the way you faced. Frostbloom's ward freezes attackers and heals you. Black Ice shatters what dies while
  it holds. Cryostasis bursts when it opens. Coldsnap is a wider snap that leaves everything brittle. Freeze costs
  14 mana, Bubble 9 and Absolute Zero 18.
- **Wind now lifts.** Launch, Levitate, Updraft and Summit Wind mark targets *airborne*: every spell hits them 20%
  harder while they are off the ground. Light creatures fly farther in wind and heavy ones barely budge. Windcut
  breaks what a target is winding up (a charge, a bow's draw, a creeper's fuse, a spell). Push slams into walls for
  damage. Repel deals 4 damage. Downdraft pins whoever stands under it when nothing is in the air. Recoil deals 5.
  Cyclone and Gale Mantle follow where you steer. Feather Fall drifts the way you look. Zephyr lasts 10 seconds and
  clears blindness, nausea and slowness. Mirrorfrost casts back at 70% power and can't be cast back again.
- **Frost and wind look and sound like themselves.** Every frost, water and wind rune has its own sound now (thirty-four
  new ones: a needle for Icicle, a creeping crystal for Hoarfrost, a deep lock for Freeze, a wet splat for Flash
  Freeze, a rising wave for Tidecall, a snatch for Disarm, a rewinding whoosh for Recoil and so on) in place of the
  handful of shared glass, powder-snow and wind pops. Runes read by silhouette: Push is one broad slab, Windcut one long
  cut, Launch a pillar of rings, Repel a dome, Icicle a needle falling from above, Chill three rings climbing the body,
  Tidewrit a wall, Undertow a corkscrew of bubbles, Flash Freeze a crown of ice spikes. Each element family has its own
  colour (rime, steel and deep cyan for frost, aqua for water, mint, sky and grey-green for wind), and a rune's size on
  screen follows its power. Timed effects now tell you when they end: ice shows hairline cracks in its last half second,
  a Bubble strains before it pops, Hoarfrost flashes white before it closes, and Frostward and Cushion show a faint ring
  while they last. Silenced creatures show a muted ring over their head, and a creature lifted by wind shows pale rings
  and lights while a spell hits it harder. Fewer particles for the same rune (Push, Prune, Repel, Tidewrit, Undertow).
- **New status: silenced.** A silenced creature can't cast; Drowning Word now silences its target. A Runebound
  telegraphing a spell has it broken by silence or a Windcut.
- Tidecall, Undertow, Tidehook, Tidewrit, Current, Disarm and Tidebreath got small mechanic tweaks (Tidecall pulls and
  soaks, Undertow drags toward the nearest water, Tidehook leaves a gasp, Current carries allies, Disarm's player
  version stops them using what they hold for 3 seconds).
- **Life and arcane runes each have their own verb, and the outliers are tuned.** The numbers now sit near their
  tier's yardstick, and every repeater is guarded (a Zone, Linger or Echo no longer multiplies a rune without limit).
  - **Exposed**, a new arcane mark: **Harm** leaves its target exposed for 3 seconds and **Reveal** for as long as it
    glows (and strips invisibility). Exposed counts as a mark for **Unweave** and **Prismatic Burst**, and **Starfall**
    and **Cometfall** send their stars and shards to exposed enemies first.
  - **Silence** does what it says: a caster can't cast for 4 seconds (3 on players) and a charge or a Runebound's
    telegraphed cast in hand is cut short; monsters get Weakness I. **Manaburn** cuts a caster's charge or cast short.
  - **Smite** is a verdict: a ring closes at the target's feet for 0.7 seconds, then a column of light deals 13 (26
    against undead) and strips Absorption. **Prismatic Burst** is 5 damage and 4 per mark (five at most) and passes each
    mark it eats to up to 3 enemies nearby. **Resonance** rings each enemy once per cast (up to 4 at a time) instead of
    growing with the square of the crowd. **Spellbrand** bursts for 7 damage of the element of the spell that sets it
    off. **Decree** condemns what it holds (your next 2 spells hurt it 40% more) and is free on a crowd of 3 or more.
    **Reflect** returns arcane damage that ignores armour and cracks with each reflection (6 at most). **Halo** smites
    every 1.5 seconds and answers whoever hurt its ally. **Starshard** (11, sparks of 4), **Starlight Tether** (1 damage
    a pull, 5 seconds), **Twin Star** (the twin casts at 75%), **Empower** (a Weakness comedown; a passive carries only
    Strength I), **Summon** (spirit wolves bite for a wolf's 4, and your mana regenerates a quarter slower while they
    live), **Manatide** (10 mana; a quarter of every spell you cast under it comes back) and **Haste** (charge fills
    30% sooner).
  - **Heal**'s overheal becomes a shield of up to 2 hearts, and repeats within one cast heal 100%, 60%, then 40%.
    **Regrowth** ramps (I, II, III). **Cleanse** washes elemental marks off too. **Remedy** turns what it cures to good
    (poison to Regeneration, slowness to Speed...). **Venom** is its own lethal poison (2 now, 0.75 a second for 4
    seconds, Poison I as a marker) that works on undead and spiders and spreads once to 3 enemies nearby.
    **Sporebloom** makes monsters fight each other for 5 seconds. **Drowse** makes the blow that wakes a sleeper deal 75%
    more. **Rootsnare** holds for 1.5 seconds and then taxes each block of movement. **Haven** shoves enemies out of
    the dome instead of giving Resistance. **Vinelash** marks its target Pulled and trips it. **Moonpetal** waxes and
    wanes with the moon (5 damage, 4 healing). **Bramble** has 4 thorns. **Fortune** strikes for double (1 in 4) and
    a fortunate kill drops extra experience. **Bloom** costs 14 and spreads Regeneration to allies near the ones it
    touches. **Second Wind** also gives Speed II and a gust that shoves enemies away. **Restore** heals 4 and mends 8%,
    an item once a minute. **Nourish** feeds pets (they heal and are ready to breed). **Grow** ages baby animals up.
    **Ancient Seed** keeps growing its field for 12 more seconds. **Glowvine** re-ripens vines. **Nullify** dissolves
    vexes and spirit wolves and shades that aren't yours.

### Fixed
- **Lightning no longer multiplies on a crowd.** Every strike hurt everything near it, so enemies packed together took a
  strike for each neighbour (three took 108 between them, five 300). Now each enemy takes the strongest strike of a cast
  once: its own 12, or half of that from a strike aimed at a neighbour. Its text also says "slows", which is what it does.
- **Stoneform answers blows, not fire.** Its aftershock fired on any damage, so burning, poison or a fall set it off
  every half second; now only a blow from an attacker does, at most once a second.
- **Ripple heals what it really took**, not what it computed: a target that was protected or invulnerable gave you the
  health anyway.
- **Earth's ground-heave no longer lifts what should sink.** Mire, Sinkhole and Fossilize hopped their targets up as they
  cast, and Tremor, Monolith, Thunderquake, Magma, Sandstorm, Basalt Surge and Pelt each drew a second crack under their
  own; they keep only their own show now.
- **Excavate no longer shakes the screen** (it played Tremor's), and shows nothing when it mines nothing. **Thunderclap's
  shake** is gentler and only comes when it hits something.
- **Shock's second victim** takes part in Conduct and Overload like the first.
- **Cinderheart stacked with itself:** casting it again started another full aura, so casting it every 1.5 seconds
  piled up several.
- **Explode, Meteor and Primer** multiplied their damage on enemies that stood together (Burst + Primer on eight bunched
  enemies dealt about 66 to each, not 10).
- **Soulfire** said it burns on through water but took the wet penalty, and did nothing to fire-proof mobs.
- **Cinderbrand** said the burn hurts more, but only spell damage did.
- **Inferno's** field and **Kindling's** burst reached farther than the circle drawn (their search boxes were square).
- **Steam** now sets off Shatter and Wildfire like every other fire hit.
- **Silence** now stops casting (it only ever made monsters forget their target for a moment). **Treasure Sense** shows only unopened treasure, nearest first, not every player chest, hopper and dispenser. **Restore** can no longer repair without limit through a Zone or Linger. **Venom** hurts undead and spiders. **Nourish** no longer wastes mana on a pet. **Light** says when there is no room for it.
- Shatter and Fracture now end a freeze hold instead of leaving the target frozen.
- Repel was drawn in red; it now uses the wind palette. Glacier's shell no longer lingers after the ice cracks.
- Tidewrit no longer repeats its sound for every target it sweeps.
- A disarmed creature that died lost its weapon; it now drops it.
- Frostward could be beaten by a long frost hold; the hold is now capped.
- Water runes were audible in quiet places where they shouldn't be; Tidecall and Tidewrit are now quiet like the rest.
- Casting Hoarfrost or Bubble repeatedly stacked their damage; each is now once per target at a time.
- Several frost and wind rune descriptions no longer matched what the runes do.
- **Borrowed Time** no longer repays health it never healed: only what you were missing is borrowed.
- **Foresight** no longer refills its dodges when the same spell lands again (a Zone, a Pulse or an Echo of it).
- **Hollow, Doomclock, Dragon Breath, Gravity Well, Riftcall and Eclipse** no longer stack on crowds or under a repeating
  shape: a creature takes each once per cast, so a pack no longer takes 180 damage from one Hollow.
- **Collect** takes 48 things at most, and makes a small fizzle when there is nothing to take.
- **Rewind** will not put you into lava, a wall or thin air.
- **Zipper** will not open a wall you may not build in (a claim, spawn protection).
- **Malison** no longer plays its cast sound twice; Umbra's ring uses the dark tint like the rest of void.
- **Warp's** description now mentions the Nausea it causes.

## [0.5.0-alpha] - 2026-09-29

Your own affinities, runes to fish up, thirty-four new runes (eighteen to build with and sixteen signature fusions),
and boss arenas that can only be entered through their dungeons.

### Added
- **Signature fusions: sixteen new fused runes.** At the Fusion Altar, sixteen pairs of *particular* effects now fuse
  into a rune of their own instead of their elements' fusion: Chill with Shock makes Frostwire, while any other Frost
  and Storm effects still make Hail. Same shard, same 3 XP levels, the lower of the two ranks. The altar's panel says
  **Signature fusion**, and it flares with a star as it makes one. See
  [Signature fusions](https://defnotean.github.io/wildercord/fusion-altar/combining/#signature-fusions).
  - **Frostwire** (Chill + Shock, storm): chills each target, then a current races through every chilled or frozen
    enemy within 6 blocks, 4 damage each (6 to one frozen solid).
  - **Seethe** (Bubble + Fire, fire): a bubble of boiling water scalds for 2 seconds, then bursts into steam: 4 damage
    around it, blinded and soaked.
  - **Bloomstep** (Grow + Blink, life, moves you): step up to 32 blocks through a door of blossoms; grass and flowers
    grow at both ends, and Regeneration where you arrive.
  - **Skyburst** (Launch + Explode, fire): flings up to 3 targets high; each explodes at the top of its flight, raining
    fire down for 7 (and fanning Wildfire).
  - **Stitchtime** (Heal + Countdown, life): heals 4, then heals back every wound of the next 4 seconds (up to 12).
  - **Parasite** (Venom + Leech, blood): poisons and drains 1 a second into you for 6 seconds, and leaps to the next
    enemy when its host dies.
  - **Razorgale** (Windcut + Bleed, wind): blades cut everything within 3 blocks and leave it bleeding, then come back
    round and tear the wounds open (Rupture).
  - **Doomclock** (Primer + Stasis, time, Tier IV): a 3-second clock every blow winds 2 tighter; it bursts for 8 and
    all it was wound (up to 20) around its bearer.
  - **Thunderstep** (Shadowstep + Lightning, storm, moves you): come down as lightning behind the first enemy hit, 8
    damage and a stun around you.
  - **Halo** (Smite + Regrowth, arcane): a halo over an ally smites the nearest enemy every 2 seconds for 3 (tripled on
    undead) and heals the ally 1 each time.
  - **Thunderquake** (Thunderclap + Tremor, earth): three shockwaves to 2, 4 and 6 blocks, 4 each: 12 at the heart.
  - **Cometfall** (Starfall + Meteor, arcane, Tier IV): a comet falls a second later for 16 within 4 blocks, setting
    them alight, and five shards strike the enemies around for 4.
  - **Riposte** (Reflect + Foresight, time): the ally's next 2 blows within 10 seconds are sidestepped and answered
    for 6.
  - **Dust Devil** (Summit Wind + Sandstorm, wind, Tier IV): a wandering dust devil catches enemies up, blinds and
    scours them for 3 a second, and flings them high when it blows out.
  - **Malison** (Hex + Resonance, void): 3 damage and a curse (your spells hit 25% harder); when its bearer dies the
    curse passes to up to 3 enemies near it.
  - **Avalanche** (Coldsnap + Stalactite, frost): 6 damage in 3 blocks (9 on a bare head), Slowness III, and drifts of
    snow that melt 10 seconds later.
  - Each wears its two runes' elements in a spell's circle, with a star to tell it from the element fusion, and has a
    hand-drawn icon in both elements' colours.
  - The Grimoire lists them apart, *Signature fusions (n of 16)*, hidden as ??? + ??? with both runes' elements as the
    hint until found. Each condenses as much mana as any fusion; the element fusions are still counted out of 55.
  - Two new advancements: **Signature** (make your first) and **Hallmarks** (find five).
- **Your affinities.** Every player now has an affinity with each of the ten elements, and it grows with what you
  do: casting the element (a point for every 10 mana its effects cost, so cheap spam is worth no more than a real
  spell), setting off reactions it takes part in, finding creatures weak to it, and everyday things that fit it.
  Smelting, killing with fire and the Nether's lava feed fire; fishing, snow and powder snow frost; thunderstorms and
  lightning (a strike you live through is worth a lot) storm; elytra flight, long falls and great heights wind; stone
  and ores mined with a pickaxe, and the deep, earth; ripe harvests, breeding, taming and healing others life; the
  End, endermen, ender pearls and the deep dark void; enchanting, meditating, Torn Pages, new runes and ley lines
  arcane; a whole night watched under the sky, time magic aging the world and a clock in hand time; melee kills, Blood
  Price and heavy blows survived blood. Every way has a daily allowance (an in-game day, which sleeping doesn't
  hurry), so no farm grows an affinity past it; casting slows to a tenth past its own instead of stopping.
- **Five levels** at 100, 600, 1,800, 4,500 and 10,000 points: each gives **+3% power** with that element's effects
  (heals and shields too: up to +15% at V), from **III** others' spells of it land **10%, 15%, 20%** softer (a
  reaction still breaks through, and no callout floats over you), and at **V** its share of a spell's price costs
  **10% less**, shown in the Cord screen and the HUD. It counts on your own spells (from your Cord, imbued, turned
  back by Mirrorfrost), not on scrolls or passives.
- Every new level is announced with a message and a **toast** with the element's mark in its colour; the first level
  of each element is a Grimoire entry worth **100 mana** toward your next Heart Circle.
- The **Grimoire page** has an **Affinities** section under your heart: each element's mark, its level and a bar
  toward the next, with a tooltip of what it gives and everything that raises it. The readout says a spell's affinity
  bonus quietly under its cost ("Fire affinity III: +9% power").
- Server settings: `features.player_affinity` switches affinities off, and `affinity.gain_multiplier` makes them grow
  faster or slower. An older settings file gains both by itself. See
  [Your Affinities](https://defnotean.github.io/wildercord/progression/affinity/).
- **Eighteen new runes to build spells with**, all crafted (a Blank Rune and a couple of items). See
  [Shapes](https://defnotean.github.io/wildercord/runes/shapes/), [Modifiers](https://defnotean.github.io/wildercord/runes/modifiers/),
  [Links](https://defnotean.github.io/wildercord/runes/links/) and each element's [Effects](https://defnotean.github.io/wildercord/runes/effects/) page.
  - **Three shapes.** **Glaive** (Tier II): a spinning glaive flies out 12 blocks and curves back to you,
    striking everything on the way out and again on the way back. **Imprint** (Tier I): an imprint where you
    stand erupts 2 seconds later on everything within 3 blocks: cast it and run. **Latch** (Tier II): a thread
    holds on to the first creature in your aim and strikes it 4 times a second apart at 70% power, until it
    strays past 24 blocks or out of sight. Charging shows where each one goes.
  - **Three modifiers.** **Kindred**: a helpful effect also lands on you and on the nearest ally it missed, at
    half power. **Thirst**: you heal for a quarter of the damage the effect deals. **Belated**: the effect lands
    1.5 seconds late, 40% stronger.
  - **Two links.** **On Reaction** fires the rest of the spell at each creature the shape before it set an
    element reaction off on; **On Weakness** at each one it struck with an element it's weak to.
  - **An effect for every element.** **Spellbrand** (arcane): a brand your next spell damage bursts for 6.
    **Gash** (blood): 3 damage, and no healing of any kind for 8 seconds, bleeding. **Prospect** (earth): every
    ore within 12 blocks glows through the rock for 20 seconds. **Searing Edge** (fire): for 15 seconds, melee
    hits set foes alight for 2 more fire damage. **Flash Freeze** (frost): 4 damage, and a wet or soaked target
    freezes solid. **Drowse** (life): sleep for 6 seconds that any damage breaks. **Galvanize** (storm): a spark
    of redstone power against a block for 5 seconds (doors, lamps, pistons), gone without a trace.
    **Prolong** (time): every good effect lasts 15 seconds longer, up to 5 minutes. **Umbra** (void): 4 damage,
    doubled in the dark, and shadowed. **Disarm** (wind): a creature's weapon is snatched for 5 seconds, then
    given back.
  - Drowse, Prolong and On Reaction also turn up in ancient city, end city and trial chamber chests.
- **Runes from fishing.** A rod in open water (vanilla's rule for treasure) now brings up magic. See
  [Fishing](https://defnotean.github.io/wildercord/world/runes-of-the-world/#fishing).
  - **Treasure catches can be runes.** About 4 treasure catches in 11 are a rune and 1 in 11 a Torn Page, beside the
    name tags, saddles and enchanted books (Luck of the Sea makes treasure likelier: about 1 catch in 55 is a
    rune with a plain rod, 1 in 24 with Luck of the Sea III). The runes are 21 crafted runes of water, frost and storm
    and a few a fisher is glad of (Tidebreath, Chill, Icicle, Icepath, Shock, Feather Fall, Night Eye, Swift, Heal,
    Collect, Leap, Bubble, Frost, Thunderclap, Jolt, Pull, Grapple, Levitate and Wave, and now and then Freeze or
    Lightning), lower tiers more often, and the two runes below.
  - **Magic waters.** Where magic runs strong at the bobber, a rune comes up tangled in the line **on top of** the
    catch: 12% a catch under a mana storm, 5% on or near a ley line, 5% in a thunderstorm with its rain (or snow) on the bobber,
    adding up to 20% at most. *"Something magical was tangled in your line!"*, and a glint on the water.
  - **Two new runes of the world, found only by fishing:**
    - **Tidehook** (Tier II frost, 9 mana): a hook of water snags each target and reels it in to your feet in three
      tugs, 4 damage, and leaves it soaked (storm then sets off Conduct). Amplify and Linger work on it.
    - **Current** (Tier II frost, moves you, 6 mana): only in water or rain, a current sweeps you about 15 blocks the
      way you look, and you land without fall damage. On dry land it fizzles. Amplify carries you further.
  - A new feat and advancement, **Reeled In**: fish a rune out of open water (250 mana toward your next circle).
  - The server's rune and page loot multipliers scale all of it, as they do chests (0 turns it off).
- Craftable runes' wiki entries now say where else they're found, and Lightning's and Shock's tooltips name trail
  ruins (brushing), where they've been found all along.

### Changed
- **Leaning follows your deepest affinity.** Your magic leans toward the element whose affinity is at level I or more
  and a quarter ahead of the rest, and your circles take its colour as before. Leaning's own +10% power is gone: the
  affinity's levels give power now, for every element you grow. The casts leaning counted before give each element a
  start (2 points a cast, never past level II).

### Fixed
- **Sweep** missed creatures at long range, and more with Quicken or Widen; it now traces the whole arc.
- The **aim preview** took its colour from the second rune (a modifier's gold or a shape's teal); it now uses the first effect's element.
- **Wall's** fence blinked off for a third of the time.
- **Dungeon arenas and vaults are warded.** Players could tunnel, blast or spell their way straight into a boss arena
  or vault without going through the dungeon. Their walls, floors and domes now can't be broken by survival players,
  explosions, block-breaking spells or pistons ("These walls are warded: the only way in is through the dungeon").
  The halls on the way can still be dug into, and a block you put down inside an arena can be broken again. Dungeons
  already in a world are warded too.

## [0.4.4-alpha] - 2026-09-29

### Fixed
- **A server reached through its own domain shows in the server list.** Minecraft greets a server with the address as
  typed when it asks for its status, but with the address its DNS (SRV) record points at when joining it. Tunnels that
  sort connections by that greeting (playit.gg's, for one) only know the second, so such a server said "Can't connect
  to server" in the list while joining it worked. The list now greets a redirected server the way joining does;
  addresses no SRV record redirects go as typed, as before.

## [0.4.3-alpha] - 2026-09-29

Loadouts, a second layer of world-changing magic (all ten elements now touch the world), and another bug hunt: the
new travel, reaction and affinity code, every rune, server performance, and the issues earlier audits left open.

### Added
- **Loadouts.** Save your whole Cord (every spell's runes and name, your passives and which are on, and the
  selected spell) under a name, and swap between up to six setups: one for fighting, one for mining, one for
  exploring. Open them from the new list badge at the end of the Cord screen's tabs row (or `Ctrl`+`L`): each
  loadout shows its name and first runes, with **Load**, **Save current here**, **Rename** and **Delete** (saving
  over and deleting ask twice), plus **Save current as new**. The arrow keys, `Enter`, `Ctrl`+`R` and `Delete` work too.
  Loading never teaches a rune: runes you don't know or your Cord can't hold, and sockets and spells it doesn't
  have, stay threaded but quiet, as on a smaller Cord. Every spell that changes starts its cooldown unless it's
  cooling already, so swapping mid-fight is no cooldown reset, and loading is refused while you charge a spell,
  duel or are sealed in a Cryostasis. See [Loadouts](https://defnotean.github.io/wildercord/spellcraft/loadouts/).
- A **Next loadout** key (unbound at first, under Wildercord in the controls) loads your next loadout and names
  it above the hotbar.
- `/loadout save <name>`, `/loadout load <name>`, `/loadout delete <name>` and `/loadout list` for every player.
- **More of the world answers your magic.** A second layer of world-changing magic, and all ten elements now take part
  (see [Magic that Changes the World](https://defnotean.github.io/wildercord/world/world-magic/)):
  - **Frost cools lava into a crust** of basalt you can walk across (up to 12 blocks). It holds about 25 seconds; for
    its last 5 it turns to glowing, cracking magma, then melts back into lava. It always melts back, even after a
    crash or with nobody near, breaking it gives nothing, and it never forms round a creature in the lava.
  - **Storm scrapes copper** a stage of oxidation clean (up to 4 blocks) and **pulses lightning rods** like a real
    strike. Each creeper it strikes has a **1 in 4 chance of being charged**: careful.
  - **Fire lights candles, candle cakes and campfires, and primes TNT** (up to 4 within 2 blocks): stand clear.
    Frost and wind now snuff candles too.
  - **Time ages the world**: crops and saplings grow a stage and copper weathers one (up to 3 blocks), baby animals
    nearby grow up 2 minutes, and a furnace, smoker or blast furnace jumps 5 seconds ahead in its smelting. Its
    helpful spells do it too, as life's do (not Stasis or Rewind).
  - **Void anchors endermen**: one it strikes can't teleport for 5 seconds, a ring of darkness at its feet.
  - **Life starts curing a zombie villager** that has Weakness, as a golden apple would.
  - **Arcane** shows invisible creatures nearby (they glow for 3 seconds) and makes bookshelves and enchanting
    tables shimmer. **Blood** ripens nether wart and grows crimson fungus.
  - Every block change keeps the old rules: only a player's spell, only where they may build, within the cast's
    block budget, and not on a server that turned world-changing magic or spells' block changes off. Charging a
    creeper, curing and growing babies are a player's spell's only, and off with world-changing magic off too.
  - The rune tooltips' dark green *"Where it lands"* lines say all of this, with the warnings.

### Changed
- **Absolute Zero freezes the same creature solid at most once every few seconds.** Its first hit leaves a target
  slowed, so with Linger, a Zone or a second copy every later hit was a certain freeze for 7 damage. Frozen solid, a
  creature can't be again until 3 seconds after it thaws (it's still slowed meanwhile).
- **Magma's pools and Tempest's strikes don't stack on one enemy.** Each lays one under (or on) every target, so an
  enemy bunched with others took every overlapping one: up to four burns a second from Magma, eight strikes from
  Tempest. Now each enemy takes your strongest one once (Magma once a second, Tempest once per strike).
- **A Blizzard or Rime Seal cast again on its own spot keeps it going** (up to three times its length) instead of laying
  a second one on top: a Zone, an Echo or a Split used to stack several storms biting at once, or seals each freezing
  the same enemy, as Hellmouth, Sinkhole and Crimson Mist already didn't.
- Bloom's description says it grows plants around the first 3 allies it touches (it said every ally), and Devour's
  that it feeds you twice a cast at most, as they always did.
- **Manatide gives 30 mana a drink at most.** Extend made it flow longer without limit (four Extends: about 480 mana for
  a 46-mana spell), and a stream that outlasted the minute between drinks ran alongside the next. It now flows 10
  seconds however it's extended, and only your newest drink flows.
- **Cheating death rests.** Once Reversal, Second Wind or a certain secret spell has saved you from a killing blow,
  none of them saves you again for a minute, and the secret spell can't be taken up again for 3 minutes. Each could be
  recast long before it ran out, so keeping one up meant never dying.
- Glacial Lance's Grimoire entry says it flies 32 blocks, as far as it has always reached (it said 40).
- Borrowed Time says its debt is forgiven when you slay a monster, as it always was (it said "something": killing an
  animal never forgave it).
- Vein's description now says what Amplify does to it: like Break, Tunnel and Smelt, an Amplified Vein mines at
  diamond-pickaxe strength.

### Fixed
- **Magma burns what it lands on from its first second.** Magma is earth too, and its own heave threw the creatures
  it landed on into a little hop, so its first burn (which only reached what stood on the ground) missed them.
- **Warp Step, Time Skip and Zipper set you down safely**, as Blink now does: never into lava or fire, and Warp Step and
  Time Skip never over a drop or the void (Time Skip takes the farthest safe spot on its way, and says so when there's none).
- A Runebound monster standing where the world has stopped running (at the edge of what's loaded) no longer winds up
  and casts at you from there.
- **The server config file shows every setting.** A fresh `wildercord.json` now lists the `travel` section (it was read
  but never written), and a file written by an older version gains any settings added since, at their defaults, the
  next time the server loads it. Everything already in the file stays as it was.
- **Bosses can't be lifted or dragged any more.** Levitate floated the Warden, the Wither, the Archivist and the
  dungeon bosses up out of their fights (Extended, high enough for the fall to hurt), Gravity Well held them in
  its pull straight through their knockback resistance, and Shulkershell's opening lifted them too. Bosses are
  only ever slowed: Levitate now gives them Slowness II instead, and Gravity Well still marks them pulled and
  crushes them, but no longer moves them.
- **A Shield now stops Swap and Shadowstep.** A Shield that blocked the spell still let Swap trade places with the
  enemy behind it, and Shadowstep put you at its back, as if the spell had gone through.
- **Icepath no longer freezes a swimmer into the ice.** It froze the water a creature (or a player, or you) was
  swimming in, leaving it stuck in a block of ice to choke. Like a frost spell's freezing, it now leaves the water
  round a swimmer alone.
- **Harvest replants with a seed from the crop it cut.** It dropped everything the crop gave and then replanted it
  for free, a seed out of nothing every time. Now one of the crop's own seeds goes back into the ground (a crop
  that dropped none isn't replanted), and a crop you may not touch (in a claim) no longer stops it harvesting the
  rest of the field.
- **Grow keeps to the spell's block budget.** Every other rune that changes blocks counts each one against the
  blocks a cast may change (32, or the server's `max_blocks_per_cast`), but Grow bone-mealed every block it
  reached, however many times a Zone, a Split or an Echo landed it.
- **Blink never lands you in lava.** Aimed at a lava pool (or a creature dying in one), it put you on the ground at
  the bottom of it, aimed out over the void or a deep drop it left you in mid-air to fall, and it could also put
  you past the world border. It still only lands where there's room to stand, and now only on ground, never in
  lava or fire or outside the border: with nowhere safe, you stay where you are.
- **Banish never drops a creature into the void.** Its target reappears near its own height, but with no ground
  within reach below (the edge of an End island, a deep ravine) it could be left in mid-air to fall. A creature
  standing on the ground now always reappears on ground, or not at all.
- **A Bubble popping, or a Stasis ending, no longer thaws a longer Freeze.** Either one let the creature go the
  moment it ended, even if a Freeze (or another hold) cast on it had seconds still to run.
- Echolocate now lights up every enemy in range (up to 32) in a big crowd too: enemies just outside its range were
  using up the count, leaving some in range dark.
- **Blackflame spreads only from a death.** A burning creature that simply went away (its ground unloaded, it
  despawned, or it went through a portal) passed its black flames on as if it had died. Blackflame, Dismantle's
  later slashes, Aftershock's second impact and a Bubble's pop also no longer follow a player through a portal.
- **`/rtp` typed over and over could stall a server.** Each `/rtp` searched for a new spot, loading (or generating) up
  to a dozen far-off chunks, and a broken warmup starts no cooldown, so it could be repeated as fast as it was typed.
  Now a player searches at most once every 10 seconds: trying again sooner goes to the spot already found (or, if the
  last search found none, says how long to wait). Operators aren't limited.
- **An accepted teleport request could land you in a duel.** If the player you were going to started a duel during
  your warmup, you still arrived beside them. Now the teleport is called off, as it is when they're duelling at the
  moment you accept.
- **Waypoint offers could flood someone's chat.** `/waypoint share` had no limit and can't be switched off like
  requests. Now you can share with the same player once every 10 seconds.
- A teleport still warming up when an operator switches travel off (and reloads the settings) no longer goes ahead.
- **Fracture, Blight, Unweave, Rupture and Elapse didn't break through a resistance.** A reaction is meant to ignore
  what a creature resists, and Shatter did, but the five set off by any damage of their element still let the
  resistance halve the hit that set them off (a Fracture on an iron golem, an Unweave on the Archivist). Now they land
  in full like the rest.
- **One Lightning cast could set off Overload again and again.** Each strike set its targets alight at once, so on a
  crowd every later strike found them burning and blew them apart again, up to seven times over in one cast. Lightning
  now sets them alight after its last strike.
- **Overload in a crowd could launch creatures sky-high.** Every burning enemy that blew apart threw all its neighbours,
  so in a packed crowd each was thrown once for every neighbour, and the throws added up (players too, in PvP). Now an
  Overload throws each creature at most once at a time; the damage is unchanged.
- **Stoneform's aftershock took on the element of the blow it answered.** Hit by a wind spell, its aftershock counted as
  wind (and could set off Rupture on a bleeding creature beside you, writing it in your Grimoire). It's always earth now.
- **A spell could set the Cinder Warden alight for a moment**, long enough for a storm effect in the same spell to set
  off Overload and crack its armour. Nothing sets it alight now, as the Ember Sanctum page says.
- **A settings file that couldn't be written lost its settings.** When the server couldn't add the newer settings to an
  older `wildercord.json` (a read-only file, say), it ran on the defaults instead of the owner's settings. The file's
  settings now hold either way; only the new ones run at their defaults.
- Adding the newer settings to an older `wildercord.json` no longer strips out comments an owner wrote in it: a file
  with comments is left as it is (its settings are read as before, and any it lacks run at their defaults).
- **Duels gave back more health than the opponent took.** A blow was counted at its damage before armour, Resistance and
  absorption hearts, so an armoured duellist got back far more than they lost, healing damage from falls and monsters
  too (and a knockout gave back the whole killing blow). Now what each blow really took from your health is given back.
- **Duels gave back used-up effects.** Every effect a duellist had at the start came back if it was gone at the end, so a
  Bad Omen a raid had used, or Absorption a monster had knocked away, returned after every duel. Only helpful effects
  come back now, and never Absorption.
- **A creature immune to an element counted toward contracts.** Frost cast at a snow golem, which frost can't hurt,
  counted as a frost cast landing on a real creature, making it a target for the Runesmith's contracts that never ran
  out. A spell has to hurt what it lands on to count now.
- Elapse no longer counts fire that couldn't hurt its target: a burning creature with Fire Resistance had at least 3
  damage land on it, and its flames put out, for a burn that was dealing nothing.
- **Less work for the server each tick, and for your game each frame.** A spell's particles are packed once for
  everyone who sees them instead of once per player (and not at all when nobody is near), lasting spells (Orbits,
  Mines, Totems, Domains...) no longer make the server look through every spell part still waiting each tick, and an
  Orbit looks for creatures once a tick instead of once per orb. On your side, the spell panel no longer reads your
  whole spell again every frame just to write its name, and ley lines, beams, motes, glows and magic circles are drawn
  without making new objects for every little piece. Nothing looks, sounds or plays any differently.
- **A passive can't stack its Orbits.** A passive that started with an effect and then held an Orbit (`Swift · Orbit ·
  Shock`) renewed every two seconds like a Self passive, so up to four rings of orbs (eight with Extend) struck at
  once for one upkeep. The effect before the Orbit counts as Self, so that passive now has two shapes and is refused,
  as the Passives page says; keep the buff and the aura in two passives.
- **Switching an Orbit passive off and on no longer doubles its orbs.** The old ring came back beside the fresh one it
  started, so every switch within its 8 seconds added another ring for one second's upkeep. Only the latest ring flies.
- **Long-lasting shapes no longer pile up waiting steps.** An Orbit, Zone, Trail, Wall, Totem, Domain, Vortex, Mine or
  Snare booked every one of its steps up front: an Orbit that Extend made last hours held hundreds of thousands of
  them, kept even after its caster had left. Each now keeps one step waiting at a time and stops once its caster is
  gone.
- **Quicken makes a Wall strike twice as often.** A Wall struck once a second, and one Quicken left it at that (two
  jumped it to five times a second). It still strikes once a second, now twice with one Quicken and four times with
  two, and the readout says how often: "A 7-block wall (5s, every 1s)".
- **A cast really goes eight links deep.** A bolt in flight, each strike of a Zone, Orbit or Domain, a Lance's or a
  Ring's hits and a Linger's second landing all used up one of the cast's eight links, so `Bolt · On Hit` four times
  over and then `Bolt · Fire` never threw its last bolt, and a Linger deep in a chain never landed again. Only links
  count now.
- **Quicken can't fling a spell across the world.** Quicken stacked without limit on flying shapes, so a spark or bolt
  with a pile of Quickens crossed thousands of blocks in one step and loaded (even generated) the world along its
  way, a cheap way to lag a server. A flying shape is now at most 8 times as fast (three Quickens on a bolt), a Wisp
  or a Ricochet stops where the loaded world ends, and a bolt flies its 48 blocks and no further (its spare ticks
  used to carry a quickened bolt 70 blocks and more), a wave its 14.
- **An Arc bursts where it lands, even on a creature.** An Arc that came down on the ground splashed everything within
  2 blocks, but one that landed on a creature struck only that creature. It bursts there too now.
- **An Echo in an imbued spell repeats it at its target.** The Echo went off from the caster instead, so a sword or a
  glyph holding `Fire · Echo` burned what it struck once and then "burned" its own maker, which did nothing. It now
  repeats what was stored where the release was set off, as the Imbuing page says.
- **A secret spell read from a scroll weighs its full price against a Shield**, as it does cast from a Cord (it counted
  only its runes' mana).
- **Singularity no longer holds a boss at its black star.** It still strikes them, but bosses are only ever slowed.
- **Stormheart's lightning is spell damage.** It struck whoever hurt you in full, even a player (spell damage to players
  is scaled down by the server's PvP setting) and even a friend whose blow landed; it now spares friends, counts as
  storm damage (a creature weak to storm feels it), a Shield meets it, and players take it at the PvP scale.
- **A widened Rain or Sweep no longer loads the world far away.** Shapes keep growing with every Widen, and a Rain's
  strikes or a Sweep's beam could reach hundreds of blocks past what was loaded, loading (even generating) the land as
  they felt for the ground or a wall. They now stop at the edge of the loaded world.

- **A passive's buffs no longer outlast it.** Whatever a passive gives lasts 15 seconds at most (it's renewed every
  2), so switching `Self · Night Eye` on for a second no longer leaves a minute of night vision for a second's
  upkeep. The same effect from a potion keeps its full length.
- **A spell's blocks never drop anything.** A Rampart blown up by an explosion dropped its packed mud (a free packed
  mud farm); a Rampart's wall, a Span's glass, Light's light and frost's crust on lava now drop nothing however
  they're broken, and a crust blown up (or taken any way but melting) gives its lava back at once.
- **Servers ignore a flood of cast requests**, as they already did for spell edits: a modified client can send at
  most a burst of 20 casts (or charges), then 20 a second, far more than any hand on the cast keys.
- **Fire spells no longer light campfires where they may not change blocks.** Lighting a campfire (as the Archive's
  braziers are lit) skipped the rules the rest of world magic keeps: now it doesn't happen in a claim or spawn
  protection, for a player who can't build there, or on a server that keeps spells off its blocks.
- **Spells that grow something ask a claim first.** Protection and claim mods were only asked when a spell took a block
  away, so Glimmer's lichen, Glowvine's vines and an Ancient Seed's flower could be put into the air inside someone
  else's claim for good. Putting a block into the air now asks them too.
- **Fangs never bite your friends.** Its evoker fangs bit anything that walked onto them, your own pets included, and
  their bites went past Shields and the server's PvP damage scale. They now bite as the spell does (6, once for each
  creature however many fangs it stands on), only what your spells may harm, and nothing once the spell has ended.
- **A creature held in Stasis (or an arrow held by Infinity) that's carried through a portal falls again** as soon as
  it arrives, instead of floating until its ground was next loaded.
- **The glyph limit counts all your glyphs**, in every dimension together (12 by default), rather than 12 in each:
  writing one more lets your oldest fade wherever it is.
- **A parried spell doesn't earn Siphon mana afresh.** Turned back, it's still the one spell paid for once, so it
  shares the original's Siphon cap however many times it's parried back and forth.
- **A duel's end only undoes what your opponent did.** It took away every harmful effect and fire gained during the
  duel, from anything: a monster's poison or lava's fire now stay, and only your opponent's (their spells, a tipped
  arrow, a flaming blade) are taken off.
- **The Tide Scribe remembers its tide.** Reloaded (a restart, or its dungeon unloading and loading again), it
  turned the tide at once; it now keeps to when the tide was due to turn.
- **Archives woken before lecterns remembered their Archivist can re-arm.** Such a lectern takes the Archivist it finds
  about the Archive as its own, and if none is there for 3 minutes while players are, it wakes a new one for whoever
  comes near, like any other.
- **Subtitles no longer cover the spell panel's name**: where the two meet in the bottom-right corner, the subtitles
  move up past it. On a narrow screen, the tracked waypoint's line moves below any boss bars instead of running under
  them.
- **A possible crash opening creative search**: the Blank Rune's and Wisp Lantern's tooltips read your own data while
  the search index was built in the background; they now read it only on the game's own thread.
- **Resizing the window no longer undoes a spell edit** made a moment before (the Cord screen copied your spells from
  the server again, before your edit had come back).

## [0.4.2-alpha] - 2026-09-28

Travel commands for servers, six new element reactions, creature affinities and elemental climate, and a bug
hunt through every part of the mod (more than 70 fixes).

### Added
- **Travel commands for servers**: `/sethome`, `/home`, `/delhome` and `/homes` (3 homes each); public warps
  (`/warp`, `/warps`, and `/setwarp` and `/delwarp` for operators); personal waypoints (`/waypoint add`, `remove`,
  `list`, `track`, `untrack` and `share`), with an arrow, name and distance in the top-left corner and a faint beam
  only you can see; teleport requests (`/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny`, `/tpcancel`, `/tptoggle`) with
  clickable answers; `/back` (to before your last teleport, or where you died), `/spawn` and `/rtp`. Every teleport
  forms a magic circle at your feet for a 3-second warmup (moving or being hurt cancels it), has a cooldown, lands
  on safe ground and works across worlds, and none can be used in a duel. Operators skip the warmup and
  cooldowns. Server owners can change the limits and times, or switch it all off, in the new `travel` section of
  the config. See [Getting Around](https://defnotean.github.io/wildercord/social/travel/).
- **Six new element reactions, so every element takes part.** Each has its own burst, its name in
  bold, a place in the Grimoire (150 mana the first time), an advancement and Runesmith contracts:
  - **Overload**: storm on a burning foe blows the flames apart: +30%, and 4 damage to every other
    enemy within 3 blocks, thrown back. The fire goes out.
  - **Fracture**: earth on a frozen foe cracks the ice: +40%, and for 5 seconds it's *cracked*: every
    spell hits it 20% harder, a friend's included.
  - **Blight**: life on a *shadowed* foe (hexed, blinded, withered, or in Hush, Eclipse, Echolocate,
    Resonant Shriek, Blackflame or Entropy) turns the darkness to rot: 3 damage and poison to it and
    up to 5 foes around it, and you heal 1 for each (once a second at most).
  - **Unweave**: arcane on a foe carrying two marks or more undoes them all, +30% for each (up to
    +120%).
  - **Rupture**: wind on a *bleeding* foe (Bleed, Rend, Cleave, Dismantle, Crimson Mist, Bonespur)
    tears the wound open: +50%, 4 more through armour, and you heal 2 (once a second at most).
  - **Elapse**: time on a burning, poisoned or withering foe lands everything they still had to deal
    at once, half again as hard (3 to 16).
- Rune tooltips in the Cord screen now say which of these a rune plays a part in: *"Leaves foes
  bleeding: wind damage on them sets off Rupture"*, *"On a frozen foe it sets off Fracture"*.
- Bolts of the right elements colliding in the air set off the new reactions too (fire and storm,
  earth and frost, life and void, blood and wind, fire and time).
- **Chain Reaction**, an advancement for setting off every reaction.
- **Creature affinities.** Creatures can be **weak** to an element (it hits them 50% harder), **resist** one
  (half) or, now and then, be **immune**. The Nether's creatures resist fire and fear frost, the cold's the
  reverse, the undead burn under life magic, golems conduct storm, arthropods are blown about by wind, the End's
  creatures resist void and fear time, and each boss has its own (the Cinder Warden is weak to frost, the
  Star-Eater and the Tide Scribe to life, the Archivist to void). A Runebound resists the element on its Cord.
  **A reaction breaks through a resistance**, so a Shatter still lands in full. A weakness struck floats
  **Weak!** over the creature in the element's colour, a resistance **Resisted**. The table is entity type tags,
  so a datapack can change it: see the [wiki](https://defnotean.github.io/wildercord/spellcraft/affinities/) and
  [docs/features/affinities.md](docs/features/affinities.md).
- **The Bestiary**, a new part of the Grimoire: every kind of creature your spells have struck, with the
  weaknesses and resistances you've found and a **?** for each still to find. Each weakness found condenses 25
  mana toward your next Heart Circle.
- **Elemental climate.** Where you fight nudges the elements: fire +20% and frost -25% in the Nether, void +20% in
  the End, storm +25% under a thunderstorm, frost in the snow, fire in hot dry lands (and less in the rain), void
  at night, life in sunlight, earth deep underground and arcane on a ley line or under a mana storm. A small mark
  after your spell's name on the HUD shows each element favoured (▲) or hindered (▼) where you stand, and the
  Grimoire lists it under *Where you stand*.
- Server config switches `features.creature_affinities` and `features.elemental_climate` (both on).

### Changed
- Joining a server whose Wildercord is **older** than yours (from before the version check) now says so in chat too,
  a few seconds after you join.
- **Cryostasis holds for 4 seconds at most**, however far Extend stretches it (three Extends made it 16 seconds
  of being untouchable), and nobody can cast from inside the ice: it's a moment's shelter, not a fortress.
- The 6th Heart Circle asks for **five different reactions** (any five of the eleven) instead of all
  of them, so it's no harder to reach than before.
- **Prismatic Burst** also uses up the new marks (cracked, shadowed, bleeding); it still deals at most 22.
- Frost no longer hits blazes, striders and magma cubes five times as hard (a vanilla rule meant for powder
  snow). They're weak to frost instead, +50% like every weakness.

### Fixed
- Other players no longer see your Cord vanish from your wrist for a moment after you respawn.
- **Fused runes skipped part of a crowd.** Cast into more than eight enemies at once (a Domain or a big
  Burst), Firestorm, Absolute Zero, Frostbite, Riftbolt, Heartstopper, Stormclock, Sanguine Rite and
  Timesteal did nothing at all to every enemy past the eighth, and Lifebloom didn't heal allies past the
  eighth. Every target now takes the hit; only the lasting and spreading parts (the fire leaping on, the
  cold setting in, the rift, the skipping heart, the bloom) are still kept to the first eight.
- **A teammate sealed in Cryostasis could be pulled into a wall.** If they went through a portal while
  sealed, the ice kept pulling them back to the spot where they were sealed, but in the new world, so they
  could land inside rock or over lava. The seal now breaks when they leave the world it was cast in, and
  Frostbloom and Geode stop answering blows there too.
- **Magma's burning ground could be parried.** Raising a Shield while standing in it parried its next second
  of burning (a counter-burst at its caster, and the Parry feat) as if a spell had just arrived. Like every
  other burning or freezing ground, its later seconds now only meet a Shield as a block. Its cracked, glowing
  ground also now lasts as long as the magma does, instead of vanishing after 4 seconds when Extend makes it
  burn longer.
- A Knot put on the Fusion Altar was called "a silent rune (its add-on is missing)". The altar now says what's
  really wrong (only effects with an element fuse, and a Knot has no power to rank up).
- **Passives couldn't be kept running for free any more.** Switching a passive on now pays its first second of
  upkeep straight away: switched on and off between two seconds, it cast its buffs at full length without ever
  paying, even with no mana at all.
- **Borrowed Time's debt can't be dodged.** Casting it again adds to what you still owe (it used to wipe most of
  it), logging out no longer forgives it, and dying settles it instead of carrying it on after you respawn. Paying
  it back could also, rarely, crash the server.
- Pets covered by your Shield no longer parry. A spell a pet turned back spared the monster that cast it and hit
  players instead; their Shields still block and shatter as before.
- A Grow cast beside a Rampart no longer crumbles the wall, and a Grow or Collect over a Span no longer takes the
  bridge away under you.
- Rewind can't take you back to where you were before you died.
- A lingering effect's later hits no longer follow a player through a portal, and On Land, On Hurt and On Low
  Health no longer play their shockwave in the world you left.
- Stasis ends when its creature is carried to another world, instead of pinning it to the old coordinates there.
- Relogging no longer resets your imbued items' shared cooldown.
- Cleave and Aftershock no longer release the imbued weapon in your hand, or roll Fortune twice.
- Soulfire's mana refund, and Siphon and Imbue in a chorus spell, are capped once per payment: a mana storm's echo or
  Twin Star no longer gets them a second time.
- A Spell Scroll costs twice what its spell costs you to cast, as the Cord screen shows it: the server's cost
  multiplier, your discounts and a found secret's price now count.
- Holding the cast key with the Tome of the Fifth Page's spell selected but the tome put away now charges your next
  spell, as a tap casts it.
- A cracked Heart Circle no longer adds to your innate rune's power.
- Rebirth no longer saves you from /kill or the void.
- In singleplayer, chest loot in a world opened after editing `wildercord.json` uses the new settings (it kept the
  old ones until /reload). A dedicated server no longer logs every config warning twice.
- Shift-clicking a Cord into its slot works alongside mods that add their own inventory slots.
- **Tooltips ran off the side of the screen.** A tooltip line was never wrapped, so most rune descriptions (and
  Imbue's, several hundred pixels long, on any screen) were cut off unless the window was very wide. Tooltips in
  the Cord screen, the Cosmetics page and the Fusion Altar, and those of the mod's items everywhere, now wrap to
  fit. One still taller than the window (the mana badge's, on a short window) is cut short at the bottom instead
  of losing its title and numbers off the top.
- An add-on's rune drew black-and-magenta squares in the magic circle beside the Cord screen and on the Fusion
  Altar. It now wears its family's art there, as its circle in the world always did.
- When the spell HUD had too little room beside the hotbar (a small window with the attack indicator or the
  off-hand slot on its side), it hid all but one of the spell's runes behind a "+2" even though they fitted.
- On a window a little too narrow for the spell HUD beside the hotbar's attack indicator (or a left-handed
  player's off-hand slot), such as 1280x720 or 2560x1440 at the automatic GUI scale, the HUD was drawn over the
  indicator or slot. It now sits on top of them when there's room beside the hotbar.
- The spell HUD's hint for an empty spell always said "K", even with the Open Cord key bound to another key.
- A Cord's beads lit up for three seconds after every respawn, as if it had just been put on (and for a player
  whose Cord showed up a moment after they came into view).
- Renaming a spell, then picking another spell before pressing Enter, gave the name to the other spell. Picking
  another spell now drops the name being typed.
- After leaving a world, the next one could show the last world's ley lines until it sent its own (on a server
  that never does, for good).
- **A finished contract you hadn't handed in yet vanished at dawn**, reward and all. It now stays on the
  Scribing Desk's board, in place of one of the new day's contracts, until you hand it in.
- **Bosses could be led out of their arenas**: any of them through a Nether portal (its altar or lectern then
  woke another, loot and all), and the Archivist in a boat or minecart. They stay where they belong now.
- A boss's bar could stay stuck on screen after its chunk unloaded, and the Archivist's showed to anyone who
  could see it from far off. Like the dungeon bosses', the Archivist's bar now shows only in its arena.
- `/kill` and the void couldn't kill a boss while it gathered itself between phases.
- **An Archivist lost without being killed never came back**: its lectern stayed dark for good. Like a dungeon's
  altar, the lectern now wakes a new one if its Archivist has been missing for a few minutes while players are
  there, and stays quiet only once it has truly fallen.
- The allies the Archivist calls up as it rewrites its Cord now go when it falls, as the dungeon bosses' do.
- Spectators watching a boss fall no longer earn its feat.
- A storm spell shocking the Tide Scribe's flooded arena also shocked your friends, your pets and any villager
  wading in it. It now spares everyone your spells can't harm (you still get a jolt if you're in the water).
- **A duel was a free heal**: when it ended, both duellists got back all the health and mana they'd had, whatever
  had taken it (a fall, a monster, spells cast at anything). Now only the harm your opponent did you is undone,
  and mana you spent stays spent.
- A duellist who logged off mid-duel was saved as the duel left them, and a server shutting down during a duel
  gave one of the two a loss. The one logging off is now put back as they began, and a shutdown ends the duel
  for nobody.
- Accepting a duel that couldn't start yet (too far apart, hurt too recently) used up the challenge, and the
  reason given when one of you was dead was that you were too far apart. The challenge now stands until it
  runs out, and the message says who can't duel.
- Turning the time back with `/time set` gave you a fresh day of rune buybacks at the Runesmith. It now counts
  as the same day, as the contract board already did.
- Any villager's trade asking for a rune (from a data pack, say) was taken for one of the Runesmith's swaps: it
  gave no experience and refused ranked runes. Only the Runesmith's own swaps are treated that way now.
- Casting at a wild wisp (which nothing can hurt) counted toward the casting and reaction contracts, as if it
  were a real foe. Like a Training Dummy, it no longer does.
- New Ember Sanctums are no longer overgrown by basalt columns and lava sheets in basalt deltas, and no longer
  generate through fortresses and bastions.
- **A server that crashed or was killed could leave a Rampart's packed mud, a Span's magenta glass or a Light
  spell's invisible light in the world for good.** They're now written down with the world and taken down as
  soon as their ground loads again. Taking them down no longer loads far-off chunks either.
- A Rampart or Span raised over a Light spell's light put the light back when it came down, leaving it lit
  forever. They now leave such a light alone.
- **Stars could stop falling in a world until the server restarted**: one that landed where nobody went
  afterwards (so it never faded) kept counting as the world's star. Stars now also land only where the world is
  running, and one landing on something built while it fell burns up instead of crushing it.
- When a Fallen Star went, its crater filled back in over anyone standing in it, and guards that had wandered
  out of loaded ground stayed in the world for good. Anyone in the bowl is now lifted clear, and every guard
  goes. A Wither could also break a star, leaving its crater open and its guards behind; stars are now safe
  from the Wither, and one taken away any other way (by a command, say) still cleans up after itself.
- **A rift's or star's monsters could pick up a fallen player's gear and take it with them when they went.**
  They no longer pick anything up, and one sent away drops whatever it was carrying.
- A rift's monster that turned into another (a zombie drowning, a skeleton freezing in powder snow) vanished on
  the spot, and its wave could then never be beaten. It now stays, still one of the rift's (or the star's) own.
- Two mana storms could roll in on top of each other from neighbouring regions. A storm no longer starts where
  it would overlap one already raging.
- `/wildercord event` worked in any dimension (a star would land on the Nether's roof). Like the events the
  server rolls, it now works only in the Overworld.
- Icepath's frosted ice never melted in the dark (in caves, or at night). Like frost's, it now thaws back into
  water after half a minute wherever it is.

## [0.4.1-alpha] - 2026-09-28

A player wiki, and the bugs found while writing it.

### Added
- **The Wildercord Wiki**, at [defnotean.github.io/wildercord](https://defnotean.github.io/wildercord/). It covers
  every rune (its icon, what it does, how to get it and which modifiers work on it), every recipe, every dungeon
  and boss with strategy, and every mechanic from your first Cord to the 8th Heart Circle.
- Mana storms now give their runes: from your 10th cast under a storm, a surge can crystallise **Manaburn** or
  **Manatide** straight into your pack, one per storm.

### Changed
- **Rapid pays for the whole spell** wherever it sits, so putting it on an empty shape no longer halves a
  spell's cooldown for free.
- An **Echo or Pulse after On Hit or On Kill** repeats for the first hit or kill only (it was charged once but
  fired for every hit). The readout says so.
- **Secret spells** show their real price and longer cooldown once you've found them. Until then, the HUD,
  spell wheel and scrolls keep their names hidden, as the Cord screen always did.
- The **spell wheel** shows the Tome of the Fifth Page's spell while the tome is held.
- **The Cord's beads glow only when there's a reason:** for a couple of seconds after you put the Cord on,
  while you charge, and in a flare after you cast. The rest of the time they show their material, instead of
  glowing all the time.
- **Joining a server with a different Wildercord version** now tells you in chat which version to install.
  Runes one version knows and the other doesn't show as Silent Runes, and their tooltip now says why.

### Fixed
- **Runes from chests and bosses came out blank.** Minecraft 26.3 reads loot tables in a new format and
  silently skipped the part that sets which rune an item is. So the Archive's chests, every dungeon chest and
  every boss drop handed out Silent Runes, and advancements gave 1 Blank Rune or Mana Crystal instead of the
  full amount. All loot now uses the new format. Any blank rune you already have turns into a random rune
  (Tier I to IV) the moment it's in your inventory.
- Manaburn and Manatide couldn't be obtained at all.
- Upgrading a Cord lost its enchantments and name. It now keeps them.
- With PvP off, your spells could hurt other players' pets and familiars. They're as safe as their owners now.
- One huge hit could skip a phase of the Archivist, unlike the other bosses.
- The server config's `world_events`, `world_changing_magic`, `duels` and `wild_magic` switches did nothing.
- The Heart Circle boss breakthrough left out the Cinder Warden, the Star-Eater and the Tide Scribe.
- Effects inside a Knot didn't count toward element contracts.
- The "Where it lands" lines were missing from rune tooltips.
- Runes of the world listed dungeon vaults and bosses among their sources, though those never give them.

## [0.4.0-alpha] - 2026-09-28

### Added
- **A fused rune for every pair of elements.** The Fusion Altar now fuses any two effects of the ten
  elements, and two of the same element too: 55 fused runes in all, 43 of them new. See the grid in
  the [Fusion Altar guide](docs/features/fusion-altar.md). A few of the new ones:
  - **Phoenix Pyre** wreathes allies in healing flame.
  - **Singularity** opens a black hole that bursts.
  - **Second Wind** saves an ally from a killing blow.
  - **Reckoning** makes every wound come due twice.
  - **Chronoshift** turns an ally's cooldowns forward.

### Changed
- **Fused runes wear both of their elements.** In a spell's magic circle, a fused rune's ring is a
  braid of two strands, each element's motif in its own colour. Its emblem is split down the middle
  between the two elements' glyphs, and a fusion of one element with itself shows that element's
  glyph widened.

### Fixed
- The Astral Observatory's altar drew as a magenta-and-black missing texture. A test now checks that
  every texture the mod's models use exists.

## [0.3.0-alpha] - 2026-09-28

The big one: three new dungeons with bosses, 51 runes you can only find out in the world, a Fusion
Altar, familiars, world events, a Runesmith villager, duels, casting gear, parries, wild magic and
an advancement tab. Player guides for each are in [`docs/features/`](docs/features/).

### Added
- **Dimension dungeons.** Each dimension has a dungeon and a keeper that fights like a caster, with
  one trick to learn:
  - the **Ember Sanctum** in the Nether: the Cinder Warden's armour turns everything except a
    reaction (Shatter, Conduct, Wildfire...)
  - the **Astral Observatory** in the End: the Star-Eater turns spells back until a heavy enough
    spell, or its own parried shard, breaks its shield
  - the **Drowned Scriptorium** on the deep sea floor: the Tide Scribe floods and drains its pit,
    and storm magic in the flood shocks everyone wading in it

  Each has Rune Seal doors, Runebound guards, a domed arena and a vault. Boss fights run in three
  phases, with a boss bar that names the spell being cast. Everyone who hurt the boss gets a Tier IV
  rune they don't know yet.
- **51 runes of the world** that can't be crafted:
  - found in vanilla structures' chests (Elder Guardians carry the ocean monument's)
  - found in Wildercord's dungeons and on their bosses
  - found in fallen-star craters, rift sieges and mana storms
  - found by **Attunement**: hold a Blank Rune and meditate in the right land at the right moment,
    and the land's rune slowly fills it

  The Grimoire lists every one by where it's found, with a riddle for the ones you haven't learned.
- **The Fusion Altar.**
  - Three of a rune make its next rank (up to III, +50% power).
  - Pairs fuse into new effects.
  - A Blank Rune and string tie a whole spell into one **Knot** that takes a single socket and costs
    10% less.
- **Familiars.** Wild **wisps** rise from ley lines at night, one per element. Tame one with its own
  element's magic or its favourite food. Your familiar floats at your shoulder, quickens your mana,
  casts a little spell of its own every few seconds and levels up as you fight together.
- **Cord cosmetics:** a Cosmetics page for your Cord's beads and glow colour. Some are bought with
  materials, some are earned (the 4th Heart Circle, slaying a Runebound), and everyone near you sees
  them.
- **World events** in the Overworld:
  - **Mana storms** over ley lines: mana flows twice as fast, spells cost 25% less and casts
    sometimes surge.
  - **Fallen stars** leave a guarded crater with a star to crack open.
  - **Rift sieges** send waves of monsters and end with the Riftcaller.
- **Magic that changes the world.**
  - Fire lights grass and boils water into blinding steam.
  - Frost freezes water you can walk on (it always thaws).
  - Storm arcs through water to everything standing in it.
  - Life makes flowers spring up and crops grow.
  - Wind knocks arrows back and blows fires out.
  - Earth heaves the ground and throws foes up.
  - Void draws loose items in.
- **Parrying.** A Shield raised at the last moment turns a flying spell back at its caster, or
  answers anything else with a counter-burst.
- **Wild magic.** Overcasting may surge into something unexpected.
- **The Runesmith**, a villager who works at the new Scribing Desk:
  - sells runes, Torn Pages and Mana Crystals
  - buys duplicate runes, or swaps two known runes for one you haven't learned
  - posts **daily contracts** on a contract board
- **Duels:** a formal one-on-one fight that puts everyone's health, mana and effects back afterwards.
  Your record shows in the Grimoire.
- **Chorus casting:** two or three allies casting the same shape at the same target within a second
  sing a chorus: the last spell gains +50% power per extra voice, and grows wider.
- **Casting gear:** held items that shape your spells.
  - Elemental and greater **staffs** (+20% / +35% power for their element, and cheaper spells).
  - The **Tome of the Fifth Page**, which gives you a fifth spell.
  - Four off-hand **foci**: Haste, Thrift, the Deep Well and Echoes.
- **A Wildercord advancement tab**, from your first Blank Rune to the 8th Heart Circle.
- **A server config** (`config/wildercord.json`) for cast budgets, loot, PvP scaling and events,
  reloadable in game.
- **An add-on API** for other mods: custom runes and effects, plus events before a cast, after a
  cast and when a spell is blocked.

### Changed
- **Crisper magic circles.** Big ground seals (Sandstorm, Vortex, Riftcall, Moonpetal, Hush, Eclipse,
  Magma, reticles and the rest) are drawn in thin lines of light with little glyphs, like a spell's
  own circle, instead of a stretched low-resolution texture with fat pixel strokes. They draw
  themselves in as they open, and the zone seals now sit on the edge of the area they cover.
- **Attunement is a ritual.** The land's own rune circle opens under you and slowly turns, motes of
  its colour rise off the ground and spiral into the Blank Rune, and it builds through the four
  stages (a rune band, then rays, then an outer ring). It finishes in a flash, rings racing out and
  the rune's emblem shining over your hand. Meditation's ring is soft light instead of squares.
- **Storm through water** now shows: branching arcs skitter over the surface from the strike to
  every creature shocked, the water flashes and sparks burst off each one.
- **Steam** is a soft white cloud that swells, rises, drifts off and lingers instead of vanilla
  puffs; a Steam fusion and a Shatter boil off the same way.
- **Wild magic**: the butterflies are bigger, brighter, many-coloured and actually flutter, with
  little fireworks overhead; every surge's swirl is broader, with motes spiralling up into a flash.
- The mod's own smoke (explosions, burning, snuffed fires, bosses, fallen stars) is soft grey
  billows instead of big black squares.

### Fixed
- **Multi-hit spells now land every hit.** On Minecraft 26.3, a creature hit twice in the same moment
  shrugged off the second hit, so Inferno, Wards, innate runes and chained effects did far less
  than intended. Every hit counts now.
- **The game no longer crashes with Improved Transparency (Fabulous-style) graphics** turned on: the
  glowing spell particles now have the rendering pass it needs.
- A discount too small to survive rounding (a staff's 10% on a cheap spell) now still takes at least
  1 mana off.
- Very long Knot names no longer disconnect you. An add-on that throws an error can't crash the
  server. Innate runes can't be tied into Knots. The Surge fusion's Strength stops at II, and fused buffs
  get stronger only from Amplify and rank, not from charge or gear.
- Combining ranked runes keeps the lower rank, not the higher. A quiet selected spell 5 falls back
  to the next spell instead of doing nothing.
- The Runesmith's buyback and swaps can't be farmed:
  - only plain rank I runes it sells
  - 8 buybacks a day
  - no XP from them
- Duels:
  - they restore a snapshot rather than healing anyone
  - they can't be started mid-fight
  - an outsider's blow calls the duel off
- Chorus only joins allies' voices, and never cancels the earlier cast.
- Contracts only count spells that land on a real creature (not Training Dummies).
- Familiars:
  - only their owner can name them
  - projectiles pass through them
  - the Void wisp only pulls drops nobody owns
- Cosmetics cost materials in creative too.
- Dungeon bosses:
  - no single blow can skip a phase
  - loot goes to the fighters
  - boss bars show to anyone near the altar
  - a boss lost without falling re-arms its altar
- The Tide Scribe's pit starts dry and drains completely after every ebb.
- Fallen-star guards stay guarded. A rift can only be sealed once you've beaten its waves, and
  rewards scale with the waves beaten. Event cooldowns survive a restart.
- Soulfire refunds mana only for damage that landed. Lingering burns refresh instead of stacking.
- Manaburn is capped. Constellation, Starshard and Cinderheart need line of sight.
- Attunement gives each land's rune once a day.
- Frosted ice always thaws, even after a restart.
- Inferno and Dragon Breath's later pulses can be blocked by a Shield but not parried.
- Familiars no longer trail far behind you in the air.
- The HUD's Shield readout no longer writes over the spell's name.
- Many flashes came out as darkness instead of light (a parry, a Shield blocking, a bolt's hit, a
  Heart Circle cracking, Chorus and Unison, Fortune, Phantom, several secret spells and the wild
  butterflies' fireworks): a colour written with a full alpha byte carried the darkness flag. They
  shine again. Void's own dark seals (Hush, Eclipse, Riftcall, Portalfall) are darkness again too,
  with a violet rim, where they had come out as faint light.
- A storm's echo, Twin Star, a Focus of Echoes or a wild surge's second go could Imbue again (a
  second imbued item or glyph for one payment) and Siphon past the per-cast cap. Every copy of one
  cast now shares its payment's limits, and keeps its casting gear (the storm's echo lost it, and
  so did a wild surge's copies and a parried spell).
- Glyphs now share their maker's imbued cooldown: a dozen on one redstone line no longer go off
  together. Breaking your own glyph and putting it down again no longer resets its re-arm.
- A glyph on a block from a mod that's since been removed could lose every glyph in that dimension;
  now only that glyph is dropped.
- Add-on effects named like a built-in rune (`example:bleed`) ran the built-in one as well.
- `BEFORE_CAST` fired on both presses of an overcast (and a circle cracked even if an add-on then
  stopped it), and asked a non-zero cost for a wild surge's free recast.
- A Blood Price spell ignored a mana storm's discount, and a secret spell's price was rounded twice.
- A parry now fires `SPELL_BLOCKED`, like any other block.
- Advancements: "know N runes" counted Knots and missing add-ons' runes (the Heart Circles don't);
  Every Word asked for runes nobody can get, and Every Page Filled for Mirrorfrost's feat (only that
  innate rune earns it) and feats that need other players (Unison, Domain Clash, Chorus).
- The server now drops floods of spellbook packets past a generous allowance, skips saving a
  spellbook that didn't change, checks a spell's cooldown before compiling it, and only rechecks
  the runes-known advancements when the runes known change.

## [0.2.1-alpha] - 2026-09-28

### Changed
- **Imbue balance.** Releasing still costs no mana, but it's no longer a way around cooldowns or a
  mana bank:
  - Everything you've imbued shares one cooldown, as long as the stored spell's own (Rapid, Vow and
    heart perks counted, at least half a second). Swapping between imbued swords, bows and armour
    can't release a spell faster than the Cord could cast it, and Vow in a stored spell (twice the
    power) now waits out its four-times cooldown. Bows follow it too: a shot while it cools is a
    plain arrow and keeps its charge.
  - You keep at most **6 imbued items**: imbuing a seventh lets the oldest one's magic fade.
  - Releases no longer Siphon mana back (they were paid for when imbued).
  - A glyph re-arms no faster than its spell's cooldown (at least a second), and catches a creature
    standing on it once, not every second until its charges are gone.

### Fixed
- A stored spell that only helps (a Heal in a sword, a Shield in a chestplate) went off at the foe
  you struck or the block you broke, and did nothing. It now goes to whoever holds the item.
- An Echo inside a stored spell repeated the whole spell from you (and was priced that way); it now
  repeats only what was stored, at the same target. Combo in a stored spell never fires, and the
  Cord screen now says so.

## [0.2.0-alpha] - 2026-09-28

### Added
- **Shield is a spell shield now**: a one-time spell block, invisible until a harmful spell comes
  at you. Then its magic circles spawn in front of the spell, stacked one behind another (one for
  every 8 mana of the spell that raised it, up to 7). A spell that cost as much or less punches
  through as many circles as its mana pays for, and the next one stops it in a flare of light and
  ripples; one that cost more cracks every circle and shatters them like glass, front to back (the
  rims into curved slivers, the rest into tumbling, glinting shards), and goes through. Flying
  spells make the circles appear a moment before they arrive, and strike the front circle. The HUD
  shows your Shield's strength and time left. (It used to give absorption hearts; Barrier and
  Stoneskin still do.)
- **Imbue**, a new Tier II link: the rest of the spell is stored, with 3 charges, instead of cast.
  With Self it goes into the item in your hand: weapons release it at what they strike, tools at
  the blocks they break, bows with their arrows, armour and shields at whatever hurts you, and
  anything else when used. **Any block** can hold magic too: aimed at a block, cast with empty hands
  at the block you're looking at, or placed from an imbued block item, it becomes a glyph that goes
  off at whoever steps on, uses (doors, chests, buttons), shoots or breaks it, or when it's powered
  by redstone. Break your own glyph and the block comes back still imbued. Imbued items glint and
  say what they hold.
- Three feats: Spellguard, Shieldbreaker and Imbuer. New sounds for a spell ringing off a shield
  and for imbuing; the shield's shatter now ends with shards pattering down.
- Monsters can cast spells: the engine now takes any living caster. **Runebound** zombies,
  skeletons, witches and illagers carry Cords, telegraph every cast with a magic circle and show
  their spell as their nameplate.
- **The Archive**, a buried library with Rune Seal doors (opened by spells of the right elements),
  Runebound guards, a domed arena and a vault of Tier IV runes; and **the Archivist**, a
  three-phase boss that rewrites its Cord and casts a secret spell.
- **Charged casting** (hold the cast key) with a growing magic circle and an aim preview; the new
  `wildercord:sigil` particle draws magic circles everywhere.
- Magic circles you can read: every spell writes its own circle (a frame, a band of script made
  of its runes, its element's pattern, a star with a point per rune and on each a roundel with that
  rune's emblem, and its shape's seal in the middle). Every rune has an emblem and ring pattern no
  other shares (generated by `tools/circle_art.py`). The same circle opens in front of a charging
  caster's raised hands, lies under every cast, is a monster's telegraph and a Domain's floor, and
  stays small: a longer spell gets more points, not a bigger circle.
- The **spell wheel** (hold the switch key; let go without pointing and it stays open until you
  pick a spell, by click or number, or press Esc), **spell names** (automatic, and renamable), and
  **spell codes** you can copy and paste in the Cord screen and that turn into readable cards in
  chat.
- Ten **secret spells** found by exact rune sequences, and **Torn Pages** with their riddles (and
  the way to the nearest Archive), found in old libraries, ruins and the Archive, and dropped by
  Runebound and the Archivist.
- The **Grimoire** page: reactions, secrets, feats and riddles discovered, with toasts; each new
  reaction, secret and feat is worth condensed mana.
- Ten **innate runes**, one awakened per caster at the 1st Circle.
- **Rhythm** casting, **elemental leaning**, and **overcasting** (crack a Heart Circle to cast
  beyond your mana).
- **Spell collisions**, **domain clashes** and **Unison** strikes between casters.
- **Ley lines** (visible to Cord-wearers; mana regenerates and Heart Circles form twice as fast on
  them) and the **Wellstone**.
- **Spell Scrolls** anyone can cast once, and the **Training Dummy** with floating damage
  numbers and DPS.
- Operator commands `/wildercord innate <rune>` (choose your innate rune) and
  `/wildercord runebound` (bind the nearest monster to a Cord).
- **41 new Tier I and II runes**, all craftable:
  - Energy balls and beams: **Spark**, **Ray** and **Nova** (Tier I); **Wisp**, **Comet**,
    **Ricochet**, **Cluster**, **Lance**, **Sweep**, **Prism** and **Stream** (Tier II), drawn as
    glowing orbs and beams of light.
  - Protection: **Barrier**, **Brace**, **Anchor**, **Bramble**, **Frostward** and **Cushion**
    (Tier I); **Deflect** and **Haven** (Tier II). Anchor, Frostward and Cushion can be passives.
  - Mining and building: **Chisel**, **Glimmer** and **Prune** (Tier I); **Tunnel**, **Vein**,
    **Smelt**, **Fell** and **Span** (Tier II). They break blocks as a pickaxe of their strength
    would, and only where you may build.
  - A simple spell for every element: **Ember**, **Icicle**, **Pelt**, **Windcut**, **Leech**,
    **Hex**, **Rend** and **Countdown** (Tier I); **Jolt**, **Bleed**, **Coldsnap**,
    **Flashfire**, **Banish** and **Cyclone** (Tier II). An Orbit passive can carry Ember,
    Icicle, Pelt and Windcut.
- **Wildercord's own sounds**, synthesised by `tools/sound_art.py` and all in one key so they
  harmonise: a cast and an impact for every element, magic circles, beams, orbs, shields, domains,
  blinks, the Cord screen, the spell wheel, the Grimoire and Heart Circles. Charging now hums,
  rising in pitch as the charge builds, chimes when it's full and rushes out on release.

- **Magic glows.** Every circle, beam and effect is drawn with additive light, so overlapping
  light burns brighter, and void magic is drawn as darkness with a thin violet rim.
- Bolts glide as **comets** with tapering trails, drawn by each player's game from the bolt's
  smoothed position.
- **Secret spell circles:** each secret spell's circle has a centrepiece of its own (a sun, a
  snowflake, a horizon, a flower, lightning, a black star, a clock, wings, nested stones, a
  constellation).
- **Casting poses:** you raise both hands while charging and move with each shape when it goes off
  (a thrust, a sweep, alternating blows, arms flung up for the great circles).
- **Screen effects:** big impacts shake the camera, a charged release kicks the view, heavy hits
  land with a punch, and a Domain tints the edges of the screen of everyone inside it. They follow
  vanilla's Screen Effect Scale.
- **The worn Cord** shows on every player's wrist: a band in its tier's material and a glowing
  bead for each rune of the spell they have ready, brighter while charging.
- The Cord screen shows the edited spell's **magic circle** beside the window, opening as you build
  it; on the Grimoire page it shows the secret spells you've found, like plates in a book.

### Changed
- Heart Circle breakthroughs ask for feats (reactions, secrets, Runebound, the Archivist) instead
  of ever-bigger kill counts.
- The HUD shows the spell's name, the rhythm beat, cracked circles and the charge.
- Impacts and bursts flash with a small glow of their own instead of the vanilla firework flash,
  which up close showed as a pale square several blocks wide.
- Casting no longer fills your own first-person view: the spin of your Heart Circles on each cast
  is shown only to the people around you, the circle under every cast is a flat magic circle
  instead of a dust ring, the cast no longer throws potion sparkles, and bolt trails start a
  little way out. (You still see your rings while meditating and when a circle forms.)
- Every shape is drawn in light (a new `wildercord:light` particle: shockwaves, beams with a hot
  core, sweeping crescent slashes and orbs): bolts are glowing comets, beams fire through a circle
  with rings of power, Crescents are blades of light, Bursts throw shells of light, Rain opens a
  circle in the sky, Pillars erupt as columns of light, and a Domain's floor is the spell's own
  circle under a dome of light.
- A Blitz on the ground runs level, so looking slightly down no longer stops it dead.
- Bolt hits are softer: finer, fading trail dust, fewer sparkles and a finer ring on impact, so the
  burst no longer hides what it hit. Big shockwaves (explosions, mines) keep their weight.
- Casting monsters hold their telegraph circle out in the right hand, so their face stays in view.
- Tectonic Rise raises real stone spires and Glacial Lance closes its targets in ice (block
  displays, never real blocks). Starfall and Starlight Cascade mark where each star will land.
- Creatures and places look the part: Runebound glow with rune marks in their spell's colour (and
  a faint aura) that flare as they telegraph; the Archivist is a hooded, hovering figure with a
  floating tome and circling pages that tear loose when it rewrites its Cord; ley lines flow as
  ribbons of pale violet light; the Archive has shafts of light and dust, flickering braziers, a
  glowing inlaid circle and quiet whispers; an awake Wellstone wears a turning halo.
- Every effect is drawn in its element's own visual language instead of dust puffs: fire licks and
  flares, frost shatters and creeps, storm forks into branching lightning, wind swirls, earth
  cracks, life blooms, void is darkness imploding round a black core, arcane draws star seals and
  orbiting comets, time turns clock hands and blood cuts and pulses. Reactions, Heart Circles,
  innate runes, Unison, domain clashes, overcasting and the secret spells got the same treatment.

- Circles and shaped light never open right in front of a player's own eyes (a beam's hand circle
  used to fill its caster's screen), and stay silent while a passive renews itself quietly.
- Crescent's recipe gains a feather, so it no longer clashes with Empower's.
- Rune emblems are handed out in the order runes are defined, so adding runes no longer changes
  existing emblems. (This reshuffled some emblems once.)

### Fixed
- Two same-element bolts colliding no longer throws an error.
- Spells no longer mine a Rampart's temporary wall (which dropped packed mud).
- Span's rules (its glass drops nothing, bridges come down when the server stops) are in force from
  the start, not only after the first Span is cast.
- The HUD's charge readout no longer overlaps the mana count.
- A spell of linked repeating shapes (Zone On Hit Zone On Hit ...) could multiply into hundreds of
  thousands of casts and stop the server; a whole cast now runs at most 128 parts.
- Overcasting can't pay for a spell costing more than twice your full mana (Overcharge stacks
  could one-shot bosses or freeze the server for the price of one cracked circle), and a prompt
  left over from another world no longer overcasts on the first press.
- Widen is capped at eight times the radius, so a heavily widened Prune, Icepath or Collect can't
  scan hundreds of blocks and load chunks; Icepath counts against the block budget, and Collect
  reaches at most 24 blocks and never out of land you can't build on.
- Spells that change blocks respect claim and protection mods (a spell's break is offered to them
  as the caster's own), as the Break rune always promised.
- Something held by Stasis or Infinity no longer floats forever when it's saved held (a player
  logging out in Stasis, arrows frozen when the server stops).
- Spectators and dead players can't cast, run passives or share a boss kill; Rebirth cast from the
  death screen no longer arms a free death save.
- A charged spell no longer fires when you open a screen (the spell wheel, chat, the pause menu):
  it waits, like a drawn bow, and dying drops it. A charge that fizzled no longer casts when you let
  go.
- Mashing the cast key no longer keeps the rhythm chain at full: a press before the beat starts it
  over, as it should.
- Spell cooldowns are saved, so logging out and back in doesn't reset them.
- Monsters' spells hit players at full power; only damage between players is scaled down.
- Execute and Unison apply to damage that lands later (Meteor, Countdown, Starfall, the second and
  third Dismantle slashes, Aftershock and Bleed after the first hit, and more).
- Zero Hour follows the PvP rule and leaves creative and spectator players alone.
- Shackle only slows bosses instead of chaining them in place.
- Stoneform on a pet no longer hurts its owner, and its knockback resistance comes off.
- Phantom's afterimages, Light's light blocks and wards no longer outlive a server stop, and state
  from one singleplayer world (wards, reaction marks, bolts in flight, overcast prompts) no longer
  leaks into the next.
- Pistons can't move a Span's glass or a Rampart's wall somewhere it would never be taken down.
- An Orbit passive is cast again straight away after a dimension change, instead of charging upkeep
  with nothing orbiting.
- The 1st Circle's title is no longer cut off by the innate rune waking in the same moment.
- The server forgets a player's charge, passives, prompts and meditation when they leave.
- `/wildercord learn` and `/wildercord innate` accept namespaced ids (`wildercord:fire`, an
  add-on's runes).
- A Spell Scroll made by command with more than 16 runes no longer disconnects everyone it's sent to.
- A charge whose Cord was taken off now fizzles.
- **The Archive:** its first two seal doors (Frost and Storm, Fire and Wind) are generated instead of
  being walled over, its stairway steps face the right way, and the vault chests turn with the
  structure.
- Runebound keep their extra health through a reload (Archive guards never had it), don't take over
  a named mob's name, keep their Cord when they turn into another monster (and lose the nameplate
  when they turn into something that can't carry one), and the Archivist's guards are bound once.
- The Wither can't destroy Rune Seals or the Archive Lectern.
- A sneaking caster's spell hits the Training Dummy instead of picking it up; its damage numbers and
  DPS nameplate no longer stay behind after a save.
- Brushing trail ruins can actually find Lightning and Shock runes.
- The Ender Dragon's runes land at the feet of whoever killed it, not over the void.
- The previous world is let go when you quit to the title screen (some effects held it in memory),
  and screen shake, comets and the charge hum reset properly between worlds and dimensions; the
  camera no longer shakes behind the pause menu.
- Switching page in the Cord screen mid-drag no longer crashes it, and category chips that don't
  fit are no longer clickable. Right-clicking a rune (to take it off a spell, or thread it from the
  Codex) works again, and a click the Cord refuses now says why under the spell's name (a spell the
  Cord doesn't hold yet, a rune too strong for it, a full spell, a rune that can't be a passive)
  instead of only playing a low note.
- The worn Cord fits slim arms and shows over sleeves.
- Orb rings keep spinning on very old worlds, the Wellstone halo no longer jumps once a day, and
  spell wheel labels stay on screen at small GUI sizes.
- The Cord enchantment tag has a name in recipe viewers.

### Removed
- The Stand shape. Cords that still hold a Stand rune keep it threaded, but it stays quiet.

## [0.1.0-alpha] - 2026-09-27

The first public version.

### Spells
- The Cord slot (above the offhand) and four Cords: Twine, Copper, Amethyst and Echo, with more
  sockets, spell slots, rune tiers and mana at each step.
- The rune sequencing engine: shapes, effects, modifiers and links, read left to right, with a
  plain-English readout, cost and cooldown for every spell.
- 135 runes across four tiers and ten elements (Fire, Frost, Storm, Wind, Earth, Life, Void,
  Arcane, Time, Blood), each with a hand-drawn icon.
- Element reactions: Shatter, Conduct, Wildfire, Implode and Collapse.
- Stasis holds every hit on a stopped target and lands it all when time moves again.

### Progression
- Mana: Mana Crystals, Clarity and Mana potions, meditation, and the Reservoir, Wellspring and
  Siphon Cord enchantments.
- Spell enchantments for Cords: Potency, Celerity, Thrift and Persistence.
- Heart Circles: condense mana by casting, earn breakthroughs, and meditate to form up to eight
  circles, each adding mana, regeneration and power, with perks at the 3rd, 5th, 7th and 8th.
- Passive spells: up to two always-on spells that cost mana every second, from a restricted set of
  sustainable runes.
- Every Tier I-III rune is craftable (costlier by tier) and appears in the recipe book; Tier IV
  runes drop from bosses and rare structures. Chest loot favours low tiers.

### Interface
- The Cord screen: drag and drop, type-to-search Codex, family tabs, category chips that appear as
  you learn runes, a Spells/Passives switch, and heart, mana and help badges.
- The spell HUD beside the hotbar, readable at every GUI scale.

### Technical
- Server-authoritative casting with per-cast budgets, friendly fire off, and PvP damage scaling.
- A generated asset pipeline (`tools/`) driven by the rune roster.
- Unit tests for the spell engine and client game tests for mechanics and layout.
