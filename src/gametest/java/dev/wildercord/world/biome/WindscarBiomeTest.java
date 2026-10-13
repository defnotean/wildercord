package dev.wildercord.world.biome;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.AuraBeasts;
import dev.wildercord.lore.LoreJournal;
import dev.wildercord.wildlife.HighlandContent;
import dev.wildercord.wildlife.PikaContent;
import dev.wildercord.wildlife.Wildlife;
import dev.wildercord.wildlife.WindreedBlock;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/** Untouched default-preset terrain, a real harvest packet and persisted owner discovery. */
public final class WindscarBiomeTest implements FabricClientGameTest {
	private static final String SEED = "-7620530482425397421";
	private int chunks, columns, stone, grass, plants, lowest = Integer.MAX_VALUE, highest = Integer.MIN_VALUE;
	private BlockPos firstPlant;

	@Override public void runTest(ClientGameTestContext context) {
		boolean hidden = context.computeOnClient(mc -> mc.gui.hud.isHidden());
		TestWorldSave save;
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
			// This nine-by-nine census is fixed before measurement. Missing crops fail; no larger retry area.
			for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
				int cx = (center.getX() >> 4) + dx, cz = (center.getZ() >> 4) + dz;
				world.getServer().runOnServer(server -> observe(server.overworld(), cx, cz));
				context.waitTicks(1);
			}
			world.getServer().runOnServer(server -> {
				Wildercord.LOGGER.info("WINDSCAR_TERRAIN seed={}, center={}, chunks={}, columns={}, stone={}, grass={}, height={}/{}, naturalWindreed={}, firstPlant={}",
					SEED, center, chunks, columns, stone, grass, lowest, highest, plants, firstPlant);
				check(chunks == 81 && columns >= 256, "The fixed survey contains a real highland region");
				check(stone > 0 && grass > 0 && highest - lowest >= 8, "Natural highland ground includes exposed rock, grass and changing elevation");
				check(plants > 0 && firstPlant != null, "Natural decoration places Windreed in its region without placement commands");
				var player = server.getPlayerList().getPlayers().getFirst();
				check(!LoreJournal.data(player).has(WindscarClimate.JOURNAL_ENTRY), "The player has not already discovered the distant region");
				player.setGameMode(GameType.SPECTATOR);
				player.teleportTo(server.overworld(), firstPlant.getX() + .5, firstPlant.getY() + 1, firstPlant.getZ() + .5, Set.of(), 0, 0, false);
			});
			context.waitTicks(LoreJournal.POLL_TICKS + 5);
			world.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().getFirst();
				check(!LoreJournal.data(player).has(WindscarClimate.JOURNAL_ENTRY), "Spectator travel does not earn regional discovery");
				player.setGameMode(GameType.CREATIVE);
			});
			context.waitTicks(LoreJournal.POLL_TICKS + 5);
			world.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().getFirst();
				check(LoreJournal.data(player).has(WindscarClimate.JOURNAL_ENTRY), "Entering the region earns the real polled field note");
				LoreJournal.poll(player);
				check(LoreJournal.data(player).entries().stream().filter(WindscarClimate.JOURNAL_ENTRY::equals).count() == 1, "Returning does not duplicate the note");
				player.setGameMode(GameType.SPECTATOR);
				Vec3 target = Vec3.atCenterOf(firstPlant), eye = target.add(9, 7, 9);
				Vec3 look = target.subtract(eye.add(0, player.getEyeHeight(), 0));
				player.teleportTo(server.overworld(), eye.x, eye.y, eye.z, Set.of(),
					(float) Math.toDegrees(Math.atan2(-look.x, look.z)), (float) -Math.toDegrees(Math.atan2(look.y, look.horizontalDistance())), false);
			});
			context.waitTicks(20);
			world.getConnection().waitForChunksRender();
			context.runOnClient(mc -> {
				check(LoreJournal.data(mc.player).has(WindscarClimate.JOURNAL_ENTRY), "The connected owner receives the discovery");
				check(!net.minecraft.network.chat.Component.translatable("journal.wildercord.entry.place.windscar_highlands").getString().startsWith("journal."), "The field note has player-facing text");
				mc.getWindow().setWindowed(1280, 720); mc.options.guiScale().set(2); mc.resizeGui();
				mc.gui.toastManager().clear(); mc.gui.hud.getChat().clearMessages(false);
				if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
			});
			context.waitTicks(5);
			context.takeScreenshot(TestScreenshotOptions.of("windscar_highlands_terrain").disableCounterPrefix());
			world.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().getFirst();
				BlockPos stand = harvestingPosition(server.overworld());
				player.teleportTo(server.overworld(), stand.getX() + .5, stand.getY(), stand.getZ() + .5, Set.of(), 0, 0, false);
				player.setGameMode(GameType.SURVIVAL);
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				check(server.overworld().getBlockState(firstPlant).getValue(WindreedBlock.AGE) == 2, "The untouched Windreed has mature tassels");
			});
			context.waitTicks(5);
			int before = world.getServer().computeOnServer(server -> harvestCount(server.getPlayerList().getPlayers().getFirst()));
			use(context);
			context.waitTicks(4);
			world.getServer().runOnServer(server -> {
				check(server.overworld().getBlockState(firstPlant).is(HighlandContent.REED)
					&& server.overworld().getBlockState(firstPlant).getValue(WindreedBlock.AGE) == 0, "A real client harvest preserves the natural root");
				check(harvestCount(server.getPlayerList().getPlayers().getFirst()) == before + 2, "The Survival harvest gives exactly two tassels");
			});
			use(context);
			context.waitTicks(4);
			world.getServer().runOnServer(server -> check(harvestCount(server.getPlayerList().getPlayers().getFirst()) == before + 2, "An immature root cannot be harvested twice"));
			save = world.getWorldSave();
		} finally {
			context.runOnClient(mc -> { if (mc.gui.hud.isHidden() != hidden) mc.gui.hud.toggle(); });
		}
		try (var world = save.open()) {
			world.getServer().runOnServer(server -> {
				check(LoreJournal.data(server.getPlayerList().getPlayers().getFirst()).has(WindscarClimate.JOURNAL_ENTRY), "The owner keeps the regional discovery after save/reopen");
				check(server.overworld().getBiome(firstPlant).is(WindscarClimate.WINDSCAR), "Saved chunks keep their Windscar identity");
				var state = server.overworld().getBlockState(firstPlant);
				check(state.is(HighlandContent.REED) && state.getValue(WindreedBlock.AGE) == 0, "The harvested root persists without another yield");
				registered(server.overworld());
			});
		}
	}

	private static void registered(ServerLevel level) {
		var holder = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(WindscarClimate.WINDSCAR);
		check(level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes().contains(holder), "The default Overworld can select Windscar");
		var biome = holder.value();
		var spawns = biome.getAttributes().applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS, MobSpawnSettings.EMPTY);
		for (var type : List.of(AuraBeasts.STONEHORN, AuraBeasts.GALECLAW, PikaContent.TUSSOCK_PIKA)) {
			check(spawns.getMobsToSpawn(MobCategory.CREATURE).unwrap().stream().anyMatch(entry -> entry.value().type() == type), "Actual highland spawn pool contains " + type);
		}
		check(spawns.getMobsToSpawn(MobCategory.AMBIENT).unwrap().stream().anyMatch(entry -> entry.value().type() == Wildlife.SKYRAY), "Skyrays retain a highland habitat");
		var feature = ResourceKey.create(Registries.PLACED_FEATURE, Wildercord.id("windreed_patch"));
		check(biome.getGenerationSettings().features().get(GenerationStep.Decoration.VEGETAL_DECORATION.ordinal())
			.stream().filter(entry -> entry.is(feature)).count() == 1, "Exactly one real Windreed decorator belongs to the region");
		for (String site : List.of("travel_watchtower", "travel_standing_stones", "master_wind_gate")) {
			var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(ResourceKey.create(Registries.STRUCTURE, Wildercord.id(site))).value();
			check(structure.biomes().contains(holder), "The loaded structure admits highland ground: " + site);
			check(holder.is(TagKey.create(Registries.BIOME, Wildercord.id("has_structure/" + site))), "The structure biome tag agrees: " + site);
		}
	}

	private static BlockPos findRegion(ServerLevel level) {
		var resolver = level.getChunkSource().getGenerator().getBiomeSource().createUncachedResolver(level.getChunkSource().randomState());
		for (int radius = 0; radius <= 8192; radius += 64) {
			for (int dx = -radius; dx <= radius; dx += 64) for (int dz = -radius; dz <= radius; dz += 64) {
				if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
				if (resolver.getNoiseBiome(dx >> 2, 16, dz >> 2).is(WindscarClimate.WINDSCAR)) return new BlockPos(dx, 64, dz);
			}
		}
		throw new AssertionError("No naturally selected Windscar in the fixed 8,192-block climate survey");
	}

	private void observe(ServerLevel level, int cx, int cz) {
		level.getChunk(cx, cz); chunks++;
		for (int x = cx * 16; x < cx * 16 + 16; x++) for (int z = cz * 16; z < cz * 16 + 16; z++) {
			int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			if (!level.getBiome(new BlockPos(x, top, z)).is(WindscarClimate.WINDSCAR)) continue;
			columns++; lowest = Math.min(lowest, top); highest = Math.max(highest, top);
			var ground = level.getBlockState(new BlockPos(x, top - 1, z));
			if (ground.is(Blocks.STONE)) stone++;
			if (ground.is(Blocks.GRASS_BLOCK)) grass++;
			for (int y = top - 1; y <= top + 1; y++) {
				var pos = new BlockPos(x, y, z);
				if (level.getBlockState(pos).is(HighlandContent.REED) && level.getBiome(pos).is(WindscarClimate.WINDSCAR)) {
					plants++; if (firstPlant == null) firstPlant = pos;
				}
			}
		}
	}

	private BlockPos harvestingPosition(ServerLevel level) {
		for (var direction : Direction.Plane.HORIZONTAL) {
			var pos = firstPlant.relative(direction);
			if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
				&& level.getBlockState(pos.below()).isCollisionShapeFullBlock(level, pos.below())) return pos;
		}
		throw new AssertionError("The first natural Windreed has no adjacent safe harvesting foothold");
	}

	private int harvestCount(ServerPlayer player) {
		int count = player.getInventory().countItem(HighlandContent.WINDREED);
		return count + player.level().getEntitiesOfClass(ItemEntity.class, new AABB(firstPlant).inflate(5),
			entity -> entity.isAlive() && entity.getItem().is(HighlandContent.WINDREED)).stream().mapToInt(entity -> entity.getItem().getCount()).sum();
	}
	private void use(ClientGameTestContext context) {
		context.runOnClient(mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
			new BlockHitResult(Vec3.atCenterOf(firstPlant), Direction.UP, firstPlant, false)));
	}
	private static void check(boolean result, String message) { if (!result) throw new AssertionError(message); }
}
