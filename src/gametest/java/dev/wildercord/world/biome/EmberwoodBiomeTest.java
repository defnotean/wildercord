package dev.wildercord.world.biome;

import dev.wildercord.Wildercord;
import dev.wildercord.lore.LoreJournal;
import dev.wildercord.wildlife.CinderFernBlock;
import dev.wildercord.wildlife.EmberContent;
import dev.wildercord.wildlife.Wildlife;
import dev.wildercord.world.sites.emberwood.EmberwoodYardChecks;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/** Untouched default-preset Emberwood Reach terrain, regional discovery persistence, and Emberwood Charcoal Yard checks. */
public final class EmberwoodBiomeTest implements FabricClientGameTest {
	private static final String SEED = "-7620530482425397421";
	private int chunks, columns, podzolOrCoarse, grass, logs, ferns;
	private BlockPos firstFern, firstColumn;

	@Override public void runTest(ClientGameTestContext context) {
		boolean hidden = context.computeOnClient(mc -> mc.gui.hud.isHidden());
		TestWorldSave save;
		BlockPos focus;
		try (var world = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(settings -> settings.setSeed(SEED)).create()) {
			context.waitTicks(35);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule random_tick_speed 0");
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather clear");
			BlockPos center = world.getServer().computeOnServer(server -> {
				check(server.overworld().getSeed() == Long.parseLong(SEED), "The terrain uses the recorded seed");
				registered(server.overworld());
				return findRegion(server.overworld());
			});
			for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
				int cx = (center.getX() >> 4) + dx, cz = (center.getZ() >> 4) + dz;
				world.getServer().runOnServer(server -> observe(server.overworld(), cx, cz));
				context.waitTicks(1);
			}
			focus = world.getServer().computeOnServer(server -> {
				BlockPos chosen = firstFern != null ? firstFern : firstColumn;
				Wildercord.LOGGER.info("EMBERWOOD_TERRAIN seed={}, center={}, chunks={}, columns={}, podzolOrCoarse={}, grass={}, logs={}, ferns={}, focus={}",
					SEED, center, chunks, columns, podzolOrCoarse, grass, logs, ferns, chosen);
				check(chunks == 81 && columns >= 256, "The fixed 81-chunk survey contains a real Emberwood Reach region");
				check(podzolOrCoarse > 0 && grass > 0 && logs > 0 && chosen != null,
					"Natural Emberwood Reach ground combines podzol/coarse-dirt ash humus, grass and warm woodland timber");
				check(ferns > 0 && firstFern != null, "Natural decoration places Cinder Fern in its region without placement commands");
				EmberwoodYardChecks.verifyLocator(server.overworld(), center);
				var player = server.getPlayerList().getPlayers().getFirst();
				check(!LoreJournal.data(player).has(EmberwoodClimate.JOURNAL_ENTRY), "The player has not already discovered the distant Emberwood region");
				player.setGameMode(GameType.SPECTATOR);
				player.teleportTo(server.overworld(), chosen.getX() + .5, chosen.getY() + 1, chosen.getZ() + .5, Set.of(), 0, 0, false);
				return chosen;
			});
			context.waitTicks(LoreJournal.POLL_TICKS + 5);
			world.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().getFirst();
				check(!LoreJournal.data(player).has(EmberwoodClimate.JOURNAL_ENTRY), "Spectator travel does not earn regional discovery");
				player.setGameMode(GameType.CREATIVE);
			});
			context.waitTicks(LoreJournal.POLL_TICKS + 5);
			world.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().getFirst();
				check(LoreJournal.data(player).has(EmberwoodClimate.JOURNAL_ENTRY), "Entering the region earns the real polled field note");
				LoreJournal.poll(player);
				check(LoreJournal.data(player).entries().stream().filter(EmberwoodClimate.JOURNAL_ENTRY::equals).count() == 1, "Returning does not duplicate the note");
				player.setGameMode(GameType.SPECTATOR);
				Vec3 target = Vec3.atCenterOf(focus), eye = camera(server.overworld(), player, target);
				Vec3 look = target.subtract(eye.add(0, player.getEyeHeight(), 0));
				player.teleportTo(server.overworld(), eye.x, eye.y, eye.z, Set.of(),
					(float) Math.toDegrees(Math.atan2(-look.x, look.z)), (float) -Math.toDegrees(Math.atan2(look.y, look.horizontalDistance())), false);
			});
			context.waitTicks(20);
			world.getConnection().waitForChunksRender();
			context.runOnClient(mc -> {
				check(LoreJournal.data(mc.player).has(EmberwoodClimate.JOURNAL_ENTRY), "The connected owner receives the discovery");
				check(!net.minecraft.network.chat.Component.translatable("journal.wildercord.entry.place.emberwood_reach").getString().startsWith("journal."), "The field note has player-facing text");
				mc.getWindow().setWindowed(1280, 720); mc.options.guiScale().set(2); mc.resizeGui();
				mc.gui.toastManager().clear(); mc.gui.hud.getChat().clearMessages(false);
				if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
			});
			context.waitTicks(5);
			context.takeScreenshot(TestScreenshotOptions.of("emberwood_reach_terrain").disableCounterPrefix());
			save = world.getWorldSave();
		} finally {
			context.runOnClient(mc -> { if (mc.gui.hud.isHidden() != hidden) mc.gui.hud.toggle(); });
		}
		try (var world = save.open()) {
			world.getServer().runOnServer(server -> {
				check(LoreJournal.data(server.getPlayerList().getPlayers().getFirst()).has(EmberwoodClimate.JOURNAL_ENTRY), "The owner keeps the regional discovery after save/reopen");
				check(server.overworld().getBiome(focus).is(EmberwoodClimate.EMBERWOOD), "Saved chunks keep their Emberwood Reach identity");
				var state = server.overworld().getBlockState(firstFern);
				check(state.is(EmberContent.FERN) && state.getValue(CinderFernBlock.AGE) == 2 && !state.getValue(CinderFernBlock.COOLED),
					"Natural warm Cinder Fern persists across save/reopen");
				registered(server.overworld());
			});
		}
		EmberwoodYardChecks.run(context);
	}

	private static void registered(ServerLevel level) {
		var holder = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(EmberwoodClimate.EMBERWOOD);
		check(level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes().contains(holder), "The default Overworld can select Emberwood Reach");
		var biome = holder.value();
		var spawns = biome.getAttributes().applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS, MobSpawnSettings.EMPTY);
		for (var type : List.of(Wildlife.CINDERFOX, dev.wildercord.wildlife.PredatorContent.DUNE_COUGAR)) {
			check(spawns.getMobsToSpawn(MobCategory.CREATURE).unwrap().stream().anyMatch(entry -> entry.value().type() == type), "Actual Emberwood creature spawn pool contains " + type);
		}
		check(spawns.getMobsToSpawn(MobCategory.MONSTER).unwrap().stream().anyMatch(entry -> entry.value().type() == EmberContent.BAILIFF), "Cinder Bailiffs patrol Emberwood Reach");
		var feature = ResourceKey.create(Registries.PLACED_FEATURE, Wildercord.id("cinder_fern_patch"));
		check(biome.getGenerationSettings().features().get(GenerationStep.Decoration.VEGETAL_DECORATION.ordinal())
			.stream().filter(entry -> entry.is(feature)).count() == 1, "Exactly one real Cinder Fern decorator belongs to Emberwood Reach");
		for (String site : List.of("master_forge_dojo", "wilds_iron_gatehouse", "emberwood_charcoal_yard", "emberwood_kiln_court")) {
			var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(ResourceKey.create(Registries.STRUCTURE, Wildercord.id(site))).value();
			check(structure.biomes().contains(holder), "The loaded structure admits Emberwood Reach: " + site);
			check(holder.is(TagKey.create(Registries.BIOME, Wildercord.id("has_structure/" + site))), "The structure biome tag agrees: " + site);
		}
	}

	private static Vec3 camera(ServerLevel level, ServerPlayer player, Vec3 target) {
		for (int[] offset : new int[][]{{-5, 3, -5}, {-6, 4, -4}, {5, 3, -5}, {-5, 3, 5}, {0, 4, -6}, {-6, 4, 0}, {5, 4, 5}, {0, 5, -4}}) {
			Vec3 feet = target.add(offset[0], offset[1], offset[2]);
			BlockPos pos = BlockPos.containing(feet);
			if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) continue;
			Vec3 eye = feet.add(0, player.getEyeHeight(), 0);
			if (level.clip(new net.minecraft.world.level.ClipContext(eye, target,
					net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, player)).getType()
					== HitResult.Type.MISS) {
				return feet;
			}
		}
		return target.add(-4, 4, -4);
	}

	private static BlockPos findRegion(ServerLevel level) {
		var resolver = level.getChunkSource().getGenerator().getBiomeSource().createUncachedResolver(level.getChunkSource().randomState());
		for (int radius = 0; radius <= 8192; radius += 64) {
			for (int dx = -radius; dx <= radius; dx += 64) for (int dz = -radius; dz <= radius; dz += 64) {
				if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
				if (resolver.getNoiseBiome(dx >> 2, 16, dz >> 2).is(EmberwoodClimate.EMBERWOOD)) return new BlockPos(dx, 64, dz);
			}
		}
		throw new AssertionError("No naturally selected Emberwood Reach in the fixed 8,192-block climate survey");
	}

	private void observe(ServerLevel level, int cx, int cz) {
		level.getChunk(cx, cz); chunks++;
		for (int x = cx * 16; x < cx * 16 + 16; x++) for (int z = cz * 16; z < cz * 16 + 16; z++) {
			int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			var surface = new BlockPos(x, top, z);
			if (!level.getBiome(surface).is(EmberwoodClimate.EMBERWOOD)) continue;
			columns++;
			if (firstColumn == null) firstColumn = surface;
			var ground = level.getBlockState(surface.below());
			if (ground.is(Blocks.PODZOL) || ground.is(Blocks.COARSE_DIRT)) podzolOrCoarse++;
			if (ground.is(Blocks.GRASS_BLOCK)) grass++;
			for (int y = top - 2; y <= top + 4; y++) {
				var pos = new BlockPos(x, y, z);
				var state = level.getBlockState(pos);
				if (state.is(BlockTags.LOGS)) logs++;
				if (state.is(EmberContent.FERN) && level.getBiome(pos).is(EmberwoodClimate.EMBERWOOD)) {
					ferns++;
					if (firstFern == null) firstFern = pos;
				}
			}
		}
	}

	private static void check(boolean result, String message) { if (!result) throw new AssertionError(message); }
}
