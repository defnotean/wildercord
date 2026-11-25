package dev.wildercord.trials;

import dev.wildercord.Wildercord;
import dev.wildercord.gametest.compat.GametestCompat;
import dev.wildercord.world.biome.AstralLeyGladeClimate;
import dev.wildercord.world.biome.CinderCalderaClimate;
import dev.wildercord.world.sites.Sites;
import dev.wildercord.world.sites.trials.TrialSites;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

import java.util.Optional;

/**
 * Verifies Astral Ley-Glade & Cinder Caldera biome registry + natural Overworld sampling,
 * natural worldgen locators for all 3 Co-Op Trial Sanctums, Sanctum Wayfinder mode cycling,
 * and Leyweaver Moth / Caldera Cinderhound entity spawning.
 */
public final class TrialBiomesAndPlacementTest implements FabricClientGameTest {
	private static void check(boolean ok, String why) {
		if (!ok) throw new AssertionError(why);
	}

	@Override
	public void runTest(ClientGameTestContext c) {
		try (var world = c.worldBuilder()
				.setUseConsistentSettings(false)
				.adjustSettings(s -> s.setSeed("20261125"))
				.create()) {
			c.waitTicks(25);
			world.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				var biomeReg = l.registryAccess().lookupOrThrow(Registries.BIOME);
				check(biomeReg.containsKey(AstralLeyGladeClimate.ASTRAL_LEY_GLADE),
					"Astral Ley-Glade biome registered");
				check(biomeReg.containsKey(CinderCalderaClimate.CINDER_CALDERA),
					"Cinder Caldera biome registered");

				var cs = l.getChunkSource();
				var gen = cs.getGenerator();
				var resolver = gen.getBiomeSource()
					.createResolver(cs.randomState().createClimateSampler(SamplerContext.EMPTY_UNCACHED));

				boolean foundGlade = false;
				boolean foundCaldera = false;
				for (int bx = -6400; bx <= 6400 && !(foundGlade && foundCaldera); bx += 128) {
					for (int bz = -6400; bz <= 6400 && !(foundGlade && foundCaldera); bz += 128) {
						var holder = resolver.getNoiseBiome(
							QuartPos.fromBlock(bx), QuartPos.fromBlock(80), QuartPos.fromBlock(bz));
						if (holder.is(AstralLeyGladeClimate.ASTRAL_LEY_GLADE)) foundGlade = true;
						if (holder.is(CinderCalderaClimate.CINDER_CALDERA)) foundCaldera = true;
					}
				}
				check(foundGlade, "Astral Ley-Glade sampled in Overworld climate within +/-6400 blocks");
				check(foundCaldera, "Cinder Caldera sampled in Overworld climate within +/-6400 blocks");

				// Natural locator coverage for all 3 Co-Op Trial Sanctums
				String[] siteIds = {TrialSites.SANCTUM_ID, TrialSites.CRUCIBLE_ID, TrialSites.ECLIPSE_ID};
				boolean[] located = new boolean[siteIds.length];
				for (int cx = -96; cx <= 96; cx += 6) {
					for (int cz = -96; cz <= 96; cz += 6) {
						var ctx = GametestCompat.realStructureContext(l, new ChunkPos(cx, cz));
						for (int i = 0; i < siteIds.length; i++) {
							if (!located[i] && Sites.locator(siteIds[i]).locate(ctx).isPresent()) {
								located[i] = true;
							}
						}
						if (located[0] && located[1] && located[2]) break;
					}
					if (located[0] && located[1] && located[2]) break;
				}
				for (int i = 0; i < siteIds.length; i++) {
					check(located[i], "Trial locator succeeded for " + siteIds[i]);
				}

				// Wayfinder sneak-right-click cycling 0 -> 1 -> 2 -> 0
				ServerPlayer sp = s.getPlayerList().getPlayers().getFirst();
				ItemStack wayfinder = new ItemStack(BiomeTrialContent.SANCTUM_WAYFINDER);
				sp.setItemInHand(InteractionHand.MAIN_HAND, wayfinder);
				check(SanctumWayfinderItem.mode(wayfinder) == 0, "Initial Wayfinder mode is 0");
				boolean prevShift = sp.isShiftKeyDown();
				try {
					sp.setShiftKeyDown(true);
					wayfinder.use(l, sp, InteractionHand.MAIN_HAND);
					check(SanctumWayfinderItem.mode(wayfinder) == 1, "Cycled Wayfinder mode to 1");
					wayfinder.use(l, sp, InteractionHand.MAIN_HAND);
					check(SanctumWayfinderItem.mode(wayfinder) == 2, "Cycled Wayfinder mode to 2");
					wayfinder.use(l, sp, InteractionHand.MAIN_HAND);
					check(SanctumWayfinderItem.mode(wayfinder) == 0, "Wrapped Wayfinder mode back to 0");
				} finally {
					sp.setShiftKeyDown(prevShift);
				}

				// Custom biome mobs instantiate & spawn cleanly
				LeyweaverMoth moth = BiomeTrialContent.LEYWEAVER_MOTH.create(l, EntitySpawnReason.COMMAND);
				CalderaCinderhound hound = BiomeTrialContent.CALDERA_CINDERHOUND.create(l, EntitySpawnReason.COMMAND);
				check(moth != null && hound != null, "Biome custom mobs created cleanly");
				moth.snapTo(sp.getX() + 3.0, sp.getY() + 1.0, sp.getZ(), 0.0F, 0.0F);
				hound.snapTo(sp.getX() - 3.0, sp.getY() + 1.0, sp.getZ(), 0.0F, 0.0F);
				check(l.addFreshEntity(moth) && l.addFreshEntity(hound), "Biome mobs spawned into level");
			});
		}
	}
}
