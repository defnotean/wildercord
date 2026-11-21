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
				mc.options.renderDistance().set(24);
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
				var starts = McWorldgen.startsForStructure(level.structureManager(), found.getX() >> 4, found.getZ() >> 4, st -> st == holder.value());
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
				var southOuterTower = blockAt(l, ox + 23, by + 18, oz + 960);
				check(!southOuterTower.isAir(), "South Outer Gatehouse Tower missing at +960: " + southOuterTower);
				var southMoat = blockAt(l, ox + 24, by - 2, oz + 974);
				check(southMoat.is(Blocks.WATER), "South Outer Moat Water missing at +974 (by-2): " + southMoat);
				var northOuterTower = blockAt(l, ox + 23, by + 18, oz - 960);
				check(!northOuterTower.isAir(), "North Outer Gatehouse Tower missing at -960: " + northOuterTower);
				var northMoat = blockAt(l, ox + 24, by - 2, oz - 974);
				check(northMoat.is(Blocks.WATER), "North Outer Moat Water missing at -974 (by-2): " + northMoat);

				// West & East Outer Walls (distance = 1,920 blocks), Colosseum (-612) & Harbor Galleons (+760)
				var westOuterTower = blockAt(l, ox - 960, by + 18, oz + 23);
				check(!westOuterTower.isAir(), "West Outer Gatehouse Tower missing at -960: " + westOuterTower);
				var colosseumSand = blockAt(l, ox - 612, by, oz - 14);
				check(colosseumSand.is(Blocks.SAND), "Tournament Colosseum arena sand missing at x=-612: " + colosseumSand);
				var harborWater = blockAt(l, ox + 760, by - 2, oz);
				check(harborWater.is(Blocks.WATER), "East Imperial Harbor basin water missing at x=+760 (by-2): " + harborWater);
				var galleonMast = blockAt(l, ox + 758, by + 18, oz - 58);
				check(galleonMast.is(Blocks.DARK_OAK_LOG), "Harbor Galleon mainmast missing at (758, -58): " + galleonMast);
				var eastSeaGate = blockAt(l, ox + 960, by + 18, oz + 108);
				check(!eastSeaGate.isAir(), "East Harbor Sea-Gate Bastion missing at x=+960: " + eastSeaGate);

				// Inner Citadel: South Gatehouse, Fountain Plaza, Canal, Forge, Sovereign Great Hall, Cathedral Spire
				var southInnerTower = blockAt(l, ox + 22, by + 20, oz + 224);
				check(!southInnerTower.isAir(), "South Inner Gatehouse Tower missing at z=+224: " + southInnerTower);
				var fountainWater = blockAt(l, ox, by + 1, oz + 182);
				check(fountainWater.is(Blocks.WATER) || fountainWater.is(Blocks.ICE), "Fountain Plaza water missing at z=+182: " + fountainWater);
				var canalWater = blockAt(l, ox, by - 2, oz + 80);
				check(canalWater.is(Blocks.WATER), "Central Canal water missing at z=+80 (by-2): " + canalWater);
				var throneCushion = blockAt(l, ox, by + OathholdOrigin.ACROPOLIS_Y + 10, oz - 191);
				check(throneCushion.is(Blocks.WOOL.red()), "Sovereign Throne cushion missing inside Great Hall: " + throneCushion);
				var cathedralSpire = blockAt(l, ox + 150, by + 115, oz - 140);
				check(!cathedralSpire.isAir(), "Sovereign Cathedral Bell-Spire missing above y=+115: " + cathedralSpire);

				// Pre-warm full Inner Citadel + Acropolis grid so shot 01 renders the whole skyline
				for (int wx = ox - 208; wx <= ox + 208; wx += 16) {
					for (int wz = oz - 224; wz <= oz + 272; wz += 16) {
						l.getChunk(wx >> 4, wz >> 4);
					}
				}
			});

			// 3. Capture 6 showcase screenshots for 1:1 visual inspection against concept art
			lookAt(context, world, ox + 28, by + 108, oz + 278, ox + 2, by + 28, oz - 48, 160);
			shot(context, "oathhold_01_aerial_overview");

			context.runOnClient(mc -> mc.options.renderDistance().set(14));

			lookAt(context, world, ox + 1.5, by + 3.5, oz + 268, ox, by + 18, oz + 224, 100);
			shot(context, "oathhold_02_south_gatehouse");

			lookAt(context, world, ox + 0.5, by + OathholdOrigin.ACROPOLIS_Y + 3, oz - 103, ox + 0.5, by + OathholdOrigin.ACROPOLIS_Y + 16, oz - 196, 100);
			shot(context, "oathhold_03_great_hall_interior");

			lookAt(context, world, ox - 46, by + 34, oz - 28, ox + 48, by + 26, oz - 118, 100);
			shot(context, "oathhold_04_districts_courtyard");

			lookAt(context, world, ox - 548, by + 48, oz + 58, ox - 612, by + 10, oz, 100);
			shot(context, "oathhold_05_outer_metropolis_colosseum");

			lookAt(context, world, ox + 590, by + 38, oz + 108, ox + 745, by + 12, oz - 48, 100);
			shot(context, "oathhold_06_harbor_and_outer_gate");
		}
	}
}
