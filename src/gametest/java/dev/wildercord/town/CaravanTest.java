package dev.wildercord.town;

import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.animal.equine.TraderLlama;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * A wandering caravan in the world: it makes camp 24 to 40 blocks from the traveller with two pack llamas on its leads,
 * stays about a day, sells a stranger two runes and the saffron rare meals need, keeps a traveller's stock for its stay,
 * never camps beside another caravan, and moves on when its stay is over.
 */
public final class CaravanTest implements FabricClientGameTest {
	private static final int Y = 200, SPAN = 48;

	private static void check(boolean x, String why) { if (!x) throw new AssertionError(why); }
	private static ServerPlayer p(MinecraftServer s) { return s.getPlayerList().getPlayers().getFirst(); }

	private static List<WayfarerKeeper> caravaneers(MinecraftServer s) {
		return s.overworld().getEntitiesOfClass(WayfarerKeeper.class, new AABB(-SPAN, Y - 4, -SPAN, SPAN, Y + 8, SPAN),
			k -> k.role() == WayfarerKeeper.Role.CARAVANEER);
	}

	private static long runes(MerchantOffers offers) {
		return offers.stream().filter(o -> o.getResult().getItem() instanceof dev.wildercord.content.RuneItem).count();
	}

	public void runTest(ClientGameTestContext c) {
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(25);
			w.getServer().runCommand("gamerule spawn_mobs false");
			w.getServer().runCommand("time set day");
			w.getServer().runCommand("forceload add -" + SPAN + " -" + SPAN + " " + SPAN + " " + SPAN);
			w.getServer().runOnServer(s -> {
				p(s).setGameMode(GameType.CREATIVE);
				p(s).teleportTo(s.overworld(), 0.5, Y + 1, 0.5, Set.<Relative>of(), 0, 0, false);
			});
			for (int i = 0; i < 60 && !w.getServer().computeOnServer(s -> s.overworld().isLoaded(new BlockPos(SPAN, Y, SPAN))
				&& s.overworld().isLoaded(new BlockPos(-SPAN, Y, -SPAN))); i++) {
				c.waitTicks(20);
			}
			// A stone sky floor, so the camp lands on dry, open ground wherever it's put.
			w.getServer().runOnServer(s -> {
				var l = s.overworld();
				for (int x = -SPAN; x <= SPAN; x++) for (int z = -SPAN; z <= SPAN; z++) {
					l.setBlock(new BlockPos(x, Y, z), Blocks.STONE.defaultBlockState(), 2);
					for (int y = Y + 1; y <= Y + 4; y++) l.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
				}
				p(s).setGameMode(GameType.SURVIVAL);
				Town.set(p(s), Town.Standing.NONE);
			});
			c.waitTicks(5);

			// The camp: a caravaneer 24 to 40 blocks off, staying about a day, with both pack llamas on its leads.
			w.getServer().runOnServer(s -> {
				WayfarerKeeper keeper = Caravans.send(s.overworld(), p(s));
				check(keeper != null, "the caravan found somewhere to camp");
				check(keeper.role() == WayfarerKeeper.Role.CARAVANEER && keeper.hasCustomName(), "it's a named caravaneer");
				double d = Math.hypot(keeper.getX() - p(s).getX(), keeper.getZ() - p(s).getZ());
				check(d >= CaravanRules.SPAWN_NEAR - 1 && d <= CaravanRules.SPAWN_FAR + 1, "it camped 24 to 40 blocks off (" + d + ")");
				check(Math.abs(keeper.getY() - (Y + 1)) < 0.01, "it stands on the ground (y=" + keeper.getY() + ")");
				check(keeper.getDespawnDelay() == CaravanRules.STAY_TICKS, "it stays about a day (" + keeper.getDespawnDelay() + ")");
				List<TraderLlama> llamas = s.overworld().getEntitiesOfClass(TraderLlama.class, keeper.getBoundingBox().inflate(6));
				check(llamas.size() == CaravanRules.LLAMAS, "it brought its pack llamas (" + llamas.size() + ")");
				check(llamas.stream().allMatch(l -> l.getLeashHolder() == keeper), "both are on the caravaneer's leads");
				check(Caravans.send(s.overworld(), p(s)) == null && caravaneers(s).size() == 1, "no second caravan camps beside the first");
				// Stand a few blocks off, facing the camp, for the screenshot.
				double dx = keeper.getX() - p(s).getX(), dz = keeper.getZ() - p(s).getZ(), back = 6 / d;
				float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
				p(s).teleportTo(s.overworld(), keeper.getX() - dx * back, Y + 1, keeper.getZ() - dz * back, Set.<Relative>of(), yaw, 10, false);
				keeper.setNoAi(true);
			});
			c.waitTicks(30);
			c.takeScreenshot(TestScreenshotOptions.of("caravan_camp").withSize(854, 480));

			// Its wares for a stranger: two runes and the saffron; the same stock again for the rest of its stay.
			w.getServer().runOnServer(s -> {
				WayfarerKeeper keeper = caravaneers(s).getFirst();
				keeper.mobInteract(p(s), InteractionHand.MAIN_HAND);
				MerchantOffers offers = keeper.getOffers();
				check(runes(offers) == CaravanRules.runes(BountyRules.Tier.STRANGER), "a stranger is offered " + CaravanRules.runes(BountyRules.Tier.STRANGER) + " runes (" + runes(offers) + ")");
				check(offers.stream().anyMatch(o -> o.getResult().is(dev.wildercord.cooking.Meals.WAYFARER_SAFFRON)
					&& o.getResult().getCount() == CaravanRules.SAFFRON), "it sells the saffron");
				check(keeper.isTrading(), "the trade opened");
			});
			c.waitTicks(20);
			c.takeScreenshot(TestScreenshotOptions.of("caravan_trades").withSize(854, 480));
			w.getServer().runOnServer(s -> {
				WayfarerKeeper keeper = caravaneers(s).getFirst();
				MerchantOffers first = keeper.getOffers();
				p(s).closeContainer();
				keeper.setTradingPlayer(null);
				keeper.mobInteract(p(s), InteractionHand.MAIN_HAND);
				check(keeper.getOffers() == first, "the traveller's stock is kept for the caravan's stay");
				p(s).closeContainer();
				keeper.setTradingPlayer(null);
			});
			c.waitTicks(5);

			// Its stay over, the caravan moves on.
			w.getServer().runOnServer(s -> {
				WayfarerKeeper keeper = caravaneers(s).getFirst();
				keeper.setNoAi(false);
				keeper.setDespawnDelay(3);
			});
			c.waitTicks(20);
			w.getServer().runOnServer(s -> check(caravaneers(s).isEmpty(), "the caravan moved on when its stay ran out"));
			w.getServer().runCommand("forceload remove all");
		}
	}
}
