package dev.wildercord.world.sites;

import dev.wildercord.Wildercord;
import dev.wildercord.compat.McWorldgen;
import dev.wildercord.world.sites.oathhold.OathholdOrigin;
import dev.wildercord.world.sites.oathhold.OathholdSites;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.List;
import java.util.Set;

/**
 * Full in-game verification &amp; 1:1 visual showcase for Oathhold, the 2,048x2,048 Sovereign Megacity.
 *
 * <p>Generates a real Overworld save, locates {@code wildercord:oathhold}, loads landmarks across the entire
 * 2,048-block span (-960..+960 North-South and East-West), asserts structural &amp; architectural integrity,
 * and captures six reference screenshots for 1:1 visual review.</p>
 */
public final class OathholdMegacityTest implements FabricClientGameTest {
	private static final String TEST_SEED = "20261121";

	private static void check(boolean cond, String msg) {
		if (!cond) throw new AssertionError(msg);
	}

	private static BlockState blockAt(ServerLevel level, int wx, int wy, int wz) {
		level.getChunk(wx >> 4, wz >> 4);
		return level.getBlockState(new BlockPos(wx, wy, wz));
	}

	private static void warmArea(ServerLevel level, int wx, int wz, int chunkRadius) {
		int ccx = wx >> 4, ccz = wz >> 4;
		for (int cx = ccx - chunkRadius; cx <= ccx + chunkRadius; cx++) {
			for (int cz = ccz - chunkRadius; cz <= ccz + chunkRadius; cz++) {
				level.getChunk(cx, cz);
			}
		}
	}

	private static void lookAt(ClientGameTestContext ctx, TestSingleplayerContext world,
			double x, double y, double z, double tx, double ty, double tz, int waitTicks) {
		double eye = 1.62, dx = tx - x, dy = ty - (y + eye), dz = tz - z;
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		world.getServer().runOnServer(s -> {
			ServerLevel level = s.overworld();
			warmArea(level, (int) Math.floor(x), (int) Math.floor(z), 8);
			warmArea(level, (int) Math.floor(tx), (int) Math.floor(tz), 8);
			ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
			p.teleportTo(level, x, y, z, Set.<Relative>of(), yaw, pitch, false);
		});
		ctx.waitTicks(waitTicks);
	}

	private static void shot(ClientGameTestContext ctx, String name) {
		ctx.runOnClient(mc -> mc.gui.toastManager().clear());
		ctx.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder()
				.setUseConsistentSettings(false)
				.adjustSettings(settings -> settings.setSeed(TEST_SEED))
				.create()) {
			context.waitTicks(40);
			for (String rule : List.of("advance_time false", "advance_weather false")) {
				world.getServer().runCommand("gamerule " + rule);
			}
			world.getServer().runCommand("time set 1200");
			world.getServer().runCommand("weather clear");

			context.runOnClient(mc -> {
				mc.getWindow().setWindowed(1280, 720);
				mc.options.renderDistance().set(12);
				mc.options.fov().set(74);
				mc.resizeGui();
				if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
			});
			world.getServer().runOnServer(s -> {
				ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
				p.setGameMode(GameType.SPECTATOR);
			});

			// 1. Locate Oathhold through vanilla Registry + McWorldgen.findNearestStructure
			int[] origin = world.getServer().computeOnServer(s -> {
				ServerLevel level = s.overworld();
				var reg = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
				var holder = reg.getOrThrow(ResourceKey.create(Registries.STRUCTURE, Wildercord.id(OathholdSites.CITY_ID)));
				BlockPos found = McWorldgen.findNearestStructure(level, HolderSet.direct(holder), BlockPos.ZERO, 200, false);
				check(found != null, "Could not locate wildercord:oathhold within 200 cells on seed " + TEST_SEED);
				level.getChunk(found.getX() >> 4, found.getZ() >> 4);
				var starts = McWorldgen.startsForStructure(level.structureManager(), found.getX() >> 4, found.getZ() >> 4, holder.value());
				check(!starts.isEmpty(), "Located chunk has no valid wildercord:oathhold StructureStart at " + found);
				StructureStart start = starts.getFirst();
				BoundingBox box = start.getBoundingBox();
				int ox = box.minX() + 16;
				int baseY = box.minY() + 24;
				int oz = box.minZ() + 16;
				return new int[]{ox, baseY, oz};
			});
			int ox = origin[0], by = origin[1], oz = origin[2];
			Wildercord.LOGGER.info("OATHHOLD_MEGACITY located origin=({}, {}, {})", ox, by, oz);

			// 2. Assert continuous 1,920-block walled / 2,048-block full city span across cardinal extremes & landmarks
			world.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				// South & North Outer Barbicans (distance = 1,920 blocks) + Outer Moat Water
				check(!blockAt(l, ox + 16, by + 18, oz + 960).isAir(), "South Outer Gatehouse Tower missing at +960");
				check(blockAt(l, ox + 24, by - 1, oz + 974).is(Blocks.WATER), "South Outer Moat Water missing at +974");
				check(!blockAt(l, ox + 16, by + 18, oz - 960).isAir(), "North Outer Gatehouse Tower missing at -960");
				check(blockAt(l, ox + 24, by - 1, oz - 974).is(Blocks.WATER), "North Outer Moat Water missing at -974");

				// West & East Outer Walls (distance = 1,920 blocks), Colosseum (-612) & Harbor Galleons (+760)
				check(!blockAt(l, ox - 960, by + 18, oz + 16).isAir(), "West Outer Gatehouse Tower missing at -960");
				check(blockAt(l, ox - 612, by, oz - 14).is(Blocks.SAND), "Tournament Colosseum arena sand missing at x=-612");
				check(blockAt(l, ox + 760, by - 1, oz).is(Blocks.WATER), "East Imperial Harbor basin water missing at x=+760");
				check(blockAt(l, ox + 758, by + 18, oz - 58).is(Blocks.DARK_OAK_LOG), "Harbor Galleon mainmast missing at (758, -58)");
				check(!blockAt(l, ox + 960, by + 18, oz + 100).isAir(), "East Harbor Sea-Gate Bastion missing at x=+960");

				// Inner Citadel: South Gatehouse, Fountain Plaza, Canal, Forge, Sovereign Great Hall, Cathedral Spire
				check(!blockAt(l, ox + 16, by + 24, oz + 224).isAir(), "South Inner Gatehouse Tower missing at z=+224");
				check(blockAt(l, ox, by + 1, oz + 182).is(Blocks.WATER), "Fountain Plaza water missing at z=+182");
				check(blockAt(l, ox, by - 1, oz + 80).is(Blocks.WATER), "Central Canal water missing at z=+80");
				check(blockAt(l, ox, by + OathholdOrigin.ACROPOLIS_Y + 9, oz - 191).is(Blocks.WOOL.red()), "Sovereign Throne cushion missing inside Great Hall");
				check(!blockAt(l, ox + 150, by + 104, oz - 140).isAir(), "Sovereign Cathedral Bell-Spire missing above y=+104");
			});

			// 3. Capture 6 showcase screenshots for 1:1 visual inspection against concept art
			lookAt(context, world, ox + 46, by + 96, oz + 340, ox + 4, by + 28, oz - 70, 120);
			shot(context, "oathhold_01_aerial_overview");

			lookAt(context, world, ox + 3, by + 4, oz + 278, ox, by + 16, oz + 222, 100);
			shot(context, "oathhold_02_south_gatehouse");

			lookAt(context, world, ox, by + OathholdOrigin.ACROPOLIS_Y + 4, oz - 102, ox, by + OathholdOrigin.ACROPOLIS_Y + 14, oz - 196, 100);
			shot(context, "oathhold_03_great_hall_interior");

			lookAt(context, world, ox + 16, by + 28, oz - 54, ox + 78, by + 28, oz - 122, 100);
			shot(context, "oathhold_04_districts_courtyard");

			lookAt(context, world, ox - 548, by + 48, oz + 58, ox - 612, by + 10, oz, 100);
			shot(context, "oathhold_05_outer_metropolis_colosseum");

			lookAt(context, world, ox + 590, by + 38, oz + 108, ox + 745, by + 12, oz - 48, 100);
			shot(context, "oathhold_06_harbor_and_outer_gate");
		}
	}
}
