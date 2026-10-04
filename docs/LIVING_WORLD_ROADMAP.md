# Wildercord: a living, handcrafted magical adventure

Active user goal, started 2026-10-02. The full objective remains open until the requirements below are implemented and verified. This roadmap is a scope and evidence ledger, not evidence of completion.

## Baseline audit

Inspected main at `14efc61e`: Aura steps 1–10 exist; step 10 is merged, despite a stale status table now corrected. Step 11 (world) and 12 (mage/swordsman) remain incomplete. Wildlife currently has six registered species with biome/pool/crowding rules. DungeonStructure currently dispatches eight dungeon types. Runes.java contains 351 RuneDef declarations; this is a source inventory count, not a claim that all 351 presentations work. Existing FieldGuide, RuneResearch, relic, companion, equipment and generation systems should be extended.

The earlier test evidence is useful but does not verify the expanded goal. Every ability needs a presentation review; every new creature needs behavior and habitat evidence. No broad visual or balance claim is established by this initial code audit.

## Content targets

These targets are additions to the baseline, subject to documented design adjustments that preserve the full goal.

- 12 new creatures: four peaceful/magical wildlife, three hostile/territorial creatures, two Aura-resistant beasts, two bosses, one useful companion. Each needs custom identity, habitat, behavior, model/animation, sound, rewards, bestiary and tests.
- Four connected ecosystems: luminous wetlands, wind-carved highlands, ember woodlands, subterranean fungal ruins. Each needs resources, shelter/foraging behavior, ecological relationships, bounded spawning, and meaningful magic interactions.
- Six distinct new dungeon/encounter locations, plus old battlefields and the sleeping blade. Include the sword tomb, its intent gates and guardian. Deliberate biome distribution, rarity, alternate routes and useful return visits.
- 36 new functional items: eight relics, eight equipment/accessories, six exploration/support tools, six placeable utilities/decorations, eight consumables/material uses. Distinct tradeoffs, acquisition and recipes; no filler recolours. The total is a minimum: if a complete ecosystem adds another useful material, retain all category minimums and exceed 36 rather than relabeling materials as equipment. The current 17-item ledger and pending nursery additions imply 37 items to fulfill every category; see `docs/audit/functional-item-categories.md`.
- At least 12 new magic/Aura abilities and 18 authored signature fusions. Audit all existing abilities and supported pairings, including formation, launch, impact, sound, mechanics and counters. Every supported pairing must resolve intentionally.
- 24 discoverable lore texts/entries, four optional connected investigation/quest chains, illustrated bestiary/journal integration and consistent regional/faction histories.
- Finish all remaining Aura step 11/12 requirements: terrain training, battlefields, sword tomb, sleeping blade, Aura beasts, village tournaments, resonant strikes, rune-etched blades and Unity.

## Milestones and current state

| Milestone | Scope | State | Required evidence |
| --- | --- | --- | --- |
| A | Audit/player journey; finish Aura world and cooperation | Started: terrain training; Marchkeeper battlefields; sword tomb and keeper; sleeping blade; two Aura-resistant beasts; village tournaments; resonant damage strikes; rune-etched blades; Unity | Runtime terrain checks/trials; all step 11/12 encounters/items; solo/cooperative balance; pictures/JAR |
| B | Visual identity and full ability roster audit | Started: caster-centered anchors;18 newly authored signatures have scoped acceptance; full lifecycle matrix remains open | Per-ability matrix; first/third-person captures; reduced effects/shaders; fusion behavior |
| C | Four ecosystems and twelve creatures | Started: highland beasts, forage/shelter and Windreed; Lantern Newts, Moonreed pollination, Reed Refuges and Reedback Crabs; full ecosystems pending | Habitat/AI/spawning/persistence tests; custom art and animation; observation gameplay |
| D | Locations, items, acquisition and counterplay | Started: tomb, finite equipment/resource tools, damage allowance and counterplay fixes; larger roster pending | Generation/loot/recipe tests; combat matchups; equipment/inventory verification |
| E | Lore, quests, field journal and guide design | Started: eight credited lore texts and illustrated guide increments; connected investigations pending | Rendered book review; discoverability; useful clues; text/nav checks |
| F | Early/mid/late progression and integration polish | Pending | Playable journey; multiple builds; existing-world upgrade; multiplayer/performance checks |

Each milestone must integrate code, generated art, sounds, recipes and documentation, capture actual gameplay, produce a tested JAR, record limits and commit/push completed work. Public releases/CurseForge uploads require an explicit user request.

## Visual and gameplay invariants

Casting circles stay behind the caster; spell-specific formation and discharge stay readable. Fusions interact mechanically and visually. Aura uses physical expression and no magic circles. Cinnamon retains her custom model, configurable owner, immortality and functioning interactions. Powerful attacks have practical counters; first contact must not consistently decide combat. Important information has non-colour cues and adjustable visual/sound intensity.

Use existing systems when useful, but preserve authored identities. No uncontrolled populations, destructive ecosystem loops, unbounded effect/cache growth, mandatory daily chores or progression based mainly on repetition. Lore connects to creatures, resources, locations and choices.

## Completion audit

For every numbered goal requirement and every target above, record the implementation paths and authoritative evidence. Verify actual behavior, persistence, packaged assets, gameplay readability and performance where applicable. Distinguish source inventory, unit checks, integrated client tests and real multi-client evidence. Missing evidence means incomplete. A tested foundation feature does not complete a milestone or the whole goal.

## Increment ledger

- Highland travel reliability: replaced random probes with bounded nearest-first sweeps that retain their cursor during ordinary wandering, and retained the exact reachable native path through movement. Fixed stopping short of food, pose changes cancelling journeys, offered meals losing their timer, and crop travel bypassing territorial proximity checks. Finite one-second movement retries stay within existing deadlines; two-second consecutive visible chewing and cover over the current footing prevent remote settlement/feeding. Dedicated native travel suite passes in 1m47s: separate food/cover, a two-block-high wall detour, actual bounding Rimehare travel, client feeding, unsafe player warning, sealed-food refusal and a complete saved-world Galeclaw restart with the exact meal deadline. Full build has 942 passing unit tests; the resources regression passes in 57s, shelter/predator regression in 1m50s, and 110 guide pages validate. Four actual-game captures and the CRC/class/resource-checked JAR are in artifacts/review/living-world-highland-travel. This increment improves existing creatures and adds no new creature/item/ability/fusion/lore count. Natural varied-terrain/multi-seed ecology, remote multiplayer, dedicated servers, full player-cast-to-crop delivery, new item deadline death handling, shaders and sustained performance remain open. Probe limits are code constraints, not a performance benchmark. The four ecosystems and the larger living-world goal remain active.

- Highland resources: three-stage Windreed, native biome-specific patch data, renewable root harvesting/replanting, bounded herbivore grazing, Life growth with building/claim checks and Wind rustling. The Draft Kite provides fuelled finite descent; the Windreed Braid provides finite Downwind observation with a speed tradeoff and sprint/retaliation limits. Authored plant/item/effect art, three physical voices, recipes, recipe discoveries, bestiary hints and player-guide coverage accompany the mechanics. This adds three functional items (one used/replantable resource, one exploration tool, one consumable), not a new creature, lore or ability/fusion count. Verification: 940 passing unit tests, native resource suite with saved-world restart (1m04s), beast regression (1m00s), shelter/predator regression (1m52s), 4,777 reproducible generated paths, 110 validated guide pages and the sound-manifest check. Six actual game captures and the CRC/source-resource-checked JAR are in artifacts/review/living-world-highland-resources. Tests cover actual item packets, native growth/impact handling, permission refusal, grazing on food cells, finite tools, owner synchronization and saved roots/effect/item/shared deadlines. Natural forage travel, complete player-cast-to-crop delivery and native death with the new deadlines remain unverified. Full four-ecosystem completion, natural multi-seed populations/distribution, remote multiplayer, shader review and sustained performance remain open.

- Highland ecology follow-up: Rimehares have a native hungry-Galeclaw avoidance goal. A successful offered/scavenged meal or completed prey kill gives Galeclaws a saved one-minute satiety deadline; only one dropped food is consumed and loading does not renew it. Stonehorns seek cover at night, Galeclaws during the bright middle of the day, both during storms. Separate model poses express folded legs, tucked wings, bowed heads and feeding. A local shelter goal immediately recognizes cover overhead after lighting settles, probes at most sixteen loaded nearby positions every five seconds and bounds travel attempts. Threats interrupt resting, and Peaceful/config suppression is preserved. No terrain destruction, new population growth or static world ledger is introduced. This advances the highland foundation and adds no creature/item/ability/fusion/lore count. Harvestable regional plants, richer resources/relationships, natural multi-seed populations, real multiplayer and sustained performance remain open. Verification: 939 unit tests, native ecology suite (1m54s), existing beast regression (1m9s), 4,749 reproducible generated paths and 110 validated guide pages. Five actual-game captures and the CRC-checked review JAR are in artifacts/review/living-world-highland-ecology. Native evidence covers avoidance movement, one-item scavenging, serialized satiety without renewal, actual minute expiry, resting under existing cover, weather response and damage interruption. Natural shelter travel and a complete satiety server restart remain unverified.

- Aura lifecycle integration: the native effect/art/resonance/fire-inscription/Unity combination verifies one response and exact separate payments/allowances. Actual client-requested death/respawn under both inventory rules preserves Unity rest/used budgets, inscription rest and the same owned inscribed blade with exactly one inventory copy. Closing and reopening the saved world into a new integrated server exposed a Unity active-window restart bug; `SERVER_STOPPING` and `JOIN` now close it while retaining rest and budgets. Native owner synchronization and cooldown refusal pass after respawn and restart. The combined suite passes in 40 seconds; original Unity regression passes in 37 seconds; full build has 936 passing unit tests; 110 guide pages validate. Three actual-game captures and a bytecode-inspected review JAR are in artifacts/review/living-world-aura-integration. This verifies one single-client server restart, not dedicated-server/multi-client/crash recovery or every system's lifecycle. Final Aura integration and all broader content targets remain open.

- Unity: a first playable hybrid ability at Form and five working Circles. The Aura page offers twelve mana/twelve Aura for twelve seconds, resting two minutes from activation. Real Cord/inscription mana returns a quarter as Aura (twelve max), real nonbacklash Aura returns half as mana (twenty-four max), and only delivered gain uses each allowance. Persistent, owner-synced deadlines and both budgets survive serialization together; a disconnect/death or lost prerequisite ends the window while keeping rest, and dimensions retain the same window. Spent/silenced, creative/free, Blood Price, echo, passive, synthetic-event and activation/refund paths cannot farm energy; Aura lessons/spars are excluded. One authored weave insignia, compact control/HUD marker, rear/side strands and three authored voices accompany it. Dedicated runtime passes actual GUI/packet activation, exact activation cost, native spell/Aura/inscription payments, unpaid/health/creative/event refusal, finite lossy counters, save/stream codecs, dimension continuity, backlash/spent refusal with unused budgets, timed expiry and retained rest. The shots use a controlled platform. Full restart, real two-client play, broader matchup balance, reduced settings/shaders and sustained performance remain unverified. Build: 936 passing unit tests; dedicated Unity runtime: 45 seconds; existing rune-etched blade regression: 28 seconds; reproducible generated paths: 4,749; validated guide pages: 110. Five actual-game captures and a tested JAR are in artifacts/review/living-world-unity. This advances one new hybrid ability, not a creature/item/fusion/lore count. Final Aura integration and all broader living-world requirements remain open.

- Rune-etched blades: a first playable one-effect inscription on eligible weapons, through the native anvil at five levels and one rank I rune. Deliberate replacement, foreign-bond and rune-family restrictions, name/wear/component preservation, right-slot sacrifice protection, tooltip and illustrated guide accompany it. Actual art health damage and landed finisher hooks wake basic Touch/effect mana payment with a persistent cross-weapon/dimension/death deadline of at least five seconds or the compiled cooldown. Delayed art hits must still hold the original weapon. Native Effects preserves presentation, shield/target rules and environmental conditions; helpful/movement effects use the caster and world effects use the contact/floor. Unknown IDs remain dormant. Dedicated runtime passes result pickup and exact consumption, real art/effect damage, self healing, unpaid/repeated/swapped activation refusal, native shields, complete stack serialization and ordinary repair preservation. Existing bonded-blade client regression also passes in 3m 23s. Three actual-game controlled-platform/anvil pictures accompany the review. Build: 930 passing unit tests; 4,745 generated paths; 109 guide pages. The full effect roster runtime, full restart, real two-client behavior, shader/reduced settings and sustained performance are not verified. This adds a system, not new item/creature/fusion target counts. Unity, the final Aura integration and all broader living-world requirements remain open.

- Resonant strikes: a first cooperation increment for Aura step 12. Successful spell health damage and a full coated swing or first answering slash/art meet within 24 ticks, in either order, from one player or two allies. Four Aura buys one response; swordsman and foe each rest eighty ticks. Shared-payment target tracking prevents periodic casts from farming it. Ten authored material responses and school incision geometries have twenty names and ten DSP voices. The weaker actual hit contributes at most three health on creatures or 0.75 on players through shields, armor, wards, boss resistance and cast guard. Utilities have explicit player limits. Bounded primitive UUID ledgers, a shared server clock, disconnect cleanup and a reentrancy guard accompany the mechanic. Training dummies now report actual damage before restoring themselves; native Thirst and Life resonance cannot farm dummy/Practice Room healing. Dedicated runtime verifies ten families, both orders, native Frost, ordinary coated attack, carried delayed damage, shield/immune/unknown/unpaid refusal, repeated pulses, allied/hostile permissions, per-striker target-switch rest, discovery, practice and competitive damage ceilings. Practice Room regression verifies native Thirst/dummy damage and twenty-four-target setup/reset/dimension return. Full build: 927 passing unit tests; generated paths: 4,745; guide: 108 pages. Review package: artifacts/review/living-world-resonant-strikes. Twelve actual-game screenshots show controlled platform behavior. Two real clients, full restart, shaders/reduced effect settings and sustained performance remain unverified. Pure control/support spells without health damage do not prime this system. This adds cooperation, not new item/creature/fusion target counts. Rune-etched blades, Unity, the final Aura integration, full roster presentation audit and all broader living-world targets remain open.

- Village tournaments: inhabited bells host the Three Bows on verified empty, flat, dry ground. Three different Aura stewards form a circuit using the existing protected duel mechanics, with explicit hosted radius/time terms and outcome handling. Stand/cloth-standard/book art, four authored ceremonial sound cues, finite saved choice-of-scroll claims, three-page lore, short entry messages and guide integration accompany the encounter. Dedicated runtime tests actual packet interactions, clear-ground/resident rules, stage scaling, nonlethal restoration, all three bouts, selected scroll and one-time saved claims, boundary forfeits, magic disqualification, interference, lore reading and an ordinary-duel lesson regression. Build: 917 unit tests; generated paths: 4,735; guide pages: 107. Review package: artifacts/review/living-world-village-tournaments. The photographs are controlled-platform evidence; natural event frequency, two-client play, full restart, shaders and sustained performance remain unverified. This delivers the scoped village tournament and one lore entry. It does not count reused duelists as new creatures or reusable scroll/Shards as new functional item targets. Next: the remaining Aura step 12 cooperation (resonant strikes, rune-etched blades and Unity), followed by the full visual/ability roster, ecosystems, creature/item/location/lore and player-journey targets. The goal remains active.

- Highland beasts: Stonehorn and Galeclaw have separate authored models, textures, animation, attack sequences and fourteen sound voices. The grazer warns before a locked-direction charge; the ridge runner marks a fixed leap landing, follows recent casting, scavenges dropped meat and hunts Rimehares. Natural habitat registration includes shared snowy slopes/groves, dry/open/high ground and a local two-per-species cap. Physical/Aura hits remain effective; spell damage is posture-dependent and support control works. Peaceful feeding provides two materials with saved cooldowns, used by a defensive poultice with a speed tradeoff and a reusable distraction whistle with commitment/retaliation limits. Dedicated runtime verifies actual elemental/magic/physical/Aura harm, charge damage/evasion, leap evasion, control thaw, client feeding and tools, saved finite shedding, loaded crafting recipes, biome entries/crowd rules, scavenging, recent-cast attraction, prey harm and Peaceful cancellation. Build: 914 unit tests; assets: 4,720 generated paths; guide: 106 pages. Review package: artifacts/review/living-world-aura-beasts. Screenshots show real game behavior on a controlled platform, not natural spawn frequency. Real two-client cooperation, a complete restart, multi-seed natural populations, shader review and sustained performance remain unverified. This advances two of twelve creatures and four functional items (two used materials, one consumable, one tool). The highland relationship is a foundation, not completion of one of the four connected ecosystems. Village tournaments and step 12 remain open, as do the larger creature/item/ability/lore/progression targets.

- Sleeping blade: three small landmark variants with four orientations, an authored stone socket and shaped Oathkeeper sword. A grounded, empty-handed Form swordsman kneels through a six-second draw; movement, damage and invalid intent cancel it. Sites accept one claimant, preserve that UUID, recover orphan poses and never mint another blade. The existing bond registry handles ownership/progression. Oathkeeper trades 20% ordinary-hit momentum for 15% cheaper raised/held guard and 30% more perfect-guard momentum. Four authored sound voices, physical ground cuts and one illustrated three-page lore entry accompany the encounter. Dedicated runtime passes actual interaction packets, cancellation, a server-side contender, finite saved claims/history, actual perfect/held guard behavior and registered normal-terrain placement. Existing bonded-blade regression passes (3m 22s). Build: 909 passing unit tests; assets: 4,679 generated paths verified; guide: 105 pages; sound validation passes. Six actual-game captures and a tested JAR are in artifacts/review/living-world-sleeping-blade. This delivers the separately scoped sleeping-blade landmark, one functional weapon and one lore entry; it does not complete milestone A. Real two-client play, a full restart, natural distribution across seeds, shader compatibility and sustained performance remain unmeasured. Next: Aura-resistant beasts with habitats, then tournaments and remaining step 12 cooperation. The broader creature/ecosystem, item, ability, lore and progression targets remain open.

- Sword tomb: a new encounter location with three burial-detail variants, Flow/Edge gates and the custom Buried Keeper boss. Physical sweep/thrust/guard counterplay, saved encounter/participant claims, two distinct technique scrolls, Aura Shards, one three-page lore book and a bestiary entry. Dedicated runtime passes all four orientations, actual gate/challenge packets, sweep damage, thrust evasion, axe guard break, shared boss classification, health/home and reward-ledger save/load, native book reading and normal-terrain command placement. Full build: 904 passing unit tests. Existing Aura-world regression passes after updating stale immediate-annihilation assertions to verify timed locks and exactly one resolution at the server deadline. Regeneration verifies 4,653 assets; sound checks and all 104 guide pages pass. Eight actual-game captures and the tested JAR are in artifacts/review/living-world-sword-tomb. Natural frequency, real multi-client participation, sustained performance and a full restart playthrough remain unmeasured. This advances one of six added encounter locations and one of the twelve new creatures; ecosystems, the other creatures/items, the sleeping blade, Aura beasts, tournaments and step 12 remain open.
- Existing expedition client regression also passes after the battlefield addition. The physical terrain art writer now uses explicit UTF-8/LF output to prevent Windows regeneration drift.
- Terrain training: pushed at `828c3fc2`, 895 passing unit tests and dedicated runtime/Aura regression suites. Review package: artifacts/review/living-world-training.
- Marchkeeper battlefields: three outdoor landmark layouts, three lore entries (nine pages), three discovered technique intents, custom memorial/book art and four memory sounds. Full build: 899 passing unit tests. Dedicated runtime suite passes all three layouts in four orientations, provenance save/load, actual player interaction/interruption, technique teaching, finite rewards, real book reading and registered structure placement on normal terrain. Assets: 4,621 generated paths verified; guide: 103 pages validated. Review package: artifacts/review/living-world-battlefields. Natural spawn frequency across seeds and a real two-client session remain unmeasured. This does not complete Aura step 11, the six additional dungeon locations, or the larger lore target.

- Mountain observatory: Summit Wind now accepts all three peaks, high snowy slopes/groves and windswept hills/gravelly hills/forests at Y200 or higher; plains towers remain invalid. Attunement refusal identifies missing conditions or the biome/height. The native suite completes the actual crouch ritual at Y254, verifies one blank consumed and reward rest through a full restart. Storm Spire has an octagonal foundation, copper bands, inset windows, switchback stairs, distinct instrument floors and a lightning crown. The native suite verifies chunk-clipped generation in all four rotations, stair orientation/headroom, actual Survival walking with guards paused, exactly three guards, four chests, seals, boss altar, persistent wards, old saved-piece layout and registered placement on normal mountain terrain. Five actual-game captures are in artifacts/review/mountain-observatory. Natural generation frequency, a new boss-fight playthrough and multiple clients remain unmeasured. This improves an existing location; it does not count as another new dungeon or complete the larger goal.

## Wetland lights and fish companions — 2026-10-03

Lantern Newt adds one authored creature with shallow-swamp habitat, finite path searches, actual seagrass travel, nondestructive browsing, aquatic movement, five sound cues, expressive gills/tail and lantern response to Tidebreath/Life. Peaceful gathering gives a used material (Dusk Pearl), one static waterloggable placeable (Marshlight), and one three-page lore entry (Tideward notes). Gathering and response rests persist across a full restart. Native wetland suite passes actual gathering/placement/reading packets, crafting, spawn registration/rules/cap, foraging, spell impact hooks, hurt/dry refusals, empty kill loot and restart. The wetland ecosystem is a foundation; broader relationships and investigation sites remain open.

Both Cinderfoxes and ordinary foxes now tame with five safe fish foods. Vanilla foxes use a saved/synced owner attachment, native trust, owner-only sit/follow commands, bounded navigation repathing and healing. Native fox suite passes all five foods for both species, actual client tame/sit packets, client sync, active following, food consumption, heal, mismatched-owner refusal and full restart. This does not count existing foxes as new-creature targets. Real two-client play, natural distribution across seeds and sustained performance remain unmeasured. Goal remains active.

Final wetland/fox evidence: 945 unit tests, zero failures/errors/skips; both native suites pass; 4,807 generated paths; 111 validated guide pages. Review JAR and five actual-game images: `artifacts/review/living-world-wetland`. Packaged 4,804 asset/data files and 24 changed classes match tested outputs.

## Moonreed gardens — 2026-10-03

Moonreed adds a three-stage perennial wet-bank flower, open-sky/moisture/night growth, actual Glimmerwing approach/contact pollination, nondestructive one-floss harvest and mature-only break loot. Life magic prepares a bud without replacing a moth visit; build/claim permissions apply. Glimmerwings extend into swamp/mangrove biome pools with existing ambient/population/config rules. Their bounded nectar search preserves caster and lamp attraction. A registered feature uses twenty-four real surface-column attempts and accepts only suitable wet banks.

Moonreed Floss and Dusk Pearl craft the reusable Dewglass Lens, which searches loaded visible reeds and reads flower/newt conditions without extracting resources or changing rests. Wear, a five-second player rest and join-restored cooldown persist normally. This advances three functional items (a plant/cutting, one used material and one field tool) and the plant/pollinator relationship; it does not complete the wetland ecosystem or add a new-creature count. Native garden suite passes actual moth flight/pollination, growth, day/distance/roof refusal, harvesting, break loot, Life and Adventure restrictions, native crafting, client lens use/search/newt inspection, full restart and registered wet/dry feature placement. Natural multi-seed distribution, two-client play, shader compatibility and sustained performance remain unmeasured. Broader ecosystem/creature/ability/lore/progression requirements remain open.

Normal-terrain follow-up: WetlandTerrainTest finds an untouched generated bud on a real swamp bank after twenty-eight searched chunks, seed -2329949645506998892. This exposed and fixed generation-time skylight dependence and natural shoreline height handling. Final garden suite passes again with the surface-height/shore fixes and the brass-framed 3D lens. This proves placement on one native seed, not distribution across seeds. The broader goal remains active.

Final Moonreed evidence: 946 unit tests, zero failures/errors/skips; garden native 65s, newt regression 56s, normal terrain 43s. Reproducible generated paths: 4,836. Guide: 112 pages. Review: `artifacts/review/living-world-moonreed`, six actual-game captures and tested JAR; 4,833 resources and 1,605 compiled classes match the packaged output.

## Reed Refuges — 2026-10-03

Craftable waterlogged woven roofs give Lantern Newts an open resting habitat. A bounded nearest-first loaded-cell search uses up to twenty-four block checks and two native path attempts per search slice, with finite journeys/retries. Daylight or rain invites a brief rest; clear nights retain activity. One resting visitor occupies a roof. Six-second maximum rests set an exact one-minute deadline on arrival, independently of pearl/browse/response clocks. Synced client pose blends closing eyes, quieter gills and a curled tail. Feeding, magic, damage and roof removal interrupt it. No healing, resource yield, offspring or ticking block entity is added.

The dedicated native suite passes in 1m32s: actual wet/dry placement and item consumption, loaded crafting, real obstacle navigation under the roof, synced client rest blending, occupancy, natural rest expiry, feeding and ordinary pearl gathering, clear-night refusal/rain-night arrival, native Tidebreath impact, actual journal reading, full saved-world restart, damage wake and exact break loot. Amphibious navigation needed LAND accessibility before water classification; settled aquatic travel also had to ignore residual movement input. Tamsin's three-page Tideward journal and illustrated guide accompany authored block/item art and two quiet sound cues. The Dewglass Lens now supplies its missing particle texture reference.

This advances one functional placeable and one lore text; it adds no new creature/ability/fusion count and does not complete an ecosystem. Natural mixed-habitat shelter selection, real two-client/dedicated-server behavior, shaders and sustained performance remain unverified. Bound search limits describe implementation constraints, not a measured speedup. The larger goal stays active.

Final Reed Refuge evidence: 946 passing unit tests, native shelter 92s and existing newt regression 53s; 4,853 reproducible generated paths, 113 validated guide pages. Review: `artifacts/review/living-world-reed-refuge`, three actual-game captures and tested JAR. All 4,850 packaged asset/data resources and 1,608 compiled main/client classes match tested outputs. The larger goal remains active.

## Reedback Crabs — 2026-10-03

A new territorial wetland creature with a custom layered mudstone shell, six segmented legs, eyestalks, hinged claws and moving reed crown. Unsafe approaches provoke a two-second fixed-direction warning, one physical forward sweep and three-second recovery. Sneaking outside close personal space avoids initial acquisition; pursuit stays near home. Tidebreath calms it for ten seconds, wind interrupts into recovery, and a separate ten-second response deadline prevents immediate renewal. Ordinary attacks break calm. Saved home/calm/response data restores without a stale sweep. Peaceful suppresses aggression/spawning; wildlife master/multiplier and individual spawn setting apply. Wet open shallow banks, one-animal groups and two nearby crabs limit natural spawning. Native death gives one to three useful vanilla clay balls; no unique item count is claimed.

Seven authored physical sound cues, exact UV material art, custom spawn egg, existing bestiary integration and an illustrated guide/GitBook chapter accompany the code. Revised the new percussion cues after the sound checker found poor small-speaker energy. Runtime caught that Tidebreath belongs to Frost in the existing rune taxonomy; the calming response now selects its explicit identity rather than a nonexistent water element. Wind and cold remain distinct.

Native suite passes in 50 seconds: actual warning, sidestep evasion, ordinary sweep harm, native Tidebreath/Windcut impacts, finite response/rest and natural expiry, actual client sneak, Peaceful cancellation, biome pool entries, shallow/dry/deep predicates, local cap, native death loot and complete saved-world restart. Spell checks use constructed hits through native impact handling, not a complete paid player cast. Pictures show a controlled bank fixture, not naturally spawned populations. This advances one of twelve new creatures. It does not complete the ecosystem, add a new functional item, or complete a lore/investigation target. Natural frequency/distribution, mixed-species bank coexistence, shields/full control roster, real multiplayer/dedicated servers, shaders and sustained profiling remain unverified.

Final evidence: 946 passing unit tests, 4,865 reproducible generated paths, sound-manifest check and 114 validated guide pages. Review: `artifacts/review/living-world-reedback`, three actual-game screenshots and tested JAR. All 4,862 packaged asset/data resources and 1,612 compiled main/client classes match build outputs. Larger living-world requirements stay active.

## Spell presentation inventory and caster anchors — 2026-10-03

Runtime command inventories 350 built-in runes (258 effects, 39 shapes, 36 modifiers, 17 links). This supersedes the initial source-declaration count for runtime inventory. All effects register signatures, but outside-dispatched visuals/sounds and actual per-effect choreography remain unverified. See docs/audit/full-spell-roster. Textual references are candidates, not confirmed ownership. Dynamic Knots/Woven runes, Aura, ranks, links, fusion, quality, shaders and multiplayer remain separate review scopes.

Self, Domain, Orbit and Trail elemental formation layers use the caster anchor; Orbit satellites and Time/Void material positions now agree. Native FormationAnchorTest passed 18 packet cases at three pitches with ten material layers, including Bolt/Beam front controls and rear-circle assertions. Full build: 946 unit tests, no failures/errors/skips. Diagnostic rear-camera screenshots and verified review JAR are in artifacts/review/spell-presentation-anchors. These are not complete paid casts, full-roster presentation evidence, first-person beauty captures or measured performance. No creature/item/ability/fusion/lore count added; full objective remains active.

## Authored fire formations — 2026-10-03

All 28 runtime fire effects now have explicit client preparation recipes, plus Bloodboil's heat recipe (runtime blood element). Firestorm braids wind/flame, Steam combines water/vapor, and other effects use distinct materials/geometry and evolving beats. Existing per-effect impact/voice dispatch is preserved. Native suite passed in 64 seconds: 28 fire recipes in Full/Minimal modes, finite/nonduplicate/evolving emission plans for 29 IDs, exact namespace refusal, visible framebuffer differences against an empty stage and one paid Survival Ember cast. Thirty actual captures include the empty reference, 28 fire preparations and paid Ember. Early captures inspected pending particles and were blank; final captures wait for rendered particles and fail on empty frames. Existing quality/event/particle bounds remain; this is not a measured performance claim. No creature/item/ability/fusion/lore target count added. All-delivery, linked origins, target-aware ground/rain/summon staging, launch/travel/sound, dynamic spells, Aura, settings/shaders/multiplayer and full goal remain open.

## Initial group delivery staging — 2026-10-03

Ordinary Cord/scroll preparations now emit one event per compiled initial group, separating group runes/materials from the complete rear glyph. Exactly one group carries that glyph. Delay/conditional branches are absent from initial assembly; empty roots are glyph-only. Caster modes cover Self/Domain/Orbit/Trail/Burst/Nova/Ring/Wave; aimed terrain modes cover Zone/Rain/Wall/Pillar/Mine/Totem/Vortex. Mastery-adjusted range reaches the client; two bounded local clips track current aim using the delivery resolver's collider/fluid/ground rules. Rain forms twelve blocks overhead and gives a ground marker. Native group suite passes in 39 seconds for ten shape placements, production decoded metadata, comparison against actual server delivery aim, delayed exclusion, glyph-only roots and a paid mixed Bolt/Self cast. Four controlled game screenshots are in artifacts/review/group-formations. No mechanics, payments or release timing changed. Client/server require this same formation wire format. Linked continuation/secret-specific staging, all remaining shapes/turning/permissions/terrain edge cases, remote multiplayer, shaders and profiling remain open. No content target counts added; full objective remains active.

## Linked formation origins — 2026-10-03

Admitted linked segments now emit their isolated preparation, without another rear glyph. Non-caster triggers preserve a fixed position/direction, ground modes use the native server resolver and Rain prepares twelve blocks overhead; Self/Orbit/Trail remain caster-attached. Effects precede modifier details in the compact visual list. No gameplay delay, payment or damage changes were introduced. The protocol adds validated vectors and requires this same build on client/server.

Native suite passes in 53s: retained initial-group fixtures, a paid delayed Fireward cast, a real paid projectile impact starting Burst at a foe, nine constructed trigger-origin fixtures and allowed/refused sneaking gates. Fire regression passes in 68s. Full build: 946 unit tests, zero failures/errors/skips; 114 guide pages validate/export. Review: artifacts/review/linked-formations, three new actual-game images, logs and tested JAR. All 4,862 resources and 1,614 compiled classes byte-match; JAR CRC clean. SHA-256: 14b76019291a245b3f75477d6808ef6b36c40fd0b11d12cbfc1c13cc542bc0db.

This advances linked formation staging, not complete pre-release/launch/travel choreography. All link variants, stored/reflected initial casts, secret-specific staging, native scroll use, remaining shape/terrain/turning/permission cases, shaders, real multiplayer and profiling remain open. No creature/item/ability/fusion/lore target counts are added; the full living-world goal stays active.

## Authored frost and water preparations — 2026-10-03

Thirty runtime frost-family effects now have individually authored two-beat client preparations. Water abilities use membranes, lung lobes, hooks, flow lanes and water courses; cold abilities grow branches, plates, needles, mirrors, columns, petals, cages and teeth. Mixed recipes include storm-charged hail, stone-supported glacier, wind/snow, life/ice petals, void/black ice, arcane floor lattice, time/ice capsule and blood/frost teeth. Existing cues, mechanics and impact/aftermath remain unchanged. The bounded canvas retains group-aware placement and the rear glyph; no measured performance claim.

Native frost suite passes in 68s: all thirty through encoded packets in Full/Minimal, finite/nonduplicate/evolving recipes, nine mixed-material retention checks, actual particle/rear-circle positions, aim-area framebuffer changes and two paid Survival casts (Bolt/Frost and Self/Tidebreath, including Water Breathing after release). Initial/linked regression passes in 44s. All thirty Full preparation captures were inspected; packet fixtures use Bolt as a controlled carrier, not thirty full effect executions. Thirty-three images, searchable gallery, logs and tested JAR: artifacts/review/frost-formations. Full build: 946 unit tests, zero failures/errors/skips; 114 guide pages validate/export. JAR CRC clean; 4,862 resources and 1,616 compiled classes byte-match. SHA-256: 9aa314d589af5ecb710bdfd25d00c3e130511c9eadcc9c0d45444a2aeee3d6d7.

This improves existing abilities, with no new ability/fusion/item/creature/lore count. Full launch/travel/impact choreography, all delivery/linked pairings, third-person roster, dynamic/add-on spells, quality combinations, shaders, real multiplayer and profiling remain open. The full objective remains active.

## Authored storm preparations — 2026-10-03

Twenty runtime storm effects now have explicit two-beat recipes: forks, clamps, leaders, magnetic filings, sunlight, charged birds, copper terminals, living stems, tears, thread webs, timed marks, cloud/rain and footings. Fused ingredients retain their material identities. Native suite passes in 57s with all twenty encoded Full/Minimal fixtures, finite/nonduplicate/evolving plans, ten ingredient checks, actual rear glyph/front material positions, visible framebuffer changes and paid Bolt/Shock plus Self/Surge (Speed/Strength after release). Initial/linked regression passes in 44s. All twenty Full preparations and both paid captures inspected.

Full build: 946 tests, zero failures/errors/skips; 114 guide pages validate/export. Review: artifacts/review/storm-formations, 23 actual screenshots and tested JAR. JAR CRC clean; 4,862 resources and 1,618 classes byte-match. SHA-256: 6b855fbc8228abe8996fcd6f23f9372553cd92e88652921c5cf331ec503dfcb0.

Preparation phase only; existing sounds, gameplay and impact/aftermath preserved. Complete launch/travel/impact choreography, remaining element families, dynamic/add-on spells, all delivery/link combinations, shaders, real multiplayer and profiling remain open. No new creature/item/ability/fusion/lore count. Full goal remains active.

## Authored fire projectile bodies — 2026-10-03

Eight effects now carry explicit authored bodies during actual projectile movement: Ember, Fire, Firestorm, Steam, Meteor, Soulfire, Starfire and Phoenix Pyre. Server entity data synchronizes up to eight distinct bounded built-in IDs; fully covered groups replace the generic comet, while mixed unsupported groups retain fallback. Existing server mechanics, voices/motes, impacts and reflection remain in charge. Same client/server build required.

Native flight suite passed in 46s for real paid Survival casts of all eight in Full/Minimal, exact client IDs, actual motion/material particles, finite/distinct recipes and framebuffer visibility. Seventeen captures include background, eight early and eight later flight frames; all sixteen inspected. Native parry regression passed in 52s. Full build: 946 tests, zero failures/errors/skips; 114 guide pages validate/export. Review: artifacts/review/fire-flights. JAR CRC clean, 4,862 resources/1,620 classes byte-match; SHA-256: c9d5b9c91ac84e972de35d62dbd43c1f2afe32260f21299d2814f56f64ef9356.

This is flight progress, not complete launch/impact/aftermath choreography. Remaining effects/families, Arc native coverage, all modifier/rank/delivery/link pairings, mixed fallback groups, reflected visual identity, close side/third-person review, shaders, real multiplayer and profiling remain open. No new content target counts. Full objective stays active.

## Complete fire projectile body roster — 2026-10-03

Expanded authored flight from eight to all 28 runtime fire effects with explicit bodies for heat shutters, charges, fuses, wood splinters, solar lenses, stamps, ash curtains, hearts, bevels, shields, grates, jaws, time forks, heated water, wind and stone mortar. Authored emission follows live entities independently of comet lifetime; removed IDs retire independently. Omitted/foreign/oversized group metadata retains fallback. Pierce narrows and elongates bodies; Minimal Starfire retains arcane material. A shared coverage contract removes duplicate server travel particles only for fully authored fire groups, while preserving travel voices. Unsupported mixed groups keep old travel hooks. No gameplay payment, speed, collision, reflection or impact changes.

Native suite passed in 105 seconds: 56 paid Survival Bolt casts across Full/Minimal, runtime roster equality, exact client metadata, real motion, nearby authored five-tick material, bounded/distinct recipes, particle-clear recovery and removed-ID cleanup. Kindling verifies foreign-innate refusal before assigning its correct innate owner. Removing the old travel layer exposed a weak particle assertion; the final check identifies the authored lifetime and allows two emission ticks of network movement plus the body offset. Pure coverage checks refuse empty, foreign, unsupported, omission-marked and trailing-empty groups. The 57 original captures include empty reference and 28 early/later pairs; all 28 later frames inspected through unscaled aim-region contact sheets. Bodies are compact at this distance. Existing preparation/cue particles remain visible near the caster. Native parry regression passed in 52 seconds.

Review: artifacts/review/fire-flights-complete. This adds no new ability/fusion/item/creature/lore count. Arc route, every modifier/rank, native mixed/omitted group casts, linked/reflected choreography, close side/third-person beauty review, release/impact completion, shaders, remote multiplayer and sustained profiling remain open. The full living-world goal remains active.

Final fire-flight evidence: 946 unit tests, zero failures/errors/skips; final build 13s; 114 validated/exported guide pages; 57 original captures; all 4,862 packaged asset/data resources and 1,621 classes match tested build outputs. SHA-256: 4e12f77b87b810d561e946d44560dbdf57f4ca18d96db03978292153d9ad904e.

## Frost and water projectile bodies — 2026-10-03

All thirty runtime frost effects now have explicit authored moving recipes, with ice needles/plates/teeth/panes/cold fronts and water membranes/lungs/channels/hooks/wave lips/sediment. Hail retains charge, Glacier stone, Blizzard wind, Black Ice void, Frostbloom life, Frostbite blood, Mirrorfrost/Rime Seal arcane, Cryostasis life/time, Rime Causeway wind and Undertow sediment. Supporting materials remain in Full and Minimal. Shared coverage replaces the generic comet and duplicate server travel particles only for covered groups; voices and unsupported fallback remain. Exact fire dispatch remains isolated after adding frost. Existing costs, speed, collision, reflection and gameplay impacts are preserved.

Native frost roster suite passed in 119s: sixty paid Survival Bolt casts, runtime roster equality, exact client identity, actual movement, nearby authored five-tick materials, bounded/distinct recipes, supporting ingredients, particle-clear recovery, removed IDs and framebuffer evidence. Six paid variant cases passed in 38s: Arc/Icicle, Arc/Hail, Pierce/Frost, Frugal/Tidehook, covered Ember/Frost and unsupported Frost/Shock. Actual falling versus straight motion, style bits, metadata and fallback state verified; both authored schools are present in the covered mixed group. The full fire-flight regression passed in 108s. These example variants do not establish every delivery/modifier/fusion combination.

Review: artifacts/review/frost-flights. Seventy-nine original captures include the main reference, thirty early/later pairs, and six matched variant references with early/later pairs. All thirty roster later frames and twelve variant spell frames inspected through unscaled region contact sheets. Early variants include substantial preparation; later frames separate more of the flight. Distant bodies are compact. Frost/Shock intentionally retains prominent fallback until storm flight is authored. Full release/impact, every shape/rank/modifier, homing/reflected/linked choreography, dynamic/addon runes, close side/third-person beauty review, natural combat, shaders, remote multiplayer and sustained profiling remain open. No new ability/fusion/item/creature/lore count; full living-world goal remains active.

Final evidence: 946 unit tests with zero failures/errors/skips; full build 16s; 114 validated/exported guide pages; all 4,862 packaged asset/data resources and 1,623 compiled main/client classes match build outputs. SHA-256: facef361046e0ab2c9f786da5cb3950b63995e3dd0e10dff1e7f151412c2f173.

## Storm projectile bodies — 2026-10-03

Twenty runtime storm effects now have explicit age-driven flight recipes: alternating contacts,
interruption gaps, advancing leaders, expanding pressure, pulsing lobes, sequential copper
terminals, circulating wind, advancing heat, inward filings, opening void, out-of-phase threads,
time hands, falling rain, running crystalline current, arriving steps and charged water/footings.
Shared emitters/material primitives remain; this is not complete bespoke release/impact choreography.
Supporting materials persist in Full/Minimal; Ripple carries warm sunlight without STORM fragments.
Fully covered fire/frost/storm groups replace generic comet and duplicate server travel particles,
keeping voices. Frost/Shock now carries both authored schools; unsupported Windcut groups keep fallback.

Native full roster passed in 82s (forty paid casts), six paid variants in 32s, full frost regression
in 108s and adjusted frost variants in 32s. New evolving-motion checks sample three ages in both
quality modes; they caught Shock's contact-cycle aliasing before its cadence was fixed and rerun.
59 original screenshots; all twenty main later and twelve variant spell frames inspected. Bodies
are distant/compact and preparations can remain near the caster. Full build 13s: 946
unit tests, zero failures/errors/skips; 114 guide pages; 74 candidate source files/350 indexed runes.
Review: artifacts/review/storm-flights. All 4862 resources/1625 classes byte-match
packaged tested outputs, CRC clean. SHA-256: 0c023c3ca96572ba9bc5e9ed5d3318ea0ab8d5722ba8320c29723db78ccda7ad.

Full buildup/release/impact/aftermath, remaining elemental families, all delivery/modifier/rank/fusion
combinations, dynamic/addon runes, reflected/homing/linked visuals, close third-person beauty,
shaders, real multiplayer, natural combat and sustained profiling remain open. Adds no new content
target count. Goal remains active; user requires custom elemental behavior beyond recolored shapes.


## Wind carries pressure and flow — 2026-10-03

Twenty-five runtime wind effects now have individually authored preparation and moving-body
recipes using a dedicated AirflowParticle renderer and an original striated filament texture.
Quadratic current bands use world lighting, transparency, moving crests and taper instead of
LightOption/LightParticle rays, arcs or rings. Each recipe specifies its current direction,
pressure, timing and supporting material. Dust Devil/Downdraft carry stone, Razorgale blood,
Recoil time, Skyglyph arcane, Zephyr/Prune leaves and Summit Wind vapor. Preparations gather the
same current identity that flight carries. Fully wind-authored projectile preparations suppress
old luminous shape bodies and duplicate covered ingredient layers; other delivery markers stay.
Rear glyph/caster-centered placement remains. Minimal explicitly selects six curve segments
instead of sixteen; reduced flash lowers alpha. A camera-aligned vertical fallback-axis bug was
fixed and checked for finite orientations. Matching client/server builds are required.

Native final preparation suite passed in 68s: all 25 encoded Full/Minimal fixtures, rear/front
placement, no LightParticle in wind-only Bolt preparations, bounded/distinct/evolving recipes,
ingredients and wire roundtrip, plus paid Bolt/Windcut and Self/Swift with its Speed effect.
Flight suite passed in 109s: fifty paid Survival Bolt casts, exact synchronized identities,
actual movement, production airflow particles, clear/recovery and removed-ID cleanup. Six paid
Arc/Pierce/Frugal/covered Windcut-Shock/unsupported Windcut-Umbra variants passed in 43s.
The shared initial/linked staging regression passed in 52s. An initial Arc/Feather Fall later
frame had only eight high-contrast pixels at its distant position; variant later captures now
use the same two-tick interval as the main suite, preserving movement/particle checks.

Review: artifacts/review/wind-choreography. 147 original native PNGs: 28 preparation,
51 ordinary flight, 50 isolated close flight/matched empty references and 18 variants. All
25 preparations, 25 main later frames, 25 close bodies and twelve variant spell frames inspected
through unscaled region sheets. Close captures use a diagnostic camera, clear unrelated particles,
and re-admit the actual paid projectile's production particles for matched framebuffer evidence;
they are not the complete unedited cast. Airflow remains faint/compact in some flight frames.
Player guide validates/exports 114 pages; textual source index covers 350 runes/75 candidate files.

This changes existing spells and adds no new content target counts. Full release/impact/aftermath,
remaining elemental families, every delivery/modifier/rank/fusion, reflected/homing/linked visuals,
dark-location readability, natural combat, shaders, remote multiplayer and measured performance
remain open. Shared curve emission is a technical utility, with explicit per-spell behavior;
full goal remains active and the user's requirement for custom element effects remains in force.

Final wind evidence: frost variants 40s, storm variants 34s; full build 17s, 950 tests with zero
failures/errors/skips; CRC clean, 4864 resources/1630 classes byte-match. SHA-256:
8d691e90316019f8dd0feb96578ec2dc70dc7d8ac6c65b85fa0438b716c72c27.


## Single-payment spell damage balance — 2026-10-03

User reported >1,000 damage below 200 mana and >5,000 over time. A real paid Survival
Touch/nine-Vows/Harm cast depleted all 1,000 dummy health; nine-mana list price, 4,659.2 base
attempted damage. The same stack on Bleed requests 6,656 from its cut and eight ticks, before
bonuses (code calculation). Finite cooldown/radius penalties did not bound growing power.

Compiler and numeric rules now limit Vow/Execute/Trial Key to one per target rune, Focus to two
and Extend to three, including Knot expansion; extra copies warn and add no cost. One Paid object
owns a per-target raw allowance min(512,12+2*effective mana price), max 96 per admitted hit.
Children, pulses, repeats, copies, circle views, linked hits and reflections share it. Separate
payments/targets remain separate. Production Venom stops after exhaustion; nonfinite damage is
rejected and the per-cast UUID map is capped without eviction. Existing armor/warding/spellguard,
healing/control and monster-only difficulty rules remain. This reduces extreme damage builds.

Native fix suite passed 39s: actual Harm 18.2, actual Bleed 26 total, production Venom stress 212
at a 100-mana price and no later refill, all copy/continuation types sharing that total, new-payment
recovery. Parry/wild magic passed 53s; defenses passed 43s. Defense's old two-Execute fixture was
updated to one Execute plus performance, still proving the same 2.5 bonus cap (26.25 actual).
Full build 15s: 963 tests, zero failures/errors/skips. 114 guide pages validate/export.
Review: artifacts/review/spell-damage-balance, four original inspected screenshots, native logs
and combined wind/balance JAR. CRC clean, 4864 resources/1632 classes byte-match;
SHA-256: a6155f8b12c975637ac2641e9248adf6e785a4efc11f7823bf906155794e9096.

Audit: docs/audit/spell-damage-balance.md. Direct add-on vanilla damage, independent companion
bites, environmental hazards and Stasis pooling of multiple paid casts are separate paths.
Natural multiplayer and boss pacing still need review. No new content target count; goal active.


## 0.9.1 release preparation and administrator fusions — 2026-10-03

The operator learnall command already taught registered fused rune results, but their recipe
keys remained hidden in the Grimoire. It now adds all 55 elemental and 22 signature recipe keys
in one synced attachment update without discovery rewards or changing innate ownership. Native
command/repeat/client-sync/full-saved-world restart coverage passed in 47 seconds. Saved spells,
names, missing-addon knowledge and unrelated discoveries are retained. Exact two-to-eight-effect
weaves remain dynamic altar creations rather than an enumerated named roster.

Version 0.9.1-alpha includes all completed changes since 0.9.0. Full build passed in 18 seconds:
963 tests, zero failures/errors/skips; 114 validated/exported guide pages. Release JAR CRC clean,
4864 packaged resources/1632 classes byte-match tested outputs. SHA-256:
f61ae8d811557680e619c654eacb7827f26624b806ba2d134da343a65eca78fe.
Three pinned launcher profiles accompany the mod/sources. Review: artifacts/review/release-0.9.1.
Public upload is explicitly authorized this turn; publishing/deployment outcomes are recorded
separately after verification. The full living-world goal remains active; no new content count.


Release correction: generated-assets CI exposed five stale modifier descriptions. Regeneration
and the exact reproducibility check now pass across 4867 paths. The corrected distinct release
is 0.9.1-alpha.1; full build 15s, 963 passing tests; resources/classes still byte-match.
SHA-256: cdd7741d6a48f4a12cd2d1f04e9b61e7b202ed68f024b42fb297020d1283fd1a.
The first upload remains historical; server staging will use only the corrected checksum.


Publication audit: docs/audit/release-0.9.1-alpha.1.md. Corrected GitHub release has mod, sources
and three pinned profiles; CurseForge accepted file 9050277 (processing when inspected).
Updated wiki/GitBook deployment and live page version checks pass. VPS checksum matches;
WildercordUpdate091 is queued and confirmed waiting for four online players to leave. Server
activation is not complete. CI asset/unit Build job passes; separate full native CI still running.
The full goal stays active.

Final CurseForge inspection: file 9050277 is Approved. Saved description survives reload with corrected alpha.1 links; screenshot in artifacts/review/release-0.9.1/curseforge-files.png. Server remains queued for four players; activation still outstanding.


## Basinfill and Aura progression guidance - 2026-10-03

Basinfill adds one rank-I utility ability with a craftable rune, authored pouring/vessel
preparation, cupped projectile, original liquid cue and permanent bounded shallow source-water
placement. A complete vessel is validated before edits; protection, evaporation, size/depth,
occupied cells and shared-payment limits apply. Native paid Touch/crafting/bucket/protection/
Nether/full restart suite passed 35 seconds. Audit: docs/audit/basinfill.md.

Aura's reported apparent soft lock was not reproduced as a universal code lock. A concrete
progression-guidance gap and missing master-trial label were fixed. Contextual XP hints,
practice-cap notice, a detailed XP tooltip and verified rank/trial instructions accompany it.
Native progression passed 83 seconds, final 64 seconds: real recovered Survival attack after
practice 40 cap, actual 30 second waterfallFlow, threshold/wrong-trial refusal, full restart,
later-stage API gates and all trial labels. Later boss/duelist wins were not simulated.
Audit: docs/audit/aura-progression.md.

Combined build 16 seconds, 963 passing unit tests; 4874 reproducible paths; 115 guide pages.
Review JAR CRC clean, 4872 resources/1633 classes byte-match; SHA256
f6450e2abffc4c7b11e6959cc50d22b9c54bfca1174afea321a319679eee015d.
Review: artifacts/review/basinfill and artifacts/review/aura-progression.

Remaining-work audit: docs/audit/living-world-remaining.md. Conservative counts: 5/12 creatures,
14/36 functional items (Tideward lore book separated), 2/12 abilities (Unity/Basinfill), 0/18 added
signatures, 8/24 lore texts, 0/4 investigations; highland/wetland foundations remain incomplete.
Sword tomb counts 1/6 locations, or 2/6 if the hosted tournament is credited as an encounter.
Baseline signatures/technique definitions were verified unchanged, so teaching them is not
credited as new ability content. Native tests are controlled single-client evidence, not
remote multiplayer, natural populations, full presentation acceptance or performance proof.
Full living-world goal stays active. Public alpha.1 remains the prior release; this JAR is a
local development review build. Queued VPS activation still awaits an empty server.

## Physical wetland fieldcraft, fungal wildlife and Earth materials — 2026-10-03

Parallel implementation produced three integrated increments: Reed Rattle, Sporeback Snail
and the complete 32-effect Earth preparation/projectile-body roster. These are scoped
additions; the full ability lifecycle and connected ecosystems remain open.

The rattle answers warning claws with six seconds of physical calm, forty-eight uses,
saved player rest and the crab's existing shared response rest. Committed sweeps remain
dangerous. Final native suite passed 58 seconds, including actual interaction, recipe,
refusals, wear, saved-world restart and final-use break. Visual review corrected the test
pond, messages, beveled chamber and held scale. Audit: docs/audit/reed-rattle.md.

Sporeback Snails browse actual mushrooms nondestructively, prepare one saved dew reserve,
respond to Life/Fire and seek genuinely dark covered footing. Bounded searches, local
population limits and saved independent gathering/foraging/response rests prevent resource
renewal by repeated magic. Mycelial Dew, Fungal Poultice and The Patient Spiral accompany
the original model, textures, animation and five physical sound cues. Initial corrected
native suite passed 74 seconds; final admission/camera checks are recorded in
docs/audit/sporeback-snails.md. This begins the fungal foundation, not a finished ecosystem.

Earth's original world-lit materials distinguish stone, slabs, sediment, dust, faults,
roots, bone and geodes, with rune-specific gathering and travel. Supporting fused elements
are retained in Full/Minimal. Native formation 77 seconds, paid flight 115 seconds and
variants 31 seconds passed, with 182 original images retained and representative frames
inspected. Arc, Pierce, Frugal, covered/uncovered mixed groups, innate ownership, world
light, reduced flash and vertical/stationary directions are covered. Self Stoneskin keeps
its caster placement. Audit: docs/audit/full-spell-roster/earth-choreography.md. These are
preparation/body checks; full impacts, sounds, delivery roster, shaders, real remote
multiplayer and sustained frame-time evidence remain separate.

Conservative expansion counts now: 6/12 creatures, 17/36 functional items, 2/12 abilities,
0/18 additional signatures, 9/24 lore texts, 0/4 investigations. The snail adds one wildlife,
dew/poultice add two functional items, rattle adds one tool, and the three-page journal
adds one lore text. Spawn eggs are not credits. Three ecosystem foundations remain
incomplete. Next Life 28, six new signatures and the Glowcap nursery investigation are
isolated drafts outside the build; no completion credit is claimed for them.

Final Sporeback native passed 64 seconds with clearer whole-creature captures and authoritative
spectator/distant gather refusals. Final combined full build passed 22 seconds: 964 unit tests, zero
failures/errors/skips; 4922 reproducible generated paths; 117 validated/exported guide pages,
including original native rattle/snail illustrations. Review JAR CRC clean, 4921 processed
resources and 1648 compiled main/client classes byte-match. SHA256:
de75e599f33cf9251cdae10f402d4d4cf648c151e513d49ae3cd3fec84eee09c.
Review: artifacts/review/parallel-living-world/wildercord-earth-fieldcraft-review+mc26.3.jar.
The new reusable tools/verify_review_jar.py checks an explicitly named JAR against build
outputs and unit reports, rejects extra/missing mod classes and duplicate/corrupt entries.
This is development content after the public alpha.1 release; no new public upload is claimed.


## Active integration and server activation — 2026-10-03

The published 0.9.1-alpha.1 release is now installed on the VPS after verified empty-server checks, a clean save/stop and an offline backup. Its installed SHA256 matches the GitHub release. Loader startup, RCON and a connected player were confirmed. See `docs/audit/server-update-0911.md`; this deployment does not include later goal content.

Life preparation and moving-body recipes, six new field signatures and the Glowcap Nursery investigation have moved from isolated drafts into development sources. Initial six-signature gameplay and Full/Minimal presentation native passes exist; expanded refusals, final visual captures, Life 29 and complete fungal chain/natural-generation acceptance are still being checked. No conditional creature/item/lore/investigation targets are promoted by this status entry.

The sustained mixed-material benchmark passed: 24 moving dummies and 64 admitted Pelt/Venom/Ember/Windcut Bolts per profile, with about 30 seconds of frame samples. Complete visual/FPS/VSync/HUD preferences are restored after benchmarking. See `docs/audit/living-world-performance.md` for exact metrics and limits. The 120FPS cap prevents claiming a relative profile speedup; natural ecosystem and remote multiplayer profiling remain open.

Generated-assets reproducibility passed across 5039 paths after integration and correcting the six carved rune images to their actual item-model texture paths. New content is still undergoing focused gameplay/visual acceptance before a combined review JAR and pushed milestone.

## Life, signatures and fungal acceptance progress — 2026-10-03

Life 29 recipe, preparation and paid Full/Minimal moving-body suites passed 19s, 64s and 112s.
Six field signatures passed the final gameplay/altar/unit run 51s and final presentation 52s;
the existing real client Fuse packets and exact paid outcomes remain covered. Collect ownership
passed its focused 29s native run. Clean elemental preparation passed 41s, retaining rear glyph
strokes and Jolt's authored arc while excluding generic orb/ray from the front body. The mixed
Ember/Umbra fallback remains verified. See the individual audit documents for exact scope.

The illustrated Living Materials chapter uses original Life frames; 120 player-guide pages
validate and export. A review caught and corrected the chapter's ingredient description:
Second Wind is Life+Time, not Life+Wind. Family recipe counts can overlap and are not a global
count of completed spell lifecycles. Complete impacts, voices and other deliveries remain open.

The final grounded Nursery earned-resource chain passed in 2m28s with the same visitor's two
real harvest cycles, actual Grow-first preparation, native recipes/placement, claimed
investigation, finite filter use and saved-world restart. Natural terrain passed in 42s on the
retained failed seed: six caps and both authentic clue kinds in the same 81 chunks, with actual
moisture, cover and cave-biome predicates unchanged. Distinct current-chunk column sampling
corrected sparse decoration. Late-search, occupancy and blocked-path checks passed in 1m10s.
This credits four functional items, three lore texts and one investigation, bringing counts
to 21 functional items, 12 lore texts and one investigation. Complete fungal ecosystem and
clearer player-review presentation remain separate acceptance work.

The earlier full GitHub native job ended at 90 minutes after both graphics backends failed before
tests began. CI-only commit 65256266 is pushed to main: documented SDL EGL selection plus a
streaming guard for dual-backend startup failure. Its Linux job 37155328593 initialized Mesa
OpenGL and reached gameplay, then failed the Runic Hearth lantern test's short timing window.
The corrected focused native test passed in 1m08s; the full Linux descriptor still needs a
subsequent run. See docs/audit/ci-graphics-startup.md. This push contains no
new Life/signature/Nursery public release.

Three agents continue in parallel: physical fungal fieldcraft/generation, independent cave
generation analysis plus isolated Life outcome/voice drafts, and the remaining twelve signature
drafts. Void 36, Life 29 outcomes/58 voices, Rootmolt and twelve signatures remain outside the
build until promoted and verified. Draft files and offline audio checks are not gameplay credits.

### Combined milestone delivery

The accepted Life/field-signature/Nursery batch now has a checked review JAR:
`artifacts/review/life-fieldcraft-milestone/wildercord-life-fieldcraft-review+mc26.3.jar`.
Final build passed with 968 unit tests, zero failures/errors/skips; 5,039 generated
paths are reproducible and the 120-page guide validates/exports. All 5,038 processed
resources and 1,674 main/client classes match the package. Clear fungal native
captures and complete affected Fire/Frost/Storm preparation reruns are retained.
See `docs/audit/life-fieldcraft-milestone.md` for the checksum and scoped acceptance.
The full Linux native descriptor and broader goal remain open. Next parallel
work addresses Life owner outcome edges, the next twelve signatures' exploit
paths, and Rootmolt creature/tether behavior in isolated drafts.

## Void development increment (2026-10-03)

Seven-family moving-body contract now covers 206 distinct identities including
Void 36. Six Void focused native suites passed, alongside five affected older
family alternate-flight regression suites. Selected real Self and movement
outcomes pass; lifecycle impact/field/summon/beam/rain/sound and real multiplayer
acceptance remain open. The nursery CI regression fixture passed its complete
focused earned-resource/restart chain; the full Linux descriptor still requires
a renewed run. Draft Rootmolt, twelve further signatures and Life outcome work
remain outside the playable source and receive no content credit.

## Expedition signatures development increment (2026-10-03)

Twelve expedition/counter/support recipes extend the previous six accepted field signatures,
bringing the goal to 18 newly authored signatures, 40 total signatures, 95 named fused recipes
and 369 runtime runes. The numeric signature target is met; it does not complete supported
pairing/delivery lifecycle, real multiplayer or the separate twelve-new-ability target.
No creature/item/lore/investigation addition is credited by this increment: 6 creatures, 21
functional items, 2 abilities, 12 lore texts and 1 investigation remain the accepted totals.
Life 105 and Rootmolt/Drainhouse content remains isolated and uncredited.

Focused Counter 42s, Support 42s, Trail 36s and Presentation 62s pass. All 12 actual client
Fuse packets and paid Full/Minimal preparation/flight checks are exercised; 38 original
native presentation PNGs are preserved. Generator 5158 paths reproduces and 978 unit tests
pass. Final bounded Counter 45s, zero-actual-heal 43s and Void 38 Full/Minimal 2m13s
also pass; 153 affected Void originals are retained. Frost 34 paid Full/Minimal 1m59s
and Life 30 paid Full/Minimal 1m48s pass; 69 Frost originals are retained. Finite budgets are
not frame-time performance measurements. Life 30 retains 121 originals, giving 381
actual native PNGs across four groups. Final build 22s passes 978 units without
failure/error/skip; 5158 generated paths reproduce; 122 guide pages and sounds validate.
Review JAR has 5157 matching resources/1711 compiled classes and verified ZIP integrity.
SHA256:1b4709a3f1a625d9f8fc3432b1927d7cd1abc75517490f3362e72d3b26d6632c.
See docs/audit/expedition-signatures-milestone.md for the exact development artifact.

The preceding fb2c07cb ordinary CI build passed, but its full Linux native descriptor
failed later at gravity-flight visual pixels. Focused acceptance and package delivery do
not establish a green full Linux descriptor, real remote multiplayer, all effects or the
whole living-world goal. Public alpha.1/server deployment remains separate.

## Life actual-outcome development increment (2026-10-03)

The former isolated Life owner integration is now live: 30 existing identities have authored material outcomes tied to actual admitted gameplay, source-aware Full/Minimal client delivery, bounded loaded-world rendering, corrected surface orientation, finite lifecycle cleanup and 56 new sound identities for 28 original effects. Generic overlap is removed only for wholly covered groups; mixed/uncovered fallback and requested rear glyphs remain. Actual spectator interception and Fortune pre-admission success feedback are fixed.

Current Core 48s, Stateful 2m47s, Edges 2m11s, Pulse 49s, source 26s, quality 26s, geometry 26s, spectator 25s and Fortune 37s focused suites pass. Combined build passes 985 units; 5270 generated paths reproduce; 123 web/GitBook pages validate. Native guide 44s final clear-capture and connected-player Bloomstep 35s passes are now recorded; compact settings keep all 18 buttons in bounds. Final build 13s, 985 units and checked review JAR are delivered with the exact checksum in the milestone audit. See docs/audit/life-outcomes-milestone.md for actual scope and remaining visual/CI/multiplayer limits. No new goal content count is awarded for polishing existing Life effects. Rootmolt/Drainhouse remains an unaccepted isolated candidate.

## Rootmolt and grounded Sporebloom development increment (2026-10-03)

Rootmolt is now live and has scoped ecology/control/counter/restart/presentation/sidestep/placement/query/alarm native acceptance. Conservative creature credit rises to 7 of 12. Sporebloom actual support, finite particle motion and revised all 30 surface checks pass. Combined 15s build passes 988 units; 5280 generated paths reproduce; 124 guide/GitBook pages validate; nine directly reviewed native originals and checked review JAR are delivered. See docs/audit/rootmolt-spore-milestone.md for exact checksum, source gates and evidence limits.

The preceding e0405713 ordinary CI build and Player Guide passed; its full Linux gameplay descriptor failed the synchronous counter health-loss assertion. The tightened provenance fixture passes locally 50s, but full Linux acceptance is still pending. Rootmolt natural scheduler distribution, sound listening, sustained population performance and remote multiple-client play remain open. No new ecosystem/location/item/ability/lore/investigation credit is added by this increment. Drainhouse remains an isolated next candidate. The goal stays active.

## Belowkeeper Drainhouse development increment (2026-10-04)

The three-room Drainhouse now has focused natural-generation, ordinary Survival entry, authentic ledger/bud/reopen, earned investigation, equipment, transaction and presentation acceptance. Its rigid vault reduces required AIR carving from 291 to 226 under the unchanged 256 ceiling; actual ore preservation, dry entrances and gutter containment are verified. Three original three-page books connect living snail gathering, Rootmolt meal/counter observation, restoration and equipment.

Mara's Empty Bell and actual feet-slot Rootbound Greaves add one relic and one equipment item. Conservative totals become 23 functional items (relic 2/equipment 2/tools 4/placeables 6/materials 9), 15 lore texts, 2 investigations and 2 locations. Creatures 7, new abilities 2, new signatures 18 and complete ecosystems 0 remain unchanged. At least 14 additional functional items are still needed to meet the category minimums. Draft Ember and Tideward content is uncredited.

Final natural gate 57s passes across 243 scanned chunks/three seed worlds with one actual naturally generated dependency site and 246 total explicit sample/known-footprint requests. Final vaulted earned chain 164s and terrain fit 37s pass. Build 13s passes 988 units; 5321 generated paths reproduce, 125 guide/GitBook pages export, and the review JAR exactly matches 5320 resources/1764 classes. Fourteen native originals and all nine final book pages were inspected. See docs/audit/belowkeeper-drainhouse-milestone.md for checksum, detailed gates and limitations.

Background blade-tooltip threading and nursery observation timing corrections are committed separately (3eb33c2a and 58904bbc). The latter unchanged/revised focused nursery suites pass 145s/140s; the full Linux descriptor remains separate and its latest run was in progress. Remote multiplayer, representative natural frequency, sustained mixed populations, complete spell/Aura lifecycle and the broad living-world goal remain open. No new CurseForge/server deployment is included.


## Cinder Bailiff and cooled fern development increment (2026-10-04)

The original ceramic three-vent Bailiff and six-state Cinder Fern now have focused real crafting, paid tending, manual harvest, competing feeding/full reopen, physical warning/fan, callback/dense-group and native visual acceptance. Acceptance fixed unreachable idle targets suppressing meals, already-reached roots rejected by path admission, ordinary harvest tied to the spell-edit setting, and claim callback reentry/authority gaps. Actual resource and once-per-payment budgets remain authoritative.

Final 15s build passes 990 units; 5352 generated paths reproduce; 126 illustrated guide/GitBook pages validate. Checked review JAR, exact checksum and ten directly inspected actual native captures are in artifacts/review/ember-woodlands. See docs/audit/ember-woodlands-milestone.md for gate provenance and limits, including an unestablished historical fixture health failure.

Conservative credit becomes 8 creatures and 24 functional items (relic 2/equipment 2/tools 4/placeables 7/materials 9). At least 14 further items remain to meet category minimums; an additional placeable does not replace missing relic/equipment/tool credits. Abilities 2, signatures 18, lore 15, investigations 2, locations 2 and complete ecosystems 0 remain unchanged. Natural patch/animal distribution, connected human multiplayer, sustained population performance and a whole ember ecosystem remain open. Root Carry, Tideward, Moonreed and Aura guard candidates are unaccepted. The goal remains active.


## Root Carry and genuine local connection increment (2026-10-04)

Root Carry is a rank II Life ability: two separately paid Survival casts select then relocate an exact young Cinder Fern, retaining growth/cooling and enforcing claims, loaded terrain, two-cell budgets, callback guards and saved twenty-second rest. Its soil/frond projectile and three actual owner transfer beats have Full/Minimal native presentation and finite playback gates. Moonreed harvest now resets before reward and validates write success, claims and actual post-state. Actual pollinator entity departure is a separate pending correction.

Final build 19s passes 995 units, 5361 generated paths reproduce, 127 illustrated guide pages validate/export. Review artifact and original native galleries are in artifacts/review/root-carry; exact gates/checksum and limits are recorded in docs/audit/root-carry-milestone.md. The generator no longer consumes legacy Life tile assignments for bespoke Root Carry art, retaining prior artwork.

Abilities become 3 (Unity, Basinfill, Root Carry), runtime 370 runes/Life 31. Creatures 8, functional items 24, signatures 18, lore 15, investigations 2, locations 2 and complete ecosystems 0 remain unchanged. The separately committed local two-JVM gate proves real TCP movement/rendering/disconnect; actual cooperative Aura gameplay remains pending. The goal stays active; this is development content, with no new public release or VPS activation.

## Tideward and paid local Unity increment (2026-10-04)

Reedwater Waders, Dewglass Spectacles and Bank Surveyor's Line now have actual craft/use/equipment/wear/rest/reopen and paid Full/Minimal visual acceptance. The forked spool reads whole supported crossings without terrain edits; waders aid actual shallow walking; spectacles read a living crab warning. Original worn/held/material art, finite local physical cues and three sound voices accompany the mechanics. Native tests corrected visible top-face admission and a real cosmetic packet arriving before vanilla block replication, retaining bounded waiting, exact expiry and cancellation.

Nine focused native gates pass. Build 15s passes 1000 units; 5392 generated paths reproduce; 128 illustrated guide pages validate/export. Checked development JAR and fourteen original presentation captures are in artifacts/review/tideward-crossing. Ten drawn frames were directly viewed; four paired backgrounds support strict pixel checks. See docs/audit/tideward-milestone.md for checksum, gate provenance, historical failures and scope limits.

Items become 27 (relic 2/equipment 4/tools 5/placeables 7/materials 9), leaving at least 11 further items to meet category minima and 38 total. Creatures 8/abilities 3/signatures 18/lore 15/investigations 2/locations 2/complete ecosystems 0 remain unchanged. Moonreed now separately checks actual pollinator liveness/body/world across callbacks. The genuine two-JVM Unity test independently pays both players, proves actual allied healing and owner-only cancellation/disconnect across distinct TCP sockets; supplied progression and local latency limits are explicit in two-client-unity.md. These scoped gates do not complete full Aura, ecosystem, shaders or multiplayer performance acceptance. Siltcrest, wetland/Aura guards and Shutterworks remain isolated candidates. Public release/server activation remains separate; the living-world goal stays active.

## Wetland complete-query increment (2026-10-04)

Newt refuge/population and crab acquisition/population/sweep collect complete raw typed pools of at most 12; saturation 13 refuses partial selection. Actual crab hurt callbacks revalidate source/action/quarry/body/world and recipient before follow-up while preserving five damage and original warning/recovery clocks. Actual bounds 35s and nine-mode sweep 97s pass, with Newt 51s/Garden 43s/Crab 49s/Rattle 47s regressions. Build 13s passes 1000 units, 5392 paths reproduce, 128 guide pages validate; checked development JAR is in artifacts/review/wetland-guards with checksum and exact evidence in docs/audit/wetland-query-guards.md. No new content count or sustained performance claim is awarded. Full Linux gameplay and whole ecosystems remain open.

## Siltcrest wetland increment (2026-10-04)

Siltcrest Bittern adds an original layered reed-bird rig, finite saved appetite, dry-bank ordinary night hunting, protected wild-fish rules, real offered food, daytime/rain canopy rest and fresh allied Tidebreath response. Ten focused native gates pass; eight original Full/Minimal model frames were directly viewed with strict paired native pixel controls. See docs/audit/siltcrest-milestone.md for failed fixture geometry, actual floating hunt correction, post-hurt source authority, readback scheduling and remaining natural/listening/shader limits. The guide gains an illustrated practical chapter.

Creature additions become 9; items 27/abilities 3/signatures 18/lore 15/investigations 2/locations 2/complete ecosystems 0 remain. Separate local workload measurement and fixture diagnostics do not award ecosystem or new item credit. Public release/server activation remains separate; the broad goal stays active.

Final Siltcrest build 15s passes 1000 units; 5400 paths reproduce and 129 guide pages export. Checked JAR/native originals are in artifacts/review/siltcrest-bittern. Two 400-tick mixed-wetland profiles against corrected physical Mud footing have actual stable 8/40 creatures, sparse catch and reed opening, with raw execution samples retained in docs/audit/evidence/mixed-wetland-20261004.json. Scoped local mean/p95/p99 are observations, not universal capacity or before/after improvement. Natural distribution and full ecosystems remain open.

## 0.10.0 release preparation — 2026-10-04

Version 0.10.0-alpha gathers every completed living-world milestone since 0.9.1-alpha.1: fungal
gardens and Rootmolts, Drainhouses, Cinder Bailiffs and ferns, Root Carry, the Tideward kit,
Siltcrest Bittern, Mossveil, Camp Concord, Rook's Rainshield, eighteen signatures and the Earth,
Life and Void materials. The runtime roster is 372 runes (55 elemental and 40 signature fusions).

Release review fixed break-callback probes with side effects (dungeon ward placed-block records,
glyph traps) for read-only and in-place checks, a Nullcatch/Quietus screen replacement on one
target, and the Grimoire-opened frame benchmark. The CHANGELOG now lists the later milestones.
The native client gate never completed within a single 90-minute CI job; it now runs as four
ordered shards. A hosted run of all four shards on the release commit is required before tagging.
Public upload and server deployment still require an explicit request. The broad living-world goal
remains active; no new content count is claimed.
