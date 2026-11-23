package dev.wildercord.world.sites;

import dev.wildercord.Wildercord;
import dev.wildercord.compat.McWorldgen;
import dev.wildercord.world.sites.oathhold.OathholdCitizen;
import dev.wildercord.world.sites.oathhold.OathholdContent;
import dev.wildercord.world.sites.oathhold.OathholdOrigin;
import dev.wildercord.world.sites.oathhold.OathholdSites;
import net.minecraft.world.phys.AABB;
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
import net.minecraft.world.level.material.Fluids;

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
			double x, double y, double z, double tx, double ty, double tz, int renderDist, int fov) {
		ctx.runOnClient(mc -> {
			mc.options.renderDistance().set(renderDist);
			mc.options.fov().set(fov);
			mc.options.broadcastOptions();
		});
		double eye = 1.62, dx = tx - x, dy = ty - (y + eye), dz = tz - z;
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		int chunkR = Math.min(8, renderDist);
		world.getServer().runOnServer(s -> {
			s.getPlayerList().setViewDistance(renderDist);
			ServerLevel level = s.overworld();
			warmArea(level, (int) Math.floor(x), (int) Math.floor(z), chunkR);
			warmArea(level, (int) Math.floor(tx), (int) Math.floor(tz), chunkR);
			ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
			p.teleportTo(level, x, y, z, Set.<Relative>of(), yaw, pitch, false);
		});
		ctx.waitTicks(40);
		try {
			world.getConnection().waitForChunksDownload();
			world.getConnection().waitForClientboundPackets();
			world.getConnection().waitForChunksRender();
		} catch (AssertionError | RuntimeException ignored) {
		}
		ctx.waitTicks(40);
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
				mc.options.simulationDistance().set(5);
				mc.options.fov().set(74);
				mc.options.broadcastOptions();
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
				check(southMoat.getFluidState().is(Fluids.WATER), "South Outer Moat Water missing at +974 (by-2): " + southMoat);
				var northOuterTower = blockAt(l, ox + 23, by + 18, oz - 960);
				check(!northOuterTower.isAir(), "North Outer Gatehouse Tower missing at -960: " + northOuterTower);
				var northMoat = blockAt(l, ox + 24, by - 2, oz - 974);
				check(northMoat.getFluidState().is(Fluids.WATER), "North Outer Moat Water missing at -974 (by-2): " + northMoat);

				// West & East Outer Walls (distance = 1,920 blocks), Colosseum (-612) & Harbor Galleons (+760)
				var westOuterTower = blockAt(l, ox - 960, by + 18, oz + 23);
				check(!westOuterTower.isAir(), "West Outer Gatehouse Tower missing at -960: " + westOuterTower);
				var colosseumSand = blockAt(l, ox - 612, by, oz - 14);
				check(colosseumSand.is(Blocks.SAND), "Tournament Colosseum arena sand missing at x=-612: " + colosseumSand);
				var harborWater = blockAt(l, ox + 760, by - 2, oz);
				check(harborWater.getFluidState().is(Fluids.WATER), "East Imperial Harbor basin water missing at x=+760 (by-2): " + harborWater);
				var galleonMast = blockAt(l, ox + 758, by + 18, oz - 58);
				check(galleonMast.is(Blocks.DARK_OAK_LOG), "Harbor Galleon mainmast missing at (758, -58): " + galleonMast);
				var eastSeaGate = blockAt(l, ox + 960, by + 18, oz + 108);
				check(!eastSeaGate.isAir(), "East Harbor Sea-Gate Bastion missing at x=+960: " + eastSeaGate);

				// Inner Citadel: South Gatehouse, Fountain Plaza, Canal, Sovereign Great Hall, Cathedral Spire & Hygiene Checks
				var southInnerTower = blockAt(l, ox + 23, by + 20, oz + 224);
				check(!southInnerTower.isAir(), "South Inner Gatehouse Tower missing at z=+224: " + southInnerTower);
				var harborQuayTop = blockAt(l, ox + 544, by + 2, oz - 20);
				check(!harborQuayTop.is(Blocks.SNOW), "Unexpected snow on East Harbor Quay: " + harborQuayTop);
				var harborSurface = blockAt(l, ox + 760, by - 1, oz);
				check(harborSurface.getFluidState().is(Fluids.WATER), "East Imperial Harbor basin water froze at x=+760 (by-1): " + harborSurface);
				var fountainWater = blockAt(l, ox, by + 1, oz + 182);
				check(fountainWater.getFluidState().is(Fluids.WATER), "Fountain Plaza water missing/frozen at z=+182: " + fountainWater);
				var canalWater = blockAt(l, ox, by - 2, oz + 80);
				check(canalWater.getFluidState().is(Fluids.WATER), "Central Canal water missing at z=+80 (by-2): " + canalWater);
				var throneCushion = blockAt(l, ox, by + OathholdOrigin.ACROPOLIS_Y + 10, oz - 191);
				check(throneCushion.is(Blocks.RED_WOOL), "Sovereign Throne cushion missing inside Great Hall: " + throneCushion);
				var cathedralSpire = blockAt(l, ox + 150, by + 115, oz - 140);
				check(!cathedralSpire.isAir(), "Sovereign Cathedral Bell-Spire missing above y=+115: " + cathedralSpire);

				var bridgeSurface = blockAt(l, ox, by + 2, oz + 240);
				check(!bridgeSurface.is(Blocks.SNOW), "Unexpected snow on South Inner Gatehouse Bridge: " + bridgeSurface);
				var bridgeAir = blockAt(l, ox, by + 12, oz + 244);
				check(bridgeAir.isAir(), "Floating canopy foliage above South Inner Gatehouse Bridge: " + bridgeAir);
				var roseCenter = blockAt(l, ox, by + OathholdOrigin.ACROPOLIS_Y + 30, oz - 203);
				check(roseCenter.is(Blocks.GLOWSTONE), "Great Hall North Solar Rose Window center missing: " + roseCenter);
				var roseBack = blockAt(l, ox, by + OathholdOrigin.ACROPOLIS_Y + 30, oz - 204);
				check(roseBack.isAir(), "Great Hall North Solar Rose Window outer backing not cleared: " + roseBack);
				var porticoSolar = blockAt(l, ox, by + OathholdOrigin.ACROPOLIS_Y + 24, oz - 86);
				check(porticoSolar.is(Blocks.OCHRE_FROGLIGHT), "Great Hall South Portico gilded pediment solar disk missing: " + porticoSolar);

				// Living-City streetscape, furnishing, interior & quay probes
				var sidewalk = blockAt(l, ox + 8, by + 1, oz + 320);
				check(!sidewalk.isAir(), "Causeway raised sidewalk missing at (ox+8, by+1, oz+320): " + sidewalk);
				var carriagewayAir = blockAt(l, ox, by + 1, oz + 320);
				check(carriagewayAir.isAir(), "Causeway carriageway should remain sunken at (ox, by+1, oz+320): " + carriagewayAir);
				var innFloor2Deck = blockAt(l, ox + 44, by + 5, oz + 106);
				check(!innFloor2Deck.isAir(), "Grand Inn Floor 2 deck missing at (ox+44, by+5, oz+106): " + innFloor2Deck);
				var innFloor2Table = blockAt(l, ox + 44, by + 6, oz + 106);
				check(!innFloor2Table.isAir(), "Grand Inn Floor 2 furnishing missing at (ox+44, by+6, oz+106): " + innFloor2Table);
				var cathedralPew = blockAt(l, ox + 141, by + OathholdOrigin.ACROPOLIS_Y + 1, oz - 142);
				check(cathedralPew.is(Blocks.DARK_OAK_STAIRS), "Cathedral nave pew missing at (ox+141, AY+1, oz-142): " + cathedralPew);
				var acropolisHedge = blockAt(l, ox - 32, by + OathholdOrigin.ACROPOLIS_Y + 1, oz - 68);
				check(!acropolisHedge.isAir(), "Acropolis Esplanade boxwood hedge missing at (ox-32, AY+1, oz-68): " + acropolisHedge);
				var harborStall = blockAt(l, ox + 542, by + 4, oz - 32);
				check(!harborStall.isAir(), "Harbor Quay market stall furnishing missing at (ox+542, by+4, oz-32): " + harborStall);
				var loftBed = blockAt(l, ox + 49, by + 6, oz + 75);
				check(!loftBed.isAir(), "Canal Shop-House mezzanine loft bed missing at (ox+49, by+6, oz+75): " + loftBed);

				// Pre-warm full Inner Citadel + Acropolis grid so shot 01 renders the whole skyline
				for (int wx = ox - 208; wx <= ox + 208; wx += 16) {
					for (int wz = oz - 224; wz <= oz + 272; wz += 16) {
						l.getChunk(wx >> 4, wz >> 4);
					}
				}

				// Living-City Guild Request Boards & persistent named OathholdCitizens placed by worldgen
				var guildBoard = blockAt(l, ox + 136, by + 1, oz + 50);
				check(guildBoard.is(OathholdContent.GUILD_REQUEST_BOARD), "Adventurers' Guild HQ Request Board missing at (ox+136, by+1, oz+50): " + guildBoard);
				var plazaBoard = blockAt(l, ox - 12, by + 2, oz + 166);
				check(plazaBoard.is(OathholdContent.GUILD_REQUEST_BOARD), "Fountain Plaza Guild Request Board missing at (ox-12, by+2, oz+166): " + plazaBoard);
				List<OathholdCitizen> innerCitizens = l.getEntitiesOfClass(
					OathholdCitizen.class,
					new AABB(ox - 220, by - 8, oz - 230, ox + 220, by + 64, oz + 280)
				);
				check(innerCitizens.size() >= 50, "Expected >= 50 live OathholdCitizens in Inner Citadel worldgen, found " + innerCitizens.size());
			});

			// 3. Capture 11 showcase screenshots for 1:1 visual inspection against Master Map & Stage 2 concepts
			lookAt(context, world, ox, by + 92, oz + 96, ox, by + 26, oz - 115, 18, 86);
			shot(context, "oathhold_01_aerial_overview");

			lookAt(context, world, ox + 0.5, by + 4.0, oz + 266, ox + 0.5, by + 18, oz + 224, 10, 72);
			shot(context, "oathhold_02_south_gatehouse");

			lookAt(context, world, ox + 0.5, by + OathholdOrigin.ACROPOLIS_Y + 4.0, oz - 106, ox + 0.5, by + OathholdOrigin.ACROPOLIS_Y + 17, oz - 196, 10, 74);
			shot(context, "oathhold_03_great_hall_interior");

			lookAt(context, world, ox - 88, by + 26, oz + 148, ox + 38, by + 18, oz - 68, 18, 78);
			shot(context, "oathhold_04_districts_courtyard");

			lookAt(context, world, ox - 546, by + 50, oz + 64, ox - 612, by + 10, oz, 10, 72);
			shot(context, "oathhold_05_outer_metropolis_colosseum");

			lookAt(context, world, ox + 520.0, by + 12.0, oz + 82.0, ox + 760.0, by + 13.0, oz - 26.0, 18, 76);
			shot(context, "oathhold_06_harbor_and_outer_gate");

			lookAt(context, world, ox + 3.5, by + 2.5, oz + 446, ox, by + 9.0, oz + 360, 16, 76);
			shot(context, "oathhold_07_causeway_boulevard_sidewalk");

			lookAt(context, world, ox + 13.5, by + 2.5, oz + 110, ox + 1.0, by + 4.0, oz + 56, 14, 76);
			shot(context, "oathhold_08_canal_promenade_alley");

			lookAt(context, world, ox + 0.5, by + OathholdOrigin.ACROPOLIS_Y + 4.5, oz - 45.0, ox + 0.5, by + OathholdOrigin.ACROPOLIS_Y + 19.0, oz - 118.0, 14, 80);
			shot(context, "oathhold_09_acropolis_esplanade");

			lookAt(context, world, ox + 45.5, by + 1.5, oz + 86.2, ox + 44.0, by + 4.2, oz + 69.0, 8, 82);
			shot(context, "oathhold_10_townhouse_shop_interior");

			lookAt(context, world, ox + 150.5, by + OathholdOrigin.ACROPOLIS_Y + 3.0, oz - 94.0, ox + 150.5, by + OathholdOrigin.ACROPOLIS_Y + 12.0, oz - 180.0, 10, 78);
			shot(context, "oathhold_11_cathedral_nave_interior");
		}
	}
}
