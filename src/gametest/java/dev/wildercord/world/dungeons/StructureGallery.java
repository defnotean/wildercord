package dev.wildercord.world.dungeons;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Photographs every registered WilderCord structure in a normal world, for judging how the builds look: each is found in
 * its own biome through the registered generation path, placed with {@code place structure}, then shot from above at a
 * three-quarter angle and from inside at its middle. Split across a few classes so no single client runs for too long.
 */
final class StructureGallery {
	private StructureGallery() {}

	private record Site(String id, BlockPos command, BoundingBox bounds) {}

	/** Shoots the structures whose sorted index is {@code shard} modulo {@code shards}. */
	static void shoot(ClientGameTestContext c, int shard, int shards) {
		var missing = new ArrayList<String>();
		try (var world = c.worldBuilder().setUseConsistentSettings(false)
				.adjustSettings(settings -> settings.setSeed(BattlefieldsTest.REPRESENTATIVE_SEED)).create()) {
			c.waitTicks(40);
			for (String rule : List.of("spawn_mobs false", "advance_time false", "advance_weather false")) world.getServer().runCommand("gamerule " + rule);
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather clear");
			c.runOnClient(mc -> { mc.getWindow().setWindowed(1280, 720); mc.options.renderDistance().set(8); mc.options.fov().set(70); mc.resizeGui(); });
			world.getServer().runOnServer(s -> {
				var p = s.getPlayerList().getPlayers().getFirst();
				p.setGameMode(GameType.SPECTATOR);
				p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1_000_000, 0, false, false));
			});
			List<String> ids = world.getServer().computeOnServer(s -> s.overworld().registryAccess().lookupOrThrow(Registries.STRUCTURE).listElementIds()
				.map(k -> k.identifier()).filter(id -> id.getNamespace().equals("wildercord")).map(id -> id.getPath()).sorted().toList());
			for (int i = 0; i < ids.size(); i++) {
				if (i % shards != shard) continue;
				String id = ids.get(i);
				BlockPos origin = new BlockPos(((i % 8) - 4) * 2500, 64, ((i / 8) - 5) * 2500);
				Site site = world.getServer().computeOnServer(s -> find(s.overworld(), id, origin));
				if (site == null) { missing.add(id); log(id, "no valid start near " + origin); continue; }
				boolean placed = world.getServer().computeOnServer(s -> {
					var b = site.bounds();
					for (int cx = (b.minX() >> 4) - 2; cx <= (b.maxX() >> 4) + 2; cx++) for (int cz = (b.minZ() >> 4) - 2; cz <= (b.maxZ() >> 4) + 2; cz++) s.overworld().getChunk(cx, cz);
					boolean[] ok = {false};
					s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput().withCallback((success, value) -> ok[0] = success && value == 1),
						"place structure wildercord:" + id + " " + site.command().getX() + " " + site.command().getY() + " " + site.command().getZ());
					return ok[0];
				});
				if (!placed) { missing.add(id); log(id, "place structure refused at " + site.command()); continue; }
				var b = site.bounds();
				log(id, "bounds " + b.minX() + "," + b.minY() + "," + b.minZ() + " to " + b.maxX() + "," + b.maxY() + "," + b.maxZ());
				double cx = (b.minX() + b.maxX() + 1) / 2.0, cz = (b.minZ() + b.maxZ() + 1) / 2.0;
				int span = Math.max(b.getXSpan(), b.getZSpan());
				double d = span * 0.8 + 10, lift = Math.max(10, span * 0.5);
				view(c, world, cx + d * 0.7, b.maxY() + lift, cz + d * 0.7, cx, b.minY() + b.getYSpan() * 0.4, cz);
				shot(c, "gallery_" + id);
				boolean alongX = b.getXSpan() >= b.getZSpan();
				double y = b.minY() + Math.max(2, b.getYSpan() * 0.3);
				view(c, world, alongX ? b.minX() + 2.5 : cx, y, alongX ? cz : b.minZ() + 2.5, alongX ? b.maxX() : cx, y - 2, alongX ? cz : b.maxZ());
				shot(c, "gallery_" + id + "_inside");
			}
		}
		log("summary", "shard " + shard + "/" + shards + " missing=" + missing);
		if (!missing.isEmpty()) throw new AssertionError("Structures that could not be found and placed near their search origins: " + missing);
	}

	/** The nearest valid registered start of {@code id}, searched outward from its biome closest to {@code origin}. */
	private static Site find(ServerLevel level, String id, BlockPos origin) {
		var holder = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(ResourceKey.create(Registries.STRUCTURE,
			net.minecraft.resources.Identifier.fromNamespaceAndPath("wildercord", id)));
		var structure = holder.value();
		var biome = level.findClosestBiome3d(structure.biomes()::contains, origin, 6400, 32, 64);
		if (biome == null) return null;
		var source = level.getChunkSource();
		var generator = source.getGenerator();
		var randomState = source.randomState();
		var sampler = randomState.createClimateSampler(SamplerContext.EMPTY_UNCACHED);
		var centre = ChunkPos.containing(biome.getFirst());
		for (int r = 0; r <= 24; r++) for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
			if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
			var chunk = new ChunkPos(centre.x() + dx, centre.z() + dz);
			var start = structure.generate(holder, level.dimension(), level.registryAccess(), generator, generator.getBiomeSource(), sampler, randomState,
				level.getStructureTemplateManager(), level.getSeed(), chunk, 0, level, structure.biomes()::contains);
			if (start.isValid()) return new Site(id, new BlockPos(chunk.getMinBlockX(), 70, chunk.getMinBlockZ()), start.getBoundingBox());
		}
		return null;
	}

	/** Puts the spectating player's eye at {@code (x, y, z)} looking at {@code (tx, ty, tz)}, and waits for the view to draw. */
	private static void view(ClientGameTestContext c, net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext world,
			double x, double y, double z, double tx, double ty, double tz) {
		double eye = 1.62, dx = tx - x, dy = ty - (y + eye), dz = tz - z;
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		world.getServer().runOnServer(s -> {
			// Generate the ground around the camera first, so the client has nothing slow left to wait for.
			int ccx = (int) Math.floor(x) >> 4, ccz = (int) Math.floor(z) >> 4;
			for (int cx = ccx - 6; cx <= ccx + 6; cx++) for (int cz = ccz - 6; cz <= ccz + 6; cz++) s.overworld().getChunk(cx, cz);
			var p = s.getPlayerList().getPlayers().getFirst();
			p.teleportTo(s.overworld(), x, y, z, Set.<Relative>of(), yaw, pitch, false);
		});
		c.waitTicks(60);
		try {
			world.getConnection().waitForChunksRender();
		} catch (AssertionError slow) {
			// A far chunk still drawing only blurs the horizon; take the picture anyway.
			log("view", "chunks still rendering at " + (int) x + "," + (int) z);
			c.waitTicks(100);
		}
		c.waitTicks(10);
	}

	private static void shot(ClientGameTestContext c, String name) {
		c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}

	private static void log(String id, String what) {
		dev.wildercord.Wildercord.LOGGER.info("STRUCTURE_GALLERY {} {}", id, what);
	}
}
