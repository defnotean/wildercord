package dev.wildercord.ocean;

import dev.wildercord.world.StructureClearance;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

	private static void directorUnderwater(ClientGameTestContext context, TestSingleplayerContext world, Vec3 eye, Vec3 target) {
		world.getServer().runOnServer(server -> {
			ServerLevel l = server.overworld();
			ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
			p.setGameMode(GameType.SPECTATOR);
			p.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 400, 0, false, false, false));
			p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false, false));
			p.teleportTo(l, eye.x, eye.y, eye.z, Set.<Relative>of(), yaw(eye, target), pitch(eye, target), false);
		});
		context.waitTicks(4);
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			mc.setCameraEntity(mc.player);
			if (!mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
			mc.gui.toastManager().clear();
			mc.gui.hud.getChat().clearMessages(false);
		});
		context.waitTicks(8);
	}

	private static void cut(ClientGameTestContext context, TestSingleplayerContext world, BlockPos anchor) {
		context.runOnClient(mc -> mc.setCameraEntity(mc.player));
		world.getServer().runOnServer(s -> {
			s.overworld()
				.getEntitiesOfClass(Display.TextDisplay.class, new AABB(anchor).inflate(140),
					e -> e.entityTags().contains(CAM_TAG))
				.forEach(Entity::discard);
			ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
			p.removeAllEffects();
			p.setGameMode(GameType.CREATIVE);
		});
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
				for (int y = waterTopY + 1; y <= waterTopY + 54; y++) {
					l.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
				}
			}
		}
		l.getEntitiesOfClass(Entity.class, new AABB(x0, floorY - 2, z0, x1, waterTopY + 60, z1),
			e -> !(e instanceof ServerPlayer)).forEach(Entity::discard);
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

			// Stage all 4 Ocean bays inside the seed-invariant Oathhold-free ocean corridor [2664..2912]
			// (region (0,0) city max extent +96 buffer <= 2640; region (1,1) min extent >= 2976).
			final int ox1 = 2696, oz1 = 2696;
			BlockPos domeOrigin = new BlockPos(ox1, 62, oz1);
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				var reg = server.registryAccess().lookupOrThrow(Registries.STRUCTURE);
				for (String sid : List.of(
					OceanSites.CORAL_DOME_ID, OceanSites.GALLEON_ID, OceanSites.WATCHTOWER_ID,
					OceanSites.FORGE_ID, OceanSites.CITADEL_ID
				)) {
					ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, dev.wildercord.Wildercord.id(sid));
					check(reg.getOptional(key).isPresent(), "Structure registered in worldgen registry: " + sid);
					check(StructureClearance.entries().containsKey(sid), "StructureClearance guards: " + sid);
				}

				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				p.setGameMode(GameType.CREATIVE);
				p.teleportTo(l, ox1 + 24.5, 96.0, oz1 + 24.5, Set.<Relative>of(), 0f, 35f, false);

				prepareOceanBasin(l, ox1 - 32, oz1 - 32, ox1 + 80, oz1 + 80, 64, 82);
				placePiece(l, new CoralDomeSanctuaryPiece(ox1, 62, oz1, Direction.NORTH));

				check(countBlocks(l, ox1 + 22, 65, oz1 + 22, ox1 + 26, 70, oz1 + 26, Blocks.CONDUIT) == 1, "Coral-Dome has central Conduit");
				check(l.getBlockState(new BlockPos(ox1 + 24, 67, oz1 + 9)).isAir(), "North Coral-Dome interior is dry AIR");
				check(countBlocks(l, ox1, 64, oz1, ox1 + 49, 76, oz1 + 49, Blocks.STAINED_GLASS.cyan()) > 80, "Coral-Dome cyan stained-glass domes built");
				check(l.getEntitiesOfClass(BrineMerchant.class, new AABB(ox1, 62, oz1, ox1 + 49, 80, oz1 + 49)).size() >= 3,
					"Coral-Dome spawns all 3 Brine-Merchants");
			});
			context.waitTicks(35);
			directorUnderwater(context, world, new Vec3(ox1 + 6.0, 77.5, oz1 + 48.0), new Vec3(ox1 + 24.5, 67.5, oz1 + 24.5));
			shot(context, "ocean_live_coral_dome_sanctuary");
			cut(context, world, domeOrigin);

			// 2. Sunken Sovereign Galleon (46x28x15)
			final int ox2 = 2816, oz2 = 2696;
			BlockPos galleonOrigin = new BlockPos(ox2, 62, oz2);
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				p.teleportTo(l, ox2 + 23.0, 86.0, oz2 + 32.0, Set.<Relative>of(), 180f, 35f, false);

				prepareOceanBasin(l, ox2 - 28, oz2 - 28, ox2 + 76, oz2 + 44, 64, 92);
				placePiece(l, new SunkenGalleonPiece(ox2, 62, oz2, Direction.SOUTH));

				check(countBlocks(l, ox2, 62, oz2, ox2 + 46, 90, oz2 + 15, Blocks.DISPENSER) >= 4, "Sunken Galleon has broadside cannons");
				check(countBlocks(l, ox2, 62, oz2, ox2 + 46, 90, oz2 + 15, Blocks.WOOL.white()) > 10, "Sunken Galleon has tattered sails");
				check(!l.getEntitiesOfClass(BarnacleHulkDredger.class, new AABB(ox2 - 2, 60, oz2 - 2, ox2 + 48, 92, oz2 + 18)).isEmpty(),
					"Sunken Galleon spawns Barnacle-Hulk Dredger");
			});
			context.waitTicks(35);
			directorUnderwater(context, world, new Vec3(ox2 - 7.0, 81.5, oz2 - 10.5), new Vec3(ox2 + 23.0, 72.5, oz2 + 7.5));
			shot(context, "ocean_live_sunken_galleon");
			cut(context, world, galleonOrigin);

			// 3. Kelp-Spire Watchtower (15xHx15 to surface) + Hydrothermal Vent Forge (27x20x27)
			final int ox3 = 2696, oz3 = 2816;
			BlockPos forgeOrigin = new BlockPos(ox3, 62, oz3);
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				p.teleportTo(l, ox3 + 22.0, 88.0, oz3 + 34.0, Set.<Relative>of(), 180f, 35f, false);

				prepareOceanBasin(l, ox3 - 26, oz3 - 26, ox3 + 78, oz3 + 58, 64, 82);
				// Height 30 reaches y=91 cleanly above waterTopY=82
				placePiece(l, new KelpSpireWatchtowerPiece(ox3 + 32, 62, oz3 + 6, Direction.NORTH, 30));
				placePiece(l, new HydrothermalForgePiece(ox3, 62, oz3, Direction.NORTH));

				check(countBlocks(l, ox3 + 32, 62, oz3 + 6, ox3 + 47, 94, oz3 + 21, Blocks.SOUL_SAND) >= 1, "Watchtower has UP bubble column");
				check(countBlocks(l, ox3 + 32, 62, oz3 + 6, ox3 + 47, 94, oz3 + 21, Blocks.MAGMA_BLOCK) >= 1, "Watchtower has DOWN bubble column");
				check(countBlocks(l, ox3, 62, oz3, ox3 + 27, 84, oz3 + 27, OceanBlocks.TRENCH_ANVIL) == 1,
					"Hydrothermal Forge houses Trench Anvil");
				check(countBlocks(l, ox3, 62, oz3, ox3 + 27, 84, oz3 + 27, Blocks.BASALT) > 20, "Hydrothermal Forge has basalt chimneys");
			});
			context.waitTicks(35);
			director(context, world, forgeOrigin, new Vec3(ox3 + 52.0, 88.5, oz3 + 34.0), new Vec3(ox3 + 21.0, 74.0, oz3 + 14.0));
			shot(context, "ocean_live_watchtower_and_forge");
			cut(context, world, forgeOrigin);

			// 4. Abyssal Trench Citadel (65x28x65) + 8 Obelisks + Sovereign Altar + Thalassor
			final int ox4 = 2816, oz4 = 2816;
			BlockPos citadelOrigin = new BlockPos(ox4, 62, oz4);
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				p.teleportTo(l, ox4 + 32.0, 92.0, oz4 + 68.0, Set.<Relative>of(), 180f, 35f, false);

				prepareOceanBasin(l, ox4 - 28, oz4 - 28, ox4 + 96, oz4 + 90, 64, 92);
				placePiece(l, new AbyssalCitadelPiece(ox4, 62, oz4, Direction.SOUTH));

				// Check 8 eight-tall obelisks (each has 5 sea_lantern blocks => 40 sea lanterns in colosseum ring)
				int ringLanterns = countBlocks(l, ox4 + 26, 65, oz4 + 10, ox4 + 69, 74, oz4 + 55, Blocks.SEA_LANTERN);
				check(ringLanterns >= 40, "Abyssal Citadel Colosseum has 8 five-lantern obelisks (found " + ringLanterns + ")");
				check(countBlocks(l, ox4, 62, oz4, ox4 + 65, 90, oz4 + 65, OceanBlocks.SOVEREIGN_ALTAR) == 1,
					"Abyssal Citadel places Sovereign Altar");
				check(!l.getEntitiesOfClass(ThalassorBoss.class, new AABB(ox4, 60, oz4, ox4 + 66, 100, oz4 + 66)).isEmpty(),
					"Abyssal Citadel spawns Thalassor above altar");
			});
			context.waitTicks(40);
			directorUnderwater(context, world, new Vec3(ox4 + 66.0, 83.0, oz4 + 52.0), new Vec3(ox4 + 28.0, 71.5, oz4 + 32.0));
			shot(context, "ocean_live_abyssal_citadel");
			cut(context, world, citadelOrigin);
		}
	}
}
