package dev.wildercord.ocean;

import dev.wildercord.world.sites.ocean.AbyssalCitadelPiece;
import dev.wildercord.world.sites.ocean.CoralDomeSanctuaryPiece;
import dev.wildercord.world.sites.ocean.HydrothermalForgePiece;
import dev.wildercord.world.sites.ocean.KelpSpireWatchtowerPiece;
import dev.wildercord.world.sites.ocean.OceanSites;
import dev.wildercord.world.sites.ocean.SunkenGalleonPiece;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Native worldgen & 1:1 structural blueprint verification for all 5 Deep Ocean structures:
 * Coral-Dome Sanctuary (49x18x49), Sunken Sovereign Galleon (46x28x15), Kelp-Spire Watchtower
 * (15xHx15 to surface), Hydrothermal Vent Forge (27x20x27), and Abyssal Trench Citadel (65x28x65)
 * with 8 eight-tall sea-lantern obelisks, Sovereign Altar, and Thalassor.
 */
public final class DeepOceanStructuresTest implements FabricClientGameTest {
	private static final String CAM_TAG = "wildercord.ocean_struct_cam";

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

	private static void director(ClientGameTestContext context, TestSingleplayerContext world, BlockPos anchor, Vec3 eye, Vec3 target) {
		int camId = world.getServer().computeOnServer(server -> {
			ServerLevel l = server.overworld();
			l.getEntitiesOfClass(Display.TextDisplay.class, new AABB(anchor).inflate(140),
				e -> e.entityTags().contains(CAM_TAG)).forEach(Entity::discard);
			Display.TextDisplay cam = EntityTypes.TEXT_DISPLAY.create(l, EntitySpawnReason.COMMAND);
			cam.snapTo(eye.x, eye.y, eye.z, yaw(eye, target), pitch(eye, target));
			cam.addTag(CAM_TAG);
			l.addFreshEntity(cam);
			return cam.getId();
		});
		context.waitTicks(4);
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
		context.waitTicks(5);
	}

	private static void cut(ClientGameTestContext context, TestSingleplayerContext world, BlockPos anchor) {
		context.runOnClient(mc -> mc.setCameraEntity(mc.player));
		world.getServer().runOnServer(s -> s.overworld()
			.getEntitiesOfClass(Display.TextDisplay.class, new AABB(anchor).inflate(140),
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

	private static void prepareOceanBasin(ServerLevel l, int x0, int z0, int x1, int z1, int floorY, int waterTopY) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				l.setBlock(new BlockPos(x, floorY - 1, z), Blocks.STONE.defaultBlockState(), 2);
				l.setBlock(new BlockPos(x, floorY, z),
					((Math.floorMod(x, 8) == 0 && Math.floorMod(z, 8) == 0) ? Blocks.SEA_LANTERN : Blocks.SAND).defaultBlockState(), 2);
				for (int y = floorY + 1; y <= waterTopY; y++) {
					l.setBlock(new BlockPos(x, y, z), Blocks.WATER.defaultBlockState(), 2);
				}
				for (int y = waterTopY + 1; y <= waterTopY + 24; y++) {
					l.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
				}
			}
		}
	}

	private static void placePiece(ServerLevel l, StructurePiece piece) {
		BoundingBox bb = piece.getBoundingBox();
		ChunkGenerator gen = l.getChunkSource().getGenerator();
		RandomSource rand = RandomSource.create(20261122L);
		piece.postProcess(l, l.structureManager(), gen, rand, bb, ChunkPos.containing(bb.getCenter()), bb.getCenter());
	}

	private static int countBlocks(ServerLevel l, int x0, int y0, int z0, int x1, int y1, int z1, net.minecraft.world.level.block.Block block) {
		int c = 0;
		for (int x = x0; x <= x1; x++) {
			for (int y = y0; y <= y1; y++) {
				for (int z = z0; z <= z1; z++) {
					if (l.getBlockState(new BlockPos(x, y, z)).is(block)) c++;
				}
			}
		}
		return c;
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder()
				.setUseConsistentSettings(false)
				.adjustSettings(s -> s.setSeed("20261125"))
				.create()) {
			context.waitTicks(20);
			for (String rule : List.of("advance_time false", "advance_weather false", "spawn_mobs false")) {
				world.getServer().runCommand("gamerule " + rule);
			}
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather clear");

			context.runOnClient(mc -> {
				mc.getWindow().setWindowed(1280, 720);
				mc.options.renderDistance().set(12);
				mc.options.fov().set(75);
				mc.options.broadcastOptions();
				mc.resizeGui();
			});

			// 1. Verify all 5 structure IDs are registered in Overworld STRUCTURE registry & place Coral-Dome Sanctuary
			BlockPos domeOrigin = new BlockPos(0, 62, 0);
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				var reg = server.registryAccess().lookupOrThrow(Registries.STRUCTURE);
				for (String sid : List.of(
					OceanSites.CORAL_DOME_ID, OceanSites.GALLEON_ID, OceanSites.WATCHTOWER_ID,
					OceanSites.FORGE_ID, OceanSites.CITADEL_ID
				)) {
					ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, dev.wildercord.Wildercord.id(sid));
					check(reg.getOptional(key).isPresent(), "Structure registered in worldgen registry: " + sid);
				}

				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				p.setGameMode(GameType.CREATIVE);
				p.teleportTo(l, 24.5, 96.0, 24.5, Set.<Relative>of(), 0f, 35f, false);

				prepareOceanBasin(l, -4, -4, 54, 54, 64, 80);
				placePiece(l, new CoralDomeSanctuaryPiece(0, 62, 0, Direction.NORTH));

				check(countBlocks(l, 22, 65, 22, 26, 70, 26, Blocks.CONDUIT) == 1, "Coral-Dome has central Conduit");
				check(l.getBlockState(new BlockPos(24, 67, 9)).isAir(), "North Coral-Dome interior is dry AIR");
				check(countBlocks(l, 0, 64, 0, 49, 76, 49, Blocks.STAINED_GLASS.cyan()) > 80, "Coral-Dome cyan stained-glass domes built");
				check(l.getEntitiesOfClass(BrineMerchant.class, new AABB(0, 62, 0, 49, 80, 49)).size() >= 3,
					"Coral-Dome spawns all 3 Brine-Merchants");
			});
			context.waitTicks(35);
			director(context, world, domeOrigin, new Vec3(7.0, 84.5, 44.5), new Vec3(24.5, 67.5, 24.5));
			shot(context, "ocean_live_coral_dome_sanctuary");
			cut(context, world, domeOrigin);

			// 2. Sunken Sovereign Galleon (46x28x15)
			BlockPos galleonOrigin = new BlockPos(100, 62, 0);
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				p.teleportTo(l, 123.0, 86.0, 32.0, Set.<Relative>of(), 180f, 35f, false);

				prepareOceanBasin(l, 96, -4, 152, 24, 64, 82);
				placePiece(l, new SunkenGalleonPiece(100, 62, 0, Direction.EAST));

				check(countBlocks(l, 100, 62, 0, 146, 90, 15, Blocks.DISPENSER) >= 4, "Sunken Galleon has broadside cannons");
				check(countBlocks(l, 100, 62, 0, 146, 90, 15, Blocks.WOOL.white()) > 10, "Sunken Galleon has tattered sails");
				check(!l.getEntitiesOfClass(BarnacleHulkDredger.class, new AABB(98, 60, -2, 148, 92, 18)).isEmpty(),
					"Sunken Galleon spawns Barnacle-Hulk Dredger");
			});
			context.waitTicks(35);
			director(context, world, galleonOrigin, new Vec3(143.5, 86.0, 28.0), new Vec3(121.0, 72.5, 7.5));
			shot(context, "ocean_live_sunken_galleon");
			cut(context, world, galleonOrigin);

			// 3. Kelp-Spire Watchtower (15xHx15 to surface) + Hydrothermal Vent Forge (27x20x27)
			BlockPos forgeOrigin = new BlockPos(200, 62, 0);
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				p.teleportTo(l, 222.0, 88.0, 34.0, Set.<Relative>of(), 180f, 35f, false);

				prepareOceanBasin(l, 196, -4, 256, 36, 64, 82);
				// Height 30 reaches y=91 cleanly above waterTopY=82
				placePiece(l, new KelpSpireWatchtowerPiece(232, 62, 6, Direction.NORTH, 30));
				placePiece(l, new HydrothermalForgePiece(200, 62, 0, Direction.NORTH));

				check(countBlocks(l, 232, 62, 6, 247, 94, 21, Blocks.SOUL_SAND) >= 1, "Watchtower has UP bubble column");
				check(countBlocks(l, 232, 62, 6, 247, 94, 21, Blocks.MAGMA_BLOCK) >= 1, "Watchtower has DOWN bubble column");
				check(countBlocks(l, 200, 62, 0, 227, 84, 27, OceanBlocks.TRENCH_ANVIL) == 1,
					"Hydrothermal Forge houses Trench Anvil");
				check(countBlocks(l, 200, 62, 0, 227, 84, 27, Blocks.BASALT) > 20, "Hydrothermal Forge has basalt chimneys");
			});
			context.waitTicks(35);
			director(context, world, forgeOrigin, new Vec3(246.0, 89.0, 38.0), new Vec3(222.0, 74.5, 13.0));
			shot(context, "ocean_live_watchtower_and_forge");
			cut(context, world, forgeOrigin);

			// 4. Abyssal Trench Citadel (65x28x65) + 8 Obelisks + Sovereign Altar + Thalassor
			BlockPos citadelOrigin = new BlockPos(300, 62, 0);
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				p.teleportTo(l, 332.0, 92.0, 68.0, Set.<Relative>of(), 180f, 35f, false);

				prepareOceanBasin(l, 296, -4, 370, 70, 64, 82);
				placePiece(l, new AbyssalCitadelPiece(300, 62, 0, Direction.EAST));

				// Check 8 eight-tall obelisks (each has 5 sea_lantern blocks => 40 sea lanterns in colosseum ring)
				int ringLanterns = countBlocks(l, 326, 65, 10, 369, 74, 55, Blocks.SEA_LANTERN);
				check(ringLanterns >= 40, "Abyssal Citadel Colosseum has 8 five-lantern obelisks (found " + ringLanterns + ")");
				check(countBlocks(l, 300, 62, 0, 365, 90, 65, OceanBlocks.SOVEREIGN_ALTAR) == 1,
					"Abyssal Citadel places Sovereign Altar");
				check(!l.getEntitiesOfClass(ThalassorBoss.class, new AABB(300, 60, 0, 366, 100, 66)).isEmpty(),
					"Abyssal Citadel spawns Thalassor above altar");
			});
			context.waitTicks(40);
			director(context, world, citadelOrigin, new Vec3(368.0, 89.5, 56.0), new Vec3(334.0, 71.0, 32.0));
			shot(context, "ocean_live_abyssal_citadel");
			cut(context, world, citadelOrigin);
		}
	}
}
