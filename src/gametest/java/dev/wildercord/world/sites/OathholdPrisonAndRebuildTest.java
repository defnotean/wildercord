package dev.wildercord.world.sites;

import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.aura.world.OathboundHalberdier;
import dev.wildercord.world.sites.oathhold.OathholdCitizen;
import dev.wildercord.world.sites.oathhold.OathholdCitizenRules;
import dev.wildercord.world.sites.oathhold.OathholdContent;
import dev.wildercord.world.sites.oathhold.OathholdJustice;
import dev.wildercord.world.sites.oathhold.OathholdLaw;
import dev.wildercord.world.sites.oathhold.OathholdLawRules;
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
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * End-to-end native verification of Oathhold house vandalism and player TNT demolition:
 * unwitnessed repairs stay peaceful, whereas guard-witnessed attacks trigger persecution, arrest
 * into an iron-barred street Watch Stockade overlooking the house, live visible NPC Master Builder
 * reconstruction, and full release + stockade cleanup once finished.
 */
public final class OathholdPrisonAndRebuildTest implements FabricClientGameTest {
	private static final BlockPos STAGE = new BlockPos(0, 120, 0);
	private static final String CAM_TAG = "wildercord.oathhold_prison_cam";

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
			Display.TextDisplay cam = EntityType.TEXT_DISPLAY.create(l, EntitySpawnReason.COMMAND);
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

	private static void buildTownhouseAndStreet(ServerLevel l) {
		for (int x = -22; x <= 22; x++) {
			for (int z = -22; z <= 22; z++) {
				l.setBlock(new BlockPos(x, 119, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
				if (Math.floorMod(x, 4) == 0 || Math.floorMod(z, 4) == 0) {
					l.setBlock(new BlockPos(x, 119, z), Blocks.POLISHED_ANDESITE.defaultBlockState(), 3);
				}
				for (int y = 120; y <= 136; y++) {
					l.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}

		// Half-timbered 2-story Oathhold townhouse at x = -5..5, z = -15..-7, y = 120..129
		int x0 = -5, x1 = 5, z0 = -15, z1 = -7, baseY = 120;
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				l.setBlock(new BlockPos(x, baseY, z), Blocks.SPRUCE_PLANKS.defaultBlockState(), 3);
				l.setBlock(new BlockPos(x, baseY + 4, z), Blocks.DARK_OAK_PLANKS.defaultBlockState(), 3);
			}
		}
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				boolean wallX = x == x0 || x == x1;
				boolean wallZ = z == z0 || z == z1;
				if (!wallX && !wallZ) continue;
				boolean post = (x == x0 || x == x1) && (z == z0 || z == z1);
				for (int y = baseY + 1; y <= baseY + 7; y++) {
					BlockState st = post ? Blocks.DARK_OAK_LOG.defaultBlockState()
						: (y <= baseY + 3 ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.CALCITE.defaultBlockState());
					l.setBlock(new BlockPos(x, y, z), st, 3);
				}
			}
		}
		// Shopfront windows & door facing south (+Z) onto the boulevard
		l.setBlock(new BlockPos(-3, baseY + 2, z1), Blocks.GLASS_PANE.defaultBlockState(), 3);
		l.setBlock(new BlockPos(3, baseY + 2, z1), Blocks.GLASS_PANE.defaultBlockState(), 3);
		l.setBlock(new BlockPos(-2, baseY + 6, z1), Blocks.GLASS_PANE.defaultBlockState(), 3);
		l.setBlock(new BlockPos(2, baseY + 6, z1), Blocks.GLASS_PANE.defaultBlockState(), 3);
		l.setBlock(new BlockPos(0, baseY + 1, z1),
			Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 18);
		l.setBlock(new BlockPos(0, baseY + 2, z1),
			Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH).setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 18);
		for (int ax = -2; ax <= 2; ax++) {
			BlockState awning = ((ax + 2) & 1) == 0 ? Blocks.WOOL.red().defaultBlockState() : Blocks.WOOL.white().defaultBlockState();
			l.setBlock(new BlockPos(ax, baseY + 3, z1 + 1), awning, 3);
		}
		l.setBlock(new BlockPos(0, baseY + 4, z1 + 1), Blocks.DARK_OAK_FENCE.defaultBlockState(), 3);
		l.setBlock(new BlockPos(0, baseY + 3, z1 + 1), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 3);
		// Pitched terracotta gable roof
		for (int step = 0; step <= 4; step++) {
			int ry = baseY + 8 + (step / 2);
			for (int x = x0 - 1; x <= x1 + 1; x++) {
				l.setBlock(new BlockPos(x, ry, z0 + step), Blocks.TERRACOTTA.defaultBlockState(), 3);
				l.setBlock(new BlockPos(x, ry, z1 - step), Blocks.TERRACOTTA.defaultBlockState(), 3);
			}
		}
		for (int x = x0 - 1; x <= x1 + 1; x++) {
			l.setBlock(new BlockPos(x, baseY + 10, -11), Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState(), 3);
		}
	}

	private static Map<BlockPos, BlockState> snapshotBox(ServerLevel l, int x0, int y0, int z0, int x1, int y1, int z1) {
		Map<BlockPos, BlockState> snap = new HashMap<>();
		for (int x = x0; x <= x1; x++) {
			for (int y = y0; y <= y1; y++) {
				for (int z = z0; z <= z1; z++) {
					BlockPos p = new BlockPos(x, y, z);
					snap.put(p, l.getBlockState(p));
				}
			}
		}
		return snap;
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder()
				.setUseConsistentSettings(false)
				.adjustSettings(settings -> settings.setSeed("20261122"))
				.create()) {
			context.waitTicks(25);
			for (String rule : List.of("advance_time false", "advance_weather false", "spawn_mobs false")) {
				world.getServer().runCommand("gamerule " + rule);
			}
			world.getServer().runCommand("time set 3000");
			world.getServer().runCommand("weather clear");

			context.runOnClient(mc -> {
				mc.getWindow().setWindowed(1280, 720);
				mc.options.renderDistance().set(10);
				mc.options.fov().set(74);
				mc.options.broadcastOptions();
				mc.resizeGui();
				if (mc.gui.hud.isHidden()) mc.gui.hud.toggle();
			});

			Map<BlockPos, BlockState> houseSnapshot = new HashMap<>();
			Map<BlockPos, BlockState> streetSnapshot = new HashMap<>();

			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				buildTownhouseAndStreet(l);
				OathholdLaw.registerCityNow(l, 0, 120, 0);
				houseSnapshot.putAll(snapshotBox(l, -6, 120, -16, 6, 131, -6));
				streetSnapshot.putAll(snapshotBox(l, -20, 119, -20, 20, 131, 20));
				houseSnapshot.keySet().forEach(streetSnapshot::remove);

				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				p.setGameMode(GameType.SURVIVAL);
				OathholdLaw.clearCrime(p, true);
				p.teleportTo(l, 2.5, 120.0, -5.0, Set.<Relative>of(), 180.0F, 0.0F, false);
			});
			context.waitTicks(6);

			// 1. Unwitnessed stealth break: block breaks without crime/persecution, and builders still repair it later.
			BlockPos stealthBlock = new BlockPos(2, 121, -7);
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
				p.gameMode.destroyBlock(stealthBlock);
				check(l.getBlockState(stealthBlock).isAir(), "Broken city house block turns to air");
				check(!OathholdLaw.isWanted(p) && !OathholdJustice.isPersecuted(p) && !OathholdJustice.isImprisoned(p),
					"Unwitnessed break must not trigger persecution or prison");
				check(OathholdJustice.activeRepairBlockCount(server, stealthBlock, 12.0) >= 1,
					"City schedules stealth damage for reconstruction");
			});
			context.waitTicks(125);
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				check(l.getBlockState(stealthBlock).equals(houseSnapshot.get(stealthBlock)),
					"Stealth damage must be quietly rebuilt to original state");
			});

			// 2. Guard-witnessed house breaking -> persecution -> capture -> cell escape block -> /adventurer release.
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();

				OathboundHalberdier halberdier = AuraWorld.OATHBOUND_HALBERDIER.create(l, EntitySpawnReason.COMMAND);
				check(halberdier != null, "Spawn Oathbound Halberdier guard");
				halberdier.snapTo(-5.5, 120.0, 0.5, -20.0F, 0.0F);
				halberdier.finalizeSpawn(l, l.getCurrentDifficultyAt(halberdier.blockPosition()), EntitySpawnReason.COMMAND, null);
				l.addFreshEntity(halberdier);

				OathholdCitizen watchman = OathholdContent.CITIZEN.create(l, EntitySpawnReason.COMMAND);
				check(watchman != null, "Spawn City Watchman guard");
				watchman.configure(OathholdCitizenRules.Archetype.CITY_WATCHMAN, 6, 120, 0, new BlockPos(6, 120, 0), 14);
				watchman.snapTo(6.5, 120.0, 0.5, 20.0F, 0.0F);
				watchman.finalizeSpawn(l, l.getCurrentDifficultyAt(watchman.blockPosition()), EntitySpawnReason.COMMAND, null);
				l.addFreshEntity(watchman);

				p.teleportTo(l, 1.5, 120.0, -5.5, Set.<Relative>of(), 180.0F, 0.0F, false);
				p.gameMode.destroyBlock(new BlockPos(1, 121, -7));
				check(OathholdLaw.isWanted(p) && OathholdLaw.bountyOf(p) >= OathholdLawRules.CrimeKind.VANDALISM.bountyEmeralds,
					"Guard-witnessed house break marks player Wanted");
				check(OathholdJustice.isPersecuted(p), "Guard-witnessed house break puts player into active Persecution");
				check(!OathholdJustice.isImprisoned(p), "Healthy wanted player must not be imprisoned immediately");
			});
			context.waitTicks(20);
			world.getServer().runOnServer(server -> {
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				check(!OathholdJustice.isImprisoned(p), "Guards still watching - no imprisonment above one heart");
				p.setHealth(2.0F); // one heart
			});
			for (int i = 0; i < 40; i++) {
				boolean imprisoned = world.getServer().computeOnServer(server ->
					OathholdJustice.isImprisoned(server.getPlayerList().getPlayers().getFirst()));
				if (imprisoned) break;
				context.waitTicks(3);
			}
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				check(OathholdJustice.isImprisoned(p), "Guard arrests and imprisons the vandal at one heart in Watch Stockade");
				OathholdJustice.PrisonRecord rec = OathholdJustice.ledger(server).prisonFor(p.getUUID());
				check(rec != null, "PrisonRecord exists while imprisoned");
				BlockPos barPos = rec.cellCenter.offset(0, 1, -2);
				check(l.getBlockState(barPos).is(Blocks.IRON_BARS), "Watch Stockade encloses player with iron bars");
				p.gameMode.destroyBlock(barPos);
				check(l.getBlockState(barPos).is(Blocks.IRON_BARS), "Prisoner cannot break Watch Stockade iron bars");
				p.teleportTo(l, 18.0, 120.0, 18.0, Set.<Relative>of(), 0.0F, 0.0F, false);
			});
			context.waitTicks(3);
			world.getServer().runOnServer(server -> {
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				OathholdJustice.PrisonRecord rec = OathholdJustice.ledger(server).prisonFor(p.getUUID());
				check(rec != null && OathholdLawRules.isInsideCell(p.getX(), p.getY(), p.getZ(),
					rec.cellCenter.getX(), rec.cellCenter.getY(), rec.cellCenter.getZ()),
					"Attempting to leave prison cell snaps prisoner back inside");
				OathholdJustice.forceRelease(p);
				check(!OathholdJustice.isImprisoned(p), "Forced release frees prisoner cleanly");
			});
			context.waitTicks(35);

			// 3. TNT explosion on the house while guards watch -> DEMOLITION crime -> prison -> live NPC rebuilding -> release!
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				for (OathholdCitizen watchman : l.getEntitiesOfClass(OathholdCitizen.class, new AABB(STAGE).inflate(40.0),
						c -> c.isAlive() && c.archetype() == OathholdCitizenRules.Archetype.CITY_WATCHMAN)) {
					watchman.snapTo(8.5, 120.0, 2.5, 20.0F, 0.0F);
					watchman.getNavigation().stop();
				}
				p.setHealth(p.getMaxHealth());
				p.teleportTo(l, -1.5, 120.0, -4.8, Set.<Relative>of(), 170.0F, 15.0F, false);
				BlockPos tntPlaceFloor = new BlockPos(-1, 119, -6);
				p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TNT, 2));
				p.gameMode.useItemOn(p, l, p.getMainHandItem(), InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(tntPlaceFloor), Direction.UP, tntPlaceFloor, false));
				BlockPos tntPos = tntPlaceFloor.above();
				check(l.getBlockState(tntPos).is(Blocks.TNT), "Player places TNT beside the townhouse front wall");
				p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
				p.gameMode.useItemOn(p, l, p.getMainHandItem(), InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(tntPos), Direction.SOUTH, tntPos, false));
				p.teleportTo(l, -1.5, 120.0, 4.5, Set.<Relative>of(), 180.0F, 0.0F, false);
			});

			// Wait for 80-tick TNT detonation + crater creation
			for (int i = 0; i < 35; i++) {
				boolean exploded = world.getServer().computeOnServer(server ->
					OathholdJustice.activeRepairBlockCount(server, new BlockPos(0, 121, -8), 20.0) >= 8
						&& OathholdLaw.bountyOf(server.getPlayerList().getPlayers().getFirst()) >= OathholdLawRules.CrimeKind.DEMOLITION.bountyEmeralds);
				if (exploded) break;
				context.waitTicks(4);
			}
			world.getServer().runOnServer(server -> {
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				check(OathholdLaw.bountyOf(p) >= OathholdLawRules.CrimeKind.DEMOLITION.bountyEmeralds,
					"Player TNT blast triggers DEMOLITION bounty, got " + OathholdLaw.bountyOf(p));
				check(OathholdJustice.isPersecuted(p), "Guards must persecute player after TNT demolition");
				check(!OathholdJustice.isImprisoned(p), "Healthy bomber must not be imprisoned immediately");
			});
			context.waitTicks(20);
			world.getServer().runOnServer(server -> {
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				check(!OathholdJustice.isImprisoned(p), "Guards still watching bomber above one heart");
				p.setHealth(2.0F);
			});

			// Wait for arrest into street-side Watch Stockade overlooking the blasted house
			for (int i = 0; i < 45; i++) {
				boolean imprisoned = world.getServer().computeOnServer(server ->
					OathholdJustice.isImprisoned(server.getPlayerList().getPlayers().getFirst()));
				if (imprisoned) break;
				context.waitTicks(3);
			}

			// Wait briefly for repair crew to reach the house & start placing blocks while player watches from prison
			context.waitTicks(28);
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				check(OathholdJustice.isImprisoned(p), "Bomber must be held in the Watch Stockade");
				OathholdJustice.PrisonRecord rec = OathholdJustice.ledger(server).prisonFor(p.getUUID());
				check(rec != null, "Active PrisonRecord missing during rebuild");
				List<OathholdCitizen> builders = l.getEntitiesOfClass(OathholdCitizen.class, new AABB(STAGE).inflate(30.0),
					c -> c.isAlive() && c.archetype() == OathholdCitizenRules.Archetype.CARPENTER_MASON);
				check(!builders.isEmpty(), "NPC Master Builders must spawn and rebuild the damaged house visibly");
				// Orient prisoner directly toward the repair site for clean first-person view through bars.
				Vec3 eye = new Vec3(p.getX(), p.getEyeY(), p.getZ());
				Vec3 target = new Vec3(0.5, 122.5, -8.5);
				p.teleportTo(l, p.getX(), p.getY(), p.getZ(), Set.<Relative>of(), yaw(eye, target), pitch(eye, target), false);
			});
			context.waitTicks(4);
			context.runOnClient(mc -> {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				if (mc.gui.hud.isHidden()) mc.gui.hud.toggle();
			});
			shot(context, "oathhold_prison_view_rebuilding");

			// Capture exterior overview showing player in Watch Stockade behind iron bars + NPC builders across street
			BlockPos[] cellAndHouse = world.getServer().computeOnServer(server -> {
				OathholdJustice.PrisonRecord rec = OathholdJustice.ledger(server).prisonFor(
					server.getPlayerList().getPlayers().getFirst().getUUID());
				return new BlockPos[]{rec.cellCenter, rec.lookTarget};
			});
			Vec3 mid = Vec3.atCenterOf(cellAndHouse[0]).lerp(Vec3.atCenterOf(cellAndHouse[1]), 0.35);
			director(context, world, mid.add(10.5, 5.2, 6.5), mid.add(0.0, 1.2, 0.0));
			shot(context, "oathhold_prison_exterior_rebuild");
			cut(context, world);

			// Wait until BOTH NPC house rebuilding finishes AND the temporary prison sentence releases naturally
			for (int i = 0; i < 160; i++) {
				boolean done = world.getServer().computeOnServer(server -> {
					ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
					return !OathholdJustice.isImprisoned(p)
						&& OathholdJustice.activeRepairBlockCount(server, STAGE, 32.0) == 0;
				});
				if (done) break;
				context.waitTicks(5);
			}

			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				check(!OathholdJustice.isImprisoned(p), "Prisoner must be released once temporary sentence ends");
				check(!OathholdLaw.isWanted(p) && OathholdLaw.bountyOf(p) == 0,
					"Player bounty must clear cleanly after serving prison sentence");
				check(OathholdJustice.activeRepairBlockCount(server, STAGE, 32.0) == 0,
					"House repairs must complete without leftover queued blocks");
				for (var e : houseSnapshot.entrySet()) {
					check(l.getBlockState(e.getKey()).equals(e.getValue()),
						"Rebuilt house mismatch at " + e.getKey() + ": expected " + e.getValue() + ", got " + l.getBlockState(e.getKey()));
				}
				for (var e : streetSnapshot.entrySet()) {
					check(l.getBlockState(e.getKey()).equals(e.getValue()),
						"Street block must restore after stockade removal at " + e.getKey() + ": got " + l.getBlockState(e.getKey()));
				}
				List<OathholdCitizen> leftoverTemp = l.getEntitiesOfClass(OathholdCitizen.class, new AABB(STAGE).inflate(40.0),
					OathholdCitizen::isTemporaryCrew);
				check(leftoverTemp.isEmpty(), "Temporary builder/warden crew must clean up after rebuild and release");
			});

			director(context, world, new Vec3(6.8, 123.6, 1.5), new Vec3(0.0, 122.4, -9.0));
			shot(context, "oathhold_house_rebuilt_after_release");
			cut(context, world);
		}
	}
}
