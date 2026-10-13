package dev.wildercord.world.biome;

import dev.wildercord.Wildercord;
import dev.wildercord.lore.LoreJournal;
import dev.wildercord.wildlife.FungalGarden;
import dev.wildercord.wildlife.MossveilContent;
import dev.wildercord.wildlife.RootmoltContent;
import dev.wildercord.wildlife.SporebackContent;
import dev.wildercord.world.sites.belowkeeper.BelowkeeperStationChecks;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/** Untouched default-preset subterranean terrain, regional discovery persistence, and Belowkeeper Listening Station checks. */
public final class BelowkeeperBiomeTest implements FabricClientGameTest {
	private static final String SEED = "-7620530482425397421";
	private int chunks, cells, dripstone, lushFloor, caps, caveFloors;
	private BlockPos firstCap, firstFloor;

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
				BlockPos chosen = firstCap != null ? firstCap : firstFloor;
				Wildercord.LOGGER.info("BELOWKEEPER_TERRAIN seed={}, center={}, chunks={}, cells={}, dripstone={}, lushFloor={}, caps={}, caveFloors={}, focus={}",
					SEED, center, chunks, cells, dripstone, lushFloor, caps, caveFloors, chosen);
				check(chunks == 81 && cells >= 256, "The fixed 81-chunk survey contains a real Belowkeeper Hollows region");
				check(dripstone > 0 && lushFloor > 0 && caveFloors > 0 && chosen != null,
					"Natural Belowkeeper Hollows caves combine dripstone formations, moss/clay shelves and traversable cave floors");
				BelowkeeperStationChecks.verifyLocator(server.overworld(), center);
				var player = server.getPlayerList().getPlayers().getFirst();
				check(!LoreJournal.data(player).has(BelowkeeperClimate.JOURNAL_ENTRY), "The player has not already discovered the distant subterranean region");
				player.setGameMode(GameType.SPECTATOR);
				player.teleportTo(server.overworld(), chosen.getX() + .5, chosen.getY(), chosen.getZ() + .5, Set.of(), 0, 0, false);
				return chosen;
			});
			context.waitTicks(LoreJournal.POLL_TICKS + 5);
			world.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().getFirst();
				check(!LoreJournal.data(player).has(BelowkeeperClimate.JOURNAL_ENTRY), "Spectator travel does not earn regional discovery");
				player.setGameMode(GameType.CREATIVE);
			});
			context.waitTicks(LoreJournal.POLL_TICKS + 5);
			world.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().getFirst();
				check(LoreJournal.data(player).has(BelowkeeperClimate.JOURNAL_ENTRY), "Entering the region earns the real polled field note");
				LoreJournal.poll(player);
				check(LoreJournal.data(player).entries().stream().filter(BelowkeeperClimate.JOURNAL_ENTRY::equals).count() == 1, "Returning does not duplicate the note");
				player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200, 0, false, false, false));
				player.setGameMode(GameType.SPECTATOR);
				Vec3 target = Vec3.atCenterOf(focus), eye = camera(server.overworld(), player, focus);
				Vec3 look = target.subtract(eye.add(0, player.getEyeHeight(), 0));
				player.teleportTo(server.overworld(), eye.x, eye.y, eye.z, Set.of(),
					(float) Math.toDegrees(Math.atan2(-look.x, look.z)), (float) -Math.toDegrees(Math.atan2(look.y, look.horizontalDistance())), false);
			});
			context.waitTicks(20);
			world.getConnection().waitForChunksRender();
			context.runOnClient(mc -> {
				check(LoreJournal.data(mc.player).has(BelowkeeperClimate.JOURNAL_ENTRY), "The connected owner receives the discovery");
				check(!net.minecraft.network.chat.Component.translatable("journal.wildercord.entry.place.belowkeeper_hollows").getString().startsWith("journal."), "The field note has player-facing text");
				mc.getWindow().setWindowed(1280, 720); mc.options.guiScale().set(2); mc.resizeGui();
				mc.gui.toastManager().clear(); mc.gui.hud.getChat().clearMessages(false);
				if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
			});
			context.waitTicks(5);
			context.takeScreenshot(TestScreenshotOptions.of("belowkeeper_hollows_terrain").disableCounterPrefix());
			save = world.getWorldSave();
		} finally {
			context.runOnClient(mc -> { if (mc.gui.hud.isHidden() != hidden) mc.gui.hud.toggle(); });
		}
		try (var world = save.open()) {
			world.getServer().runOnServer(server -> {
				check(LoreJournal.data(server.getPlayerList().getPlayers().getFirst()).has(BelowkeeperClimate.JOURNAL_ENTRY), "The owner keeps the regional discovery after save/reopen");
				check(server.overworld().getBiome(focus).is(BelowkeeperClimate.HOLLOWS), "Saved chunks keep their Belowkeeper Hollows identity");
				registered(server.overworld());
			});
		}
		BelowkeeperStationChecks.run(context);
	}

	private static void registered(ServerLevel level) {
		var holder = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(BelowkeeperClimate.HOLLOWS);
		check(level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes().contains(holder), "The default Overworld can select Belowkeeper Hollows");
		var biome = holder.value();
		var spawns = biome.getAttributes().applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS, MobSpawnSettings.EMPTY);
		for (var type : List.of(SporebackContent.SNAIL, MossveilContent.DORMOUSE)) {
			check(spawns.getMobsToSpawn(MobCategory.CREATURE).unwrap().stream().anyMatch(entry -> entry.value().type() == type), "Actual cave creature spawn pool contains " + type);
		}
		check(spawns.getMobsToSpawn(MobCategory.MONSTER).unwrap().stream().anyMatch(entry -> entry.value().type() == RootmoltContent.STRIDER), "Rootmolt Striders inhabit Belowkeeper Hollows");
		var vegetal = biome.getGenerationSettings().features().get(GenerationStep.Decoration.VEGETAL_DECORATION.ordinal());
		for (String feat : List.of("glowcap_patch", "breathmark_site")) {
			var key = ResourceKey.create(Registries.PLACED_FEATURE, Wildercord.id(feat));
			check(vegetal.stream().filter(entry -> entry.is(key)).count() == 1, "Belowkeeper Hollows includes cave vegetal feature: " + feat);
		}
		var topLayer = biome.getGenerationSettings().features().get(GenerationStep.Decoration.TOP_LAYER_MODIFICATION.ordinal());
		var drainhouse = ResourceKey.create(Registries.PLACED_FEATURE, Wildercord.id("belowkeeper_drainhouse"));
		check(topLayer.stream().filter(entry -> entry.is(drainhouse)).count() == 1, "Belowkeeper Hollows includes drainhouse feature");
		for (String site : List.of("master_resonance_chamber", "belowkeeper_listening_station", "belowkeeper_bell_sanctum")) {
			var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(ResourceKey.create(Registries.STRUCTURE, Wildercord.id(site))).value();
			check(structure.biomes().contains(holder), "The loaded structure admits Belowkeeper Hollows: " + site);
			check(holder.is(TagKey.create(Registries.BIOME, Wildercord.id("has_structure/" + site))), "The structure biome tag agrees: " + site);
		}
	}

	private static Vec3 camera(ServerLevel level, ServerPlayer player, BlockPos focus) {
		Vec3 center = Vec3.atCenterOf(focus);
		for (double dy : new double[]{0, 1, 2}) {
			for (int[] offset : new int[][]{{2, 0}, {-2, 0}, {0, 2}, {0, -2}, {2, 2}, {-2, 2}, {2, -2}, {-2, -2}, {3, 0}, {-3, 0}, {0, 3}, {0, -3}}) {
				Vec3 feet = Vec3.atBottomCenterOf(focus).add(offset[0], dy, offset[1]);
				Vec3 eye = feet.add(0, player.getEyeHeight(), 0);
				BlockPos footPos = BlockPos.containing(feet), headPos = BlockPos.containing(eye);
				if (!level.getBlockState(footPos).isAir() || !level.getBlockState(headPos).isAir()) continue;
				var hit = level.clip(new net.minecraft.world.level.ClipContext(eye, center,
					net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, player));
				if (hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(focus)) return feet;
			}
		}
		return Vec3.atBottomCenterOf(focus);
	}

	private static BlockPos findRegion(ServerLevel level) {
		var resolver = level.getChunkSource().getGenerator().getBiomeSource().createUncachedResolver(level.getChunkSource().randomState());
		for (int radius = 0; radius <= 8192; radius += 64) {
			for (int dx = -radius; dx <= radius; dx += 64) for (int dz = -radius; dz <= radius; dz += 64) {
				if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
				for (int y : new int[]{0, -16, 16, -32}) {
					if (resolver.getNoiseBiome(dx >> 2, y >> 2, dz >> 2).is(BelowkeeperClimate.HOLLOWS)) return new BlockPos(dx, y, dz);
				}
			}
		}
		throw new AssertionError("No naturally selected Belowkeeper Hollows in the fixed 8,192-block climate survey");
	}

	private void observe(ServerLevel level, int cx, int cz) {
		level.getChunk(cx, cz); chunks++;
		for (int x = cx * 16; x < cx * 16 + 16; x++) for (int z = cz * 16; z < cz * 16 + 16; z++) {
			for (int y = -48; y <= 40; y += 2) {
				var pos = new BlockPos(x, y, z);
				if (!level.getBiome(pos).is(BelowkeeperClimate.HOLLOWS)) continue;
				cells++;
				var state = level.getBlockState(pos);
				if (state.is(Blocks.DRIPSTONE_BLOCK) || state.is(Blocks.POINTED_DRIPSTONE)) dripstone++;
				if (state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.CLAY)) lushFloor++;
				if (state.is(FungalGarden.GLOWCAP)) {
					caps++;
					if (firstCap == null) firstCap = pos;
				}
				if (state.isAir() && level.getBlockState(pos.above()).isAir() && !level.getBlockState(pos.below()).isAir() && level.getFluidState(pos).isEmpty()) {
					caveFloors++;
					if (firstFloor == null || (level.getBlockState(pos.below()).is(Blocks.MOSS_BLOCK) && !level.getBlockState(firstFloor.below()).is(Blocks.MOSS_BLOCK))) {
						firstFloor = pos;
					}
				}
			}
		}
	}

	private static void check(boolean result, String message) { if (!result) throw new AssertionError(message); }
}
