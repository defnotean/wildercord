package dev.wildercord.ocean;

import dev.wildercord.gametest.compat.GametestCompat;
import dev.wildercord.world.sites.Sites;
import dev.wildercord.world.sites.ocean.OceanSites;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.phys.Vec3;

/**
 * Natural-placement regression for the five Deep Ocean structures.
 *
 * <p>{@code OceanPiece.locateSubmergedFloor} used to reject any column whose {@code WORLD_SURFACE_WG}
 * first-occupied height was below sea level ({@code surf < sea}). An ocean's water surface block sits at
 * {@code sea - 1} (overworld sea level 63, topmost water at 62), and {@code getFirstOccupiedHeight} returns
 * {@code getBaseHeight - 1}, so {@code surf} is exactly {@code sea - 1} in every ocean column. The old guard
 * therefore rejected <em>every</em> ocean spot and no deep-ocean structure could ever locate. The fix allows
 * {@code surf == sea - 1}.
 *
 * <p>The existing {@link DeepOceanStructuresTest} places pieces straight onto artificial basins and never
 * exercises the locator, so it could not catch this. This test sweeps genuine deep-ocean chunks from real
 * world generation and asserts each ocean locator finds a submerged floor, then places the Coral-Dome
 * Sanctuary at its naturally located chunk for a real-game screenshot.
 */
public final class DeepOceanPlacementTest implements FabricClientGameTest {
	private static void check(boolean cond, String msg) {
		if (!cond) throw new AssertionError(msg);
	}

	private static boolean isDeepOcean(net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome) {
		return biome.is(Biomes.DEEP_OCEAN) || biome.is(Biomes.DEEP_COLD_OCEAN)
			|| biome.is(Biomes.DEEP_FROZEN_OCEAN) || biome.is(Biomes.DEEP_LUKEWARM_OCEAN);
	}

	private static float yaw(Vec3 from, Vec3 to) {
		Vec3 d = to.subtract(from);
		return (float) Math.toDegrees(Math.atan2(-d.x, d.z));
	}

	private static float pitch(Vec3 from, Vec3 to) {
		Vec3 d = to.subtract(from);
		return (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
	}

	private static int countBlocks(ServerLevel l, BoundingBox bb, Block block) {
		int c = 0;
		for (int x = bb.minX(); x <= bb.maxX(); x++) {
			for (int y = bb.minY(); y <= bb.maxY(); y++) {
				for (int z = bb.minZ(); z <= bb.maxZ(); z++) {
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
				.adjustSettings(s -> s.setSeed("20261123"))
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

			// Sweep real deep-ocean chunks and assert every ocean locator finds a submerged floor.
			int[] evidence = world.getServer().computeOnServer(server -> {
				ServerLevel l = server.overworld();
				var cs = l.getChunkSource();
				var gen = cs.getGenerator();
				var randomState = cs.randomState();
				int sea = gen.getSeaLevel();
				var sampler = randomState.sampler();

				List<ChunkPos> candidates = new ArrayList<>();
				for (int cx = -128; cx <= 128; cx += 4) {
					for (int cz = -128; cz <= 128; cz += 4) {
						int bx = cx * 16 + 8;
						int bz = cz * 16 + 8;
						var biome = gen.getBiomeSource().getNoiseBiome(
							QuartPos.fromBlock(bx), QuartPos.fromBlock(sea - 2), QuartPos.fromBlock(bz), sampler);
						if (isDeepOcean(biome)) candidates.add(new ChunkPos(cx, cz));
					}
				}
				check(!candidates.isEmpty(), "Test seed must expose genuine deep ocean within +/-2048 blocks");
				// Spread a bounded sample across the whole sweep so the large Citadel footprint gets a fair
				// chance at a flat deep floor instead of clustering in one corner.
				if (candidates.size() > 240) {
					List<ChunkPos> spread = new ArrayList<>();
					int stride = (int) Math.ceil(candidates.size() / 240.0);
					for (int i = 0; i < candidates.size(); i += stride) spread.add(candidates.get(i));
					candidates = spread;
				}

				String[] ids = {
					OceanSites.CORAL_DOME_ID, OceanSites.GALLEON_ID, OceanSites.WATCHTOWER_ID,
					OceanSites.FORGE_ID, OceanSites.CITADEL_ID
				};
				int[] perStructure = new int[ids.length];
				int coralSuccess = 0;
				int tested = 0;
				ChunkPos firstChunk = null;
				int firstSurf = -1;
				int firstFloor = -1;
				for (ChunkPos cp : candidates) {
					if (tested >= 240) break;
					tested++;
					var ctx = GametestCompat.realStructureContext(l, cp);
					for (int i = 0; i < ids.length; i++) {
						Optional<Structure.GenerationStub> stub = Sites.locator(ids[i]).locate(ctx);
						if (stub.isEmpty()) continue;
						perStructure[i]++;
						if (i == 0) {
							coralSuccess++;
							if (firstChunk == null) {
								firstChunk = cp;
								int bx = cp.getMinBlockX() + 24;
								int bz = cp.getMinBlockZ() + 24;
								firstSurf = gen.getFirstOccupiedHeight(bx, bz, Heightmap.Types.WORLD_SURFACE_WG, l, randomState);
								firstFloor = gen.getFirstOccupiedHeight(bx, bz, Heightmap.Types.OCEAN_FLOOR_WG, l, randomState);
							}
						}
					}
				}
				for (int i = 0; i < ids.length; i++) {
					check(perStructure[i] > 0,
						"Deep-ocean locator found no submerged floor for " + ids[i] + " across " + tested + " genuine deep-ocean chunks");
				}
				check(firstSurf == sea - 1,
					"Regression boundary: ocean WORLD_SURFACE_WG first-occupied height must be sea-1 (got " + firstSurf + ", sea " + sea + ")");
				check(firstFloor <= firstSurf - 4, "Located floor must sit under at least four blocks of open water");
				System.out.println("[DeepOceanPlacement] sea=" + sea + " candidates=" + candidates.size()
					+ " tested=" + tested + " coralSuccess=" + coralSuccess
					+ " firstSurf=" + firstSurf + " firstFloor=" + firstFloor
					+ " perStructure=" + Arrays.toString(perStructure));
				return new int[] {
					sea, candidates.size(), coralSuccess, firstSurf, firstFloor,
					firstChunk == null ? 0 : firstChunk.x(), firstChunk == null ? 0 : firstChunk.z()
				};
			});

			// Place the Coral-Dome Sanctuary at its naturally located chunk and screenshot it in real deep ocean.
			final int fcx = evidence[5];
			final int fcz = evidence[6];
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				ChunkPos cp = new ChunkPos(fcx, fcz);
				var ctx = GametestCompat.realStructureContext(l, cp);
				var stub = Sites.locator(OceanSites.CORAL_DOME_ID).locate(ctx).orElseThrow();
				BlockPos c = stub.position();
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				p.setGameMode(GameType.SPECTATOR);
				p.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 600, 0, false, false, false));
				p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0, false, false, false));
				p.teleportTo(l, c.getX() + 34.0, c.getY() + 20.0, c.getZ() + 34.0, Set.<Relative>of(), -135f, 32f, false);
			});
			context.waitTicks(60);
			world.getServer().runOnServer(server -> {
				ServerLevel l = server.overworld();
				ChunkPos cp = new ChunkPos(fcx, fcz);
				var ctx = GametestCompat.realStructureContext(l, cp);
				var stub = Sites.locator(OceanSites.CORAL_DOME_ID).locate(ctx).orElseThrow();
				StructurePiecesBuilder builder = stub.getPiecesBuilder();
				var key = ResourceKey.create(Registries.STRUCTURE, dev.wildercord.Wildercord.id(OceanSites.CORAL_DOME_ID));
				var structure = server.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(key);
				StructureStart start = new StructureStart(structure.value(), cp, 0, builder.build());
				var gen = l.getChunkSource().getGenerator();
				RandomSource rand = RandomSource.create(20261123L);
				for (StructurePiece piece : start.getPieces()) {
					var bb = piece.getBoundingBox();
					piece.postProcess(l, l.structureManager(), gen, rand, bb, ChunkPos.containing(bb.getCenter()), bb.getCenter());
				}
				BoundingBox box = start.getBoundingBox();
				check(countBlocks(l, box, Blocks.CONDUIT) >= 1, "Naturally located Coral-Dome placed its central Conduit");
				check(countBlocks(l, box, Blocks.STAINED_GLASS.cyan()) > 80, "Naturally located Coral-Dome built its cyan glass domes");

				// Frame the camera on the placed dome from a close underwater vantage.
				BlockPos c = stub.position();
				Vec3 eye = new Vec3(c.getX() + 34.0, c.getY() + 20.0, c.getZ() + 34.0);
				Vec3 target = new Vec3(c.getX(), c.getY() + 6.0, c.getZ());
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				p.teleportTo(l, eye.x, eye.y, eye.z, Set.<Relative>of(), yaw(eye, target), pitch(eye, target), false);
			});
			context.waitTicks(20);
			context.runOnClient(mc -> {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				mc.setCameraEntity(mc.player);
				if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
				mc.gui.toastManager().clear();
				mc.gui.hud.getChat().clearMessages(false);
			});
			context.waitTicks(8);
			context.takeScreenshot(TestScreenshotOptions.of("ocean_placement_coral_dome").disableCounterPrefix());
		}
	}
}