# Progression audit (roadmap Priority 2)

This audit covers every progression unlock a Survival player can earn. For each it records how the player learns it, where it is taught, whether the controls and tradeoffs can be seen in-game, and the lowest legitimate route to it. It was made by reading the source, the in-game text (`en_us.json`) and the player wiki. The **Fixed here** column lists gaps closed in this pass. **Open** lists what still needs an owner or a native run.

Legend for "Visible in-game": **Yes** means the controls, costs and tradeoffs are shown on a screen, tooltip or HUD. **Partly** means the unlock is shown but some tradeoff only appears in the wiki.

## Aura

| Unlock | How it is learned | Where it is taught | Visible in-game | Lowest legitimate route |
|---|---|---|---|---|
| Glow (start) | Use a Breathing Manual | Manual tooltip; Cord -> Aura page | Yes | Buy a manual from a weaponsmith or cleric villager, or loot one (trial chambers, stronghold library, ancient city, Wildercord vaults) |
| Flow / Edge | Reach 150 / 600 total Aura XP, then pass a trial: 30 s stillness at a ley crossing, waterfall or summit, or beat a stronger foe within 60 s | Aura page (XP hover, trial text); chat note at the practice cap | Yes (reworked in `aura-progression.md`) | Fight hostile mobs with recovered sword swings, then crouch still with a blade at a waterfall |
| Form / Sovereign | 1,800 / 4,500 XP, then a thunderstorm stillness, a boss within 180 s, or a duelist duel without magic | Aura page | Yes | Wait for a thunderstorm at a ley crossing, or duel a wandering duelist |
| Breathing method change | Read another manual | Manual tooltip, Aura page | Yes | As for Glow |
| Arts (First Art to Final Art) | Come with method and stage; spelled by swing strings | Aura page art list with strings, costs and rests; string indicator on HUD | Yes | Automatic with stage |
| Master's Arts: Spellcut (Edge), Rising Break and Driving Cut (Form) | Automatic with stage | Aura page lists the arts and their current keys; key binds under "Wildercord: Master's Arts"; art descriptions state cost and rest | Yes | Reach Edge / Form |
| Awakening | Edge or above, full pool, momentum half or more: tap Aura then hold | Aura page; banner on use; Spent state on HUD | Yes | Reach Edge |
| Techniques / Ways | Technique scrolls (loot, Sword Tombs, tournaments, Aura Shard crafting), beaten duelists, your Way | Scroll tooltip; Techniques page | Yes | Craft a scroll from paper plus an Aura Shard |

**Fixed here:** the player wiki (`gitbook/progression/aura.md`) had no section on Master's Arts or Master forms. A short section with keys, stages, costs and teachers was added.

## Master forms (one shared slot, key C, rebindable)

| Unlock | How it is learned | Where it is taught | Visible in-game | Lowest legitimate route |
|---|---|---|---|---|
| Reed Slip | Form stage plus a recorded Gale Master clear; crouch-use a Gale duelist with an empty hand | Teacher hint in chat; lesson screen; Cord -> Aura -> Master form | Yes (HUD state, cost, rest) | Win the Gale Masters trial (Circle VIII invitation or Aura route), then find a Gale duelist |
| Wall Turn | Sovereign plus Gale clear; same teacher flow | Teacher hint; illustrated lesson; Master form page with locked/source help; retained story | Yes (narrated, keyboard reachable; `WallTurnLessonTest`) | As Reed Slip, at Sovereign |
| Cinder Lunge | Sovereign plus Ember clear; Ember duelist | Same as above | Yes | Ember Masters clear |
| Stone Hinge (gated) | Sovereign plus Stone clear; Stone duelist; only if `aura.experimental_stone_hinge` is on | Same; the locked text says how to learn it | Yes when enabled. When disabled the teacher does not offer it, and a learned form stays learned but cannot be equipped | Stone Masters clear plus the server switch |

Respec: the slot swaps freely between known forms while grounded and out of combat, after the shared rest. Forms are never lost.

**Fixed here:** Wall Turn now has its segmented articulated palette and continuous movement presentation (`ArticulatedCombatPose.sampleWallTurn` and `wallTurnMove`; `ArticulatedCombat.extract`). It is unit-tested by `ArticulatedWallTurnPoseTest` and `ArticulatedWallTurnArmorTest`. The native smoke also found a client clock skew: each re-sent kick step was stamped with the server tick, which is ahead of the client, so its age was negative and no kick pose showed, in Classic or articulated. `MasterFormsClient` now clamps the receipt tick to the local clock.

## Heart Circles (I to XX)

| Unlock | How it is learned | Where it is taught | Visible in-game | Lowest legitimate route |
|---|---|---|---|---|
| Circles I-VIII and perks (Mana Skin III, Flow V, Overflow VII, Archmage VIII) | Condense lifetime mana, meet the breakthrough requirements, meditate 10 s | Cord -> Heart page lists each need with progress and the feat text | Yes | Cast spells; the feats are soloable |
| Circles IX-XX | Same, with higher mana and feats (Tempered, Fusion, Low Tide, Starbreaker, Knotted, Heartwood, Grounded, secrets, Runebound) | Heart page; advancements | Yes | Same |
| Masters trials invitation | Circle VIII | Chat invitation when VIII forms | Yes | Circle VIII |
| Relay Circle | Active VIII plus The Last Page; copy at a spent Archive Lectern, read, Learn | Grimoire -> Master studies; lesson pages | Yes | Defeat the Archivist |
| Tollgate (X), Lifeline (XIV), Conduit (XVIII) | Active circle plus the boss feat; copy the boss's book, study, learn | Grimoire -> Master studies; grammar and storage problems are shown in the editor | Yes | Defeat the Cinder Warden / Star-Eater / Storm Conductor |
| Ebb Ledger / Reweave (XII) | Low Tide holder; Grimoire -> Master studies | Same; HUD shows time, beats and rewrite usefulness | Yes | Defeat the Tide Scribe |
| Excise (XVI) | Heartwood holder; Rootbound lesson | Same | Yes | Defeat the Root Guardian |
| **Circle Vows** (IX, XI, XIII, XV, XVII, XIX, XX), new | Free choice between two sides when the circle is formed | Chat offer with buttons and hover text when the circle forms, again on meditation (once per session); `/vow` lists everything | Yes (chat). No Cord panel | Form the circle |

**Fixed here:** circles IX, XI, XIII, XV, XVII, XIX and XX gave only the flat +15 mana, +0.5 regen and +3% power. Each now asks for a bounded vow (`docs/design/circle-vows.md`). Every circle from VIII to XX now grants exactly one kind of new thing, and `CircleVowsTest` enforces this. The wiki circle table now names each circle's gift, and has a vow table.

## Rune tiers, ranks and feats

| Unlock | How it is learned | Where it is taught | Visible in-game | Lowest legitimate route |
|---|---|---|---|---|
| Rune tiers I-III | Craft from Blank Runes; the Cord tier limits which you can hold | Recipe book unlocks on holding a Blank Rune; tooltip shows the tier | Yes | Crafting |
| Tier IV | Bosses, dungeon vaults, rare structures | Tooltip | Yes | Archivist (Archive) |
| Rune ranks II / III | Three copies at the Fusion Altar | Tooltip "Rank II: +25% power, at the same mana" | Yes | Craft three copies |
| Feats | Specific deeds (boss methods, Long Incantation, In Rhythm, Fusion, Knotted...) | Grimoire feat list; Heart page names the feat a circle needs, with its description | Yes | Per feat; all needed for circles can be earned solo |

## Respec summary

| What | Respec | Cost |
|---|---|---|
| Circle Vow | `/vow release <circle>`, then choose again | 5 levels (free in creative); works while cracked |
| Spell traits (Mastery) | Reroll or unbind | Once per rank (`MasteryRules.CHANGE_LEVELS`) |
| Techniques | Change a technique | `TechniqueRules.CHANGE_LEVELS` |
| Master / field forms | Swap in the one slot | Free, grounded and out of combat, after rest |
| Breathing method | Read another manual | A manual |
| Circles, stages, lessons | None by design: earned permanently | - |

## Tests added in this pass

- `CircleVowsTest` (7 tests): VIII-XX coverage, stable save bits, old saves, malformed bits, take/release rules, active-only and bounded effects.
- `ProgressionSaveCompatTest` (6 tests): empty and old-shaped NBT/JSON for Aura data, Awakening, Ways, field forms, Master forms (Wall Turn before Stone Hinge), Mastery entries, circles and vows.
- `PartyProgressionRulesTest` (4 tests): party members never harm each other with forms or lessons unless dueling; protection ends on leave/kick and is kept across disconnect; entitlement is personal; the Lifeline refusal window holds for allies.
- Native: `ProgressionVowWallTurnNativeTest` covers an old Circle XX save without vows, take/paid release/retake through the real player command, restart persistence, and a real Wall Turn brace and kick producing the segmented articulated frame. It passed natively on 2026-10-08 (14.8 s). `WallTurnLessonTest` and `StoneHingeLessonTest` were rerun and also passed.

## Open

- Vow balance numbers are first-pass values (owner decision).
- Vows use chat plus `/vow`; a Cord-screen panel would be more discoverable (owner decision).
- Native visual acceptance of the Wall Turn articulated palette (screenshots, third person, observers) is not done. The native smoke checks only that the frame exists and is weighted.
- Stone Hinge stays gated; see Priority 4.
