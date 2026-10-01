# Uncommitted working-tree file inventory

Snapshot of the WilderCord checkout on September 30, 2026, after implementation. This includes changes already present before this implementation; it does not attribute every path to one work session. No commit or push was performed.

**637 paths:** 476 untracked, 161 tracked changes. Generated captures, logs, profiles and build outputs are ignored and listed separately in the implementation report.

## Directory totals

| Directory | Paths |
|---|---:|
| .github | 1 |
| .gitignore | 1 |
| README.md | 1 |
| build.gradle | 1 |
| docs | 22 |
| profiles | 5 |
| src | 587 |
| tools | 19 |

## Full path list

Git status uses `??` for an untracked file; the two status columns otherwise describe index and working-tree changes.

| Status | Path |
|---|---|
| ` M` | `.github/workflows/build.yml` |
| ` M` | `.gitignore` |
| ` M` | `README.md` |
| ` M` | `build.gradle` |
| ` M` | `docs/RECIPES.md` |
| `??` | `docs/audit/2026-09-30-circle-expansion.md` |
| `??` | `docs/audit/2026-09-30-gameplay-performance-roadmap.md` |
| `??` | `docs/audit/2026-09-30-implementation-changelog.md` |
| `??` | `docs/audit/2026-09-30-performance-measurements.md` |
| `??` | `docs/audit/2026-09-30-physical-magic-expansion.md` |
| `??` | `docs/audit/2026-09-30-uncommitted-file-inventory.md` |
| `??` | `docs/audit/implementation-progress.md` |
| `??` | `docs/features/cinnamon.md` |
| `??` | `docs/features/circle-disciplines.md` |
| `??` | `docs/features/defensive-foci.md` |
| ` M` | `docs/features/dungeons.md` |
| `??` | `docs/features/elemental-armor.md` |
| `??` | `docs/features/expeditions.md` |
| `??` | `docs/features/familiars-events-trials.md` |
| ` M` | `docs/features/fusion-altar.md` |
| ` M` | `docs/features/gear-config-api.md` |
| `??` | `docs/features/home-projects.md` |
| `??` | `docs/features/new-encounters.md` |
| `??` | `docs/features/physical-magic.md` |
| `??` | `docs/features/practice.md` |
| `??` | `docs/features/research-library.md` |
| `??` | `profiles/README.md` |
| `??` | `profiles/balanced/config/wildercord-visuals.json` |
| `??` | `profiles/cinematic/config/wildercord-visuals.json` |
| `??` | `profiles/dependencies.lock.json` |
| `??` | `profiles/performance/config/wildercord-visuals.json` |
| ` M` | `src/client/java/dev/wildercord/client/CordScreen.java` |
| ` M` | `src/client/java/dev/wildercord/client/FusionAltarScreen.java` |
| ` M` | `src/client/java/dev/wildercord/client/GuiSpellCircle.java` |
| `??` | `src/client/java/dev/wildercord/client/MagicSettingsScreen.java` |
| `??` | `src/client/java/dev/wildercord/client/RuneNotebookScreen.java` |
| ` M` | `src/client/java/dev/wildercord/client/WildercordClient.java` |
| ` M` | `src/client/java/dev/wildercord/client/WildercordKeys.java` |
| ` M` | `src/client/java/dev/wildercord/client/cosmetic/CordTrails.java` |
| ` M` | `src/client/java/dev/wildercord/client/fx/AimPreview.java` |
| ` M` | `src/client/java/dev/wildercord/client/fx/ChargeCircles.java` |
| `??` | `src/client/java/dev/wildercord/client/fx/FrameBenchmark.java` |
| ` M` | `src/client/java/dev/wildercord/client/fx/LightParticle.java` |
| `??` | `src/client/java/dev/wildercord/client/fx/MagicQuality.java` |
| `??` | `src/client/java/dev/wildercord/client/fx/MaterialParticle.java` |
| ` M` | `src/client/java/dev/wildercord/client/fx/ScreenEffects.java` |
| ` M` | `src/client/java/dev/wildercord/client/fx/SpellCircleParticle.java` |
| `??` | `src/client/java/dev/wildercord/client/fx/SpellFormations.java` |
| `??` | `src/client/java/dev/wildercord/client/pet/CinnamonModel.java` |
| `??` | `src/client/java/dev/wildercord/client/pet/CinnamonRenderState.java` |
| `??` | `src/client/java/dev/wildercord/client/pet/CinnamonRenderer.java` |
| ` M` | `src/client/java/dev/wildercord/client/render/DungeonBossRenderState.java` |
| ` M` | `src/client/java/dev/wildercord/client/render/DungeonBossRenderer.java` |
| ` M` | `src/client/java/dev/wildercord/client/render/DungeonRenderers.java` |
| `??` | `src/client/java/dev/wildercord/client/render/RootGuardianModel.java` |
| `??` | `src/client/java/dev/wildercord/client/render/RootGuardianRenderer.java` |
| `??` | `src/client/java/dev/wildercord/client/render/StormConductorModel.java` |
| `??` | `src/client/java/dev/wildercord/client/render/StormConductorRenderer.java` |
| `??` | `src/gametest/java/dev/wildercord/cast/ArmorResponsesTest.java` |
| `??` | `src/gametest/java/dev/wildercord/cast/CircleDisciplineTest.java` |
| `??` | `src/gametest/java/dev/wildercord/cast/ControlMatchupsTest.java` |
| `??` | `src/gametest/java/dev/wildercord/cast/DefensiveFociTest.java` |
| `??` | `src/gametest/java/dev/wildercord/cast/HomeProjectsTest.java` |
| `??` | `src/gametest/java/dev/wildercord/cast/NewEncountersTest.java` |
| `??` | `src/gametest/java/dev/wildercord/cast/PerformanceProfilesTest.java` |
| `??` | `src/gametest/java/dev/wildercord/cast/PhysicalMagicTest.java` |
| `??` | `src/gametest/java/dev/wildercord/cast/PracticeRoomTest.java` |
| ` M` | `src/gametest/java/dev/wildercord/cast/RunicAnimationGalleryTest.java` |
| `??` | `src/gametest/java/dev/wildercord/cast/WildercordRuneGapsTest.java` |
| `??` | `src/gametest/java/dev/wildercord/familiar/ContentSystemsTest.java` |
| `??` | `src/gametest/java/dev/wildercord/gametest/WildercordCinnamonTest.java` |
| ` M` | `src/gametest/java/dev/wildercord/gametest/WildercordCordTest.java` |
| ` M` | `src/gametest/java/dev/wildercord/gametest/WildercordFamiliarTest.java` |
| ` M` | `src/gametest/java/dev/wildercord/gametest/WildercordFeatureTour.java` |
| ` M` | `src/gametest/java/dev/wildercord/gametest/WildercordFireBloodShots.java` |
| ` M` | `src/gametest/java/dev/wildercord/gametest/WildercordFireBloodTest.java` |
| ` M` | `src/gametest/java/dev/wildercord/gametest/WildercordFlightTest.java` |
| ` M` | `src/gametest/java/dev/wildercord/gametest/WildercordFrostWindTest.java` |
| ` M` | `src/gametest/java/dev/wildercord/gametest/WildercordFusedFlameTest.java` |
| ` M` | `src/gametest/java/dev/wildercord/gametest/WildercordFusedFrostTest.java` |
| ` M` | `src/gametest/java/dev/wildercord/gametest/WildercordFusionTest.java` |
| ` M` | `src/gametest/java/dev/wildercord/gametest/WildercordGearSlotsTest.java` |
| ` M` | `src/gametest/java/dev/wildercord/gametest/WildercordGearTest.java` |
| ` M` | `src/gametest/java/dev/wildercord/gametest/WildercordLifeArcaneTest.java` |
| ` M` | `src/gametest/java/dev/wildercord/gametest/WildercordLoadoutsTest.java` |
| `??` | `src/gametest/java/dev/wildercord/gametest/WildercordMagicReleaseTest.java` |
| ` M` | `src/gametest/java/dev/wildercord/gametest/WildercordNewRunesTest.java` |
| ` M` | `src/gametest/java/dev/wildercord/gametest/WildercordParryTest.java` |
| ` M` | `src/gametest/java/dev/wildercord/gametest/WildercordReactionsTest.java` |
| ` M` | `src/gametest/java/dev/wildercord/gametest/WildercordScreenshots.java` |
| ` M` | `src/gametest/java/dev/wildercord/gametest/WildercordShaderTest.java` |
| ` M` | `src/gametest/java/dev/wildercord/gametest/WildercordShowcase.java` |
| ` M` | `src/gametest/java/dev/wildercord/gametest/WildercordSignatureFusionTest.java` |
| ` M` | `src/gametest/java/dev/wildercord/gametest/WildercordStormEarthTest.java` |
| `??` | `src/gametest/java/dev/wildercord/player/ResearchLibraryTest.java` |
| `??` | `src/gametest/java/dev/wildercord/world/dungeons/ExpeditionsTest.java` |
| ` M` | `src/gametest/resources/fabric.mod.json` |
| ` M` | `src/main/java/dev/wildercord/Wildercord.java` |
| `??` | `src/main/java/dev/wildercord/cast/ArmorResponses.java` |
| ` M` | `src/main/java/dev/wildercord/cast/Cast.java` |
| ` M` | `src/main/java/dev/wildercord/cast/CastEngine.java` |
| ` M` | `src/main/java/dev/wildercord/cast/CastLock.java` |
| `??` | `src/main/java/dev/wildercord/cast/DecorationBudget.java` |
| `??` | `src/main/java/dev/wildercord/cast/DefensiveFoci.java` |
| ` M` | `src/main/java/dev/wildercord/cast/DungeonEntities.java` |
| ` M` | `src/main/java/dev/wildercord/cast/Effects.java` |
| `??` | `src/main/java/dev/wildercord/cast/FormationVfx.java` |
| ` M` | `src/main/java/dev/wildercord/cast/FusedEffects.java` |
| ` M` | `src/main/java/dev/wildercord/cast/Fx.java` |
| ` M` | `src/main/java/dev/wildercord/cast/Innates.java` |
| `??` | `src/main/java/dev/wildercord/cast/PhysicalMagic.java` |
| `??` | `src/main/java/dev/wildercord/cast/PracticeRoom.java` |
| `??` | `src/main/java/dev/wildercord/cast/RootGuardian.java` |
| ` M` | `src/main/java/dev/wildercord/cast/Runebound.java` |
| ` M` | `src/main/java/dev/wildercord/cast/RunicAnimations.java` |
| ` M` | `src/main/java/dev/wildercord/cast/SecretSpells.java` |
| ` M` | `src/main/java/dev/wildercord/cast/Sigils.java` |
| ` M` | `src/main/java/dev/wildercord/cast/SignatureFusions.java` |
| `??` | `src/main/java/dev/wildercord/cast/SoulWeaving.java` |
| ` M` | `src/main/java/dev/wildercord/cast/SpellCaster.java` |
| ` M` | `src/main/java/dev/wildercord/cast/SpellDefence.java` |
| `??` | `src/main/java/dev/wildercord/cast/SpellMaterials.java` |
| `??` | `src/main/java/dev/wildercord/cast/SpellTrials.java` |
| ` M` | `src/main/java/dev/wildercord/cast/Statuses.java` |
| `??` | `src/main/java/dev/wildercord/cast/StormConductor.java` |
| ` M` | `src/main/java/dev/wildercord/cast/Techniques.java` |
| ` M` | `src/main/java/dev/wildercord/cast/TemporaryBlocks.java` |
| ` M` | `src/main/java/dev/wildercord/cast/TrainingDummy.java` |
| ` M` | `src/main/java/dev/wildercord/cast/Vfx.java` |
| `??` | `src/main/java/dev/wildercord/cast/VisualMetrics.java` |
| ` M` | `src/main/java/dev/wildercord/cast/WildSurge.java` |
| `??` | `src/main/java/dev/wildercord/cast/events/EventAftermath.java` |
| ` M` | `src/main/java/dev/wildercord/cast/events/FallenStarBlockEntity.java` |
| ` M` | `src/main/java/dev/wildercord/cast/events/FallenStars.java` |
| ` M` | `src/main/java/dev/wildercord/cast/events/ManaStorm.java` |
| ` M` | `src/main/java/dev/wildercord/cast/events/RiftSiege.java` |
| ` M` | `src/main/java/dev/wildercord/cast/events/WorldEvents.java` |
| ` M` | `src/main/java/dev/wildercord/cast/feel/EarthFeels.java` |
| ` M` | `src/main/java/dev/wildercord/cast/feel/Feels.java` |
| ` M` | `src/main/java/dev/wildercord/cast/feel/FireFeels.java` |
| ` M` | `src/main/java/dev/wildercord/cast/feel/FrostFeels.java` |
| ` M` | `src/main/java/dev/wildercord/cast/feel/LifeFeels.java` |
| ` M` | `src/main/java/dev/wildercord/cast/feel/StormFeels.java` |
| ` M` | `src/main/java/dev/wildercord/cast/feel/WindFeels.java` |
| `??` | `src/main/java/dev/wildercord/command/RuneLabCommand.java` |
| ` M` | `src/main/java/dev/wildercord/command/WildercordCommand.java` |
| ` M` | `src/main/java/dev/wildercord/content/ArchiveLecternBlockEntity.java` |
| `??` | `src/main/java/dev/wildercord/content/BlankRuneItem.java` |
| ` M` | `src/main/java/dev/wildercord/content/DungeonRelicItem.java` |
| ` M` | `src/main/java/dev/wildercord/content/ManaCrystalItem.java` |
| `??` | `src/main/java/dev/wildercord/content/MaterialOption.java` |
| `??` | `src/main/java/dev/wildercord/content/PhysicalBlocks.java` |
| `??` | `src/main/java/dev/wildercord/content/RelicCharmItem.java` |
| ` M` | `src/main/java/dev/wildercord/content/RuneItem.java` |
| `??` | `src/main/java/dev/wildercord/content/RunicHearthBlock.java` |
| `??` | `src/main/java/dev/wildercord/content/RunicHearthEntity.java` |
| ` M` | `src/main/java/dev/wildercord/content/SpellScrollItem.java` |
| ` M` | `src/main/java/dev/wildercord/content/WellstoneBlockEntity.java` |
| ` M` | `src/main/java/dev/wildercord/content/WildercordBlocks.java` |
| ` M` | `src/main/java/dev/wildercord/content/WildercordItems.java` |
| ` M` | `src/main/java/dev/wildercord/content/WildercordParticles.java` |
| ` M` | `src/main/java/dev/wildercord/content/dungeons/DungeonAltarBlock.java` |
| ` M` | `src/main/java/dev/wildercord/content/dungeons/DungeonAltarBlockEntity.java` |
| ` M` | `src/main/java/dev/wildercord/content/dungeons/DungeonBlocks.java` |
| `??` | `src/main/java/dev/wildercord/content/dungeons/ExpeditionMechanism.java` |
| `??` | `src/main/java/dev/wildercord/content/dungeons/ExpeditionMechanismEntity.java` |
| ` M` | `src/main/java/dev/wildercord/familiar/FamiliarMagic.java` |
| `??` | `src/main/java/dev/wildercord/familiar/FamiliarRoles.java` |
| `??` | `src/main/java/dev/wildercord/gear/ElementalArmor.java` |
| ` M` | `src/main/java/dev/wildercord/gear/Gear.java` |
| ` M` | `src/main/java/dev/wildercord/gear/GearDef.java` |
| ` M` | `src/main/java/dev/wildercord/menu/FusionAltarMenu.java` |
| ` M` | `src/main/java/dev/wildercord/mixin/LightningRodBlockMixin.java` |
| `??` | `src/main/java/dev/wildercord/net/FormationPayload.java` |
| `??` | `src/main/java/dev/wildercord/net/NotebookPayload.java` |
| ` M` | `src/main/java/dev/wildercord/net/WildercordNetworking.java` |
| `??` | `src/main/java/dev/wildercord/pet/CinnamonCompanion.java` |
| `??` | `src/main/java/dev/wildercord/pet/CinnamonContent.java` |
| `??` | `src/main/java/dev/wildercord/pet/CinnamonDog.java` |
| `??` | `src/main/java/dev/wildercord/pet/CinnamonState.java` |
| `??` | `src/main/java/dev/wildercord/pet/CompanionLanding.java` |
| ` M` | `src/main/java/dev/wildercord/player/Heart.java` |
| `??` | `src/main/java/dev/wildercord/player/RuneResearch.java` |
| `??` | `src/main/java/dev/wildercord/player/SpellLibrary.java` |
| `??` | `src/main/java/dev/wildercord/spell/CircleDisciplines.java` |
| `??` | `src/main/java/dev/wildercord/spell/CircleGeometry.java` |
| `??` | `src/main/java/dev/wildercord/spell/CompiledSpellCache.java` |
| ` M` | `src/main/java/dev/wildercord/spell/Feats.java` |
| ` M` | `src/main/java/dev/wildercord/spell/Fusions.java` |
| ` M` | `src/main/java/dev/wildercord/spell/RuneCategories.java` |
| ` M` | `src/main/java/dev/wildercord/spell/Runes.java` |
| `??` | `src/main/java/dev/wildercord/spell/ShapeFormation.java` |
| ` M` | `src/main/java/dev/wildercord/spell/SpellCompiler.java` |
| ` M` | `src/main/java/dev/wildercord/spell/SpellNumbers.java` |
| ` M` | `src/main/java/dev/wildercord/spell/Trait.java` |
| `??` | `src/main/java/dev/wildercord/spell/VisualElements.java` |
| ` M` | `src/main/java/dev/wildercord/spell/WovenRunes.java` |
| ` M` | `src/main/java/dev/wildercord/travel/TravelFx.java` |
| `??` | `src/main/java/dev/wildercord/world/dungeons/ClockworkCryptPiece.java` |
| ` M` | `src/main/java/dev/wildercord/world/dungeons/DungeonPiece.java` |
| ` M` | `src/main/java/dev/wildercord/world/dungeons/DungeonStructure.java` |
| ` M` | `src/main/java/dev/wildercord/world/dungeons/DungeonWards.java` |
| ` M` | `src/main/java/dev/wildercord/world/dungeons/DungeonWorldgen.java` |
| `??` | `src/main/java/dev/wildercord/world/dungeons/LivingGreenhousePiece.java` |
| `??` | `src/main/java/dev/wildercord/world/dungeons/MovingSkyRuinPiece.java` |
| ` M` | `src/main/java/dev/wildercord/world/dungeons/RootboundMazePiece.java` |
| ` M` | `src/main/java/dev/wildercord/world/dungeons/StormSpirePiece.java` |
| ` M` | `src/main/resources/assets/wildercord/animations/rune_choreography.txt` |
| `??` | `src/main/resources/assets/wildercord/blockstates/cinder_bulwark.json` |
| `??` | `src/main/resources/assets/wildercord/blockstates/clockwork_control.json` |
| ` M` | `src/main/resources/assets/wildercord/blockstates/dungeon_altar.json` |
| `??` | `src/main/resources/assets/wildercord/blockstates/greenhouse_heart.json` |
| `??` | `src/main/resources/assets/wildercord/blockstates/lifted_water.json` |
| `??` | `src/main/resources/assets/wildercord/blockstates/raised_strata.json` |
| `??` | `src/main/resources/assets/wildercord/blockstates/rime_step.json` |
| `??` | `src/main/resources/assets/wildercord/blockstates/root_bulwark.json` |
| `??` | `src/main/resources/assets/wildercord/blockstates/runic_hearth.json` |
| `??` | `src/main/resources/assets/wildercord/blockstates/sky_anchor.json` |
| `??` | `src/main/resources/assets/wildercord/blockstates/thunder_step.json` |
| `??` | `src/main/resources/assets/wildercord/blockstates/water_reservation.json` |
| `??` | `src/main/resources/assets/wildercord/blockstates/wind_step.json` |
| `??` | `src/main/resources/assets/wildercord/equipment/emberweave.json` |
| `??` | `src/main/resources/assets/wildercord/equipment/mirror_thread.json` |
| `??` | `src/main/resources/assets/wildercord/equipment/rimebound.json` |
| `??` | `src/main/resources/assets/wildercord/equipment/stonebound.json` |
| `??` | `src/main/resources/assets/wildercord/items/cinnamon_toy.json` |
| `??` | `src/main/resources/assets/wildercord/items/emberweave_boots.json` |
| `??` | `src/main/resources/assets/wildercord/items/emberweave_chestplate.json` |
| `??` | `src/main/resources/assets/wildercord/items/emberweave_helmet.json` |
| `??` | `src/main/resources/assets/wildercord/items/emberweave_leggings.json` |
| `??` | `src/main/resources/assets/wildercord/items/focus_of_grounding.json` |
| `??` | `src/main/resources/assets/wildercord/items/focus_of_reprieve.json` |
| `??` | `src/main/resources/assets/wildercord/items/keepers_hourglass.json` |
| `??` | `src/main/resources/assets/wildercord/items/living_seedpod.json` |
| `??` | `src/main/resources/assets/wildercord/items/mirror_thread_mantle.json` |
| `??` | `src/main/resources/assets/wildercord/items/rimebound_boots.json` |
| `??` | `src/main/resources/assets/wildercord/items/rimebound_chestplate.json` |
| `??` | `src/main/resources/assets/wildercord/items/rimebound_helmet.json` |
| `??` | `src/main/resources/assets/wildercord/items/rimebound_leggings.json` |
| ` M` | `src/main/resources/assets/wildercord/items/rune.json` |
| `??` | `src/main/resources/assets/wildercord/items/runic_hearth.json` |
| `??` | `src/main/resources/assets/wildercord/items/sky_feather.json` |
| `??` | `src/main/resources/assets/wildercord/items/stonebound_boots.json` |
| `??` | `src/main/resources/assets/wildercord/items/stonebound_chestplate.json` |
| `??` | `src/main/resources/assets/wildercord/items/stonebound_helmet.json` |
| `??` | `src/main/resources/assets/wildercord/items/stonebound_leggings.json` |
| ` M` | `src/main/resources/assets/wildercord/lang/en_us.json` |
| `??` | `src/main/resources/assets/wildercord/models/block/cinder_bulwark.json` |
| `??` | `src/main/resources/assets/wildercord/models/block/clockwork_control_0.json` |
| `??` | `src/main/resources/assets/wildercord/models/block/clockwork_control_1.json` |
| `??` | `src/main/resources/assets/wildercord/models/block/clockwork_control_2.json` |
| `??` | `src/main/resources/assets/wildercord/models/block/clockwork_control_3.json` |
| `??` | `src/main/resources/assets/wildercord/models/block/clockwork_control_4.json` |
| `??` | `src/main/resources/assets/wildercord/models/block/dungeon_altar_root.json` |
| `??` | `src/main/resources/assets/wildercord/models/block/dungeon_altar_storm.json` |
| `??` | `src/main/resources/assets/wildercord/models/block/greenhouse_heart_0.json` |
| `??` | `src/main/resources/assets/wildercord/models/block/greenhouse_heart_1.json` |
| `??` | `src/main/resources/assets/wildercord/models/block/greenhouse_heart_2.json` |
| `??` | `src/main/resources/assets/wildercord/models/block/greenhouse_heart_3.json` |
| `??` | `src/main/resources/assets/wildercord/models/block/greenhouse_heart_4.json` |
| `??` | `src/main/resources/assets/wildercord/models/block/lifted_water.json` |
| `??` | `src/main/resources/assets/wildercord/models/block/raised_strata.json` |
| `??` | `src/main/resources/assets/wildercord/models/block/rime_step.json` |
| `??` | `src/main/resources/assets/wildercord/models/block/root_bulwark.json` |
| `??` | `src/main/resources/assets/wildercord/models/block/runic_hearth_0.json` |
| `??` | `src/main/resources/assets/wildercord/models/block/runic_hearth_1.json` |
| `??` | `src/main/resources/assets/wildercord/models/block/runic_hearth_2.json` |
| `??` | `src/main/resources/assets/wildercord/models/block/runic_hearth_3.json` |
| `??` | `src/main/resources/assets/wildercord/models/block/sky_anchor_0.json` |
| `??` | `src/main/resources/assets/wildercord/models/block/sky_anchor_1.json` |
| `??` | `src/main/resources/assets/wildercord/models/block/sky_anchor_2.json` |
| `??` | `src/main/resources/assets/wildercord/models/block/sky_anchor_3.json` |
| `??` | `src/main/resources/assets/wildercord/models/block/sky_anchor_4.json` |
| `??` | `src/main/resources/assets/wildercord/models/block/thunder_step.json` |
| `??` | `src/main/resources/assets/wildercord/models/block/water_reservation.json` |
| `??` | `src/main/resources/assets/wildercord/models/block/wind_step.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/cinnamon_toy.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/emberweave_boots.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/emberweave_chestplate.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/emberweave_helmet.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/emberweave_leggings.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/focus_of_grounding.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/focus_of_reprieve.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/keepers_hourglass.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/living_seedpod.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/mirror_thread_mantle.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/rimebound_boots.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/rimebound_chestplate.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/rimebound_helmet.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/rimebound_leggings.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/rune/anchor_circle.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/rune/bloom_circle.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/rune/boiling_surge.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/rune/cinder_bulwark.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/rune/confluence_circle.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/rune/crucible_circle.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/rune/eclipse_circle.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/rune/gyre_circle.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/rune/mercy_circle.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/rune/needle_circle.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/rune/pilgrim_circle.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/rune/reservoir_circle.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/rune/rime_causeway.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/rune/root_bulwark.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/rune/strata_rise.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/rune/tempest_circle.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/rune/thunder_tide.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/rune/thunder_walk.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/rune/tidal_lift.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/rune/vigil_circle.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/rune/wind_steps.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/sky_feather.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/stonebound_boots.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/stonebound_chestplate.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/stonebound_helmet.json` |
| `??` | `src/main/resources/assets/wildercord/models/item/stonebound_leggings.json` |
| `??` | `src/main/resources/assets/wildercord/textures/block/cinder_bulwark.png` |
| `??` | `src/main/resources/assets/wildercord/textures/block/clockwork_control_0.png` |
| `??` | `src/main/resources/assets/wildercord/textures/block/clockwork_control_1.png` |
| `??` | `src/main/resources/assets/wildercord/textures/block/clockwork_control_2.png` |
| `??` | `src/main/resources/assets/wildercord/textures/block/clockwork_control_3.png` |
| `??` | `src/main/resources/assets/wildercord/textures/block/clockwork_control_4.png` |
| `??` | `src/main/resources/assets/wildercord/textures/block/clockwork_control_side.png` |
| `??` | `src/main/resources/assets/wildercord/textures/block/greenhouse_heart_0.png` |
| `??` | `src/main/resources/assets/wildercord/textures/block/greenhouse_heart_1.png` |
| `??` | `src/main/resources/assets/wildercord/textures/block/greenhouse_heart_2.png` |
| `??` | `src/main/resources/assets/wildercord/textures/block/greenhouse_heart_3.png` |
| `??` | `src/main/resources/assets/wildercord/textures/block/greenhouse_heart_4.png` |
| `??` | `src/main/resources/assets/wildercord/textures/block/greenhouse_heart_side.png` |
| `??` | `src/main/resources/assets/wildercord/textures/block/lifted_water.png` |
| `??` | `src/main/resources/assets/wildercord/textures/block/raised_strata.png` |
| `??` | `src/main/resources/assets/wildercord/textures/block/rime_step.png` |
| `??` | `src/main/resources/assets/wildercord/textures/block/root_bulwark.png` |
| `??` | `src/main/resources/assets/wildercord/textures/block/runic_hearth_0.png` |
| `??` | `src/main/resources/assets/wildercord/textures/block/runic_hearth_1.png` |
| `??` | `src/main/resources/assets/wildercord/textures/block/runic_hearth_2.png` |
| `??` | `src/main/resources/assets/wildercord/textures/block/runic_hearth_3.png` |
| `??` | `src/main/resources/assets/wildercord/textures/block/runic_hearth_side.png` |
| `??` | `src/main/resources/assets/wildercord/textures/block/sky_anchor_0.png` |
| `??` | `src/main/resources/assets/wildercord/textures/block/sky_anchor_1.png` |
| `??` | `src/main/resources/assets/wildercord/textures/block/sky_anchor_2.png` |
| `??` | `src/main/resources/assets/wildercord/textures/block/sky_anchor_3.png` |
| `??` | `src/main/resources/assets/wildercord/textures/block/sky_anchor_4.png` |
| `??` | `src/main/resources/assets/wildercord/textures/block/sky_anchor_side.png` |
| `??` | `src/main/resources/assets/wildercord/textures/block/thunder_step.png` |
| `??` | `src/main/resources/assets/wildercord/textures/block/wind_step.png` |
| `??` | `src/main/resources/assets/wildercord/textures/entity/cinnamon.png` |
| `??` | `src/main/resources/assets/wildercord/textures/entity/cinnamon_sleeping.png` |
| `??` | `src/main/resources/assets/wildercord/textures/entity/equipment/humanoid/emberweave.png` |
| `??` | `src/main/resources/assets/wildercord/textures/entity/equipment/humanoid/mirror_thread.png` |
| `??` | `src/main/resources/assets/wildercord/textures/entity/equipment/humanoid/rimebound.png` |
| `??` | `src/main/resources/assets/wildercord/textures/entity/equipment/humanoid/stonebound.png` |
| `??` | `src/main/resources/assets/wildercord/textures/entity/equipment/humanoid_leggings/emberweave.png` |
| `??` | `src/main/resources/assets/wildercord/textures/entity/equipment/humanoid_leggings/mirror_thread.png` |
| `??` | `src/main/resources/assets/wildercord/textures/entity/equipment/humanoid_leggings/rimebound.png` |
| `??` | `src/main/resources/assets/wildercord/textures/entity/equipment/humanoid_leggings/stonebound.png` |
| `??` | `src/main/resources/assets/wildercord/textures/entity/root_guardian.png` |
| `??` | `src/main/resources/assets/wildercord/textures/entity/root_guardian_glow.png` |
| `??` | `src/main/resources/assets/wildercord/textures/entity/storm_conductor.png` |
| `??` | `src/main/resources/assets/wildercord/textures/entity/storm_conductor_glow.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/cinnamon_toy.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/emberweave_boots.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/emberweave_chestplate.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/emberweave_helmet.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/emberweave_leggings.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/focus_of_grounding.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/focus_of_reprieve.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/keepers_hourglass.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/living_seedpod.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/mirror_thread_mantle.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/rimebound_boots.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/rimebound_chestplate.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/rimebound_helmet.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/rimebound_leggings.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/rune/anchor_circle.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/rune/bloom_circle.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/rune/boiling_surge.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/rune/boiling_surge.png.mcmeta` |
| `??` | `src/main/resources/assets/wildercord/textures/item/rune/cinder_bulwark.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/rune/cinder_bulwark.png.mcmeta` |
| `??` | `src/main/resources/assets/wildercord/textures/item/rune/confluence_circle.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/rune/confluence_circle.png.mcmeta` |
| `??` | `src/main/resources/assets/wildercord/textures/item/rune/crucible_circle.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/rune/eclipse_circle.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/rune/eclipse_circle.png.mcmeta` |
| `??` | `src/main/resources/assets/wildercord/textures/item/rune/gyre_circle.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/rune/mercy_circle.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/rune/needle_circle.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/rune/pilgrim_circle.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/rune/reservoir_circle.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/rune/rime_causeway.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/rune/rime_causeway.png.mcmeta` |
| `??` | `src/main/resources/assets/wildercord/textures/item/rune/root_bulwark.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/rune/root_bulwark.png.mcmeta` |
| `??` | `src/main/resources/assets/wildercord/textures/item/rune/strata_rise.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/rune/tempest_circle.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/rune/tempest_circle.png.mcmeta` |
| `??` | `src/main/resources/assets/wildercord/textures/item/rune/thunder_tide.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/rune/thunder_tide.png.mcmeta` |
| `??` | `src/main/resources/assets/wildercord/textures/item/rune/thunder_walk.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/rune/thunder_walk.png.mcmeta` |
| `??` | `src/main/resources/assets/wildercord/textures/item/rune/tidal_lift.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/rune/vigil_circle.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/rune/wind_steps.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/sky_feather.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/stonebound_boots.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/stonebound_chestplate.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/stonebound_helmet.png` |
| `??` | `src/main/resources/assets/wildercord/textures/item/stonebound_leggings.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/anchor_circle_band.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/anchor_circle_mark.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/bloom_circle_band.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/bloom_circle_mark.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/boiling_surge_band.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/boiling_surge_band2.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/boiling_surge_mark.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/boiling_surge_mark2.png` |
| ` M` | `src/main/resources/assets/wildercord/textures/particle/circle/bounce_band.png` |
| ` M` | `src/main/resources/assets/wildercord/textures/particle/circle/chain_band.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/cinder_bulwark_band.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/cinder_bulwark_band2.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/cinder_bulwark_mark.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/cinder_bulwark_mark2.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/confluence_circle_band.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/confluence_circle_mark.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/crucible_circle_band.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/crucible_circle_mark.png` |
| ` M` | `src/main/resources/assets/wildercord/textures/particle/circle/drowning_word_mark.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/eclipse_circle_band.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/eclipse_circle_mark.png` |
| ` M` | `src/main/resources/assets/wildercord/textures/particle/circle/extend_mark.png` |
| ` M` | `src/main/resources/assets/wildercord/textures/particle/circle/flash_freeze_mark.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/gyre_circle_band.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/gyre_circle_mark.png` |
| ` M` | `src/main/resources/assets/wildercord/textures/particle/circle/hoarfrost_mark.png` |
| ` M` | `src/main/resources/assets/wildercord/textures/particle/circle/homing_band.png` |
| ` M` | `src/main/resources/assets/wildercord/textures/particle/circle/kindled_band.png` |
| ` M` | `src/main/resources/assets/wildercord/textures/particle/circle/kindled_mark.png` |
| ` M` | `src/main/resources/assets/wildercord/textures/particle/circle/linger_mark.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/mercy_circle_band.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/mercy_circle_mark.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/needle_circle_band.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/needle_circle_mark.png` |
| ` M` | `src/main/resources/assets/wildercord/textures/particle/circle/pierce_band.png` |
| ` M` | `src/main/resources/assets/wildercord/textures/particle/circle/pierce_mark.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/pilgrim_circle_band.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/pilgrim_circle_mark.png` |
| ` M` | `src/main/resources/assets/wildercord/textures/particle/circle/prospect_mark.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/reservoir_circle_band.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/reservoir_circle_mark.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/rime_causeway_band.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/rime_causeway_band2.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/rime_causeway_mark.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/rime_causeway_mark2.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/root_bulwark_band.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/root_bulwark_band2.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/root_bulwark_mark.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/root_bulwark_mark2.png` |
| ` M` | `src/main/resources/assets/wildercord/textures/particle/circle/sandstorm_mark.png` |
| ` M` | `src/main/resources/assets/wildercord/textures/particle/circle/stalactite_mark.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/strata_rise_band.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/strata_rise_mark.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/tempest_circle_band.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/tempest_circle_mark.png` |
| ` M` | `src/main/resources/assets/wildercord/textures/particle/circle/thirst_band.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/thunder_tide_band.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/thunder_tide_band2.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/thunder_tide_mark.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/thunder_tide_mark2.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/thunder_walk_band.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/thunder_walk_band2.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/thunder_walk_mark.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/thunder_walk_mark2.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/tidal_lift_band.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/tidal_lift_mark.png` |
| ` M` | `src/main/resources/assets/wildercord/textures/particle/circle/tidecall_mark.png` |
| ` M` | `src/main/resources/assets/wildercord/textures/particle/circle/tidewrit_mark.png` |
| ` M` | `src/main/resources/assets/wildercord/textures/particle/circle/tusk_charge_mark.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/vigil_circle_band.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/vigil_circle_mark.png` |
| ` M` | `src/main/resources/assets/wildercord/textures/particle/circle/volley_band.png` |
| ` M` | `src/main/resources/assets/wildercord/textures/particle/circle/widen_band.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/wind_steps_band.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/circle/wind_steps_mark.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/material_0_0.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/material_0_1.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/material_10_0.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/material_10_1.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/material_11_0.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/material_11_1.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/material_1_0.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/material_1_1.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/material_2_0.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/material_2_1.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/material_3_0.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/material_3_1.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/material_4_0.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/material_4_1.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/material_5_0.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/material_5_1.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/material_6_0.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/material_6_1.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/material_7_0.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/material_7_1.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/material_8_0.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/material_8_1.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/material_9_0.png` |
| `??` | `src/main/resources/assets/wildercord/textures/particle/material_9_1.png` |
| `??` | `src/main/resources/data/minecraft/tags/item/chest_armor.json` |
| `??` | `src/main/resources/data/minecraft/tags/item/foot_armor.json` |
| `??` | `src/main/resources/data/minecraft/tags/item/head_armor.json` |
| `??` | `src/main/resources/data/minecraft/tags/item/leg_armor.json` |
| `??` | `src/main/resources/data/wildercord/advancement/recipes/misc/cinnamon_toy.json` |
| `??` | `src/main/resources/data/wildercord/advancement/recipes/misc/emberweave_boots.json` |
| `??` | `src/main/resources/data/wildercord/advancement/recipes/misc/emberweave_chestplate.json` |
| `??` | `src/main/resources/data/wildercord/advancement/recipes/misc/emberweave_helmet.json` |
| `??` | `src/main/resources/data/wildercord/advancement/recipes/misc/emberweave_leggings.json` |
| `??` | `src/main/resources/data/wildercord/advancement/recipes/misc/focus_of_grounding.json` |
| `??` | `src/main/resources/data/wildercord/advancement/recipes/misc/focus_of_reprieve.json` |
| `??` | `src/main/resources/data/wildercord/advancement/recipes/misc/mirror_thread_mantle.json` |
| `??` | `src/main/resources/data/wildercord/advancement/recipes/misc/rimebound_boots.json` |
| `??` | `src/main/resources/data/wildercord/advancement/recipes/misc/rimebound_chestplate.json` |
| `??` | `src/main/resources/data/wildercord/advancement/recipes/misc/rimebound_helmet.json` |
| `??` | `src/main/resources/data/wildercord/advancement/recipes/misc/rimebound_leggings.json` |
| `??` | `src/main/resources/data/wildercord/advancement/recipes/misc/rune_anchor_circle.json` |
| `??` | `src/main/resources/data/wildercord/advancement/recipes/misc/rune_bloom_circle.json` |
| `??` | `src/main/resources/data/wildercord/advancement/recipes/misc/rune_confluence_circle.json` |
| `??` | `src/main/resources/data/wildercord/advancement/recipes/misc/rune_crucible_circle.json` |
| `??` | `src/main/resources/data/wildercord/advancement/recipes/misc/rune_eclipse_circle.json` |
| `??` | `src/main/resources/data/wildercord/advancement/recipes/misc/rune_gyre_circle.json` |
| `??` | `src/main/resources/data/wildercord/advancement/recipes/misc/rune_mercy_circle.json` |
| `??` | `src/main/resources/data/wildercord/advancement/recipes/misc/rune_needle_circle.json` |
| `??` | `src/main/resources/data/wildercord/advancement/recipes/misc/rune_pilgrim_circle.json` |
| `??` | `src/main/resources/data/wildercord/advancement/recipes/misc/rune_reservoir_circle.json` |
| `??` | `src/main/resources/data/wildercord/advancement/recipes/misc/rune_strata_rise.json` |
| `??` | `src/main/resources/data/wildercord/advancement/recipes/misc/rune_tempest_circle.json` |
| `??` | `src/main/resources/data/wildercord/advancement/recipes/misc/rune_tidal_lift.json` |
| `??` | `src/main/resources/data/wildercord/advancement/recipes/misc/rune_vigil_circle.json` |
| `??` | `src/main/resources/data/wildercord/advancement/recipes/misc/rune_wind_steps.json` |
| `??` | `src/main/resources/data/wildercord/advancement/recipes/misc/runic_hearth.json` |
| `??` | `src/main/resources/data/wildercord/advancement/recipes/misc/stonebound_boots.json` |
| `??` | `src/main/resources/data/wildercord/advancement/recipes/misc/stonebound_chestplate.json` |
| `??` | `src/main/resources/data/wildercord/advancement/recipes/misc/stonebound_helmet.json` |
| `??` | `src/main/resources/data/wildercord/advancement/recipes/misc/stonebound_leggings.json` |
| `??` | `src/main/resources/data/wildercord/advancement/world/root_guardian.json` |
| `??` | `src/main/resources/data/wildercord/advancement/world/storm_conductor.json` |
| `??` | `src/main/resources/data/wildercord/dimension/practice.json` |
| `??` | `src/main/resources/data/wildercord/loot_table/blocks/runic_hearth.json` |
| ` M` | `src/main/resources/data/wildercord/loot_table/chests/archive_library.json` |
| ` M` | `src/main/resources/data/wildercord/loot_table/chests/archive_vault.json` |
| `??` | `src/main/resources/data/wildercord/loot_table/chests/clockwork_crypt_hall.json` |
| `??` | `src/main/resources/data/wildercord/loot_table/chests/clockwork_crypt_vault.json` |
| ` M` | `src/main/resources/data/wildercord/loot_table/chests/drowned_scriptorium_hall.json` |
| ` M` | `src/main/resources/data/wildercord/loot_table/chests/ember_sanctum_hall.json` |
| `??` | `src/main/resources/data/wildercord/loot_table/chests/living_greenhouse_hall.json` |
| `??` | `src/main/resources/data/wildercord/loot_table/chests/living_greenhouse_vault.json` |
| `??` | `src/main/resources/data/wildercord/loot_table/chests/moving_sky_ruin_hall.json` |
| `??` | `src/main/resources/data/wildercord/loot_table/chests/moving_sky_ruin_vault.json` |
| ` M` | `src/main/resources/data/wildercord/loot_table/chests/rootbound_maze_hall.json` |
| ` M` | `src/main/resources/data/wildercord/loot_table/chests/rootbound_maze_vault.json` |
| ` M` | `src/main/resources/data/wildercord/loot_table/chests/storm_spire_hall.json` |
| ` M` | `src/main/resources/data/wildercord/loot_table/chests/storm_spire_vault.json` |
| ` M` | `src/main/resources/data/wildercord/loot_table/entities/cinder_warden.json` |
| `??` | `src/main/resources/data/wildercord/loot_table/entities/root_guardian.json` |
| `??` | `src/main/resources/data/wildercord/loot_table/entities/storm_conductor.json` |
| ` M` | `src/main/resources/data/wildercord/loot_table/entities/tide_scribe.json` |
| `??` | `src/main/resources/data/wildercord/recipe/cinnamon_toy.json` |
| `??` | `src/main/resources/data/wildercord/recipe/emberweave_boots.json` |
| `??` | `src/main/resources/data/wildercord/recipe/emberweave_chestplate.json` |
| `??` | `src/main/resources/data/wildercord/recipe/emberweave_helmet.json` |
| `??` | `src/main/resources/data/wildercord/recipe/emberweave_leggings.json` |
| `??` | `src/main/resources/data/wildercord/recipe/focus_of_grounding.json` |
| `??` | `src/main/resources/data/wildercord/recipe/focus_of_reprieve.json` |
| `??` | `src/main/resources/data/wildercord/recipe/mirror_thread_mantle.json` |
| `??` | `src/main/resources/data/wildercord/recipe/rimebound_boots.json` |
| `??` | `src/main/resources/data/wildercord/recipe/rimebound_chestplate.json` |
| `??` | `src/main/resources/data/wildercord/recipe/rimebound_helmet.json` |
| `??` | `src/main/resources/data/wildercord/recipe/rimebound_leggings.json` |
| `??` | `src/main/resources/data/wildercord/recipe/rune_anchor_circle.json` |
| `??` | `src/main/resources/data/wildercord/recipe/rune_bloom_circle.json` |
| `??` | `src/main/resources/data/wildercord/recipe/rune_confluence_circle.json` |
| `??` | `src/main/resources/data/wildercord/recipe/rune_crucible_circle.json` |
| `??` | `src/main/resources/data/wildercord/recipe/rune_eclipse_circle.json` |
| `??` | `src/main/resources/data/wildercord/recipe/rune_gyre_circle.json` |
| `??` | `src/main/resources/data/wildercord/recipe/rune_mercy_circle.json` |
| `??` | `src/main/resources/data/wildercord/recipe/rune_needle_circle.json` |
| `??` | `src/main/resources/data/wildercord/recipe/rune_pilgrim_circle.json` |
| `??` | `src/main/resources/data/wildercord/recipe/rune_reservoir_circle.json` |
| `??` | `src/main/resources/data/wildercord/recipe/rune_strata_rise.json` |
| `??` | `src/main/resources/data/wildercord/recipe/rune_tempest_circle.json` |
| `??` | `src/main/resources/data/wildercord/recipe/rune_tidal_lift.json` |
| `??` | `src/main/resources/data/wildercord/recipe/rune_vigil_circle.json` |
| `??` | `src/main/resources/data/wildercord/recipe/rune_wind_steps.json` |
| `??` | `src/main/resources/data/wildercord/recipe/runic_hearth.json` |
| `??` | `src/main/resources/data/wildercord/recipe/stonebound_boots.json` |
| `??` | `src/main/resources/data/wildercord/recipe/stonebound_chestplate.json` |
| `??` | `src/main/resources/data/wildercord/recipe/stonebound_helmet.json` |
| `??` | `src/main/resources/data/wildercord/recipe/stonebound_leggings.json` |
| `??` | `src/main/resources/data/wildercord/tags/worldgen/biome/has_structure/clockwork_crypt.json` |
| `??` | `src/main/resources/data/wildercord/tags/worldgen/biome/has_structure/living_greenhouse.json` |
| `??` | `src/main/resources/data/wildercord/tags/worldgen/biome/has_structure/moving_sky_ruin.json` |
| ` M` | `src/main/resources/data/wildercord/tags/worldgen/structure/dungeon.json` |
| `??` | `src/main/resources/data/wildercord/worldgen/structure/clockwork_crypt.json` |
| `??` | `src/main/resources/data/wildercord/worldgen/structure/living_greenhouse.json` |
| `??` | `src/main/resources/data/wildercord/worldgen/structure/moving_sky_ruin.json` |
| `??` | `src/main/resources/data/wildercord/worldgen/structure_set/clockwork_crypt.json` |
| `??` | `src/main/resources/data/wildercord/worldgen/structure_set/living_greenhouse.json` |
| `??` | `src/main/resources/data/wildercord/worldgen/structure_set/moving_sky_ruin.json` |
| ` M` | `src/main/resources/fabric.mod.json` |
| ` M` | `src/test/java/dev/wildercord/DataFormatTest.java` |
| ` M` | `src/test/java/dev/wildercord/gear/GearBonusesTest.java` |
| `??` | `src/test/java/dev/wildercord/spell/CircleDisciplinesTest.java` |
| `??` | `src/test/java/dev/wildercord/spell/CompiledSpellCacheTest.java` |
| `??` | `src/test/java/dev/wildercord/spell/EveryRuneCompilationTest.java` |
| ` M` | `src/test/java/dev/wildercord/spell/FusionTest.java` |
| `??` | `src/test/java/dev/wildercord/spell/MultiWeaveTest.java` |
| ` M` | `src/test/java/dev/wildercord/spell/RuneChoreographyTest.java` |
| `??` | `src/test/java/dev/wildercord/spell/VisualElementsTest.java` |
| `??` | `tools/armor_art.py` |
| `??` | `tools/build_profiles.py` |
| `??` | `tools/capture_profile.ps1` |
| `??` | `tools/cinnamon_art.py` |
| ` M` | `tools/circle_art.py` |
| `??` | `tools/circle_discipline_art.py` |
| ` M` | `tools/dungeon_assets.py` |
| `??` | `tools/encounter_art.py` |
| `??` | `tools/expedition_art.py` |
| ` M` | `tools/gear_art.py` |
| ` M` | `tools/generate_assets.py` |
| `??` | `tools/hearth_art.py` |
| ` M` | `tools/item_art.py` |
| `??` | `tools/material_art.py` |
| `??` | `tools/physical_art.py` |
| `??` | `tools/relic_art.py` |
| `??` | `tools/review_gallery.py` |
| `??` | `tools/test_manifest.py` |
| `??` | `tools/working_tree_inventory.py` |
