package dev.wildercord.world.biome;

import dev.wildercord.Wildercord;
import dev.wildercord.wildlife.ReedbackContent;
import dev.wildercord.wildlife.SiltcrestContent;
import dev.wildercord.wildlife.WetlandContent;
import dev.wildercord.wildlife.WetlandGarden;
import dev.wildercord.wildlife.Wildlife;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/** A real default-preset biome, with natural banks and plants, followed by a saved-world reopen. */
public final class ReedlandBiomeTest implements FabricClientGameTest {
	private static final String SEED = "-7620530482425397421";
	private static final BlockPos SEARCH_ORIGIN = new BlockPos(-1744, 64, -800);
	private int columns, waterColumns, plants, chunks;
	private BlockPos firstPlant;

	@Override
	public void runTest(ClientGameTestContext context) {
		boolean originalHudHidden = context.computeOnClient(mc -> mc.gui.hud.isHidden());
		net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave save;
		BlockPos center;
		try (var world = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(settings -> settings.setSeed(SEED)).create()) {
			context.waitTicks(35);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("time set 6000");
			center = world.getServer().computeOnServer(server -> {
				var level = server.overworld();
				check(level.getSeed() == Long.parseLong(SEED), "The natural terrain uses its recorded fixed seed");
				registered(level);
				return findRegion(level);
			});
			// Fixed seven-by-seven census. Never expand it after a missing-plant result.
			try (var probe = dev.wildercord.wildlife.WetlandGenerationProbe.beginRegion("reedlands", Long.parseLong(SEED),
				(center.getX() >> 4) - 3, (center.getX() >> 4) + 3, (center.getZ() >> 4) - 3, (center.getZ() >> 4) + 3)) {
				for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
					int cx = (center.getX() >> 4) + dx, cz = (center.getZ() >> 4) + dz;
					world.getServer().runOnServer(server -> observe(server.overworld(), cx, cz));
					context.waitTicks(1);
				}
			}
			world.getServer().runOnServer(server -> {
				Wildercord.LOGGER.info("REEDLANDS_TERRAIN seed={}, center={}, chunks={}, regionColumns={}, waterAt62={}, naturalMoonreeds={}, firstPlant={}",
					SEED, center, chunks, columns, waterColumns, plants, firstPlant);
				check(chunks == 49 && columns >= 256, "The fixed census contains a naturally selected region, not an isolated forced biome cell");
				check(waterColumns > 0, "The custom biome inherits natural swamp surface pools");
				check(plants > 0 && firstPlant != null, "The fixed census contains naturally decorated Moonreed without placement commands");
				dev.wildercord.world.sites.water.ReedkeeperLandingChecks.verifyLocator(server.overworld(), center);
				var player = server.getPlayerList().getPlayers().getFirst();
				player.setGameMode(GameType.CREATIVE);
				player.teleportTo(server.overworld(), firstPlant.getX() + .5, firstPlant.getY() + 2, firstPlant.getZ() + .5, Set.of(), 0, 0, false);
			});
			context.waitTicks(dev.wildercord.lore.LoreJournal.POLL_TICKS + 5);
			world.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().getFirst();
				check(dev.wildercord.lore.LoreJournal.data(player).has(ReedlandClimate.JOURNAL_ENTRY), "Entering the natural region is discovered by the real journal poll");
				dev.wildercord.lore.LoreJournal.poll(player);
				check(dev.wildercord.lore.LoreJournal.data(player).entries().stream().filter(ReedlandClimate.JOURNAL_ENTRY::equals).count() == 1, "Revisiting does not duplicate the regional entry");
				player.setGameMode(GameType.SPECTATOR);
				Vec3 target = Vec3.atCenterOf(firstPlant), eye = camera(server.overworld(), player, target);
				Vec3 look = target.subtract(eye.add(0, player.getEyeHeight(), 0));
				player.teleportTo(server.overworld(), eye.x, eye.y, eye.z, Set.of(),
					(float) Math.toDegrees(Math.atan2(-look.x, look.z)),
					(float) -Math.toDegrees(Math.atan2(look.y, look.horizontalDistance())), false);
			});
			context.waitTicks(20);
			world.getConnection().waitForChunksRender();
			context.runOnClient(mc -> {
				check(dev.wildercord.lore.LoreJournal.data(mc.player).has(ReedlandClimate.JOURNAL_ENTRY), "The owner receives the regional discovery");
				mc.getWindow().setWindowed(1280, 720); mc.options.guiScale().set(2); mc.resizeGui();
				mc.gui.toastManager().clear(); mc.gui.hud.getChat().clearMessages(false);
				if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
			});
			context.waitTicks(5);
			context.takeScreenshot(TestScreenshotOptions.of("luminous_reedlands_day").disableCounterPrefix());
			world.getServer().runCommand("time set 18000");
			context.waitTicks(15);
			context.takeScreenshot(TestScreenshotOptions.of("luminous_reedlands_night").disableCounterPrefix());
			save = world.getWorldSave();
		} finally {
			context.runOnClient(mc -> { if (mc.gui.hud.isHidden() != originalHudHidden) mc.gui.hud.toggle(); });
		}
		try (var world = save.open()) {
			world.getServer().runOnServer(server -> {
				check(dev.wildercord.lore.LoreJournal.data(server.getPlayerList().getPlayers().getFirst()).has(ReedlandClimate.JOURNAL_ENTRY), "The regional field note survives a real save/reopen");
				check(server.overworld().getBiome(firstPlant).is(ReedlandClimate.REEDLANDS), "Saved chunks retain the custom biome identity after reopening");
				check(server.overworld().getBlockState(firstPlant).is(WetlandGarden.REED), "The natural plant persists with its region");
				registered(server.overworld());
			});
			dev.wildercord.world.sites.water.ReedkeeperLandingChecks.captureNatural(context, world, center);
		}
		dev.wildercord.world.sites.water.ReedkeeperLandingChecks.run(context);
	}

	private static void registered(ServerLevel level) {
		var holder = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ReedlandClimate.REEDLANDS);
		var biome = holder.value();
		check(level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes().contains(holder), "The ordinary Overworld preset can actually choose Reedlands");
		var spawns = biome.getAttributes().applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS, MobSpawnSettings.EMPTY);
		for (var kind : List.of(WetlandContent.NEWT, ReedbackContent.CRAB, SiltcrestContent.BITTERN)) {
			check(spawns.getMobsToSpawn(MobCategory.CREATURE).unwrap().stream().anyMatch(entry -> entry.value().type() == kind), "Regional fauna uses the actual shared spawn pool: " + kind);
		}
		check(spawns.getMobsToSpawn(MobCategory.AMBIENT).unwrap().stream().anyMatch(entry -> entry.value().type() == Wildlife.GLIMMERWING), "The Moonreed pollinator belongs to the region");
		var moonreed = ResourceKey.create(Registries.PLACED_FEATURE, Wildercord.id("moonreed_patch"));
		check(biome.getGenerationSettings().features().get(GenerationStep.Decoration.VEGETAL_DECORATION.ordinal())
			.stream().filter(feature -> feature.is(moonreed)).count() == 1, "One real Moonreed decorator belongs to the region");
	}

	private static BlockPos findRegion(ServerLevel level) {
		var resolver = level.getChunkSource().getGenerator().getBiomeSource().createUncachedResolver(level.getChunkSource().randomState());
		// A declared bounded spiral samples noise only; it does not generate a search area of chunks.
		for (int radius = 0; radius <= 4096; radius += 64) {
			for (int dx = -radius; dx <= radius; dx += 64) for (int dz = -radius; dz <= radius; dz += 64) {
				if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
				BlockPos pos = SEARCH_ORIGIN.offset(dx, 0, dz);
				if (resolver.getNoiseBiome(pos.getX() >> 2, 16, pos.getZ() >> 2).is(ReedlandClimate.REEDLANDS)) return pos;
			}
		}
		throw new AssertionError("No naturally selected Reedlands in the fixed 4,096-block climate survey");
	}

	private static Vec3 camera(ServerLevel level, net.minecraft.server.level.ServerPlayer player, Vec3 target) {
		// Choose a real clear sight line; never clear trees or move the naturally generated flower for a capture.
		for (Vec3 offset : List.of(new Vec3(7, 3, 7), new Vec3(-7, 3, 7), new Vec3(-7, 3, -7),
			new Vec3(7, 3, -7), new Vec3(0, 5, 8), new Vec3(0, 5, -8), new Vec3(0, 8, 0))) {
			Vec3 feet = target.add(offset);
			var hit = level.clip(new net.minecraft.world.level.ClipContext(feet.add(0, player.getEyeHeight(), 0), target,
				net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, player));
			if (hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) return feet;
		}
		throw new AssertionError("No clear camera sight line to the natural Reedlands flower");
	}

	private void observe(ServerLevel level, int cx, int cz) {
		var chunk = level.getChunk(cx, cz); chunks++;
		dev.wildercord.wildlife.WetlandGenerationProbe.afterRequestedChunk(chunk.getPos());
		int allHeights = 0;
		for (var section : chunk.getSections()) if (section.maybeHas(state -> state.is(WetlandGarden.REED))) {
			for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) {
				if (section.getBlockState(x, y, z).is(WetlandGarden.REED)) allHeights++;
			}
		}
		Wildercord.LOGGER.info("REEDLANDS_CENSUS chunk={}/{}, reedsAtAllHeights={}", cx, cz, allHeights);
		for (int x = cx * 16; x < cx * 16 + 16; x++) for (int z = cz * 16; z < cz * 16 + 16; z++) {
			int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			if (!level.getBiome(new BlockPos(x, top, z)).is(ReedlandClimate.REEDLANDS)) continue;
			columns++;
			if (level.getFluidState(new BlockPos(x, 62, z)).is(FluidTags.WATER)) waterColumns++;
			for (int y = top - 1; y <= top + 1; y++) {
				var pos = new BlockPos(x, y, z);
				if (level.getBlockState(pos).is(WetlandGarden.REED) && level.getBiome(pos).is(ReedlandClimate.REEDLANDS)) {
					plants++; if (firstPlant == null) firstPlant = pos;
				}
			}
		}
	}

	private static void check(boolean result, String message) { if (!result) throw new AssertionError(message); }
}
