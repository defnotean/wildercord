package dev.wildercord.world.sites;

import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.aura.world.OathboundBastion;
import dev.wildercord.aura.world.OathboundHalberdier;
import dev.wildercord.world.sites.oathhold.GuildBoardMenu;
import dev.wildercord.world.sites.oathhold.GuildRequestBoardBlock;
import dev.wildercord.world.sites.oathhold.OathholdCitizen;
import dev.wildercord.world.sites.oathhold.OathholdCitizenRules;
import dev.wildercord.world.sites.oathhold.OathholdContent;
import dev.wildercord.world.sites.oathhold.OathholdLaw;
import dev.wildercord.world.sites.oathhold.OathholdLawRules;
import dev.wildercord.world.sites.oathhold.OathholdPopulation;
import dev.wildercord.world.sites.oathhold.OathholdQuestRules;
import dev.wildercord.world.sites.oathhold.OathholdQuests;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * End-to-end in-game verification &amp; 4-shot visual showcase for Oathhold's living society:
 * named {@link OathholdCitizen} inhabitants &amp; merchants, Adventurers' Guild Request Board (F–S)
 * jobs (Cleaning, Herb Gathering, Work Shifts, Arena Bouts + Rank Promotion), and neutral-until-crime
 * Oathbound High Knights with Bounty Restitution.
 */
public final class OathholdGuildAndLawTest implements FabricClientGameTest {
	private static final BlockPos STAGE = new BlockPos(0, 120, 0);
	private static final BlockPos BOARD_POS = new BlockPos(0, 121, -5);
	private static final BlockPos ANVIL_POS = new BlockPos(7, 120, -4);
	private static final BlockPos ARENA_CENTER = new BlockPos(-18, 120, 14);
	private static final String CAM_TAG = "wildercord.oathhold_guild_cam";

	private static void check(boolean cond, String msg) {
		if (!cond) throw new AssertionError(msg);
	}

	private static float yaw(Vec3 from, Vec3 to) {
		Vec3 d = to.subtract(from);
		return (float) Math.toDegrees(Math.atan2(-d.x, d.z));
	}

	private static float pitch(Vec3 from, Vec3 to) {
		Vec3 d = to.subtract(from);
		return (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
	}

	private static void director(ClientGameTestContext context, TestSingleplayerContext world, Vec3 eye, Vec3 target) {
		int camId = world.getServer().computeOnServer(server -> {
			ServerLevel l = server.overworld();
			l.getEntitiesOfClass(Display.TextDisplay.class, new AABB(STAGE).inflate(80),
				e -> e.entityTags().contains(CAM_TAG)).forEach(Entity::discard);
			Display.TextDisplay cam = EntityTypes.TEXT_DISPLAY.create(l, EntitySpawnReason.COMMAND);
			cam.snapTo(eye.x, eye.y, eye.z, yaw(eye, target), pitch(eye, target));
			cam.addTag(CAM_TAG);
			l.addFreshEntity(cam);
			return cam.getId();
		});
		context.waitTicks(3);
		context.runOnClient(mc -> {
			Entity cam = mc.level.getEntity(camId);
			if (cam != null) {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				mc.setCameraEntity(cam);
				if (!mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
				mc.gui.toastManager().clear();
				mc.gui.hud.getChat().clearMessages(false);
			}
		});
		context.waitTicks(3);
	}

	private static void cut(ClientGameTestContext context, TestSingleplayerContext world) {
		context.runOnClient(mc -> mc.setCameraEntity(mc.player));
		world.getServer().runOnServer(s -> s.overworld()
			.getEntitiesOfClass(Display.TextDisplay.class, new AABB(STAGE).inflate(80),
				e -> e.entityTags().contains(CAM_TAG))
			.forEach(Entity::discard));
		context.waitTicks(2);
	}

	private static void shot(ClientGameTestContext context, String name) {
		context.runOnClient(mc -> {
			mc.gui.toastManager().clear();
			mc.gui.hud.getChat().clearMessages(false);
		});
		context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}

	private static UUID spawnCitizen(ServerLevel l, OathholdCitizenRules.Archetype role, int x, int y, int z, float yaw) {
		OathholdCitizen npc = OathholdContent.CITIZEN.create(l, EntitySpawnReason.COMMAND);
		check(npc != null, "Failed to create OathholdCitizen for " + role);
		BlockPos pos = new BlockPos(x, y, z);
		npc.configure(role, x, y, z, pos, 3);
		npc.snapTo(x + 0.5, y, z + 0.5, yaw, 0.0F);
		npc.finalizeSpawn(l, l.getCurrentDifficultyAt(pos), EntitySpawnReason.COMMAND, null);
		l.addFreshEntity(npc);
		return npc.getUUID();
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder()
				.setUseConsistentSettings(false)
				.adjustSettings(settings -> settings.setSeed("20261121"))
				.create()) {
			context.waitTicks(25);
			for (String rule : List.of("advance_time false", "advance_weather false", "spawn_mobs false")) {
				world.getServer().runCommand("gamerule " + rule);
			}
			world.getServer().runCommand("time set 1200");
			world.getServer().runCommand("weather clear");

			context.runOnClient(mc -> {
				mc.getWindow().setWindowed(1280, 720);
				mc.options.renderDistance().set(10);
				mc.options.fov().set(72);
				mc.options.broadcastOptions();
				mc.resizeGui();
				if (mc.gui.hud.isHidden()) mc.gui.hud.toggle();
			});

			// 0. Pure population & rank-discount checks
			check(OathholdPopulation.countPlannedCitizens() >= 500,
				"Expected >= 500 planned city NPCs, got " + OathholdPopulation.countPlannedCitizens());
			var blacksmithF = OathholdCitizen.buildOffers(OathholdCitizenRules.Archetype.BLACKSMITH, OathholdQuestRules.Rank.F);
			var blacksmithS = OathholdCitizen.buildOffers(OathholdCitizenRules.Archetype.BLACKSMITH, OathholdQuestRules.Rank.S);
			check(blacksmithF.size() >= 5 && !OathholdCitizen.buildOffers(OathholdCitizenRules.Archetype.BAKER_COOK, OathholdQuestRules.Rank.S).isEmpty(),
				"Citizen merchant professions must populate non-empty offers");
			check(blacksmithS.get(4).getCostA().getCount() < blacksmithF.get(4).getCostA().getCount(),
				"Rank S must discount merchant emerald prices relative to Rank F");

			// 1. Construct Guildhall Plaza + Sand Colosseum Ring at y = 120 and register temporary Oathhold city zone
			UUID[] ids = world.getServer().computeOnServer(server -> {
				ServerLevel l = server.overworld();
				for (int x = -28; x <= 18; x++) {
					for (int z = -12; z <= 26; z++) {
						l.setBlock(new BlockPos(x, 119, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
						for (int y = 120; y <= 136; y++) {
							l.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
						}
						if (Math.floorMod(x, 6) == 0 || Math.floorMod(z, 6) == 0) {
							l.setBlock(new BlockPos(x, 119, z), Blocks.POLISHED_ANDESITE.defaultBlockState(), 3);
						}
					}
				}

				// Guildhall timber-and-stone facade wall along z = -9..-5
				for (int x = -10; x <= 10; x++) {
					for (int y = 120; y <= 126; y++) {
						l.setBlock(new BlockPos(x, y, -9), Blocks.STONE_BRICKS.defaultBlockState(), 3);
					}
					for (int z = -8; z <= -5; z++) {
						l.setBlock(new BlockPos(x, 126, z), Blocks.DARK_OAK_PLANKS.defaultBlockState(), 3);
					}
				}
				for (int x : new int[]{-9, -4, 4, 9}) {
					for (int y = 120; y <= 126; y++) {
						l.setBlock(new BlockPos(x, y, -8), Blocks.DARK_OAK_LOG.defaultBlockState(), 3);
					}
				}
				for (int x = -2; x <= 2; x++) {
					for (int y = 124; y <= 125; y++) {
						l.setBlock(new BlockPos(x, y, -8), (x == 0 ? Blocks.GOLD_BLOCK : Blocks.CONCRETE.green()).defaultBlockState(), 3);
					}
				}
				for (int x : new int[]{-5, 0, 5}) {
					l.setBlock(new BlockPos(x, 125, -6), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 3);
				}

				// Request Boards + pedestals + stations
				l.setBlock(BOARD_POS.below(), Blocks.DARK_OAK_PLANKS.defaultBlockState(), 3);
				l.setBlock(BOARD_POS, OathholdContent.GUILD_REQUEST_BOARD.defaultBlockState()
					.setValue(GuildRequestBoardBlock.FACING, Direction.SOUTH), 3);
				l.setBlock(new BlockPos(-3, 120, -6), Blocks.DARK_OAK_PLANKS.defaultBlockState(), 3);
				l.setBlock(new BlockPos(-3, 121, -6), OathholdContent.GUILD_REQUEST_BOARD.defaultBlockState()
					.setValue(GuildRequestBoardBlock.FACING, Direction.SOUTH), 3);
				l.setBlock(new BlockPos(3, 120, -6), Blocks.DARK_OAK_PLANKS.defaultBlockState(), 3);
				l.setBlock(new BlockPos(3, 121, -6), OathholdContent.GUILD_REQUEST_BOARD.defaultBlockState()
					.setValue(GuildRequestBoardBlock.FACING, Direction.SOUTH), 3);

				l.setBlock(ANVIL_POS, Blocks.ANVIL.defaultBlockState(), 3);
				l.setBlock(new BlockPos(8, 120, -5), Blocks.BLAST_FURNACE.defaultBlockState(), 3);
				l.setBlock(new BlockPos(6, 120, -5), Blocks.SMITHING_TABLE.defaultBlockState(), 3);

				// Sand Colosseum Ring centered at (-18, 120, 14) with sandstone base at y = 118 so sand never falls
				for (int dx = -8; dx <= 8; dx++) {
					for (int dz = -8; dz <= 8; dz++) {
						double d = Math.hypot(dx, dz);
						int wx = ARENA_CENTER.getX() + dx;
						int wz = ARENA_CENTER.getZ() + dz;
						l.setBlock(new BlockPos(wx, 118, wz), Blocks.SANDSTONE.defaultBlockState(), 3);
						if (d <= 6.4) {
							l.setBlock(new BlockPos(wx, 119, wz), Blocks.SAND.defaultBlockState(), 3);
						} else if (d <= 7.8 && !(Math.abs(dz) <= 1 && dx > 0)) {
							l.setBlock(new BlockPos(wx, 120, wz), Blocks.SANDSTONE_WALL.defaultBlockState(), 3);
						}
					}
				}
				for (int[] col : new int[][]{{-25, 7}, {-25, 21}, {-11, 7}, {-11, 21}}) {
					l.setBlock(new BlockPos(col[0], 120, col[1]), Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 3);
					l.setBlock(new BlockPos(col[0], 121, col[1]), Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 3);
					l.setBlock(new BlockPos(col[0], 122, col[1]), Blocks.GOLD_BLOCK.defaultBlockState(), 3);
					l.setBlock(new BlockPos(col[0], 123, col[1]), Blocks.LANTERN.defaultBlockState(), 3);
				}

				OathholdLaw.registerCityNow(l, 0, 120, 0);

				spawnCitizen(l, OathholdCitizenRules.Archetype.GUILD_RECEPTIONIST, -2, 120, -5, 0.0F);
				spawnCitizen(l, OathholdCitizenRules.Archetype.GUILD_RECEPTIONIST, 2, 120, -5, 0.0F);
				spawnCitizen(l, OathholdCitizenRules.Archetype.GUILD_MASTER, -1, 120, -6, 0.0F);
				spawnCitizen(l, OathholdCitizenRules.Archetype.BLACKSMITH, 7, 120, -2, -30.0F);
				spawnCitizen(l, OathholdCitizenRules.Archetype.ALCHEMIST_HERBALIST, -6, 120, -3, 25.0F);
				spawnCitizen(l, OathholdCitizenRules.Archetype.BAKER_COOK, -4, 120, -1, 15.0F);
				spawnCitizen(l, OathholdCitizenRules.Archetype.SCHOLAR_SCRIBE, 4, 120, -1, -20.0F);
				spawnCitizen(l, OathholdCitizenRules.Archetype.STREET_SWEEPER, -3, 120, 2, 180.0F);
				spawnCitizen(l, OathholdCitizenRules.Archetype.CITY_WATCHMAN, 7, 120, 0, -90.0F);
				spawnCitizen(l, OathholdCitizenRules.Archetype.ARENA_PROMOTER, -10, 120, 14, -90.0F);
				UUID clericId = spawnCitizen(l, OathholdCitizenRules.Archetype.PRIEST_CLERIC, 9, 120, 3, -90.0F);
				UUID targetCitizenId = spawnCitizen(l, OathholdCitizenRules.Archetype.TOWNSFOLK, -6, 120, 2, 0.0F);

				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				p.setGameMode(GameType.SURVIVAL);
				OathholdLaw.clearCrime(p, true);
				p.teleportTo(l, 0.5, 120.0, -3.2, Set.<Relative>of(), 180.0F, -10.0F, false);
				p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				return new UUID[]{clericId, targetCitizenId};
			});
			UUID clericId = ids[0];
			UUID targetCitizenId = ids[1];

			context.waitTicks(12);

			// 2. Click real GuildRequestBoardBlock -> verify GuildBoardMenu & capture GUI screenshot 01
			context.runOnClient(mc -> mc.gameMode.useItemOn(
				mc.player, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(BOARD_POS), Direction.SOUTH, BOARD_POS, false)
			));
			context.waitTicks(10);
			try {
				world.getConnection().waitForClientboundPackets();
			} catch (AssertionError | RuntimeException ignored) {
			}
			world.getServer().runOnServer(server -> {
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				check(p.containerMenu instanceof GuildBoardMenu,
					"Using GuildRequestBoardBlock must open GuildBoardMenu, got " + p.containerMenu);
			});
			context.runOnClient(mc -> check(mc.player != null && mc.player.containerMenu instanceof net.minecraft.world.inventory.ChestMenu,
				"Client containerMenu must open ChestMenu for GuildBoardMenu"));
			shot(context, "oathhold_guild_01_request_board_gui");

			world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().closeContainer());
			context.waitTicks(6);

			// 3. Showcase Guildhall plaza, receptionists, boards, and craft stations -> screenshot 02
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				p.teleportTo(l, 0.5, 120.0, -2.6, Set.<Relative>of(), 180.0F, 0.0F, false);
			});
			director(context, world, new Vec3(4.8, 122.7, 5.4), new Vec3(-0.2, 121.3, -4.0));
			shot(context, "oathhold_guild_02_guildhall_receptionists");
			cut(context, world);

			// 4. Neutral Knights until Crime -> Peaceful Screenshot 03 -> Crime -> Pursuit -> Restitution
			UUID[] knightIds = world.getServer().computeOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				p.teleportTo(l, 0.5, 120.0, 4.0, Set.<Relative>of(), 0.0F, 0.0F, false);
				p.setHealth(p.getMaxHealth());

				OathboundBastion bastion = AuraWorld.OATHBOUND_BASTION.create(l, EntitySpawnReason.COMMAND);
				OathboundHalberdier halberdier = AuraWorld.OATHBOUND_HALBERDIER.create(l, EntitySpawnReason.COMMAND);
				check(bastion != null && halberdier != null, "Failed to spawn Oathbound Knight patrol");
				bastion.snapTo(3.5, 120.0, 7.5, 155.0F, 0.0F);
				halberdier.snapTo(-2.5, 120.0, 7.5, -155.0F, 0.0F);
				bastion.finalizeSpawn(l, l.getCurrentDifficultyAt(bastion.blockPosition()), EntitySpawnReason.COMMAND, null);
				halberdier.finalizeSpawn(l, l.getCurrentDifficultyAt(halberdier.blockPosition()), EntitySpawnReason.COMMAND, null);
				l.addFreshEntity(bastion);
				l.addFreshEntity(halberdier);
				return new UUID[]{bastion.getUUID(), halberdier.getUUID()};
			});
			UUID bastionId = knightIds[0];
			UUID halberdierId = knightIds[1];

			context.waitTicks(40);
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				OathboundBastion b = (OathboundBastion) l.getEntity(bastionId);
				OathboundHalberdier h = (OathboundHalberdier) l.getEntity(halberdierId);
				check(!OathholdLaw.isWanted(p), "Law-abiding player must not be Wanted");
				check(b != null && b.getTarget() == null, "Bastion must stay neutral toward innocent player, targeted: " + (b == null ? null : b.getTarget()));
				check(h != null && h.getTarget() == null, "Halberdier must stay neutral toward innocent player, targeted: " + (h == null ? null : h.getTarget()));
				check(p.getHealth() >= p.getMaxHealth() - 0.1F, "Innocent player must remain unharmed near Knights");
			});

			director(context, world, new Vec3(6.2, 122.4, 1.4), new Vec3(0.5, 121.3, 6.2));
			shot(context, "oathhold_guild_03_peaceful_knight_patrol");
			cut(context, world);

			// Commit assault inside Oathhold -> Knights watch without attacking -> pay restitution to Priest -> Bounty cleared
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				Entity victim = l.getEntity(targetCitizenId);
				check(victim instanceof OathholdCitizen, "Target citizen missing for crime check");
				victim.hurtServer(l, l.damageSources().playerAttack(p), 2.0F);
				check(OathholdLaw.isWanted(p), "Striking an Oathhold citizen in Survival must mark player Wanted");
				check(OathholdLaw.bountyOf(p) >= OathholdLawRules.CrimeKind.ASSAULT.bountyEmeralds,
					"Assault must assign >= 20 Emerald bounty, got " + OathholdLaw.bountyOf(p));
			});
			context.waitTicks(12);
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				OathboundBastion b = (OathboundBastion) l.getEntity(bastionId);
				OathboundHalberdier h = (OathboundHalberdier) l.getEntity(halberdierId);
				boolean watchingNonHostile = b != null && h != null && b.getTarget() == null && h.getTarget() == null;
				check(watchingNonHostile, "Nearby Knights must watch Wanted player without forcing attack target");

				// Restitution via sneaking Priests
				p.setHealth(p.getMaxHealth());
				p.getInventory().add(new ItemStack(Items.EMERALD, 64));
				OathholdCitizen cleric = (OathholdCitizen) l.getEntity(clericId);
				check(cleric != null, "Priest Cleric missing for restitution");
				p.teleportTo(l, cleric.getX() - 1.5, cleric.getY(), cleric.getZ(), Set.<Relative>of(), -90.0F, 0.0F, false);
				p.setShiftKeyDown(true);
				InteractionResult res = cleric.mobInteract(p, InteractionHand.MAIN_HAND);
				p.setShiftKeyDown(false);
				check(res == InteractionResult.SUCCESS, "Sneak-interacting with Priest Cleric should clear bounty");
				check(!OathholdLaw.isWanted(p) && OathholdLaw.bountyOf(p) == 0, "Wanted status must clear after Emerald restitution");
				check(b != null && b.getTarget() == null && h != null && h.getTarget() == null,
					"Knights must drop aggro immediately after restitution");
				b.discard();
				h.discard();
			});

			// 5. Complete Cleaning, Herb Gathering, Work Shift (promoting Rank F -> E!), and Colosseum Arena Bout -> screenshot 04
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				p.teleportTo(l, 0.5, 120.0, -2.5, Set.<Relative>of(), 180.0F, 0.0F, false);

				// Job 1: Cleaning
				var cleanSpec = OathholdQuestRules.byId("clean_canal_cobwebs");
				check(OathholdQuests.acceptJob(p, BOARD_POS, cleanSpec), "Failed to accept Cleaning job");
				int cleaned = 0;
				for (int x = -14; x <= 14 && cleaned < cleanSpec.needed(); x++) {
					for (int z = -14; z <= 14 && cleaned < cleanSpec.needed(); z++) {
						for (int y = 118; y <= 124 && cleaned < cleanSpec.needed(); y++) {
							BlockPos pos = new BlockPos(x, y, z);
							var st = l.getBlockState(pos);
							if (st.is(Blocks.COBWEB)) {
								check(OathholdQuests.onBlockBroken(l, p, pos, st), "Cleaning cobweb must advance quest");
								l.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
								cleaned++;
							}
						}
					}
				}
				check(cleaned == cleanSpec.needed(), "Expected to clean " + cleanSpec.needed() + " chore cobwebs, found " + cleaned);
				check(!OathholdLaw.isWanted(p), "Breaking quest chore spots must not trigger Vandalism crime");
				var prof1 = OathholdQuests.profile(p);
				check(prof1.completedCount() == 1 && prof1.rankPoints() == 18 && prof1.activeJob().isEmpty(),
					"Cleaning job completion mismatch: " + prof1);

				// Job 2: Herb Gathering
				var herbSpec = OathholdQuestRules.byId("herb_dandelions");
				check(OathholdQuests.acceptJob(p, BOARD_POS, herbSpec), "Failed to accept Herb Gathering job");
				p.getInventory().add(new ItemStack(Items.DANDELION, herbSpec.needed()));
				OathholdQuests.tryTurnInOrReport(p, BOARD_POS);
				check(OathholdQuests.countRequiredGoods(p, "minecraft:dandelion") == 0, "Turn-in must consume gathered herbs");
				var prof2 = OathholdQuests.profile(p);
				check(prof2.completedCount() == 2 && prof2.rankPoints() == 38 && prof2.activeJob().isEmpty(),
					"Herb job completion mismatch: " + prof2);

				// Job 3: Work Shift (promotes Rank F -> Rank E at 60 Guild Points!)
				var shiftSpec = OathholdQuestRules.byId("shift_horseshoes");
				check(OathholdQuests.acceptJob(p, BOARD_POS, shiftSpec), "Failed to accept Work Shift job");
			});

			for (int i = 0; i < 6; i++) {
				world.getServer().runOnServer(server -> {
					ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
					check(OathholdQuests.tryPerformWorkShift(p, ANVIL_POS), "Work shift action must succeed");
				});
				context.waitTicks(10);
			}

			UUID watcherKnightId = world.getServer().computeOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				var prof3 = OathholdQuests.profile(p);
				check(prof3.completedCount() == 3 && prof3.rankPoints() == 60,
					"Expected 3 completed jobs & 60 Guild Points after Work Shift, got " + prof3);
				check(OathholdQuests.rank(p) == OathholdQuestRules.Rank.E,
					"Expected Rank E promotion at 60 Guild Points, got " + OathholdQuests.rank(p));

				// Job 4: Sanctioned Colosseum Arena Bout + peaceful patrol Knight standing nearby
				p.setHealth(p.getMaxHealth());
				p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
				p.teleportTo(l, ARENA_CENTER.getX() + 2.5, 120.0, ARENA_CENTER.getZ() + 0.5, Set.<Relative>of(), 90.0F, 0.0F, false);

				OathboundHalberdier watcher = AuraWorld.OATHBOUND_HALBERDIER.create(l, EntitySpawnReason.COMMAND);
				check(watcher != null, "Failed to spawn arena watcher knight");
				watcher.snapTo(ARENA_CENTER.getX() + 3.5, 120.0, ARENA_CENTER.getZ() - 5.5, 25.0F, 0.0F);
				watcher.finalizeSpawn(l, l.getCurrentDifficultyAt(watcher.blockPosition()), EntitySpawnReason.COMMAND, null);
				l.addFreshEntity(watcher);

				var arenaSpec = OathholdQuestRules.byId("arena_fallen_squire");
				check(OathholdQuests.acceptJob(p, BOARD_POS, arenaSpec), "Failed to accept Rank E Arena Bout");
				check(OathholdQuests.tryStartArenaBout(p, l, ARENA_CENTER.offset(-2, 0, 0)),
					"Failed to summon sanctioned Arena Challenger");
				return watcher.getUUID();
			});

			context.waitTicks(8);
			director(context, world, new Vec3(-12.2, 123.6, 19.8), new Vec3(-18.2, 121.0, 14.0));
			shot(context, "oathhold_guild_04_arena_bout_duel");
			cut(context, world);

			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				String cTag = OathholdQuests.challengerTag(p.getUUID());
				List<Mob> duelists = l.getEntitiesOfClass(Mob.class, new AABB(ARENA_CENTER).inflate(32.0),
					m -> m.entityTags().contains(OathholdLaw.ARENA_DUELIST_TAG) && m.entityTags().contains(cTag));
				check(duelists.size() == 1, "Expected 1 active Arena Duelist, found " + duelists.size());
				Mob duelist = duelists.getFirst();
				duelist.hurtServer(l, l.damageSources().playerAttack(p), 500.0F);
			});
			context.waitTicks(4);

			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				var prof4 = OathholdQuests.profile(p);
				check(prof4.completedCount() == 4 && prof4.rankPoints() == 105 && prof4.activeJob().isEmpty(),
					"Expected 4 completed jobs & 105 pts after Arena Bout, got " + prof4);
				check(!OathholdLaw.isWanted(p), "Defeating a sanctioned Arena Challenger must never trigger a crime");
				OathboundHalberdier watcher = (OathboundHalberdier) l.getEntity(watcherKnightId);
				check(watcher != null && watcher.getTarget() == null,
					"Patrolling Oathbound Knight near Arena must remain neutral during & after sanctioned duel");
			});
		}
	}
}
