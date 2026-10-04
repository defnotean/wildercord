# Living-world goal: outstanding work (2026-10-03)

This is a completion audit against the active goal and `docs/LIVING_WORLD_ROADMAP.md`.
Counts are additions since the goal baseline, not all content already in Wildercord.
Existing features, recolours, source references and controlled screenshots do not prove a complete milestone.

## Content targets

| Target | Evidenced additions | Remaining |
|---|---|---|
| 12 creatures | 6: Buried Keeper, Stonehorn, Galeclaw, Lantern Newt, Reedback Crab, Sporeback Snail | 6: two peaceful/magical wildlife, two hostile/territorial creatures, another boss and a useful companion |
| 4 connected ecosystems | Highland, wetland and first fungal foundations | Finish their connected relationships; build ember woodlands; none yet meets the full ecosystem acceptance criteria |
| 6 new dungeon/encounter locations | Sword tomb is the conservative confirmed new dungeon; hosted village tournament is an additional encounter candidate | 5 further fixed locations under the conservative architectural count, or 4 if the tournament counts; finalize this scope explicitly |
| Battlefields and sleeping blade | Both implemented with native evidence | Natural rarity/distribution, remote participation, return-visit design and remaining lifecycle coverage |
| At least 36 functional items | 21 accepted functional additions, including the four Nursery items | Seven relics, seven equipment and two tools remain to meet category minimums, yielding 37. See the category ledger; do not relabel materials to force 36. |
| 12 abilities | Unity and verified Basinfill: 2 credited | 10 still uncredited; audit any newly taught technique against the baseline before counting it |
| 18 new signature fusions |18 newly authored: six accepted field signatures plus twelve expedition/counter/support signatures with real client altar and paid Full/Minimal focused checks | Numeric target met; complete supported-pairing/delivery lifecycle and remote multiplayer acceptance remain. Runtime totals40 signatures/95 fused recipes/369 runes; baseline22 signatures are not credited as additions. |
| 24 discoverable lore texts | 12 credited: previous nine and three Nursery texts | 12 plus illustrated journal/discovery integration; bestiary pages should be reconciled before further credit |
| 4 connected optional investigations | Glowcap Nursery earned-resource chain passed with saved-state and once-only reward checks | 3 further complete chains, clues, persistent state and rewards |

The prior increment ledger credited fifteen items by including Tideward notes. This audit separates that lore book from functional equipment/materials. The four accepted Nursery additions now bring functional additions to twenty-one. Spawn eggs and lore texts are not equipment credits. Existing signature fusions and battlefield/tomb technique rewards were confirmed unchanged against baseline commit `14efc61e`; teaching them does not count as new abilities. Life presentations, six signature fusions and the Glowcap Nursery investigation have focused native acceptance. Combined review JAR and pushed milestone delivery remain pending.

## 1. Visual identity

Finish the asset-by-asset audit of equipment, accessories, blocks, creatures, structure details,
menus and HUD. Establish consistent regional/faction materials and silhouettes. Review the
illustrated field journal in game across chapter types, discovery states and GUI scales.
Existing guide improvements and120 validated/exported pages, including the illustrated Life materials chapter, are a foundation, not full visual approval.

## 2. Every spell and Aura technique

The development runtime now has369 runes (277 effect declarations). Twelve new signatures have dedicated paid Full/Minimal preparation/flight acceptance, in addition to the prior206-identity seven-family moving-body contract. These are scoped stage checks, not completed spell lifecycles; see `full-spell-roster/coverage-stages.md`. Life29 and all18 new signatures have accepted focused preparation/paid moving-body evidence. Finish all remaining void, time, arcane, blood and
cross-family exceptions. Finish actual release,
impact, aftermath and sound identity for every effect. Audit beam, rain, self, summon, trap,
ward, field and construct deliveries separately. Verify ranks/modifiers, links, reflection,
homing, mixed fallback groups, dynamic Knots/weaves and addons. Review first-person aim,
third-person readability, Minimal/reduced flash, shaders and concurrent players.

Rear-circle/group/linked/caster-origin fixes exist; complete delivery and camera coverage remains.
Aura's physical standards, fractures and articulated Soar wings exist, but a full technique roster
review and lifecycle/multiplayer proof remain. Every fusion must retain ingredient behavior and
motion, not only its colour. Inventory/reference indices are not presentation proof.

## 3. Creatures and ecosystems

Six further creatures need original models, animation, voices, habitat, behavior, counterplay,
acquisition/rewards and bestiary evidence. Highlands and wetlands need broader ecological
relationships and natural observation playthroughs. Ember forests and fungal ruins need their
connected habitat loops. Validate food/shelter/predation/pollination under mixed populations,
multiple seeds, unloaded chunks and existing worlds. Sporeback Snails now establish actual
fungus browsing, finite gathering and shade checks; fungal ruins still need their wider connected
habitat. Measure population/simulation costs.
Cinnamon and fish-tamed foxes already exist; preserve their ownership, sit/follow and persistence.
They are not counted as newly created species for this expansion.

## 4. Equipment, crafting and combat

Add 16 further functional items and reconcile the minimum total against the eight relic/eight equipment/six
tool/six placeable/eight consumable categories. Design acquisition and alternatives; complete
upgrades, research, cooperative support, wards/apparatus and useful home infrastructure.
Check inventories, backpack, accessory slots and all new recipes in the real UI.

Extreme Vow/DoT damage and shared-payment continuation budgets are fixed and tested. Review
natural PvP and boss pacing, healing/control/resistance tradeoffs and remaining separate damage
paths (addon vanilla damage, companions, hazards, Stasis pooling separate payments). Complete
all supported fusion pairing checks and deliberate unsupported-combination feedback.

## 5. Exploration, lore and retention

Build four to five further distinct locations, depending on tournament encounter credit with architecture, inhabitants, puzzles, lore and rewards;
Storm Spire is an improved existing dungeon, not another added location. Check generation
frequency and spacing across seeds, biome placement, old saved layouts and return routes.

Add twelve further credited lore texts and three connected investigations. Develop memorable
characters and faction motives, gameplay clues, consequences and optional story progression.
Complete bestiary/journal collections, artifact restoration, mentorship, home projects and
cooperative expeditions. Run early/middle/late-game journeys with multiple builds and verify
that research and exploration reward new choices without repetitive grind or mandatory chores.

## 6. Aura world/cooperation acceptance

Terrain training, Marchkeeper battlefields, sword tomb/Keeper, sleeping blade, Stonehorn/Galeclaw,
village tournaments, resonant strikes, rune-etched blades and Unity all have playable increments.
Remaining acceptance includes natural distribution, dedicated-server/real multiple-client play,
complete crash/restart/dimension/death cases, all inscription effects and non-damage cooperation,
broader matchup balance and a complete combined Aura progression playthrough. The existence
of these systems does not close milestone A.

## 7. Reliability, performance and delivery

Complete real remote multiplayer tests; integrated single-client servers are different evidence.
Profile sustained mixed combat and natural ecosystems. Search limits/cache caps are constraints,
not measured speedups. Finish concurrent effect/sound readability, non-colour communication,
accessibility and upgrade safety. Resolve full CI outcomes before calling all gates green.
The previous full native CI job was cancelled after graphics initialization failed before tests;
the ordinary build passed. CI-only correction 65256266 is pushed. Its subsequent Linux run initialized Mesa OpenGL and reached gameplay, then failed a timing assumption in HomeProjectsTest. The corrected focused local test passes; a subsequent full Linux descriptor remains required.
See `ci-graphics-startup.md` for the distinct graphics and gameplay results.

Continue delivering native screenshots, validated code/assets/recipes/docs, checked review JARs,
clear limits and pushed commits per milestone. Public releases need explicit user authorization.
The 0.9.1-alpha.1 asset mismatch is corrected. VPS activation of the published 0.9.1-alpha.1 release was verified on 2026-10-03
with an offline backup, installed checksum, loader startup and responsive RCON.
See `server-update-0911.md`. Later goal content is not included in that release.

## Next concrete work

Basinfill, Reed Rattle and Sporeback Snails are implemented and have scoped native evidence.
Life 29, the six field signatures and the Glowcap Nursery earned-resource investigation have accepted focused native evidence; combined build and pushed delivery are pending. The current parallel work includes removal of generic front projectile overlays and drafts of the next twelve signatures. Fungal tests now pass finite late-search and blocked-path traversal, natural generation on the retained failed seed, and the grounded canopy acquisition/restart chain. A Grow-only obstruction cause was not established; actual Grow-first acceptance passes. Clearer native presentation captures are being prepared. Continue the remaining elemental
roster and other creatures, ecosystems, locations and progression. No requirement
above is considered complete merely because this audit exists.

## Current development increment

The twelve expedition signatures bring the numeric new-signature target to18. Their
Counter42s, Support42s, Trail36s and Presentation62s focused tests pass; all12 real client
Fuse packets, paid Full/Minimal preparation/flight assertions, resource/permission/counter
checks and scoped reopen behavior are evidenced. Runtime totals are369 runes,40 signatures
and95 named fused recipes. See expedition-signatures-milestone.md for final delivery gates.

Prior Void36 stage acceptance covered206 canonical identities across seven families;
new signatures do not turn that contract into a complete lifecycle count. Life105 and
Rootmolt/Drainhouse remain isolated drafts, with no creature/item/lore/investigation credits.
The preceding fb2c07cb ordinary build passed; full Linux gameplay failed later at
gravity-flight visual pixels. Broader CI, multiplayer, all-delivery and ecosystem
acceptance stays open after a successful scoped development milestone.

## Life actual-outcome development increment (2026-10-03)

The former isolated Life owner integration is now live:30 existing identities have authored material outcomes tied to actual admitted gameplay, source-aware Full/Minimal client delivery, bounded loaded-world rendering, corrected surface orientation, finite lifecycle cleanup and56 new sound identities for28 original effects. Generic overlap is removed only for wholly covered groups; mixed/uncovered fallback and requested rear glyphs remain. Actual spectator interception and Fortune pre-admission success feedback are fixed.

Current Core48s, Stateful2m47s, Edges2m11s, Pulse49s, source26s, quality26s, geometry26s, spectator25s and Fortune37s focused suites pass. Combined build passes985 units;5270 generated paths reproduce;123 web/GitBook pages validate. Native guide44s final clear-capture and connected-player Bloomstep35s passes are now recorded; compact settings keep all18 buttons in bounds. Final build13s,985 units and checked review JAR are delivered with the exact checksum in the milestone audit. See docs/audit/life-outcomes-milestone.md for actual scope and remaining visual/CI/multiplayer limits. No new goal content count is awarded for polishing existing Life effects. Rootmolt/Drainhouse remains an unaccepted isolated candidate.
