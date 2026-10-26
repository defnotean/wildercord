package dev.wildercord.world.dungeons;

import dev.wildercord.aura.world.LanternBearer;
import dev.wildercord.aura.world.LanternBearerRules;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import java.util.Set;

/**
 * A barrow field generated the ordinary way, by a player walking up to it: its Lantern Bearer is bound while the barrow is
 * still being generated on a worldgen thread, and binding once reached into the level for the lantern and the candles, which
 * waited on the server for a chunk while the server waited on that very generation, and the world hung for good.
 */
public final class BarrowBearerGenerationTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext c) {
		try (var world = c.worldBuilder().setUseConsistentSettings(false)
				.adjustSettings(settings -> settings.setSeed(BattlefieldsTest.REPRESENTATIVE_SEED)).create()) {
			c.waitTicks(40);
			world.getServer().runCommand("gamerule spawn_mobs false");
			BlockPos found = world.getServer().computeOnServer(s -> {
				var level = s.overworld();
				var holder = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(ResourceKey.create(Registries.STRUCTURE,
					Identifier.fromNamespaceAndPath("wildercord", "barrow_field")));
				return level.findNearestMapStructure(HolderSet.direct(holder), BlockPos.ZERO, 100, false);
			});
			check(found != null, "A barrow field generates somewhere near spawn on the representative seed");
			world.getServer().runOnServer(s -> {
				var p = s.getPlayerList().getPlayers().getFirst();
				p.setGameMode(GameType.SPECTATOR);
				p.teleportTo(s.overworld(), found.getX() + .5, 140, found.getZ() + .5, Set.<Relative>of(), 0, 60, false);
			});
			// Generation now runs as it would for any player arriving; a hang shows up as the run timing out here.
			var box = new AABB(found.getX() - 64, -64, found.getZ() - 64, found.getX() + 64, 320, found.getZ() + 64);
			int bearers = 0;
			for (int t = 0; t < 1200 && bearers == 0; t += 20) {
				c.waitTicks(20);
				bearers = world.getServer().computeOnServer(s -> s.overworld().getEntitiesOfClass(LanternBearer.class, box).size());
			}
			check(bearers == 1, "The barrow's one Lantern Bearer is in the world once the field has generated, found " + bearers);
			int move = world.getServer().computeOnServer(s -> s.overworld().getEntitiesOfClass(LanternBearer.class, box).getFirst().move());
			check(move == LanternBearerRules.DORMANT, "The bearer lies dormant in its grave, move " + move);
			c.waitTicks(60);
			c.runOnClient(mc -> mc.gui.toastManager().clear());
			c.takeScreenshot(TestScreenshotOptions.of("barrow_bearer_generated").disableCounterPrefix());
		}
	}

	private static void check(boolean ok, String why) {if (!ok) throw new AssertionError(why);}
}
