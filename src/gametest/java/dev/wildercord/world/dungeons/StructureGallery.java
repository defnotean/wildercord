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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Photographs every registered WilderCord structure in a normal world, for judging how the builds look: each is found in
 * its own biome through the registered generation path, placed with {@code place structure}, then shot from above at a
 * three-quarter angle from two opposite corners (each turned until nothing outside the build hides it), at eye level from the ground, straight down, and from inside at its middle. Split across a few classes so no single client runs for too long.
 */
final class StructureGallery {
	private StructureGallery() {}

	private record Site(String id, ResourceKey<net.minecraft.world.level.Level> dimension, BlockPos command, BoundingBox bounds) {}

	/** Shoots the structures whose sorted index is {@code shard} modulo {@code shards}. */
	static void shoot(ClientGameTestContext c, int shard, int shards) {
		var missing = new ArrayList<String>();
		try (var world = c.worldBuilder().setUseConsistentSettings(false)
				.adjustSettings(settings -> settings.setSeed(BattlefieldsTest.REPRESENTATIVE_SEED)).create()) {
			c.waitTicks(40);
			for (String rule : List.of("spawn_mobs false", "advance_time false", "advance_weather false")) world.getServer().runCommand("gamerule " + rule);
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather clear");
			c.runOnClient(mc -> { mc.getWindow().setWindowed(1280, 720); mc.options.renderDistance().set(8); mc.options.fov().set(70); mc.resizeGui();
				if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle(); });
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
				Site site = world.getServer().computeOnServer(s -> find(home(s, id), id, origin));
				if (site == null) { missing.add(id); log(id, "no valid start near " + origin); continue; }
				boolean placed = world.getServer().computeOnServer(s -> {
					var b = site.bounds();
					var level = s.getLevel(site.dimension());
					for (int cx = (b.minX() >> 4) - 2; cx <= (b.maxX() >> 4) + 2; cx++) for (int cz = (b.minZ() >> 4) - 2; cz <= (b.maxZ() >> 4) + 2; cz++) level.getChunk(cx, cz);
					boolean[] ok = {false};
					s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput().withCallback((success, value) -> ok[0] = success && value == 1),
						"execute in " + site.dimension().identifier() + " run place structure wildercord:" + id + " " + site.command().getX() + " " + site.command().getY() + " " + site.command().getZ());
					return ok[0];
				});
				if (!placed) { missing.add(id); log(id, "place structure refused at " + site.command()); continue; }
				var b = site.bounds();
				log(id, "bounds " + b.minX() + "," + b.minY() + "," + b.minZ() + " to " + b.maxX() + "," + b.maxY() + "," + b.maxZ());
				double cx = (b.minX() + b.maxX() + 1) / 2.0, cz = (b.minZ() + b.maxZ() + 1) / 2.0;
				int span = Math.max(b.getXSpan(), b.getZSpan());
				double d = span * 0.6 + 8, lift = Math.max(8, span * 0.35 + 4), ty = b.minY() + b.getYSpan() * 0.4;
				var dim = site.dimension();
				// Under the Nether's bedrock roof there is no sky to shoot from, so keep every camera below it.
				double roof = dim == net.minecraft.world.level.Level.NETHER ? 120 : Double.MAX_VALUE;
				double air = Math.min(roof, b.maxY() + lift);
				// Two aerial three-quarter views from roughly opposite corners, so every face is seen once, each turned until
				// no tree or hillside stands between the camera and the build.
				double[] front = world.getServer().computeOnServer(s -> spot(s.getLevel(dim), b, Math.PI / 4, d, (x, z) -> air, cx, ty, cz));
				view(c, world, dim, front[0], front[1], front[2], cx, ty, cz);
				shot(c, "gallery_" + id);
				double[] back = world.getServer().computeOnServer(s -> spot(s.getLevel(dim), b, Math.atan2(front[2] - cz, front[0] - cx) + Math.PI, d, (x, z) -> air, cx, ty, cz));
				view(c, world, dim, back[0], back[1], back[2], cx, ty, cz);
				shot(c, "gallery_" + id + "_back");
				// Eye level from the ground beside it, as a player walking up would see it, and a plan from straight above.
				double[] ground = world.getServer().computeOnServer(s -> {
					var level = s.getLevel(dim);
					return spot(level, b, Math.PI * 0.83, d * 0.9, (x, z) -> footing(level, b, roof, x, z), cx, ty, cz);
				});
				view(c, world, dim, ground[0], ground[1], ground[2], cx, Math.max(ground[1] + 1.5, ty), cz);
				shot(c, "gallery_" + id + "_ground");
				view(c, world, dim, cx + 0.01, Math.min(roof, b.maxY() + span * 0.75 + 6), cz + 0.01, cx, b.minY(), cz);
				shot(c, "gallery_" + id + "_plan");
				boolean alongX = b.getXSpan() >= b.getZSpan();
				double[] in = world.getServer().computeOnServer(s -> inside(s.getLevel(dim), b, alongX));
				view(c, world, dim, in[0], in[1], in[2], alongX ? b.maxX() : in[0], in[1] - 0.5, alongX ? in[2] : b.maxZ());
				shot(c, "gallery_" + id + "_inside");
			}
		}
		log("summary", "shard " + shard + "/" + shards + " missing=" + missing);
		if (!missing.isEmpty()) throw new AssertionError("Structures that could not be found and placed near their search origins: " + missing);
	}

	/** The dimension whose biomes can host {@code id}: the overworld unless only the Nether's or the End's biomes are in its tag. */
	private static ServerLevel home(net.minecraft.server.MinecraftServer server, String id) {
		var biomes = server.overworld().registryAccess().lookupOrThrow(Registries.STRUCTURE).getValueOrThrow(ResourceKey.create(Registries.STRUCTURE,
			net.minecraft.resources.Identifier.fromNamespaceAndPath("wildercord", id))).biomes();
		for (ServerLevel level : server.getAllLevels())
			if (level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes().stream().anyMatch(biomes::contains)) return level;
		return server.overworld();
	}

	/** The nearest valid registered start of {@code id}, searched outward from its biome closest to {@code origin}. */
	private static Site find(ServerLevel level, String id, BlockPos origin) {
		var holder = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(ResourceKey.create(Registries.STRUCTURE,
			net.minecraft.resources.Identifier.fromNamespaceAndPath("wildercord", id)));
		var structure = holder.value();
		var source = level.getChunkSource();
		var generator = source.getGenerator();
		var randomState = source.randomState();
		var sampler = randomState.createClimateSampler(SamplerContext.EMPTY_UNCACHED);
		java.util.function.Function<ChunkPos, Site> at = chunk -> {
			var start = structure.generate(holder, level.dimension(), level.registryAccess(), generator, generator.getBiomeSource(), sampler, randomState,
				level.getStructureTemplateManager(), level.getSeed(), chunk, 0, level, structure.biomes()::contains);
			return start.isValid() ? new Site(id, level.dimension(), new BlockPos(chunk.getMinBlockX(), 70, chunk.getMinBlockZ()), start.getBoundingBox()) : null;
		};
		var biome = level.findClosestBiome3d(structure.biomes()::contains, origin, 6400, 32, 64);
		if (biome != null) {
			var centre = ChunkPos.containing(biome.getFirst());
			for (int r = 0; r <= 24; r++) for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
				if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
				Site site = at.apply(new ChunkPos(centre.x() + dx, centre.z() + dz));
				if (site != null) return site;
			}
		}
		// Picky sites (a wide flat footprint, say) may find no room near the closest patch of their biome: fall back on the
		// game's own locate, which walks the real placement grid and only answers with a start that generates.
		BlockPos found = level.findNearestMapStructure(net.minecraft.core.HolderSet.direct(holder), origin, 100, false);
		if (found == null) return null;
		log(id, "found by locate at " + found);
		return at.apply(ChunkPos.containing(found));
	}

	/**
	 * A camera spot {@code d} out from the middle of {@code b}, starting at {@code angle} and turning either way, then
	 * stepping further out, until nothing outside the structure hides its middle. Falls back on the least hidden spot.
	 */
	private static double[] spot(ServerLevel level, BoundingBox b, double angle, double d, java.util.function.DoubleBinaryOperator feet,
			double tx, double ty, double tz) {
		double cx = (b.minX() + b.maxX() + 1) / 2.0, cz = (b.minZ() + b.maxZ() + 1) / 2.0;
		double[] best = null;
		int fewest = Integer.MAX_VALUE;
		for (double scale : new double[] {1, 1.3, 0.85, 1.7}) for (int k = 0; k < 12; k++) {
			double a = angle + ((k + 1) / 2) * (k % 2 == 0 ? 1 : -1) * Math.PI / 6;
			double x = cx + Math.cos(a) * d * scale, z = cz + Math.sin(a) * d * scale, y = feet.applyAsDouble(x, z);
			if (b.inflatedBy(1).isInside(BlockPos.containing(x, y + 1.62, z))) continue;
			int n = blockers(level, b, x, y + 1.62, z, tx, ty, tz);
			if (n == 0) return new double[] {x, y, z};
			if (n < fewest) { fewest = n; best = new double[] {x, y, z}; }
		}
		return best != null ? best : new double[] {cx + d, feet.applyAsDouble(cx + d, cz), cz};
	}

	/** Solid blocks between the eye and the target that are not part of the structure itself. */
	private static int blockers(ServerLevel level, BoundingBox b, double x, double y, double z, double tx, double ty, double tz) {
		double dx = tx - x, dy = ty - y, dz = tz - z, len = Math.sqrt(dx * dx + dy * dy + dz * dz);
		var site = b.inflatedBy(1);
		int n = 0;
		BlockPos last = null;
		for (double t = 0; t < len; t += 0.5) {
			var pos = BlockPos.containing(x + dx * t / len, y + dy * t / len, z + dz * t / len);
			if (pos.equals(last)) continue;
			last = pos;
			if (site.isInside(pos)) break;
			if (!level.getBlockState(pos).isAir()) n++;
		}
		return n;
	}

	/** Where a player standing at {@code (x, z)} has their feet: the surface, or in the Nether the first open footing above the site's floor. */
	private static double footing(ServerLevel level, BoundingBox b, double roof, double x, double z) {
		int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
		if (roof == Double.MAX_VALUE) return level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz) + 0.5;
		// The Nether's heightmap is its roof: walk up from the site's floor to the first open footing instead.
		for (int y = b.minY(); y < roof - 2; y++)
			if (level.getBlockState(new BlockPos(bx, y, bz)).isAir() && level.getBlockState(new BlockPos(bx, y + 1, bz)).isAir()
					&& !level.getBlockState(new BlockPos(bx, y - 1, bz)).isAir()) return y;
		return b.minY() + 1;
	}

	/** An open spot inside the structure, near one end of its longer axis, with a few blocks of air ahead to look down. */
	private static double[] inside(ServerLevel level, BoundingBox b, boolean alongX) {
		int span = alongX ? b.getXSpan() : b.getZSpan(), cross = alongX ? (b.minZ() + b.maxZ()) / 2 : (b.minX() + b.maxX()) / 2;
		for (double frac : new double[] {0.1, 0.25, 0.4, 0.55}) for (int y = b.minY() + 1; y < b.maxY() - 1; y++) {
			int along = (alongX ? b.minX() : b.minZ()) + (int) (span * frac);
			boolean open = true;
			for (int k = 0; k < 5 && open; k++) for (int h = 0; h < 2 && open; h++) {
				var pos = alongX ? new BlockPos(along + k, y + h, cross) : new BlockPos(cross, y + h, along + k);
				open = level.getBlockState(pos).isAir();
			}
			if (open) return alongX ? new double[] {along + 0.5, y, cross + 0.5} : new double[] {cross + 0.5, y, along + 0.5};
		}
		double y = b.minY() + Math.max(2, b.getYSpan() * 0.3);
		return alongX ? new double[] {b.minX() + 2.5, y, cross + 0.5} : new double[] {cross + 0.5, y, b.minZ() + 2.5};
	}

	/** Puts the spectating player's eye at {@code (x, y, z)} looking at {@code (tx, ty, tz)}, and waits for the view to draw. */
	private static void view(ClientGameTestContext c, net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext world,
			ResourceKey<net.minecraft.world.level.Level> dim, double x, double y, double z, double tx, double ty, double tz) {
		double eye = 1.62, dx = tx - x, dy = ty - (y + eye), dz = tz - z;
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		world.getServer().runOnServer(s -> {
			// Generate the ground around the camera first, so the client has nothing slow left to wait for.
			int ccx = (int) Math.floor(x) >> 4, ccz = (int) Math.floor(z) >> 4;
			var level = s.getLevel(dim);
			for (int cx = ccx - 6; cx <= ccx + 6; cx++) for (int cz = ccz - 6; cz <= ccz + 6; cz++) level.getChunk(cx, cz);
			var p = s.getPlayerList().getPlayers().getFirst();
			p.teleportTo(level, x, y, z, Set.<Relative>of(), yaw, pitch, false);
		});
		// A fixed wait: on the VPS waitForChunksRender always ran into its minute-long timeout over far horizon chunks,
		// and the ground is already generated, so a few seconds draws everything near the camera.
		c.waitTicks(120);
	}

	private static void shot(ClientGameTestContext c, String name) {
		// Quest and advancement toasts still draw with the HUD hidden.
		c.runOnClient(mc -> mc.gui.toastManager().clear());
		c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}

	private static void log(String id, String what) {
		dev.wildercord.Wildercord.LOGGER.info("STRUCTURE_GALLERY {} {}", id, what);
	}
}
